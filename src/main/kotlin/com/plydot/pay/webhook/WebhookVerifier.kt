package com.plydot.pay.webhook

import com.plydot.pay.internal.JsonMapper
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class WebhookEvent(
    val event: String,
    val createdAt: Instant,
    val data: Map<String, Any?>,
) {
    companion object {
        @JvmStatic
        fun parse(json: String): WebhookEvent = JsonMapper.read(json)
    }
}

object WebhookVerifier {
    private const val SIGNATURE_HEADER = "X-Plydot-Signature"

    /**
     * Verifies HMAC-SHA256 hex signature of the raw webhook body.
     */
    @JvmStatic
    fun verify(
        secret: String,
        rawBody: String,
        signatureHeader: String?,
    ): Boolean {
        if (signatureHeader.isNullOrBlank()) {
            return false
        }
        val expected = sign(secret, rawBody)
        return constantTimeEquals(expected, signatureHeader.trim())
    }

    @JvmStatic
    fun signatureHeaderName(): String = SIGNATURE_HEADER

    @JvmStatic
    fun sign(secret: String, payload: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        return mac.doFinal(payload.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private fun constantTimeEquals(left: String, right: String): Boolean {
        if (left.length != right.length) {
            return false
        }
        var result = 0
        for (i in left.indices) {
            result = result or (left[i].code xor right[i].code)
        }
        return result == 0
    }
}
