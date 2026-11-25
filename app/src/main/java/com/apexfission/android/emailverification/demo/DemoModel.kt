package com.apexfission.android.emailverification.demo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apexfission.android.emailverification.EmailVerificationController
import com.apexfission.android.emailverification.TestHostGateway
import com.apexfission.android.emailverification.VerificationAttempt
import com.apexfission.android.emailverification.VerificationFailure
import com.apexfission.android.emailverification.VerificationGateway
import com.apexfission.android.emailverification.VerificationRequestException
import com.apexfission.android.emailverification.VerificationResult
import java.io.IOException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONException

internal const val DEFAULT_ORIGIN = "https://dev.components.email-verification.apexfission.com"
internal const val DEMO_CODE = "271828"
internal enum class DemoDestination { Catalog, Offline, Connection, Live }

/** Owns in-memory demo sessions. Recreation retains them; process death returns to the catalog. */
internal class DemoModel(
    private val loadDirectory: suspend () -> List<DemoEnvironment> = { loadDemoDirectory() },
) : ViewModel() {
    var destination by mutableStateOf(DemoDestination.Catalog)
        private set
    var controller by mutableStateOf(offline())
        private set
    var directory by mutableStateOf(DemoDirectory())
        private set
    private var directoryRequest: Job? = null
    var origin by mutableStateOf<String?>(null)
        private set

    fun openOffline() {
        clearSession()
        destination = DemoDestination.Offline
    }

    fun openConnection() {
        clearSession()
        destination = DemoDestination.Connection
        refreshDirectory()
    }

    fun returnToCatalog() {
        clearSession()
        destination = DemoDestination.Catalog
    }

    fun refreshDirectory() {
        if (destination != DemoDestination.Connection) return
        directoryRequest?.cancel()
        directory = DemoDirectory(loading = true)
        directoryRequest = viewModelScope.launch {
            directory = try {
                DemoDirectory(entries = loadDirectory())
            } catch (_: IOException) {
                DemoDirectory(unavailable = true)
            } catch (_: JSONException) {
                DemoDirectory(unavailable = true)
            }
        }
    }

    fun connect(origin: String, token: String): Boolean {
        if (destination != DemoDestination.Connection ||
            token.isBlank() || token.any { it.isWhitespace() || it.isISOControl() }
        ) return false
        val gateway = try {
            TestHostGateway(origin) { token }
        } catch (_: IllegalArgumentException) {
            return false
        }
        directoryRequest?.cancel()
        controller.reset()
        controller = EmailVerificationController(gateway, viewModelScope)
        this.origin = origin
        destination = DemoDestination.Live
        return true
    }

    private fun clearSession() {
        directoryRequest?.cancel()
        directoryRequest = null
        controller.reset()
        controller = offline()
        origin = null
        directory = DemoDirectory()
    }

    override fun onCleared() {
        controller.reset()
        super.onCleared()
    }

    private fun offline() = EmailVerificationController(OfflineGateway(), viewModelScope, "tester@example.invalid")
}

private class OfflineGateway : VerificationGateway {
    private var accepted = false

    override suspend fun start(email: String, attempt: VerificationAttempt): VerificationResult {
        if (!email.matches(Regex("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"))) {
            throw VerificationRequestException(VerificationFailure.Invalid)
        }
        accepted = false
        return VerificationResult("pending")
    }

    override suspend fun confirm(attempt: VerificationAttempt, code: String): VerificationResult {
        if (code != DEMO_CODE) throw VerificationRequestException(VerificationFailure.Invalid)
        accepted = true
        return VerificationResult("acknowledged", accepted = true)
    }

    override suspend fun status(attempt: VerificationAttempt) =
        VerificationResult(if (accepted) "acknowledged" else "pending", accepted)
}
