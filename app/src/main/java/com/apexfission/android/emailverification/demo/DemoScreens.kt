package com.apexfission.android.emailverification.demo

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.apexfission.android.ui.common.CatalogScreen
import com.apexfission.android.ui.common.ContentCard
import com.apexfission.android.ui.common.SectionHeading

@Composable
internal fun DemoTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme(), content = content)
}

/** Pure visual launcher. Opening it never starts discovery, verification or a permission request. */
@Composable
internal fun DemoCatalogScreen(
    onOpenOffline: () -> Unit,
    onOpenLive: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    CatalogScreen(
        title = stringResource(R.string.catalog_title),
        description = stringResource(R.string.catalog_description),
        headerActions = {
            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
            ) { Text(stringResource(R.string.app_settings)) }
        },
    ) {
        item(key = "offline") {
            ContentCard {
                SectionHeading(stringResource(R.string.offline_mode))
                Text(stringResource(R.string.offline_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onOpenOffline, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small) {
                    Text(stringResource(R.string.open_offline))
                }
            }
        }
        item(key = "live") {
            ContentCard {
                SectionHeading(stringResource(R.string.live_mode))
                Text(stringResource(R.string.live_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = onOpenLive, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small) {
                    Text(stringResource(R.string.configure_live))
                }
            }
        }
    }
}

@Composable
internal fun DemoFlowScreen(live: String?, onBack: () -> Unit, content: @Composable () -> Unit) {
    CatalogScreen(
        title = stringResource(if (live == null) R.string.offline_mode else R.string.live_mode),
        description = if (live == null) stringResource(R.string.offline_help, DEMO_CODE)
            else stringResource(R.string.live_session_help),
        headerActions = { BackToDemoMenu(onBack) },
    ) {
        item(key = "verification") {
            ContentCard {
                if (live != null) Text(live, style = MaterialTheme.typography.bodyMedium)
                content()
            }
        }
    }
}

@Composable
internal fun BackToDemoMenu(onBack: () -> Unit) {
    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small) {
        Text(stringResource(R.string.back_to_menu))
    }
}
