# LipaHaraka API

Invoice-to-cash platform for Kenyan SMEs: digital invoicing, automated collections, M-Pesa
payments, invoice-backed risk scoring, and short-term cash advances via a partner lender.

## A note on architecture vs. the project document

Chapter 3.3.1 of the accompanying thesis specifies **Elixir/Phoenix** for this backend,
chosen for the BEAM VM's concurrency model on webhook-heavy financial workloads. This codebase
implements the same requirements in **Spring Boot** instead. Spring Boot is a mainstream,
well-supported choice for this kind of system, but it is a deviation from the written
methodology — worth calling out explicitly if a supervisor or examiner compares the two. To
keep the same operational properties the thesis cares about:

- reminder fan-out and M-Pesa callback post-processing run off the request thread on a
  dedicated pool (`AsyncConfig`), so a burst of webhooks doesn't starve normal API traffic;
- outbound calls to M-Pesa and the partner lender are wrapped in Resilience4j circuit
  breakers + retries, so a slow/flapping third party degrades gracefully;
- each item in a batch job (reminders, overdue-marking) is processed in its own try/catch, so
  one bad record can't take down the whole run.

## Tech stack

- Java 17, Spring Boot 3.3
- PostgreSQL + Spring Data JPA + Flyway (schema is Flyway-owned; `ddl-auto: validate` only)
- Redis (caching; ready for rate-limiting / idempotency keys)
- Spring Security + stateless JWT (jjwt)
- Resilience4j (circuit breaker + retry for M-Pesa and the lender API)
- springdoc-openapi (Swagger UI at `/docs`)
- Lombok + MapStruct
- JUnit 5, Mockito, AssertJ, Testcontainers (Postgres)

## Package layout (package-by-feature)

```
com.lipaharaka.api
├── common/          BaseEntity, exceptions, response envelope, JPA auditing
├── security/        JWT filter/service, CurrentUserProvider
├── config/          SecurityConfig, AsyncConfig, OpenApiConfig, RestClientConfig
├── notification/    SmsGateway / EmailGateway abstraction (SMS/email provider swap point)
├── user/            Auth, OTP, User (FR-1.1)
├── business/        SME business profile + KYC documents/review (FR-1.2–1.4)
├── invoice/         Invoice, line items, tax calc, shareable links (FR-2.x)
├── collections/     Reminder entity + scheduled escalation job (FR-3.x)
├── payment/         Transaction ledger, M-Pesa Daraja adapter + STK/B2C flows (FR-4.x)
├── risk/            Risk scoring service (FR-5.1)
├── advance/         Advance requests, lender routing, disbursement, auto-repayment (FR-5.2–5.4)
├── admin/           Platform KPIs, KYC review endpoints (FR-6.1)
└── audit/           Immutable audit log + @Audited AOP aspect (FR-6.2)
```

Each domain package follows the same internal shape: `Entity → Repository → Service →
Controller`, with `dto/` holding request/response records. This keeps a new feature's blast
radius contained to one package, which is the main "maintainable" lever here — you should be
able to open `advance/` and understand the whole financing flow without reading `invoice/`
beyond its public service methods.

## Key design decisions

- **Money is `BigDecimal`, always**, with explicit `RoundingMode.HALF_UP` at each calculation
  step (see `InvoiceService.applyLineItems`, `AdvanceService.quote`). Never use `double` for
  currency.
- **Optimistic locking (`@Version`) on `Invoice` and `Advance`**, plus a **pessimistic
  row lock** (`findByIdForUpdate` / `findDisbursedAdvanceForInvoiceForUpdate`) specifically on
  the payment-reconciliation path. Two M-Pesa callbacks racing for the same invoice, or a
  callback racing an admin edit, must not double-apply money — this is enforced at the
  database level, not just in application code.
- **Idempotent webhook handling**: the STK callback is matched to the original request via
  Safaricom's `CheckoutRequestID` (stored on the `PENDING` transaction when the push is
  initiated), and a unique index on `mpesa_receipt_number` is the hard backstop against
  Safaricom's documented callback retries causing double-credits.
