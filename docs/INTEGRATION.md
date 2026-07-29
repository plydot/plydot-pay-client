# Integration guide

> **Canonical docs:** [API Specification handbook](../../handbook/plydot-pay-api-spec-v1.0.md) — Guide § Merchant integration.

Complete guide for third-party merchants using `plydot-pay-client`.

---

## 1. Prerequisites

### Merchant setup (one-time, by Plydot admin)

1. Create merchant: `POST /v1/merchants` with `type: "MERCHANT"`.
2. Issue API key: `POST /v1/merchants/{id}/api-keys` — save the `rawKey` (shown once).
3. Register webhook: `POST /v1/webhooks/endpoints` with your HTTPS callback URL — save the `secret` (`whsec_…`).

Your integrator backend only needs the **API key** and **webhook secret**.

### What you build

- **Backend** — creates checkouts, initiates pay, handles webhooks
- **Frontend** (optional) — shows payment status; triggers pay with customer phone number

---

## 2. Install the library

```kotlin
// build.gradle.kts
implementation("com.plydot:plydot-pay-client:0.1.0")
```

Requires Java 17+. Works in Spring Boot, Quarkus, plain Java/Kotlin, Android (with desugaring if needed).

---

## 3. Configure the client

Create one shared instance (Spring `@Bean`, Koin singleton, etc.):

```kotlin
@Configuration
class PayConfig {
    @Bean
    fun plydotPayClient(
        @Value("\${plydot.pay.api-key}") apiKey: String,
        @Value("\${plydot.pay.base-url:https://pay.plydot.dev}") baseUrl: String,
    ): PlydotPayClient =
        PlydotPayClient.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .build()
}
```

```properties
# application.properties
plydot.pay.api-key=pk_test_…
plydot.pay.base-url=https://pay.plydot.dev
```

Never expose the API key to browsers or mobile apps. All Pay calls go through **your backend**.

---

## 4. Discover providers and payers

Before checkout, load assigned providers with nested payers:

```kotlin
val providers = payClient.listProviders()
// [{ code: "SANDBOX", providerId: "…", payers: [{ id, code: "MTN_MOMO", … }] }]

val sandbox = providers.first { it.code == "SANDBOX" }
val mtnPayer = sandbox.payers.first { it.code == "MTN_MOMO" }
```

Use `providerId` + `payerId` when creating the checkout. The rail is fixed for that checkout; pay only sends `payerRef` (MSISDN).

---

## 5. Create a checkout

When a customer starts checkout on your site, create a Pay checkout with **your product SKU** and the chosen provider/payer:

```kotlin
fun startCheckout(order: Order, providerId: UUID, payerId: UUID): CreateCheckoutResponse {
    return payClient.createThirdPartyCheckout(
        ThirdPartyCheckoutRequest.builder()
            .productId(order.sku)                    // your SKU, not a Pay UUID
            .amountMinor(order.totalUgx)
            .currency("UGX")
            .providerId(providerId)
            .payerId(payerId)
            .customer(Customer(
                name = order.customerName,
                phone = order.customerPhone,
                email = order.customerEmail,
            ))
            .description(order.description)
            .build(),
        idempotencyKey = "checkout-${order.id}",   // use your order ID
    )
}
```

**Response fields you care about:**

| Field | Value |
|-------|-------|
| `id` | Pay checkout UUID — use for `payCheckout` |
| `productId` | Your SKU echoed back |
| `amountMinor` | Charge amount |
| `status` | `PENDING` until paid |
| `expiresAt` | Checkout expiry — create a new one if expired |
| `accountRef` | Your merchant code (same for all your sales) |

---

## 6. Collect payment

When the customer enters their phone number, pay with `payerRef` only (provider and payer are already on the checkout):

```kotlin
fun collectPayment(checkoutId: UUID, phone: String): PayCheckoutResponse {
    return payClient.payCheckout(
        checkoutId,
        PayCheckoutRequest(payerRef = phone),   // e.g. "256700000099"
    )
}
```

The response includes `payment.instructions` — show these to the customer (e.g. "Approve the Mobile Money prompt on 256700000099").

Payment starts in `PENDING` or `PROCESSING`. Final state arrives via **webhook** or polling.

---

