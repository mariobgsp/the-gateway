# Plan — The Gateway: Deepen Architecture

> **Status:** READY FOR REVIEW — picks 1+2 grilled, hybrid A+C+D locked
> **Branch:** `main` (clean, `9e05e76`) | **Stack:** Next.js 15 App Router + Spring Boot 3.1 (Java 17) + PostgreSQL/H2
> **Skill:** `deepen-architecture` + `LANGUAGE.md` + `DEEPENING.md` + `INTERFACE-DESIGN.md` — full grilling + design-it-twice complete
> **Docs:** `specs/tech-architecture/tech-stack.md` **created** (terms `BffGateway`/`GatewayForward`/`ForwardRequest`/`UpstreamPort` canonized). `specs/adr/` none. `scripts/bp-churn-rank.sh` missing — manual churn used.

---

## 1. Context

**Project:** `the-gateway` — API Gateway management UI. Frontend BFF (`/gw/*` route handlers proxy to Spring Boot, JWT in `httpOnly`/`SameSite=Lax`/`Secure` cookie, `middleware.ts` guards `/home`, `/api/*`, `/store/*`) + Backend (`gateway-service` JWT auth via `JwtRequestFilter`, JPA entities `ApiGateway`/`StoreAccount`/`User`, upstream forwarding via `HttpServices`/`RestTemplate`).

**Why deepen:** Recent `feature/nextjs-migration` landed 11 near-identical BFF route handlers and backend services that share boilerplate but leak details across seams. Understanding one concept (e.g., "how an error surfaces") requires bouncing between `lib/backend.ts`, `lib/api.ts`, `CommonUtil`, every controller, and every service. Low churn last 90 days (only `2cfc35d` + `9e05e76`) means friction is structural, not hot-file — ranking by line-count shows `ApiDetailPage` (400 LOC) and `ApiGatewayServices` (247 LOC) as leverage magnets.

**Exploration done:**

- Read all 11 BFF routes (`frontend/src/app/gw/**`), `lib/backend.ts`, `lib/session.ts`, `lib/api.ts`, `middleware.ts`, `types.ts`, `AddApiModal`, `home/page.tsx`, `api/[identifier]/page.tsx`, `store/[id]/page.tsx`.
- Read backend `ApiGatewayServices.java`, `StoreService.java`, `GatewayController.java`, `StoreController.java`, `CommonUtil.java`, `JwtUtil.java`, `HttpServices.java`.
- Manual churn: `git log --name-only --since="90 days ago"` — single commit touched all layers; no single hot file, so ranked by size/complexity instead.
- No domain glossary or ADRs to conflict with — new deepened module names would lazily create `specs/tech-architecture/tech-stack.md` per skill §4.

**Forcing function rule:** A deep module must solve a real pain, not "nice abstraction." Every candidate below passes the **deletion test** — delete it and complexity reappears across N callers.

---

## 2. Approach — Deepening Principles

Use **LANGUAGE.md** terms exactly: **Module**, **Interface**, **Implementation**, **Depth**, **Seam**, **Adapter**, **Leverage**, **Locality**.

- **Deletion test:** If deleting the module makes complexity vanish, it's a pass-through. If complexity reappears across N callers, it earns its keep.
- **Interface is test surface.** Tests cross the same seam as callers. If tests must poke past the interface, the module is wrong shape.
- **One adapter = hypothetical seam. Two adapters = real seam.** Don't introduce a port unless two adapters justified (prod + test).
- **Dependency categories (DEEPENING.md):** 1) In-process 2) Local-substitutable (H2 for Postgres) 3) Remote but owned (port + HTTP/in-memory adapters) 4) True external (mock adapter).
- **Ponytail ladder still applies:** smallest diff, reuse before new dep, stdlib first.

---

## 3. Candidates — Numbered Deepening Opportunities

_Prioritized by **Module Depth score** (1=shallow, 5=deep, Ousterhout). ≤2 prioritized._

### Candidate 1 — BFF Gateway Proxy (11 handlers → 1 deep Module) — Depth **1**