- **Third-party integrations are behind interfaces** (`MpesaClient`, `LenderApiClient`,
  `SmsGateway`/`EmailGateway`). The rest of the codebase never sees Daraja's or the lender's
  wire format directly — only the adapter package does. Swapping SMS providers or the lender
  is a one-file change.
- **The platform never carries credit risk.** `AdvanceService` originates the request and
  quotes a fee, but `LenderApiClient.submitAdvanceRequest` is where the actual underwriting
  decision happens, on the licensed partner's side — matching the regulatory model in the
  thesis (financing flows through a CBK-licensed partner, not LipaHaraka's own balance sheet).
- **Every state-changing action is audited** via the `@Audited` annotation + `AuditAspect`,
  writing to an append-only `audit_logs` table in its own `REQUIRES_NEW` transaction so an
  audit-write failure can never silently mask (or roll back) a successful business operation.
- **Risk scoring is a transparent heuristic**, not an opaque model: every factor that
  contributed to a score is stored alongside it (`risk_scores.factors` JSONB), because a
  credit-adjacent decision that rejects an SME should be explainable.

## Running locally

```bash
# 1. Start Postgres and Redis (docker-compose.yml is included)
docker compose up -d

# 2. Run migrations + start the app
./mvnw spring-boot:run

# API:       http://localhost:8080
# Swagger UI: http://localhost:8080/docs
```

Environment variables (all have dev-friendly defaults in `application.yml`):

| Variable | Purpose |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL connection |
| `REDIS_HOST`, `REDIS_PORT` | Redis connection |
| `JWT_SECRET` | HMAC signing key — **must** be changed and kept secret in production |
| `MPESA_CONSUMER_KEY`, `MPESA_CONSUMER_SECRET`, `MPESA_SHORTCODE`, `MPESA_PASSKEY` | Daraja sandbox/production credentials |
| `MPESA_CALLBACK_BASE_URL` | Public HTTPS URL Safaricom can reach (use ngrok in dev) |
| `LENDER_BASE_URL`, `LENDER_API_KEY` | Partner lender's underwriting API |

## Testing

```bash
./mvnw test
```

`InvoiceServiceTest` and `AdvanceServiceTest` are Mockito-based unit tests covering the tax
calculation, fee-quoting math, and eligibility/idempotency guards — the business rules most
worth protecting with fast, deterministic tests. For end-to-end persistence behaviour
(migrations actually applying, unique constraints firing, pessimistic locks working), add a
Testcontainers-backed `@SpringBootTest` per module; the `testcontainers` + `postgresql`
dependencies are already wired into `pom.xml` for this.

## What's stubbed vs. production-ready

| Area | Status |
|---|---|
| Auth, OTP, JWT | Production-ready |
| Invoicing, tax calc, shareable links | Production-ready |
| Reminders scheduler | Production-ready logic; swap `LoggingSmsGateway`/`LoggingEmailGateway` for a real provider |
| M-Pesa STK Push + callback reconciliation | Structurally complete; needs real Daraja sandbox credentials to exercise end-to-end |
| M-Pesa B2C disbursement | Adapter complete; `SecurityCredential` generation (RSA-encrypted initiator password) is a placeholder — must be implemented against Safaricom's certificate before going live |
| Lender routing | Adapter complete against a provisional request/response shape; **confirm the actual contract with the partner lender** and update `HttpLenderApiClient` |
| Risk scoring | Deliberately simple, explainable heuristic — replace with a calibrated model behind the same `RiskScoringService` interface when you have labelled outcome data |
| Admin KPIs | Core aggregates implemented; add whatever additional dashboard cuts the SME/admin research (Ch. 5) surfaces as most useful |

## Database migrations

All schema changes go through Flyway (`src/main/resources/db/migration/V*.sql`). Never set
`spring.jpa.hibernate.ddl-auto` to anything but `validate` — Hibernate should confirm the schema
matches the entities, not own the schema itself. Add a new `V2__...sql` file for any change.
# lipaharaka-api