## 7. Handle webhooks (recommended)

Register `POST https://your-api.com/webhooks/plydot` in Pay.

### Spring Boot example

```kotlin
@RestController
@RequestMapping("/webhooks/plydot")
class PlydotWebhookController(
    @Value("\${plydot.webhook.secret}") private val webhookSecret: String,
    private val orderService: OrderService,
) {
    @PostMapping
    fun handle(
        @RequestBody rawBody: String,
        @RequestHeader("X-Plydot-Signature", required = false) signature: String?,
    ): ResponseEntity<Void> {
        if (!WebhookVerifier.verify(webhookSecret, rawBody, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        val event = WebhookEvent.parse(rawBody)
        when (event.event) {
            "payment.succeeded" -> orderService.markPaid(event.data)
            "payment.failed" -> orderService.markFailed(event.data)
        }
        return ResponseEntity.ok().build()
    }
}
```

**Notes:**
- Read the body as a **raw string** for signature verification (don't re-serialize JSON).
- Return `200` quickly; do heavy fulfillment asynchronously.
- Webhooks may retry — make fulfillment **idempotent** (key on `paymentId`).

### Mapping webhook data to your order

```kotlin
fun markPaid(data: Map<String, Any?>) {
    val paymentId = data["paymentId"] as String
    val productId = data["productId"] as String
    val metadata = data["metadata"] as? Map<*, *>
    val customerPhone = metadata?.get("customerPhone") as? String
    // match to your order via metadata you stored, or poll getPayment(checkoutId)
}
```

Store your internal `orderId` when creating the checkout (in your DB keyed by Pay `checkoutId` or `idempotencyKey`).

---

## 8. Poll payment status (fallback)

Use when webhooks are delayed or for a status page:

```kotlin
fun waitForPayment(paymentId: UUID, timeoutMs: Long = 120_000): PaymentResponse {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
        val payment = payClient.getPayment(paymentId)
        when (payment.status) {
            PaymentStatus.SUCCEEDED, PaymentStatus.FAILED,
            PaymentStatus.CANCELLED, PaymentStatus.STALE -> return payment
            else -> Thread.sleep(3_000)
        }
    }
    throw TimeoutException("Payment $paymentId did not complete in time")
}
```

---

## 9. List payers (platform admin)

Show available rails in your checkout UI (or use `GET /v1/providers` for provider-nested discovery):

```kotlin
val payers = payClient.listPayers()
// [{ code: "MTN_MOMO", displayName: "MTN Mobile Money", active: true }, …]
```

---

## 10. Idempotency

Always pass `idempotencyKey` when creating checkouts:

```kotlin
payClient.createThirdPartyCheckout(request, idempotencyKey = "order-${order.id}")
```

- Same key + same body → returns original checkout (safe retry)
- Same key + different body → `IDEMPOTENCY_KEY_REUSED` error

Use your internal order ID as the key.

---

## 11. Test vs live

| | Test | Live |
|---|------|------|
| API key prefix | `pk_test_` | `pk_live_` |
| Base URL | Same (`https://pay.plydot.dev`) | Same |
| MoMo | Sandbox/test routes | Production routes |

Use test keys during development. Switch to `pk_live_` in production.

---

## 12. Troubleshooting

| Problem | Fix |
|---------|-----|
| `UNAUTHORIZED` | Check API key; ensure `Authorization: Bearer pk_…` |
| `CUSTOMER_NAME_REQUIRED` | Use `ThirdPartyCheckoutRequest` with `Customer.name` |
| `CHECKOUT_EXPIRED` | Create a new checkout |
| `PAYMENT_IN_PROGRESS` | Poll existing payment or wait for webhook |
| Webhook signature fails | Verify against raw body; check `whsec_` secret |
| Payment stays `PENDING` | Customer hasn't approved MoMo prompt yet |

---

## 13. API docs

- Swagger UI: https://pay.plydot.com/api/swagger-ui.html
- Scalar docs: https://pay.plydot.com/api/docs/
- Playground: https://pay.plydot.com/api/playground/
- Maven artifact: https://central.sonatype.com/artifact/com.plydot/plydot-pay-client

For platform/admin APIs (merchant bootstrap, refund approval), use the REST API directly — they are not in this SDK v1.