- **Files:** `frontend/src/app/gw/apis/route.ts`, `.../save/route.ts`, `.../delete/route.ts`, `.../detail/[identifier]/route.ts`, `.../execute/[identifier]/route.ts`, `.../stores/route.ts`, `.../stores/save/route.ts`, `.../stores/delete/route.ts`, `.../stores/detail/[id]/route.ts`, `.../stores/regenerate/route.ts`, `frontend/src/app/gw/auth/login/route.ts`, `.../logout/route.ts`, `frontend/src/lib/backend.ts` (72 LOC), `frontend/src/lib/session.ts` (40 LOC)
- **Problem:** Every handler repeats same 12-line preamble: `getSessionToken() → 401 if null → callBackend(path, {token, method, body/params}) → if envelope.code !== "00" → envelopeError → NextResponse.json`. Auth check, envelope mapping, and status translation leak across 11 seams. `execute` even re-implements header/param sanitization that `save` validates differently. **Shallow** — interface (one route) nearly as complex as implementation (full proxy). Bug fix to auth or error mapping must touch all 11. Deletion test: delete any handler, complexity reappears in its copy.
- **Solution (plain English):** Collapse into one deep **BffGateway** module. Small interface: `proxy(request, endpointConfig) → Response` where `endpointConfig = { backendPath, method, requireAuth, validate }`. All auth, `callBackend`, `envelopeError`, and `NextResponse` mapping hidden behind seam. Each route becomes a 3-line adapter declaring its config + validation. `execute` sanitizers become private helpers, not exported functions.
- **Benefits:**
  - **Locality:** Auth + error + envelope translation fixed once, fixed everywhere.
  - **Leverage:** One behavior behind small interface — 11 callers get correct proxy for free.
  - **Tests:** Replace 11 handler unit tests with tests at deep module interface (`proxy` with stubbed `callBackend`); assert on observable outcome (status + envelope), not internal fetch calls.
- **Dependencies:** Remote but owned — frontend → Spring Boot is owned service across network. Port at seam, HTTP adapter for prod, in-memory adapter (stub `callBackend`/`fetch`) for tests. Internal helpers (`sanitizeHeaders`) stay private — not exposed for testing.
- **Interface sketch (not proposal, just constraint):** `proxy(req: Request, ctx: { backendPath: string, validate?: (body:any)=>string|null }) → NextResponse`

### Candidate 2 — Typed Forward Contract (`processForwardApi` + `CommonUtil.processRequest`) — Depth **1**

- **Files:** `gateway-service/src/main/java/com/example/gatewayservice/service/ApiGatewayServices.java` (247 LOC, `processForwardApi(Map<String,Object>)`), `gateway-service/src/main/java/com/example/gatewayservice/util/CommonUtil.java` (67 LOC, `processRequest(String, HttpHeaders, Object) → Map`), `gateway-service/src/main/java/com/example/gatewayservice/controller/GatewayController.java` (58 LOC, `forwardApi(@PathVariable String path, @RequestBody Object)`)
- **Problem:** The seam between controller and service is `Map<String,Object>` with string keys `"path"`, `"pathName"`, `"requestParam"`, `"httpHeaders"`, `"requestBody"`. Every access is `Map path = (Map) request.get("path")` with unchecked casts. Query parsing manually splits on `&`/`=` without decoding, header allowlist checked via `splitConfig(apiGateway.getHeader())` per request. **Depth 1** — interface complexity (remember 5 string keys + cast discipline) equals implementation. Bugs hide in how callers assemble the map, not in parsing itself. `CommonUtil.gson` even exposes `excludeFieldsWithoutExposeAnnotation()` globally. No type safety, no IDE nav, hard to test past interface.
- **Solution:** Deepen into a **GatewayForward** module with typed interface: controller parses `path` + query + headers into a `ForwardRequest(pathName, queryParams, headers, body)` value object; service takes `ForwardRequest` directly. `CommonUtil` query/header parsing becomes private implementation inside module, not shared util. Split out URL decoding, header filtering, and config splitting as internal helpers.
- **Benefits:**
  - **Locality:** All forward-path parsing, encoding (URLEncoder), and header filtering in one place.
  - **Leverage:** Callers just pass typed object — one impl pays back across `processForwardApi` + any future forward paths.
  - **Tests:** Test at module interface with typed inputs (including encoded `%3F` query the controller currently re-encodes); survive internal refactors. Old `CommonUtil` pure-function tests become waste once interface tests exist — delete them per "replace, don't layer."
- **Dependencies:** In-process + Local-substitutable — pure computation + JPA lookup (`ApiGatewayRepository.findByApiIdentifier`, testable with H2). No adapter needed; test through interface directly.

### Candidate 3 — Error/Response Translation (`CommonUtil.applyError` + `Response.setSuccess` repeated) — Depth **1–2**

