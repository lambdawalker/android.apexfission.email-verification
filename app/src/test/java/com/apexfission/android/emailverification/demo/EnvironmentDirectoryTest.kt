package com.apexfission.android.emailverification.demo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnvironmentDirectoryTest {
    @Test
    fun directoryMayOnlySelectTheMatchingTestHost() {
        assertTrue(trustedDemoOrigin("dev", "https://dev.components.email-verification.apexfission.com"))
        assertTrue(trustedDemoOrigin("qa", "https://qa.components.email-verification.apexfission.com/"))
        listOf(
            "http://dev.components.email-verification.apexfission.com",
            "https://qa.components.email-verification.apexfission.com",
            "https://dev.components.email-verification.apexfission.com.example.org",
            "https://dev.components.email-verification.apexfission.com@other.example.org",
            "https://dev.components.email-verification.apexfission.com/redirect",
        ).forEach {
            assertFalse(trustedDemoOrigin("dev", it))
        }
        assertFalse(trustedDemoOrigin("prod", "https://prod.components.email-verification.apexfission.com"))
    }
    @Test
    fun readsThePublishedIndexContract() {
        val entries = decodeDemoDirectory(
            """
            {"schemaVersion":1,"environments":[
              {"id":"qa","revision":2,"configuration":{
                "apiUrl":"https://qa.components.email-verification.apexfission.com",
                "cognitoUserPoolId":"us-east-2_qa","cognitoClientId":"qa-client"}},
              {"id":"dev","revision":1,"configuration":{
                "apiUrl":"https://unrelated.example.org",
                "cognitoUserPoolId":"us-east-2_dev","cognitoClientId":"dev-client"}}
            ]}
            """.trimIndent(),
        )
        assertEquals(
            listOf(DemoEnvironment("qa", "https://qa.components.email-verification.apexfission.com", "us-east-2_qa", "qa-client")),
            entries,
        )
    }
}
