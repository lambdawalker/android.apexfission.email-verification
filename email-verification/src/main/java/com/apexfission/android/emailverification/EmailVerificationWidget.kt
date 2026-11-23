// Email entry and code-confirmation flow adapted from android.apexfission.auth (Apache-2.0).
package com.apexfission.android.emailverification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexfission.android.ui.common.CodeVerificationCard

/** Email-verification flow rendered in the calling application's Material 3 theme. */
@Composable
fun EmailVerificationWidget(controller: EmailVerificationController, modifier: Modifier = Modifier) {
    val state by controller.state.collectAsStateWithLifecycle()
    EmailVerificationWidget(
        state = state,
        onEmailChange = controller::editEmail,
        onCodeChange = controller::editCode,
        onSend = controller::submitEmail,
        onVerify = controller::submitCode,
        onCheckStatus = controller::checkStatus,
        onStartOver = controller::reset,
        modifier = modifier,
    )
}

/** Controlled widget for hosts that own their flow state and lifecycle. */
@Composable
fun EmailVerificationWidget(
    state: EmailVerificationState,
    onEmailChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onSend: () -> Unit,
    onVerify: () -> Unit,
    onCheckStatus: () -> Unit,
    onStartOver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val message = state.message?.let { stringResource(it.textId) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (state.step) {
            VerificationStep.Email -> {
                Text(stringResource(R.string.ev_enter_email), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.ev_enter_email_help), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = state.email,
                    onValueChange = onEmailChange,
                    label = { Text(stringResource(R.string.ev_email_address)) },
                    singleLine = true,
                    enabled = !state.busy,
                    isError = state.message == VerificationMessage.EmailRequired ||
                        state.message == VerificationMessage.InvalidEmail,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.EmailAddress },
                )
                VerificationNotice(message, if (state.busy) stringResource(R.string.ev_sending) else null)
                Button(onClick = onSend, enabled = !state.busy, shape = MaterialTheme.shapes.small) {
                    Text(stringResource(R.string.ev_send_code))
                }
            }
            VerificationStep.Code -> {
                Text(stringResource(R.string.ev_check_email), style = MaterialTheme.typography.headlineSmall)
                CodeVerificationCard(
                    value = state.code,
                    onValueChange = onCodeChange,
                    codeLabel = stringResource(R.string.ev_confirmation_code),
                    codeContentDescription = stringResource(R.string.ev_six_digit_code),
                    pasteLabel = stringResource(R.string.ev_paste),
                    instructions = stringResource(R.string.ev_check_email_help),
                    recipient = state.email,
                    enabled = !state.busy,
                    autofillContentType = ContentType("emailOTPCode"),
                    busyMessage = if (state.busy) stringResource(R.string.ev_checking) else null,
                    errorMessage = message,
                )
                Button(
                    onClick = onVerify,
                    enabled = !state.busy && state.code.length == 6,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(stringResource(R.string.ev_verify))
                }
                TextButton(onClick = onCheckStatus, enabled = !state.busy, shape = MaterialTheme.shapes.small) {
                    Text(stringResource(R.string.ev_check_status))
                }
                TextButton(onClick = onStartOver, enabled = !state.busy, shape = MaterialTheme.shapes.small) {
                    Text(stringResource(R.string.ev_change_email))
                }
            }
            VerificationStep.WaitingForHost -> {
                Text(stringResource(R.string.ev_waiting), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.ev_waiting_help), style = MaterialTheme.typography.bodyMedium)
                VerificationNotice(message, if (state.busy) stringResource(R.string.ev_checking_status) else null)
                Button(onClick = onCheckStatus, enabled = !state.busy, shape = MaterialTheme.shapes.small) {
                    Text(stringResource(R.string.ev_check_status))
                }
                TextButton(onClick = onStartOver, shape = MaterialTheme.shapes.small) {
                    Text(stringResource(R.string.ev_start_over))
                }
            }
            VerificationStep.Verified -> {
                Text(
                    stringResource(R.string.ev_verified),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                Text(state.email, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onStartOver, shape = MaterialTheme.shapes.small) {
                    Text(stringResource(R.string.ev_start_over))
                }
            }
            VerificationStep.Stopped -> {
                Text(stringResource(R.string.ev_stopped), style = MaterialTheme.typography.headlineSmall)
                VerificationNotice(message, null)
                Button(onClick = onStartOver, shape = MaterialTheme.shapes.small) {
                    Text(stringResource(R.string.ev_start_over))
                }
            }
        }
    }
}

@Composable
private fun VerificationNotice(error: String?, progress: String?) {
    if (progress != null) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Text(progress, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    }
    if (error != null) {
        Text(
            error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

private val VerificationMessage.textId: Int
    get() = when (this) {
        VerificationMessage.EmailRequired -> R.string.ev_email_required
        VerificationMessage.InvalidEmail -> R.string.ev_invalid_email
        VerificationMessage.InvalidCode -> R.string.ev_invalid_code
        VerificationMessage.Unauthorized -> R.string.ev_unauthorized
        VerificationMessage.Expired -> R.string.ev_expired
        VerificationMessage.AttemptsExhausted -> R.string.ev_attempts_exhausted
        VerificationMessage.DeliveryFailed -> R.string.ev_delivery_failed
        VerificationMessage.Unresolved -> R.string.ev_unresolved
        VerificationMessage.Unavailable -> R.string.ev_unavailable
    }
