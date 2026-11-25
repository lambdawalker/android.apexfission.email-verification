package com.apexfission.android.emailverification.demo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apexfission.android.emailverification.EmailVerificationWidget

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DemoTheme {
                val model: DemoModel = viewModel()
                BackHandler(enabled = model.destination != DemoDestination.Catalog) {
                    model.returnToCatalog()
                }
                when (model.destination) {
                    DemoDestination.Catalog -> DemoCatalogScreen(
                        onOpenOffline = model::openOffline,
                        onOpenLive = model::openConnection,
                        onOpenSettings = {
                            startActivity(Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", packageName, null),
                            ))
                        },
                    )
                    DemoDestination.Connection -> LiveConnectionScreen(
                        onConnect = model::connect,
                        onCancel = model::returnToCatalog,
                        directory = model.directory,
                        onRefreshDirectory = model::refreshDirectory,
                    )
                    DemoDestination.Offline, DemoDestination.Live -> DemoFlowScreen(
                        live = model.origin,
                        onBack = model::returnToCatalog,
                    ) {
                        EmailVerificationWidget(model.controller)
                    }
                }
            }
        }
    }
}
