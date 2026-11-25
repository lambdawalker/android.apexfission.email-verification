package com.apexfission.android.emailverification.demo

import com.apexfission.android.emailverification.VerificationMessage
import com.apexfission.android.emailverification.VerificationStep
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DemoModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun catalogAndOfflineFlowNeverRequestTheDirectory() = runTest(dispatcher) {
        var requests = 0
        val model = DemoModel { requests++; emptyList() }
        assertEquals(DemoDestination.Catalog, model.destination)
        model.refreshDirectory()
        model.openOffline()
        val controller = model.controller
        controller.submitEmail()
        runCurrent()
        assertEquals(VerificationStep.Code, controller.state.value.step)
        controller.editCode("000000")
        controller.submitCode()
        runCurrent()
        assertEquals(VerificationMessage.InvalidCode, controller.state.value.message)
        controller.editCode(DEMO_CODE)
        controller.submitCode()
        runCurrent()
        assertEquals(VerificationStep.Verified, controller.state.value.step)
        controller.reset()
        assertEquals(VerificationStep.Email, controller.state.value.step)
        model.returnToCatalog()
        model.openOffline()
        assertNotSame(controller, model.controller)
        assertEquals(VerificationStep.Email, model.controller.state.value.step)
        assertEquals(0, requests)
    }

    @Test fun leavingLiveDiscardsTheGatewayAndRecipient() = runTest(dispatcher) {
        val model = DemoModel { emptyList() }
        model.openConnection()
        runCurrent()
        assertTrue(model.connect("https://host.example.test", "synthetic-session"))
        val liveController = model.controller
        liveController.editEmail("live@example.invalid")
        assertEquals(DemoDestination.Live, model.destination)
        model.returnToCatalog()
        assertEquals(DemoDestination.Catalog, model.destination)
        assertNull(model.origin)
        assertNotSame(liveController, model.controller)
        assertEquals("tester@example.invalid", model.controller.state.value.email)
        assertFalse(model.connect("https://host.example.test", "synthetic-session"))
    }

    @Test fun unavailableDirectoryAllowsManualConfigurationAndOfflineRecovery() = runTest(dispatcher) {
        val model = DemoModel { throw IOException("fixture failure") }
        model.openConnection()
        runCurrent()
        assertTrue(model.directory.unavailable)
        assertFalse(model.connect("http://host.example.test", "synthetic-session"))
        assertEquals(DemoDestination.Connection, model.destination)
        assertTrue(model.connect("https://host.example.test", "synthetic-session"))
        model.openOffline()
        assertEquals(DemoDestination.Offline, model.destination)
        assertNull(model.origin)
    }

    @Test fun backingOutCancelsPendingDiscovery() = runTest(dispatcher) {
        var cancelled = false
        val model = DemoModel {
            try { awaitCancellation() } finally { cancelled = true }
        }
        model.openConnection()
        runCurrent()
        assertTrue(model.directory.loading)
        model.returnToCatalog()
        runCurrent()
        assertTrue(cancelled)
        assertEquals(DemoDestination.Catalog, model.destination)
        assertEquals(DemoDirectory(), model.directory)
    }
}
