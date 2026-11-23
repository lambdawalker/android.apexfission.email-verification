package com.apexfission.android.emailverification

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EmailVerificationControllerTest {
    @Test
    fun startRetryPreservesAttemptAndWaitsForHostAcceptance() = runTest {
        val gateway = FakeGateway()
        val controller = EmailVerificationController(gateway, backgroundScope)
        controller.editEmail("tester@example.invalid")
        controller.submitEmail()
        runCurrent()
        assertEquals(VerificationMessage.Unavailable, controller.state.value.message)

        controller.submitEmail()
        runCurrent()
        assertSame(gateway.attempts[0], gateway.attempts[1])
        assertEquals(VerificationStep.Code, controller.state.value.step)

        controller.editCode("012 345")
        controller.submitCode()
        runCurrent()
        assertEquals("012345", gateway.confirmedCode)
        assertEquals(VerificationStep.WaitingForHost, controller.state.value.step)
        assertEquals("", controller.state.value.code)
        advanceTimeBy(10_001)
        assertEquals(VerificationStep.Verified, controller.state.value.step)
        assertFalse(controller.state.value.busy)
    }

    @Test
    fun resetCancelsPendingPollingAndKeepsEnteredAddress() = runTest {
        val gateway = FakeGateway().also { it.failFirst = false }
        val controller = EmailVerificationController(gateway, backgroundScope, "tester@example.invalid")
        controller.submitEmail()
        runCurrent()
        controller.editCode("012345")
        controller.submitCode()
        runCurrent()
        controller.reset()
        advanceTimeBy(20_000)
        assertEquals(0, gateway.statusCalls)
        assertEquals(EmailVerificationState(email = "tester@example.invalid"), controller.state.value)
    }
}

private class FakeGateway : VerificationGateway {
    val attempts = mutableListOf<VerificationAttempt>()
    var failFirst = true
    var confirmedCode: String? = null
    var statusCalls = 0

    override suspend fun start(email: String, attempt: VerificationAttempt): VerificationResult {
        attempts += attempt
        if (failFirst && attempts.size == 1) throw VerificationRequestException(VerificationFailure.Unavailable)
        return VerificationResult("pending")
    }

    override suspend fun confirm(attempt: VerificationAttempt, code: String): VerificationResult {
        confirmedCode = code
        return VerificationResult("confirmed")
    }

    override suspend fun status(attempt: VerificationAttempt): VerificationResult {
        statusCalls++
        return VerificationResult("acknowledged", accepted = true)
    }
}
