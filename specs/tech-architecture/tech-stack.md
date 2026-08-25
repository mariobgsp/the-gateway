# Tech Stack — The Gateway

> **Source:** Derived via `map-codebase` discipline + `deepen-architecture` grilling for candidates 1 & 2. Vocabulary per `define-language` / `model-domain` CONTEXT-FORMAT.

## Contexts

| Context | Purpose | Stack |
| --------- | --------- | ------- |
| **BFF Gateway** | Next.js App Router frontend that proxies all backend calls (`/gw/*`), owns session cookie, guards routes | Next.js 15, React 19, TypeScript 5, `fetch` + `AbortController`, `httpOnly`/`SameSite=Lax`/`Secure` cookie, middleware |
| **Gateway Service** | Spring Boot backend: auth, API/Store domains, upstream forwarding | Spring Boot 3.1 (Java 17), Spring Security + `jjwt` 0.12.x, Spring Data JPA, PostgreSQL (prod) / H2 (local/test), `RestTemplate` |
| **Upstream** | Third-party APIs configured per `ApiGateway` (e.g. `thecatapi.com`) | HTTP via `HttpServices`/`RestTemplate` |
| **Infra** | Local dev / Docker | Docker Compose (frontend :3000, backend :8080, postgres :5432), `data.sql` seed, Vitest + Playwright (frontend), JUnit/MockMvc (backend) |

## Domain Language (Ubiquitous Terms)

| Term | Meaning | Where it lives |
| ------ | --------- | ---------------- |
| **BffGateway** | **Deep Module** — single proxy that hides `getSessionToken` → 401, `callBackend`, `envelopeError`, `NextResponse` mapping. Each `/gw/*` route is a thin **Adapter** declaring `{backendPath, method, validate}`. | `frontend/src/lib/backend.ts` (deepened) + `frontend/src/app/gw/**` adapters |
| **GatewayForward** | **Deep Module** — typed forward path: `GatewayController` parses HTTP into `ForwardRequest(pathName, queryParams, headers, body)`, `ApiGatewayServices:processForwardApi` handles allowlist filtering, query encoding, and `HttpServices` dispatch. Former `CommonUtil.processRequest: Map` is internal implementation, not seam. | `gateway-service/.../controller/GatewayController.java` + `service/ApiGatewayServices.java` + new `ForwardRequest.java` |
| **ForwardRequest** | Value object — the **Interface** between controller and service for forwarding. Replaces `Map<String,Object>` with 5 string keys. Fields: `pathName: string`, `queryParams: Map<String,String>`, `headers: HttpHeaders`, `body: Object`. | `gateway-service/.../models/rqrs/ForwardRequest.java` (new) |
| **UpstreamPort** | **Seam** at `GatewayForward` for external upstream — internal to module, not exposed to callers. Adapters: `RestTemplateAdapter` (prod) and `InMemoryAdapter` (test). Single-adapter case is hypothetical seam → inline instead. | Inside `GatewayForward` (via `HttpServices.java` or successor) |
| **GatewayResult** | (Deferred — Candidate 3) Single error/response translation for `Response<T>` / `code "00"/"99"` / `CommonException`. Not deepened in this slice but term reserved. | `.../models/rqrs/Response.java`, `.../util/CommonUtil.applyError` |
| **ApiGateway** | Persisted gateway config entity — `apiIdentifier` (unique), `apiHost`, `apiPath`, `method`, `status`, `header` (semicolon list), `requireRequestParam`/`Body`, `param` (semicolon list) | `models/entity/ApiGateway.java` |
| **StoreAccount** | Store identity — `storeName`, `clientId`, `secretKey` (`gw_` + Base64 32B) | `models/entity/StoreAccount.java` |
| **SystemProperties** | Runtime config store (`TOKEN_SECRET_KEY`, `TOKEN_EXPIRATION`) queried via `SystemPropertiesServices.getProps` | `models/entity/SystemProperties.java` |

## Architecture Decisions Implied (not yet ADR)

- `BffGateway` owns sanitization of `sanitizeHeaders`/`sanitizeParams` (strips `host`/`cookie`/`authorization` etc.) **privately** — not exposed for testing past interface, and not duplicated in `GatewayForward`. Backend does second-pass allowlist via `ApiGateway.header`/`param` configs.
- `GatewayForward` owns canonical query encoding — fixes current double-encode (`encodeURIComponent` in BFF then `URLEncoder` in backend) to single place.
- `UpstreamPort` seam is **internal** to `GatewayForward` — only introduced when 2 adapters justified (prod HTTP + test in-memory). Until then single `RestTemplate` inline = no seam.

## ADRs

None yet. If any candidate is rejected with load-bearing reason, offer ADR per `deepen-architecture` §4.

## Conventions to Preserve

- JWT stays `httpOnly`/`SameSite=Lax`/`Secure` in prod (`COOKIE_SECURE` / `NODE_ENV`), never `localStorage`.
- Envelope `code "00"` = success, else error — mapped centrally in `BffGateway` and `GatewayResult` (when deepened).
- H2 `local` profile (`application-local.properties` + `data.sql`) is the Local-substitutable stand-in for JPA tests — no need to expose repo seams externally.

## Gray Areas (To Harden in Grilling)

- Host validation: `ApiGatewayServices.validateSaveApi` rejects embedded credentials (`@` in host) and requires `http(s)://` — same rule should live once in `GatewayForward`, not duplicated in BFF `save/route.ts:validate`.
- Query param tolerance: `CommonUtil.parseQueryString` currently puts `""` for valueless keys — keep tolerant behavior in new typed parser for back-compat unless user opts strict.
