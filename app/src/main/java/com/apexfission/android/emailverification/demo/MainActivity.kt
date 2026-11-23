package com.apexfission.android.emailverification.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.apexfission.android.emailverification.EmailVerificationController
import com.apexfission.android.emailverification.EmailVerificationWidget
import com.apexfission.android.emailverification.TestHostGateway
import com.apexfission.android.emailverification.VerificationAttempt
import com.apexfission.android.emailverification.VerificationFailure
import com.apexfission.android.emailverification.VerificationGateway
import com.apexfission.android.emailverification.VerificationRequestException
import com.apexfission.android.emailverification.VerificationResult

internal const val DEFAULT_ORIGIN = "https://dev.components.email-verification.apexfission.com"
internal const val DEMO_CODE = "271828"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DemoTheme {
                val model: DemoModel = viewModel()
                val controller = model.controller
                DemoFrame {
                    if (model.configuring) {
                        LiveConnectionForm(
                            onConnect = model::connect,
                            onCancel = { model.configuring = false },
                        )
                    } else {
                        DemoMode(
                            live = model.origin,
                            onSwitch = {
                                if (model.origin == null) model.configuring = true else model.useOffline()
                            },
                        )
                        EmailVerificationWidget(controller)
                    }
                }
            }
        }
    }
}

internal class DemoModel : ViewModel() {
    var controller by mutableStateOf(offline())
        private set
    var configuring by mutableStateOf(false)
    var origin by mutableStateOf<String?>(null)
        private set

    fun connect(origin: String, token: String): Boolean {
        if (token.isBlank() || token.any { it.isWhitespace() || it.isISOControl() }) return false
        val gateway = try {
            TestHostGateway(origin) { token }
        } catch (_: IllegalArgumentException) {
            return false
        }
        controller.reset()
        controller = EmailVerificationController(gateway, viewModelScope)
        this.origin = origin
        configuring = false
        return true
    }

    fun useOffline() {
        controller.reset()
        controller = offline()
        origin = null
    }

    private fun offline() = EmailVerificationController(OfflineGateway(), viewModelScope, "tester@example.invalid")
}

@Composable
internal fun DemoTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme(), content = content)
}

@Composable
internal fun DemoFrame(content: @Composable () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.windowInsetsPadding(WindowInsets.safeDrawing).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) { content() }
    }
}

@Composable
internal fun DemoMode(live: String?, onSwitch: () -> Unit) {
    Text(
        stringResource(if (live == null) R.string.offline_mode else R.string.live_mode),
        style = MaterialTheme.typography.titleMedium,
    )
    Text(
        live ?: stringResource(R.string.offline_help, DEMO_CODE),
        style = MaterialTheme.typography.bodyMedium,
    )
    TextButton(onClick = onSwitch, shape = MaterialTheme.shapes.small) {
        Text(stringResource(if (live == null) R.string.configure_live else R.string.use_offline))
    }
}

@Composable
internal fun LiveConnectionForm(onConnect: (String, String) -> Boolean, onCancel: () -> Unit) {
    var endpoint by remember { mutableStateOf(DEFAULT_ORIGIN) }
    var token by remember { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }
    Text(stringResource(R.string.live_title), style = MaterialTheme.typography.headlineSmall)
    Text(stringResource(R.string.live_help))
    OutlinedTextField(
        value = endpoint,
        onValueChange = {
            endpoint = it
            invalid = false
        },
        label = { Text(stringResource(R.string.live_origin)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = token,
        onValueChange = {
            token = it
            invalid = false
        },
        label = { Text(stringResource(R.string.live_token)) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )
    if (invalid) Text(stringResource(R.string.live_invalid), color = MaterialTheme.colorScheme.error)
    Button(
        onClick = { invalid = !onConnect(endpoint.trim(), token.trim()) },
        enabled = endpoint.isNotBlank() && token.isNotBlank(),
        shape = MaterialTheme.shapes.small,
    ) {
        Text(stringResource(R.string.live_continue))
    }
    TextButton(onClick = onCancel, shape = MaterialTheme.shapes.small) {
        Text(stringResource(R.string.live_cancel))
    }
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