- **Files:** `gateway-service/src/main/java/com/example/gatewayservice/util/CommonUtil.java` (`applyError(Response, Exception)`), `gateway-service/src/main/java/com/example/gatewayservice/models/rqrs/Response.java` (88 LOC), `gateway-service/src/main/java/com/example/gatewayservice/service/ApiGatewayServices.java` (5 methods each `try{...}catch(e){log.error; CommonUtil.applyError; return rs}`), `gateway-service/src/main/java/com/example/gatewayservice/service/StoreService.java` (5 methods same pattern), `gateway-service/src/main/java/com/example/gatewayservice/exception/definition/*` + `CommonException.java`
- **Problem:** Every service method manually constructs `Response<Object>`, calls `rs.setSuccess(...)`, catches `Exception`, logs, then `CommonUtil.applyError(rs, e)`. The error translation (matching `CommonException` → `httpStatus`/`code`/`errorMessage`, else `99`/`InternalServerError`) is shallow — callers must remember the pattern. New service will copy it. **Depth 1** — interface (call `applyError` correctly) as complex as impl.
- **Solution:** Deepen into a **GatewayResult** module (or Spring `@ControllerAdvice` + typed `Result<T>`). Small interface: service returns `Result<T>` or throws `CommonException`; a single adapter translates to `Response`/HTTP outside the service. Services lose all `try/catch` + `CommonUtil` calls.
- **Benefits:** Locality (one place for `code`/`99`/`errorMessage` mapping), Leverage (every service gets correct translation), tests at result interface assert on HTTP status + envelope, not on each service repeating it.
- **Dependencies:** In-process — pure mapping, no I/O. Direct test.

### Candidate 4 — Auth Seam (`JwtUtil` + `JwtRequestFilter` + `SecurityConfig` + `TokenBlacklistService` + `LoginAttemptService`) — Depth **2**

- **Files:** `gateway-service/src/main/java/com/example/gatewayservice/util/JwtUtil.java` (81 LOC, `getSignInKey()` reads `SystemPropertiesServices.getProps("TOKEN_SECRET_KEY")` per call), `gateway-service/src/main/java/com/example/gatewayservice/config/security/JwtRequestFilter.java` (93 LOC), `gateway-service/src/main/java/com/example/gatewayservice/config/security/SecurityConfig.java` (91 LOC), `gateway-service/src/main/java/com/example/gatewayservice/service/security/TokenBlacklistService.java` (42 LOC), `LoginAttemptService.java` (39 LOC), `AuthUserService.java` (140 LOC)
- **Problem:** Auth is fragmented across 6 shallow modules. Each hides little — `JwtUtil` just wraps `Jwts.builder()`, filter just extracts header, services just wrap repos. Caller must know issuance + validation + blacklist + throttling as separate interfaces. `JwtUtil` decodes Base64 secret on every call (leaky invariant: `SystemProperties` holds secret as string). Hard to test auth flow end-to-end because bugs hide in composition, not in any one pure function.
- **Solution:** Deepen into **AuthGateway** module — single interface `issue(username) → token`, `verify(token) → principal | throw`, `revoke(token)` — hiding secret management, JTI, expiry, blacklist, and attempt throttling behind seam. Filter and `SecurityConfig` become thin adapters.
- **Benefits:** Locality (secret rotation, expiry unit `*1000`, JTI, blacklist all in one place), Leverage (filter + login + logout share one auth impl), tests at interface with in-memory clock/blacklist adapter; `JwtUtil` unit tests replaced.
- **Dependencies:** Local-substitutable — JPA `TokenLog`/`SystemProperties` via H2 in tests; no need to expose repo seam externally.

### Candidate 5 — HomePage God Module — Depth **2**

- **Files:** `frontend/src/app/home/page.tsx` (213 LOC, tabs + 2 tables + modals + `Promise.all([getApis(), getStores()])` + delete handlers + toast), `frontend/src/components/AddApiModal.tsx` (104 LOC), `AddStoreModal.tsx` (62 LOC), `frontend/src/lib/api.ts` (144 LOC), `frontend/src/types.ts` (75 LOC)
- **Problem:** `HomePage` mixes state for 2 domains, data loading, table rendering, and navigation. Its interface is React component props (none), but implementation leaks domain details (maps `GatewayListRs`/`StoreRs` directly). Extracted `AddApiModal` is shallow — just forms that reconstruct `SaveApiPayload` with hard-coded `host: "https://api.thecatapi.com"` and `header: "Content-Type"` — host hardcoded in UI, not config. Changes like "host becomes selectable" touch modal + page + `api.ts` + type. **Depth 2** — some leverage from `lib/api.ts` helpers, but God component still drags.
- **Solution:** Deepen into **GatewayCatalog** + **StoreCatalog** modules (or single **DashboardCatalog**). Small interface: `useCatalog() → { apis, stores, loading, error, refresh() }` hook hiding `Promise.all`, error mapping, and cache. Tables and modals become adapters consuming that interface. Host/config moves to a seam (`CatalogConfig` provider) not hard-coded modal.
- **Benefits:** Locality (data-fetch + error + refresh in one hook), Leverage (home, detail pages reuse same catalog logic), tests at hook interface survive table refactors.
- **Dependencies:** Remote but owned (catalog reads are prod HTTP, test in-memory adapter stubbing `getApis`/`getStores`).

