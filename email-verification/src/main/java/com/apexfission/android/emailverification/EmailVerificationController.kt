package com.apexfission.android.emailverification

import androidx.annotation.MainThread
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class VerificationStep { Email, Code, WaitingForHost, Verified, Stopped }

enum class VerificationMessage {
    EmailRequired, InvalidEmail, InvalidCode, Unauthorized, Expired, AttemptsExhausted,
    DeliveryFailed, Unresolved, Unavailable,
}

data class EmailVerificationState(
    val step: VerificationStep = VerificationStep.Email,
    val email: String = "",
    val code: String = "",
    val busy: Boolean = false,
    val message: VerificationMessage? = null,
)

/** One flow, driven from the main thread. Cancel the owning scope when leaving the feature. */
@MainThread
class EmailVerificationController(
    private val gateway: VerificationGateway,
    private val scope: CoroutineScope,
    initialEmail: String = "",
) {
    private val mutableState = MutableStateFlow(EmailVerificationState(email = initialEmail))
    val state = mutableState.asStateFlow()

    private var attempt: VerificationAttempt? = null
    private var work: Job? = null

    fun editEmail(email: String) {
        val current = state.value
        if (current.step != VerificationStep.Email || current.busy || email == current.email) return
        attempt = null
        mutableState.value = current.copy(email = email, message = null)
    }

    fun editCode(input: String) {
        val current = state.value
        if (current.step != VerificationStep.Code || current.busy) return
        val code = input.filter { it in '0'..'9' }.take(6)
        if (code != current.code) mutableState.value = current.copy(code = code, message = null)
    }

    fun reset() {
        work?.cancel()
        attempt = null
        mutableState.value = EmailVerificationState(email = state.value.email)
    }

    fun submitEmail() {
        val current = state.value
        if (current.step != VerificationStep.Email || current.busy) return
        val email = current.email.trim()
        if (email.isEmpty()) {
            mutableState.value = current.copy(message = VerificationMessage.EmailRequired)
            return
        }
        val active = attempt ?: VerificationAttempt.create().also { attempt = it }
        mutableState.value = current.copy(email = email)
        request(VerificationMessage.InvalidEmail) { gateway.start(email, active) }
    }

    fun submitCode() {
        val current = state.value
        val active = attempt ?: return
        if (current.step != VerificationStep.Code || current.busy || current.code.length != 6) return
        request(VerificationMessage.InvalidCode) { gateway.confirm(active, current.code) }
    }

    fun checkStatus() {
        val current = state.value
        val active = attempt ?: return
        if (current.busy || current.step !in setOf(VerificationStep.Code, VerificationStep.WaitingForHost)) return
        request(VerificationMessage.Unavailable) { gateway.status(active) }
    }

    private fun request(invalidMessage: VerificationMessage, block: suspend () -> VerificationResult) {
        val previous = state.value
        mutableState.value = previous.copy(busy = true, message = null)
        work = scope.launch {
            try {
                val result = block()
                ensureActive()
                applyResult(result, previous)
                // Test hosts finish the account action asynchronously. Poll briefly, then leave a manual check.
                repeat(9) {
                    if (state.value.step != VerificationStep.WaitingForHost) return@launch
                    delay(10_000)
                    val active = attempt ?: return@launch
                    val status = gateway.status(active)
                    ensureActive()
                    applyResult(status, state.value)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: VerificationRequestException) {
                ensureActive()
                val current = state.value
                mutableState.value = when (failure.failure) {
                    VerificationFailure.Invalid -> current.copy(busy = false, message = invalidMessage)
                    VerificationFailure.Unauthorized -> current.copy(busy = false, message = VerificationMessage.Unauthorized)
                    VerificationFailure.Expired -> stopped(VerificationMessage.Expired)
                    VerificationFailure.AttemptsExhausted -> stopped(VerificationMessage.AttemptsExhausted)
                    VerificationFailure.Unresolved -> current.copy(busy = false, message = VerificationMessage.Unresolved)
                    VerificationFailure.Unavailable -> current.copy(busy = false, message = VerificationMessage.Unavailable)
                }
            } finally {
                // A reset has already replaced the state and may have started a new attempt.
                if (state.value.busy && currentCoroutineContext().isActive) {
                    mutableState.value = state.value.copy(busy = false)
                }
            }
        }
    }

    private fun applyResult(result: VerificationResult, previous: EmailVerificationState) {
        mutableState.value = when {
            result.accepted -> {
                attempt = null
                previous.copy(step = VerificationStep.Verified, code = "", busy = false, message = null)
            }
            else -> when (result.state) {
                "pending" -> previous.copy(step = VerificationStep.Code, busy = false, message = null)
                "confirmed", "ready", "acknowledged" ->
                    previous.copy(step = VerificationStep.WaitingForHost, code = "", busy = true, message = null)
                "expired" -> stopped(VerificationMessage.Expired)
                "attempts_exhausted" -> stopped(VerificationMessage.AttemptsExhausted)
                "mail_attempts_exhausted" -> stopped(VerificationMessage.DeliveryFailed)
                "sign_attempts_exhausted", "unresolved_delivery" -> stopped(VerificationMessage.Unresolved)
                else -> previous.copy(busy = false, message = VerificationMessage.Unavailable)
            }
        }
    }

    private fun stopped(message: VerificationMessage): EmailVerificationState {
        attempt = null
        return state.value.copy(step = VerificationStep.Stopped, code = "", busy = false, message = message)
    }
}
