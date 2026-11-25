package com.apexfission.android.emailverification.demo

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Shapes
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apexfission.android.ui.common.CatalogScreen
import com.apexfission.android.ui.common.ContentCard
import com.apexfission.android.ui.common.SectionHeading

@Composable
internal fun DemoTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val typography = Typography()
    MaterialTheme(
        colorScheme = if (dark) darkColorScheme(
            primary = Color(0xFF2C70E5), onPrimary = Color.White,
            primaryContainer = Color(0xFF263B59), onPrimaryContainer = Color(0xFFAFC6FF),
            background = Color(0xFF0F131B), onBackground = Color(0xFFE1E4EC),
            surface = Color(0xFF0F131B), onSurface = Color(0xFFE1E4EC),
            onSurfaceVariant = Color(0xFFC0C7D5), surfaceContainerLow = Color(0xFF191E26),
            surfaceContainer = Color(0xFF1D222B), surfaceContainerHigh = Color(0xFF272C36),
        ) else lightColorScheme(primary = Color(0xFF245FCB),
            primaryContainer = Color(0xFFDCE7FF), onPrimaryContainer = Color(0xFF123264)),
        shapes = Shapes(small = RoundedCornerShape(8.dp), medium = RoundedCornerShape(12.dp),
            large = RoundedCornerShape(16.dp)),
        typography = typography.copy(headlineMedium = typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold)),
        content = content,
    )
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
@OptIn(ExperimentalMaterial3Api::class)
internal fun DemoFlowScreen(live: String?, onBack: () -> Unit, content: @Composable () -> Unit) {
    Scaffold(topBar = {
        Column {
            TopAppBar(
                title = { Text(stringResource(if (live == null) R.string.offline_mode else R.string.live_mode)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.demo_back), contentDescription = stringResource(R.string.back_to_menu))
                    }
                },
            )
            HorizontalDivider()
        }
    }) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).imePadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(if (live == null) stringResource(R.string.offline_help, DEMO_CODE)
                else stringResource(R.string.live_session_help),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (live != null) Text(live, style = MaterialTheme.typography.bodySmall)
            content()
        }
    }
}

@Composable
internal fun BackToDemoMenu(onBack: () -> Unit) {
    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small) {
        Text(stringResource(R.string.back_to_menu))
    }
}
