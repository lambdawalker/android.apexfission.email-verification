package com.apexfission.android.emailverification

import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64

/** A host can implement this interface for its own verification API. */
interface VerificationGateway {
    suspend fun start(email: String, attempt: VerificationAttempt): VerificationResult
    suspend fun confirm(attempt: VerificationAttempt, code: String): VerificationResult
    suspend fun status(attempt: VerificationAttempt): VerificationResult
}

/** Generate once per verification. Retain in memory for retries, and never log the proof. */
class VerificationAttempt private constructor(
    val operation: String,
    val secret: String,
    val authorizedAt: Instant,
) {
    val challenge: String
        get() = encode(MessageDigest.getInstance("SHA-256").digest(secret.toByteArray(UTF_8)))

    companion object {
        private val random = SecureRandom()

        fun create(authorizedAt: Instant = Instant.now()): VerificationAttempt =
            VerificationAttempt(randomProof(), randomProof(), authorizedAt)

        private fun randomProof() = encode(ByteArray(32).also(random::nextBytes))
        private fun encode(value: ByteArray) = Base64.getUrlEncoder().withoutPadding().encodeToString(value)
    }
}

/** [accepted] is the host's final confirmation. A state of confirmed alone is still pending. */
data class VerificationResult(val state: String, val accepted: Boolean = false)

enum class VerificationFailure {
    Invalid,
    Unauthorized,
    Expired,
    AttemptsExhausted,
    Unresolved,
    Unavailable,
}

class VerificationRequestException(val failure: VerificationFailure) : Exception(failure.name)