### Candidate 6 — HttpServices Upstream Adapter — Depth **2**

- **Files:** `gateway-service/src/main/java/com/example/gatewayservice/service/HttpServices.java` (45 LOC, `RestTemplate` with `SimpleClientHttpRequestFactory`, fixed 5s/30s timeouts, verbose `log.info` per call), `ApiGatewayServices.processForwardApi` caller
- **Problem:** **Shallow adapter** — interface `invokeUrl(url, method, headers, body) → ResponseEntity<Object>` exposes everything `RestTemplate.exchange` does but hides nothing. Timeouts hard-coded, no retry, no circuit-breaker, logs on success unconditionally. With one adapter (only `RestTemplate`) it's a hypothetical seam — cost without benefit. Testing `processForwardApi` requires mocking `RestTemplate` because seam is at wrong place.
- **Solution:** Either keep as deep module (merge into GatewayForward, test with mock adapter) or split into **UpstreamPort** (interface at `GatewayForward` seam) with two adapters: `RestTemplateAdapter` (prod) and `InMemoryAdapter` (test). Don't expose `HttpServices` directly to callers — it's internal to forward module.
- **Benefits:** If two adapters justified, real seam pays: tests don't touch HTTP, prod keeps `RestTemplate`. If one adapter, delete module — inline `RestTemplate` in forward module.
- **Dependencies:** True external (upstream `thecatapi.com` you don't control) — mock adapter for tests.

---

## 4. Reuse — Existing Utilities to Prefer

- **Frontend:** `lib/backend.ts:callBackend` (centralized `fetch` + `AbortController` timeout), `lib/session.ts:getSessionToken`/`sessionCookieOptions`, `lib/api.ts:request<T>` + `ApiError`, `middleware.ts` guard pattern.
- **Backend:** `ApiGatewayRepository`/`StoreAccountRepository`/`UserRepository`/`UserStoreRRepository`, `Response<T>` envelope, `CommonException` hierarchy, `SystemPropertiesServices.getProps`.
- **Tests:** `frontend/src/lib/__tests__/api.test.ts`, `gateway-service/.../service/*Test.java`, `frontend/playwright.config.ts`.
- **Infra:** `project/docker-compose.yml`, `gateway-service/src/main/resources/data.sql` local seed.

_Grep before adding — re-implementing a helper one folder over is most common waste._

---

## 5. Steps — Picks Locked: 1 + 2 in Grilling

- [x] **User picks 1–2 candidates** → **1 (BFF Proxy) + 2 (Typed Forward)** — sequential grilling, highest leverage pair (frontend + backend end-to-end forward path)
- [x] **Lazily created `specs/tech-architecture/tech-stack.md`** — terms `BffGateway`, `GatewayForward`, `ForwardRequest`, `UpstreamPort` canonized per CONTEXT-FORMAT.md
- [x] **Grilling loop done for 1+2** — decisions locked: Sanitizers private in BFF, Host validation backend-only, Query tolerant + fix once, Naming `BffGateway`/`GatewayForward` keep, Hybrid **A+C+D** picked
- [x] **Design-it-twice complete** — 4 variants (A minimal, B flexible, C common-case, D ports-first) compared; hybrid **A+C+D** recommended and accepted: `bffProxy(req, endpoint)` + private `bffGet`/`bffPost` facades; `forward(ForwardRequest)` external, internal `UpstreamPort` (RestTemplate prod / InMemory test)
- [x] **`specs/tech-architecture/tech-stack.md` finalized** with locked interfaces (see §7b)
- [x] **Deepen slice implementation** — smallest diff: BFF Gateway (`src/lib/bffGateway.ts` hybrid A+C+D) + GatewayForward (`ForwardRequest.java`, `CommonUtil.toForwardRequest`, `GatewayController.forwardApi→ForwardRequest`, `ApiGatewayServices.processForwardApi(ForwardRequest)` + Map wrapper) + adapters thinned (9 `/gw/*` routes), host validation moved backend-only, query tolerant decode fixed
- [x] **Import-boundary hygiene** — `test -f specs/import-boundaries.json` → absent (no `scripts/lib/*.sh` source edges) — skip `check-import-boundaries.sh`
- [x] **Verify** — `npm run lint` clean, `npm run typecheck` clean, `npm test` 15/15, `npm run build` success, `./mvnw test` 37/0 BUILD SUCCESS

_Deferred:_ Candidate 3 (Error translation) next ROI (10 methods repeat `try/catch`→`applyError`), then 4/5/6.

---

## 6. Verification

**Per-candidate verification (interface is test surface):**

| Candidate | How to test | Commands |
| ----------- | ------------- | ---------- |
| 1 — BFF Proxy | New tests at `proxy()` interface: stub `callBackend`, assert `NextResponse` status + JSON for authenticated/unauthenticated/invalid envelope cases. Delete per-handler boilerplate tests. | `cd frontend && npm run lint && npm run typecheck && npm test` |
| 2 — Typed Forward | New tests at `GatewayForward(ForwardRequest)` with H2 `ApiGateway` stub; include `%3F` encoded query, header allowlist filtering, `requireRequestParam/Body` branches. Delete `CommonUtil` map-based tests. | `cd gateway-service && ./mvnw test` |
| 3 — Error translation | Tests at `Result<T>`/`@ControllerAdvice` seam: service returns/throws, adapter asserts envelope `code`/`errorMessage`/`httpStatus`. | `cd gateway-service && ./mvnw test` |
| 4 — Auth | Tests at `AuthGateway.verify/issue/revoke` with controllable clock + in-memory blacklist; filter becomes thin adapter tested via MockMvc. | `cd gateway-service && ./mvnw test -Dtest=JwtRequestFilterTest` |
| 5 — Catalog hook | Tests at `useCatalog` hook via Vitest + mocked `lib/api`; assert loading/error/refresh observable outcomes. | `cd frontend && npm test` |
| 6 — Upstream | If port introduced: prod `RestTemplateAdapter` vs `InMemoryAdapter` swap; `processForwardApi` tested without HTTP. If kept single adapter: no new test seam. | `cd gateway-service && ./mvnw test` |

**Full regression after any deepening:**

```bash
cd frontend && npm run lint && npm run typecheck && npm run build
cd gateway-service && ./mvnw test
cd frontend && npm run test:e2e   # full-stack, H2 + dev servers auto-started
# If modules split/merged with new source edges:
test -f specs/import-boundaries.json && bash scripts/check-import-boundaries.sh
```

**Manual:** Exercise forward path via UI at `http://localhost:3000` (creds `ario_test / password123`) + try `/api/<identifier>` detail → Try-It; confirm `httpOnly` cookie still set, middleware still guards, upstream `thecatapi.com` (or seeded mock) returns through typed path.

---

## 7. Grilling — Decisions Locked for Picks 1 + 2

> **User decisions (2026-08-25):** Sanitizers = Private in BFF (plan) · Host validation = Backend only (deduplicate, remove BFF `save/route.ts:validate` host check) · Query = Tolerant + fix once (keep `foo=""` compat, canonical encode only in `GatewayForward`) · Naming = keep `BffGateway`/`GatewayForward`/`ForwardRequest` · Variants = spawn `design-it-twice` (see §7b).

### Problem space framing (Step 1 of INTERFACE-DESIGN)

**Problem space framing (Step 1 of INTERFACE-DESIGN):** The forward path is currently two shallow seams: 11 BFF handlers each mixing auth+proxy+validation, and a `Map<String,Object>` backend seam that hides types. Any new interface must:

- Keep `callBackend`'s `AbortController` timeout (20s) and `httpOnly` cookie auth hidden behind `BffGateway` seam.
- Replace `Map` seam with typed `ForwardRequest` without exposing `HttpServices` directly to callers — `UpstreamPort` stays internal until two adapters justified.
- Preserve back-compat: `execute` query is today `encodeURIComponent(identifier) + %3F + encodeURIComponent(k)=encodeURIComponent(v)` in BFF, then re-parsed in `CommonUtil.parseQueryString` (tolerant, no decode) and re-encoded via `URLEncoder` in `ApiGatewayServices`. Fix double-encode once in `GatewayForward`.
- Minimal file churn: BFF deep module lives in `frontend/src/lib/backend.ts` (or new `frontend/src/lib/bffGateway.ts`); backend deep module touches only `GatewayController` + `ApiGatewayServices` + new `ForwardRequest.java`; `CommonUtil` query logic becomes private impl.

**Dependency categories:**

- Pick 1: **Remote but owned** (frontend → Spring Boot you own) → port at `BffGateway` seam, HTTP adapter = `callBackend`/`fetch`, in-memory adapter = stubbed `fetch` for interface tests.
- Pick 2: **In-process + Local-substitutable** (pure parsing + JPA `ApiGatewayRepository` via H2) → no external port, test through interface directly.
- Pick 2 internal `UpstreamPort`: **True external** (upstream `thecatapi.com`) → mock adapter for tests, but seam is internal so don't expose it at `GatewayForward` interface.

**What sits behind seam vs. exposed:**

- Behind `BffGateway`: `getSessionToken`, `callBackend`, `envelopeError`, `NextResponse` mapping, `sanitizeHeaders`/`sanitizeParams`, `ALLOWED_METHODS` check, `validate(save)` host/path checks.
- Exposed: `proxy(req, { backendPath, validate? }) → NextResponse` — adapters just declare config.
- Behind `GatewayForward`: `ForwardRequest` parsing (URL decode, header allowlist `splitConfig(apiGateway.getHeader())`, param allowlist, `URLEncoder` query build), `requireRequestParam`/`requireRequestBody` guards, `httpServices.invokeUrl`.
- Exposed: `processForwardApi(ForwardRequest) → Response<Object>`.

**Grill answers locked:**

- **1a ✓** Private in BFF — `sanitizeHeaders`/`sanitizeParams` stay private helpers inside `BffGateway`, not exposed at interface; backend still does `ApiGateway.header`/`param` allowlist second-pass.
- **1b ✓** Backend only — remove host `http(s)://` + `@` check from `frontend/src/app/gw/apis/save/route.ts:validate`, keep only in `GatewayForward.validateSaveApi`.
- **2a/2b ✓** Tolerant + fix once — `ForwardRequest` parser keeps `?foo` → `""` behavior (via tolerant split), but canonical `URLEncoder` lives only in `GatewayForward`; BFF's `execute` stops per-param `encodeURIComponent`, just forwards raw query map.
- **Naming ✓** Keep `BffGateway`/`GatewayForward`/`ForwardRequest`/`UpstreamPort` as canon in `specs/tech-architecture/tech-stack.md`.
- **Variants ✓** Spawn 4 design-it-twice variants — see §7b below.

### 7b. Interface Variants — Design It Twice (Step 2 & 3 of INTERFACE-DESIGN)

> Constraints any variant must satisfy (from §7 framing): `BffGateway` must hide `getSessionToken`→401, `callBackend`+20s Abort, `envelopeError` mapping; `GatewayForward` must take typed `ForwardRequest` (not `Map`), keep tolerant parse compat, and own canonical encoding; `UpstreamPort` stays internal to `GatewayForward` (single-adapter = no external seam).

#### Variant A — Minimal interface (1–3 entry points, maximise leverage per entry)

**Agent 1 brief: minimise interface.**

```ts
// frontend — BffGateway (single entry)
export function bffProxy(req: Request, endpoint: { backendPath: string, method?: string, body?: unknown, params?: Record<string,string>, validate?: (raw:any)=>string|null }): Promise<NextResponse>
```

```java
// backend — GatewayForward (single entry)
public record ForwardRequest(String pathName, Map<String,String> queryParams, HttpHeaders headers, Object body) {}
public Response<Object> forward(ForwardRequest req);
```

- **Usage:** Every `/gw/**` adapter is 3 lines: `return bffProxy(req, {backendPath: "/api/gateway/getApiList", validate})`; controller does `ForwardRequest fr = ForwardRequest.parse(path, headers, body); return gatewayForward.forward(fr);`.
- **Hides:** All auth/envelope/sanitize/encode inside. Callers learn one function per tier.
- **Deps:** 1: Remote but owned (stub fetch), 2: In-process + Local-substitutable (H2), internal `UpstreamPort` = private `RestTemplate` inline (no external adapter — hypothetical seam not created).
- **Trade:** Highest leverage, smallest to learn; thinner flex — every domain maps to same `forward()` so domain-specific errors (e.g., `storeName too long`) surface as generic envelope `04` without typed hint.

#### Variant B — Maximal flexibility (many use cases, extension hooks)

```ts
type BffEndpoint = {
  backendPath: string | ((req:Request, params:any)=>string)
  method?: "GET"|"POST"|"PUT"|"DELETE"|"PATCH"
  requireAuth?: boolean
  validate?: (raw:any)=>string|null
  mapRequest?: (raw:any)=>{body?:unknown, params?:Record<string,string>, headers?:Record<string,string>}
  mapResponse?: (envelope:ApiEnvelope)=>NextResponse
  timeoutMs?: number
}
export function createBffGateway(opts?: { callBackend?: typeof callBackend, getToken?: typeof getSessionToken }): (req:Request, ep:BffEndpoint)=>Promise<NextResponse>
```

```java
public interface GatewayForward {
  Response<Object> forward(ForwardRequest req);
  Response<Object> forwardWithContext(ForwardRequest req, ApiGateway config, ForwardContext ctx); // ctx carries allowlists+flags
}
public interface UpstreamPort { ResponseEntity<Object> invoke(String url, HttpMethod m, HttpHeaders h, Object b); }
// GatewayForward takes UpstreamPort as ctor-injected port
```

- **Usage:** Adapters can customise `mapRequest`/`mapResponse` per endpoint (e.g., `execute` needs raw body passthrough); backend `forwardWithContext` lets future forward types reuse same port.
- **Hides:** Same as A, but seam exposes hooks so future endpoints don't require changing `BffGateway`.
- **Deps:** B has explicit **Ports & Adapters** for both tiers — `UpstreamPort` seam is real (RestTemplate prod + InMemory test adapters). BFF seam also injectable (`callBackend` stub) for tests — two adapters real.
- **Trade:** Most future-proof; lowest depth — interface ~ as complex as impl for uncommon hooks; tests must learn `mapRequest` contract.

#### Variant C — Common case trivial (optimise default caller)

```ts
// BFF — convenience facades, common case trivial
export async function bffGet(backendPath:string): Promise<NextResponse>
export async function bffPost(backendPath:string, body:unknown, validate?: (b:any)=>string|null): Promise<NextResponse>
// Internally both delegate to private proxy()
// Routes: return bffGet("/api/gateway/getApiList")
export async function bffExecute(path:string, payload: ExecuteApiRequest): Promise<NextResponse> // only complex one keeps sanitizers
```

```java
// Backend — domain-typed facades that wrap typed ForwardRequest internally
public Response<Object> getApiList() // no ForwardRequest needed
public Response<Object> getDetail(String apiIdentifier)
public Response<Object> saveApi(SaveApiRequest req)
public Response<Object> forwardApi(ForwardRequest req) // only forward keeps raw seam
```

- **Usage:** 80% of routes (list/detail/save/delete) become one-liners `bffGet`/`bffPost`; only `execute` (needs sanitize + query build) uses full power. Backend keeps existing domain methods (`getListGateways`, `saveApi`) but they internally delegate to shared `ForwardRequest` parsing where overlap.
- **Hides:** Same, but default caller learns just `bffGet`/`bffPost`, not a config object.
- **Deps:** In-process/local-substitutable — no new ports; test trivial helpers via stubbed `fetch`.
- **Trade:** Best ergonomics for current 11 adapters (most are simple list/save/delete); adds thin convenience layer that is itself shallow (risk: 2 helpers ≈ 2 shallow modules if not careful — keep them as private facades, not exported seams).

#### Variant D — Ports & adapters first (cross-seam dependencies explicit)

```ts
// BFF — explicit port
export interface BackendPort { call(path:string, opts:CallOptions): Promise<BackendResponse> }
export class BffGateway {
  constructor(private port: BackendPort) {}
  proxy(req:Request, ep:EndpointConfig): Promise<NextResponse>
}
// prod: new BffGateway(new FetchBackendPort()), test: new BffGateway(new InMemoryBackendPort(stub))
```

```java
public interface UpstreamPort { ResponseEntity<Object> invoke(ForwardRequest req, ApiGateway config); }
public class GatewayForward {
  public GatewayForward(UpstreamPort upstream, ApiGatewayRepository repo) {}
  public Response<Object> forward(ForwardRequest req) // internally resolves ApiGateway then delegates to port
}
// prod binds RestTemplateAdapter, test binds InMemoryAdapter returning canned Json
```

- **Usage:** Both tiers construct deep module with port injected at edge (`route.ts` or Spring `@Bean`). Business logic in `proxy()`/`forward()` fully isolated from transport.
- **Hides:** Transport (fetch/RestTemplate) behind port; logic stays deep even across network boundary — exactly DEEPENING.md category 3 & 4.
- **Deps:** Remote but owned (frontend→Spring) + True external (Spring→thecatapi) — both get real second adapter (in-memory), so seam is justified.
- **Trade:** Cleanest separation for testing (no mocking `fetch`/`RestTemplate` globally — just inject port); most ceremony for current single-adapter reality — today only one prod adapter exists, so seam adds indirection until test adapter proves value.

#### Comparison (depth / locality / seam)

| Variant | Depth | Locality | Seam | When to use |
| --------- | ------- | ---------- | ------ | ------------- |
| **A Minimal** | ★★★★★ — one entry, max leverage | High — fix once | Internal `UpstreamPort` stays private (hypothetical seam not exposed) | Want smallest learning curve; happy with generic envelope errors |
| **B Flexible** | ★★ — config object leaks complexity | Medium — hooks spread | Two real external ports (explicit) | Expect many new forward types / per-endpoint mapping |
| **C Common-case** | ★★★★ — trivial default + power for execute | High for 80% routes | Convenience facades private | 11 adapters today are simple; optimise default |
| **D Ports-first** | ★★★ — good depth, but port injection leaks | Highest locality (transport isolated) | Two real ports, both internal-external | Need in-memory adapters now (e.g., contract tests for upstream) |

#### Recommendation — Hybrid **A + C ergonomics + D's internal port** (opinionated)

- **Ship A as primary interface** — single `bffProxy(req, endpoint)` + single `forward(ForwardRequest)` — max depth, minimal to learn.
- **Layer C's trivial facades as private helpers** inside `BffGateway` (`bffGet`/`bffPost` delegate to `bffProxy`) — keep them unexported convenience, not second seam, so common case is one-liner without leaking config.
- **Adopt D's port internally but not externally** — `UpstreamPort` lives _inside_ `GatewayForward` as private field (not constructor param in public interface); prod binds `RestTemplate`, tests inject `InMemory` via package-private setter or test `@Bean`. External interface stays `forward(ForwardRequest)` (A), internal gets two adapters (D) without exposing port to callers.
- **Defer B's `mapRequest`/`mapResponse` hooks** — YAGNI until a second forward type truly needs custom mapping; add when second caller appears (one adapter = hypothetical).

> This hybrid scores **depth 4–5** (small external iface), **locality high**, **seam justified** (internal second adapter for upstream contract tests), and follows deletion test: delete `BffGateway`, auth+envelope+sanitize reappears across 11 callers.

**Next step after your pick:** lock hybrid (or variant) as the plan's interface, update `specs/tech-architecture/tech-stack.md` verbatim signatures, then submit for `plannotator` review.

---

## 8. Risks / Assumptions

- No ADRs exist to conflict — candidates don't re-litigate prior decisions; if one does, it will be marked with ADR-conflict note in grilling.
- No `import-boundaries.json` exists yet — section 5 hygiene only triggers if deepening introduces new `source` edges in `scripts/lib/*.sh` (unlikely for this repo; noted for e45s14 verifiability).
- Assumes `local` H2 profile stays for tests — enables Local-substitutable category without new stand-ins.
- `execute` path currently double-encodes query (`encodeURIComponent` in BFF then `URLEncoder` in backend) — deepening 1+2 should fix canonical encoding in one place.

---

## 9. Files to Modify (Template — Narrows After Pick)

| Pick | Files that will change |
| ------ | ------------------------ |
| 1 — BFF Proxy | `frontend/src/lib/backend.ts` (deep module), `frontend/src/app/gw/**` (handlers thin to adapters), `frontend/src/lib/__tests__/**` (interface tests) |
| 2 — Typed Forward | `gateway-service/.../service/ApiGatewayServices.java`, `.../util/CommonUtil.java`, `.../controller/GatewayController.java`, new `ForwardRequest.java` value object |
| 3 — Error translation | `.../util/CommonUtil.java`, `.../models/rqrs/Response.java`, `.../service/*.java` (remove boilerplate), new `GatewayResult.java` or `Advice` |
| 4 — Auth | `.../util/JwtUtil.java`, `.../config/security/*`, `.../service/security/*` |
| 5 — Catalog | `frontend/src/app/home/page.tsx`, `frontend/src/lib/api.ts`, `frontend/src/hooks/*` or `frontend/src/lib/catalog.ts` |
| 6 — Upstream | `.../service/HttpServices.java` (either deleted or deepened as internal adapter) |
