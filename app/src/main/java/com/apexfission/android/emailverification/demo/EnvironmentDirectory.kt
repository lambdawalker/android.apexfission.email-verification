package com.apexfission.android.emailverification.demo

import java.io.IOException
import java.net.URI
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val DISCOVERY_DOMAIN = "components.email-verification.apexfission.com"
private const val DIRECTORY_URL = "https://index.$DISCOVERY_DOMAIN/v1/environments"
private val environmentId = Regex("[a-z][a-z0-9]*(?:-[a-z0-9]+)*")

internal data class DemoEnvironment(
    val id: String,
    val origin: String,
    val poolId: String,
    val clientId: String,
)

internal data class DemoDirectory(
    val loading: Boolean = false,
    val unavailable: Boolean = false,
    val entries: List<DemoEnvironment> = emptyList(),
)

internal fun trustedDemoOrigin(id: String, origin: String): Boolean {
    if (id.length > 32 || !environmentId.matches(id) || id in setOf("prod", "production")) return false
    val expected = "https://$id.$DISCOVERY_DOMAIN"
    return origin == expected || origin == "$expected/"
}

internal suspend fun loadDemoDirectory(): List<DemoEnvironment> = withContext(Dispatchers.IO) {
    val connection = URI.create(DIRECTORY_URL).toURL().openConnection() as HttpsURLConnection
    try {
        connection.instanceFollowRedirects = false
        connection.connectTimeout = 5_000
        connection.readTimeout = 5_000
        connection.setRequestProperty("Accept", "application/json")
        if (connection.responseCode != 200) throw IOException("Environment directory unavailable")
        val bytes = connection.inputStream.use { input ->
            val buffer = ByteArray(128 * 1024 + 1)
            var size = 0
            while (size < buffer.size) {
                val count = input.read(buffer, size, buffer.size - size)
                if (count == -1) break
                size += count
            }
            if (size == buffer.size) throw IOException("Environment directory too large")
            buffer.decodeToString(endIndex = size)
        }
        decodeDemoDirectory(bytes)
    } finally {
        connection.disconnect()
    }
}

internal fun decodeDemoDirectory(payload: String): List<DemoEnvironment> {
    val response = JSONObject(payload)
    if (response.getInt("schemaVersion") != 1) throw IOException("Unsupported environment directory")
    val records = response.getJSONArray("environments")
    return buildList {
        for (i in 0 until records.length()) {
            val entry = records.getJSONObject(i)
            val configuration = entry.getJSONObject("configuration")
            val id = entry.getString("id")
            val origin = configuration.getString("apiUrl")
            val poolId = configuration.optString("cognitoUserPoolId")
            val clientId = configuration.optString("cognitoClientId")
            if (trustedDemoOrigin(id, origin) && poolId.isNotBlank() && clientId.isNotBlank()) {
                add(DemoEnvironment(id, origin, poolId, clientId))
            }
        }
    }.distinctBy { it.id }.sortedBy { it.id }
}
