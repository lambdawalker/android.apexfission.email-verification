package com.apexfission.android.emailverification.demo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.apexfission.android.ui.common.CatalogScreen
import com.apexfission.android.ui.common.ContentCard

@Composable
internal fun LiveConnectionScreen(
    onConnect: (String, String) -> Boolean,
    onCancel: () -> Unit,
    directory: DemoDirectory,
    onRefreshDirectory: () -> Unit,
) {
    CatalogScreen(
        title = stringResource(R.string.live_title),
        description = stringResource(R.string.live_help),
        headerActions = { BackToDemoMenu(onCancel) },
    ) {
        item(key = "connection") {
            ContentCard {
                LiveConnectionForm(onConnect, directory, onRefreshDirectory)
            }
        }
    }
}

@Composable
internal fun LiveConnectionForm(
    onConnect: (String, String) -> Boolean,
    directory: DemoDirectory = DemoDirectory(),
    onRefreshDirectory: () -> Unit = {},
) {
    var endpoint by remember { mutableStateOf(DEFAULT_ORIGIN) }
    var token by remember { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<DemoEnvironment?>(null) }
    var expanded by remember { mutableStateOf(false) }
    when {
        directory.loading -> Text(stringResource(R.string.directory_loading))
        directory.unavailable -> Text(stringResource(R.string.directory_unavailable))
        directory.entries.isEmpty() -> Text(stringResource(R.string.directory_empty))
        else -> Column {
            OutlinedButton(onClick = { expanded = true }, shape = MaterialTheme.shapes.small) {
                Text(selected?.id ?: stringResource(R.string.directory_choose))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                directory.entries.forEach { entry ->
                    DropdownMenuItem(
                        text = { Text(entry.id) },
                        onClick = {
                            selected = entry
                            endpoint = entry.origin
                            token = ""
                            invalid = false
                            expanded = false
                        },
                    )
                }
            }
        }
    }
    selected?.let { entry ->
        Text(stringResource(R.string.directory_cognito, entry.poolId, entry.clientId))
    }
    if (!directory.loading) {
        TextButton(onClick = onRefreshDirectory, shape = MaterialTheme.shapes.small) {
            Text(stringResource(R.string.directory_refresh))
        }
    }
    OutlinedTextField(
        value = endpoint,
        onValueChange = {
            endpoint = it
            selected = null
            token = ""
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
}

