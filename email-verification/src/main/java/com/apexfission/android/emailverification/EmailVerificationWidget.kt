// Email entry and code-confirmation flow adapted from android.apexfission.auth (Apache-2.0).
package com.apexfission.android.emailverification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexfission.android.ui.common.CodeVerificationCard
import com.apexfission.android.ui.common.VerificationCardDetails

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
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(if (state.step == VerificationStep.Code) 8.dp else 12.dp)) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface) {
                        Icon(painterResource(R.drawable.ev_mail_check), contentDescription = null,
                            modifier = Modifier.padding(10.dp).size(24.dp))
                    }
                    Text(stringResource(R.string.ev_check_email), style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                CodeVerificationCard(
                    details = VerificationCardDetails(
                        codeLengthLabel = stringResource(R.string.ev_code_length),
                        explanation = stringResource(R.string.ev_code_explanation),
                        recipientStatus = stringResource(R.string.ev_awaiting_confirmation),
                    ),
                    value = state.code,
                    onValueChange = onCodeChange,
                    codeLabel = stringResource(R.string.ev_confirmation_code),
                    codeContentDescription = stringResource(R.string.ev_six_digit_code),
                    pasteLabel = null,
                    instructions = stringResource(R.string.ev_check_email_help),
                    recipient = state.email,
                    enabled = !state.busy,
                    autofillContentType = ContentType("emailOTPCode"),
                    busyMessage = if (state.busy) stringResource(R.string.ev_checking) else null,
                    errorMessage = message,
                    recipientLeadingIcon = {
                        Surface(shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                            Icon(painterResource(R.drawable.ev_mail), contentDescription = null,
                                modifier = Modifier.padding(8.dp).size(20.dp))
                        }
                    },
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onVerify,
                    enabled = !state.busy && state.code.length == 6,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Icon(painterResource(R.drawable.ev_shield_check), contentDescription = null)
                    Text(stringResource(R.string.ev_verify),
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.titleSmall)
                    Icon(painterResource(R.drawable.ev_arrow_forward), contentDescription = null)
                }
                TextButton(onClick = onCheckStatus, enabled = !state.busy, shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.ev_check_status))
                }
                TextButton(onClick = onStartOver, enabled = !state.busy, shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.ev_change_email))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ev_lock), contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Text(stringResource(R.string.ev_code_privacy), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
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
