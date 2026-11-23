package com.apexfission.android.emailverification

import java.io.IOException
import java.net.URI
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

/** HTTP adapter for the standalone Go email-verification test host. */
class TestHostGateway(
    origin: String,
    private val accessToken: suspend () -> String,
) : VerificationGateway {
    private val endpoint = URI.create(origin).also {
        require(it.scheme.equals("https", ignoreCase = true) && it.host != null &&
            it.rawUserInfo == null && it.rawQuery == null && it.rawFragment == null &&
            (it.rawPath.isNullOrEmpty() || it.rawPath == "/") && it.port in -1..65535 && it.port != 0)
    }

    override suspend fun start(email: String, attempt: VerificationAttempt) = post("start", attempt) {
        put("email", email)
        put("challenge", attempt.challenge)
        put("authorized_at", attempt.authorizedAt.toString())
    }

    override suspend fun confirm(attempt: VerificationAttempt, code: String) = post("confirm", attempt) {
        put("secret", attempt.secret)
        put("code", code)
    }

    override suspend fun status(attempt: VerificationAttempt) = post("status", attempt) {}

    private suspend fun post(
        action: String,
        attempt: VerificationAttempt,
        fields: JSONObject.() -> Unit,
    ): VerificationResult {
        val token = accessToken()
        if (token.isBlank() || token.any { it.isWhitespace() || it.isISOControl() }) {
            throw VerificationRequestException(VerificationFailure.Unauthorized)
        }

        return withContext(Dispatchers.IO) {
            val connection = endpoint.resolve("/v1/verification/$action").toURL()
                .openConnection() as HttpsURLConnection
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 10_000
                connection.readTimeout = 15_000
                connection.requestMethod = "POST"
                connection.useCaches = false
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $token")
                val payload = JSONObject().put("operation", attempt.operation).apply(fields)
                    .toString().encodeToByteArray()
                connection.setFixedLengthStreamingMode(payload.size)
                connection.outputStream.use { it.write(payload) }

                val status = connection.responseCode
                if (status == 401 || status == 403) {
                    throw VerificationRequestException(VerificationFailure.Unauthorized)
                }
                val stream = if (status == 200) connection.inputStream else connection.errorStream
                val response = stream?.use {
                    val buffer = ByteArray(8193)
                    var size = 0
                    while (size < buffer.size) {
                        val read = it.read(buffer, size, buffer.size - size)
                        if (read == -1) break
                        size += read
                    }
                    if (size == buffer.size) throw IOException()
                    JSONObject(buffer.decodeToString(endIndex = size))
                }
                val error = response?.optString("error").orEmpty()
                if (status != 200 || error.isNotBlank()) {
                    val failure = when (error) {
                        "invalid" -> VerificationFailure.Invalid
                        "expired" -> VerificationFailure.Expired
                        "attempts_exhausted" -> VerificationFailure.AttemptsExhausted
                        "unresolved" -> VerificationFailure.Unresolved
                        else -> VerificationFailure.Unavailable
                    }
                    throw VerificationRequestException(failure)
                }
                val state = response?.optString("state").orEmpty()
                if (response == null || response.optString("operation") != attempt.operation || state.isBlank()) {
                    throw VerificationRequestException(VerificationFailure.Unavailable)
                }
                VerificationResult(state, response.opt("accepted") == true)
            } catch (_: IOException) {
                throw VerificationRequestException(VerificationFailure.Unavailable)
            } catch (_: JSONException) {
                throw VerificationRequestException(VerificationFailure.Unavailable)
            } finally {
                connection.disconnect()
            }
        }
    }

}
