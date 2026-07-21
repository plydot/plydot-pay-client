# Plydot Pay Client

Official **Java/Kotlin SDK** for third-party merchants integrating with [Plydot Pay](https://pay.plydot.dev).

Collect Mobile Money payments (MTN MoMo, Airtel Money) from your own app or store without building HTTP/auth/idempotency plumbing yourself.

| | |
|---|---|
| **Maven coordinates** | `com.plydot:plydot-pay-client` |
| **Latest version** | `0.1.0` |
| **Java** | 17+ |
| **Spring** | Not required |

---

## Table of contents

1. [Before you start](#before-you-start)
2. [Add the dependency](#add-the-dependency)
3. [Quick start (Kotlin)](#quick-start-kotlin)
4. [Quick start (Java)](#quick-start-java)
5. [End-to-end integration flow](#end-to-end-integration-flow)
6. [Third-party checkout rules](#third-party-checkout-rules)
7. [API reference](#api-reference)
8. [Webhooks](#webhooks)
9. [Error handling](#error-handling)
10. [Configuration](#configuration)
11. [Further reading](#further-reading)

---

## Before you start

You need a **Plydot Pay merchant account** and an **API key** (`pk_test_…` or `pk_live_…`).

1. Plydot creates your merchant (`type: MERCHANT`) and issues an API key.
2. Register a **webhook endpoint** in Pay (admin UI or API) so you receive `payment.succeeded` / `payment.failed`.
3. Add this library to your backend.

**Production API:** `https://pay.plydot.dev`  
**Local Docker (dev):** `http://localhost:8044`

API reference (OpenAPI): https://pay.plydot.dev/swagger-ui.html

---

## Add the dependency

### Maven

```xml
<dependency>
  <groupId>com.plydot</groupId>
  <artifactId>plydot-pay-client</artifactId>
  <version>0.1.0</version>
</dependency>
```

### Gradle (Kotlin DSL)

```kotlin
implementation("com.plydot:plydot-pay-client:0.1.0")
```

### Gradle (Groovy)

```groovy
implementation 'com.plydot:plydot-pay-client:0.1.0'
```

---

## Quick start (Kotlin)

```kotlin
import com.plydot.pay.PlydotPayClient
import com.plydot.pay.model.PayCheckoutRequest
import com.plydot.pay.model.PaymentStatus
import com.plydot.pay.thirdparty.Customer
import com.plydot.pay.thirdparty.ThirdPartyCheckoutRequest

// 1. Create client (reuse as a singleton in your app)
val pay = PlydotPayClient.builder()
    .apiKey("pk_test_your_key_here")
    .baseUrl("https://pay.plydot.dev")
    .build()

// 2. Create checkout — use your own product SKU and customer details
val checkout = pay.createThirdPartyCheckout(
    ThirdPartyCheckoutRequest.builder()
        .productId("acme-gold-plan")
        .amountMinor(50_000)          // 50,000 UGX
        .currency("UGX")
        .customer(Customer(
            name = "Jane Okello",
            phone = "256700000099",
            email = "jane@example.com",
        ))
        .description("Gold plan subscription")
        .build(),
    idempotencyKey = "order-${yourOrderId}",  // strongly recommended
)

// 3. Start payment — customer approves MoMo prompt on their phone
val result = pay.payCheckout(
    checkout.id,
    PayCheckoutRequest(
        paymentMethodCode = "MTN_MOMO",   // or AIRTEL_MONEY
        payerRef = "256700000099",        // MSISDN
    ),
)

// 4. Poll until final (or wait for webhook — preferred)
var payment = result.payment
while (payment.status == PaymentStatus.PENDING || payment.status == PaymentStatus.PROCESSING) {
    Thread.sleep(3_000)
    payment = pay.getPayment(payment.id)
}

when (payment.status) {
    PaymentStatus.SUCCEEDED -> fulfillOrder(yourOrderId)
    PaymentStatus.FAILED -> notifyCustomerFailed()
    else -> { /* handle other states */ }
}
```

---

## Quick start (Java)

```java
import com.plydot.pay.PlydotPayClient;
import com.plydot.pay.model.PayCheckoutRequest;
import com.plydot.pay.model.PayCheckoutResponse;
import com.plydot.pay.model.CreateCheckoutResponse;
import com.plydot.pay.thirdparty.Customer;
import com.plydot.pay.thirdparty.ThirdPartyCheckoutRequest;

PlydotPayClient pay = PlydotPayClient.builder()
    .apiKey("pk_test_your_key_here")
    .baseUrl("https://pay.plydot.dev")
    .build();

CreateCheckoutResponse checkout = pay.createThirdPartyCheckout(
    ThirdPartyCheckoutRequest.builder()
        .productId("acme-gold-plan")
        .amountMinor(50_000L)
        .currency("UGX")
        .customer(new Customer("Jane Okello", "256700000099", "jane@example.com"))
        .description("Gold plan subscription")
        .build(),
    "order-" + yourOrderId
);

PayCheckoutResponse result = pay.payCheckout(
    checkout.getId(),
    new PayCheckoutRequest("MTN_MOMO", "256700000099")
);
```

---

## End-to-end integration flow

```text
Your app                         Plydot Pay                    MoMo network
   |                                 |                              |
   |-- createThirdPartyCheckout() -->|                              |
   |<-- checkout (PENDING) ----------|                              |
   |-- payCheckout(MTN_MOMO) ------->|---- collect request -------->|
   |<-- payment (PENDING) -----------|                              |
   |                                 |<---- customer approves ------|
   |<-- webhook payment.succeeded ---|                              |
   |   (or poll getPayment)          |                              |
   |-- fulfill order                 |                              |
```

**Recommended:** fulfill orders from the **webhook**, not polling alone. Use polling as a fallback or for UI status screens.

See [docs/INTEGRATION.md](docs/INTEGRATION.md) for a full walkthrough including webhook handler examples.

---

## Third-party checkout rules

As a third-party integrator you **do not** use Pay's product catalog. You send your own SKU:

| Field | Required | Notes |
|-------|----------|-------|
| `productId` | Yes | Your SKU, e.g. `acme-gold-plan` |
| `amountMinor` | Yes | Amount in whole currency units (UGX shillings) |
| `currency` | Yes | e.g. `UGX` |
| `customer.name` | Yes | End customer name |
| `customer.phone` and/or `email` | Yes | At least one contact field |
| `accountRef` | No | **Ignored** — Pay sets this to your merchant code |

Use `ThirdPartyCheckoutRequest` — it validates customer fields **before** calling the API.

---

## API reference

### Client builder

```kotlin
PlydotPayClient.builder()
    .apiKey("pk_test_…")                              // required
    .baseUrl("https://pay.plydot.dev")                // optional, this is the default
    .connectTimeout(Duration.ofSeconds(5))            // optional
    .readTimeout(Duration.ofSeconds(30))              // optional
    .build()
```

### Methods

| Method | Description |
|--------|-------------|
| `createThirdPartyCheckout(request, idempotencyKey?)` | Create checkout with validated customer metadata |
| `createCheckout(request, idempotencyKey?)` | Low-level checkout create |
| `getCheckout(id)` | Get checkout by ID |
| `listCheckouts(status?, …)` | List checkouts for your merchant |
| `payCheckout(checkoutId, request)` | Start MoMo collection |
| `cancelCheckout(checkoutId)` | Cancel a pending checkout |
| `getPayment(id)` | Get payment status |
| `listPayments(status?, …)` | List payments |
| `listPaymentMethods()` | List available methods (`MTN_MOMO`, `AIRTEL_MONEY`, …) |

### Payment method codes

Call `listPaymentMethods()` for the current list. Common values:

- `MTN_MOMO` — MTN Mobile Money (Uganda)
- `AIRTEL_MONEY` — Airtel Money (Uganda)

---

## Webhooks

Pay POSTs JSON to your registered URL when a payment completes.

**Header:** `X-Plydot-Signature` — HMAC-SHA256 hex of the raw body using your endpoint secret (`whsec_…`).

```kotlin
import com.plydot.pay.webhook.WebhookEvent
import com.plydot.pay.webhook.WebhookVerifier

fun handleWebhook(rawBody: String, signature: String?, secret: String) {
    if (!WebhookVerifier.verify(secret, rawBody, signature)) {
        throw SecurityException("Invalid webhook signature")
    }

    val event = WebhookEvent.parse(rawBody)
    when (event.event) {
        "payment.succeeded" -> {
            val paymentId = event.data["paymentId"]
            val productId = event.data["productId"]
            val metadata = event.data["metadata"] as? Map<*, *>
            // fulfill order using productId + metadata
        }
        "payment.failed" -> { /* notify customer */ }
    }
}
```

**Important:** Always verify the signature against the **raw request body** before parsing JSON.

Event payload shape:

```json
{
  "event": "payment.succeeded",
  "createdAt": "2026-07-21T09:00:00Z",
  "data": {
    "paymentId": "…",
    "checkoutId": "…",
    "amountMinor": 50000,
    "currency": "UGX",
    "status": "SUCCEEDED",
    "productId": "acme-gold-plan",
    "metadata": {
      "customerName": "Jane Okello",
      "customerPhone": "256700000099"
    }
  }
}
```

---

## Error handling

Failed API calls throw `PlydotPayException`:

```kotlin
import com.plydot.pay.exception.PlydotPayException

try {
    pay.getCheckout(checkoutId)
} catch (ex: PlydotPayException) {
    println("${ex.code}: ${ex.message}")   // e.g. CHECKOUT_EXPIRED
    println("HTTP ${ex.httpStatus}")

    when {
        ex.isCheckoutExpired() -> showExpiredMessage()
        ex.isCheckoutNotPayable() -> showAlreadyPaidMessage()
        ex.isPaymentInProgress() -> showInProgressMessage()
        ex.isIdempotencyConflict() -> retryWithSameKeyOrNewKey()
        ex.isUnauthorized() -> checkApiKey()
    }
}
```

Common error codes:

| Code | Meaning |
|------|---------|
| `UNAUTHORIZED` | Invalid or missing API key |
| `CHECKOUT_EXPIRED` | Checkout past `expiresAt` |
| `CHECKOUT_NOT_PAYABLE` | Checkout not in `PENDING` state |
| `PAYMENT_IN_PROGRESS` | Open payment already exists |
| `VALIDATION_ERROR` | Request validation failed |
| `IDEMPOTENCY_KEY_REUSED` | Same key used with different body |

---

## Configuration

| Setting | Default | Description |
|---------|---------|-------------|
| `baseUrl` | `https://pay.plydot.dev` | Pay API base URL |
| `connectTimeout` | 5 seconds | TCP connect timeout |
| `readTimeout` | 30 seconds | HTTP read timeout |

Authentication is always `Authorization: Bearer {apiKey}`.

For idempotent checkout creation, pass a unique `idempotencyKey` per logical order (e.g. your order ID). Retries with the same key return the original response.

---

## Further reading

- [docs/INTEGRATION.md](docs/INTEGRATION.md) — full integration guide with webhook servlet example
- [docs/PUBLISHING.md](docs/PUBLISHING.md) — Maven Central releases (auto on `main`, secrets, manual publish)
- [Plydot Pay API docs](https://pay.plydot.dev/swagger-ui.html)
- [Maven Central](https://central.sonatype.com/artifact/com.plydot/plydot-pay-client)

---

## License

Apache License 2.0 — see [LICENSE](LICENSE).
