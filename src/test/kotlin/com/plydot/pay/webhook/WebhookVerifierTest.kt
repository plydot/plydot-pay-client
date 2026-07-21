package com.plydot.pay.webhook

import com.plydot.pay.internal.JsonMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class WebhookVerifierTest {
    @Test
    fun `verify valid signature`() {
        val secret = "whsec_test_secret"
        val body =
            """
            {
              "event": "payment.succeeded",
              "createdAt": "2026-07-21T09:00:00Z",
              "data": {"paymentId": "11111111-1111-1111-1111-111111111111"}
            }
            """.trimIndent()

        val signature = WebhookVerifier.sign(secret, body)
        assertTrue(WebhookVerifier.verify(secret, body, signature))
    }

    @Test
    fun `reject invalid signature`() {
        val body = """{"event":"payment.succeeded"}"""
        assertFalse(WebhookVerifier.verify("whsec_test", body, "deadbeef"))
    }

    @Test
    fun `parse webhook event envelope`() {
        val json =
            """
            {
              "event": "payment.succeeded",
              "createdAt": "2026-07-21T09:00:00Z",
              "data": {
                "paymentId": "11111111-1111-1111-1111-111111111111",
                "amountMinor": 50000,
                "currency": "UGX"
              }
            }
            """.trimIndent()

        val event = WebhookEvent.parse(json)
        assertEquals("payment.succeeded", event.event)
        assertEquals(Instant.parse("2026-07-21T09:00:00Z"), event.createdAt)
        assertEquals("11111111-1111-1111-1111-111111111111", event.data["paymentId"])
        assertEquals(50000, (event.data["amountMinor"] as Number).toInt())

        // round-trip sanity
        val serialized = JsonMapper.write(event)
        val reparsed = WebhookEvent.parse(serialized)
        assertEquals(event.event, reparsed.event)
    }
}
