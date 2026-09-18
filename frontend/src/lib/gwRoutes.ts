import { NextResponse } from "next/server";
import { bffProxy, callBackend, forwardProxy } from "@/lib/bffGateway";
import { envelopeError } from "@/lib/bffGateway";
import {
  SESSION_COOKIE,
  clearSessionToken,
  getSessionToken,
  sessionCookieOptions,
} from "@/lib/session";
import type { ExecuteApiRequest, SaveApiPayload } from "@/types";

type Method = "GET" | "POST";
type Params = Record<string, string>;

type RouteContext = {
  request: Request;
  params: Params;
};

type Route = {
  method: Method;
  match: (segments: string[]) => Params | null;
  run: (context: RouteContext) => Promise<NextResponse>;
};

const methods = new Set(["GET", "POST", "PUT", "DELETE", "PATCH"]);

function exact(method: Method, ...expected: string[]): Route {
  return {
    method,
    match: (segments) =>
      segments.length === expected.length &&
      expected.every((segment, index) => segments[index] === segment)
        ? {}
        : null,
    run: async ({ request }) =>
      bffProxy({
        backendPath: backendPath(expected),
        method,
        ...(method === "POST" ? { body: await readBody(request) } : {}),
      }),
  };
}

function backendPath(segments: string[]): string {
  const paths: Record<string, string> = {
    "apis": "/api/gateway/getApiList",
    "stores": "/api/store/getList",
    "apis save": "/api/gateway/saveApi",
    "apis delete": "/api/gateway/deleteApi",
    "stores save": "/api/store/save",
    "stores delete": "/api/store/delete",
    "stores regenerate": "/api/store/regenerateSecret",
  };
  return paths[segments.join(" ")] ?? "";
}

async function readBody(request: Request): Promise<unknown> {
  try {
    return await request.json();
  } catch {
    throw badRequest("invalid request body");
  }
}

function badRequest(message: string): NextResponse {
  return NextResponse.json(
    { status: "error", code: "04", message },
    { status: 400 },
  );
}

function withBody(
  request: Request,
): Promise<Record<string, unknown>> {
  return readBody(request) as Promise<Record<string, unknown>>;
}

function idRoute(
  method: Method,
  resource: "apis" | "stores",
): Route {
  return {
    method,
    match: (segments) =>
      segments.length === 3 &&
      segments[0] === resource &&
      segments[1] === "detail"
        ? { id: segments[2] }
        : null,
    run: async ({ params }) => {
      if (resource === "apis") {
        return bffProxy({
          backendPath: "/api/gateway/getDetailedApi",
          method: "POST",
          params: { api_identifier: params.id },
        });
      }
      const storeId = Number.parseInt(params.id, 10);
      if (!Number.isInteger(storeId) || storeId <= 0) {
        return badRequest("invalid store id");
      }
      return bffProxy({
        backendPath: "/api/store/getDetail",
        method: "POST",
        params: { store_id: String(storeId) },
      });
    },
  };
}

const routes: Route[] = [
  exact("GET", "apis"),
  exact("GET", "stores"),
  idRoute("GET", "apis"),
  idRoute("GET", "stores"),
  {
    method: "POST",
    match: (segments) =>
      segments.length === 2 && segments[0] === "apis" && segments[1] === "save"
        ? {}
        : null,
    run: async ({ request }) => {
      const payload = await withBody(request) as unknown as SaveApiPayload;
      const error = validateApi(payload);
      return error
        ? badRequest(error)
        : bffProxy({ backendPath: "/api/gateway/saveApi", method: "POST", body: payload });
    },
  },
  {
    method: "POST",
    match: (segments) =>
      segments.length === 2 && segments[0] === "apis" && segments[1] === "delete"
        ? {}
        : null,
    run: async ({ request }) => {
      const body = await withBody(request);
      const identifier = typeof body.apiIdentifier === "string" ? body.apiIdentifier.trim() : "";
      return identifier
        ? bffProxy({ backendPath: "/api/gateway/deleteApi", method: "POST", params: { api_identifier: identifier } })
        : badRequest("api identifier is required");
    },
  },
  {
    method: "POST",
    match: (segments) =>
      segments.length === 2 && segments[0] === "stores" && segments[1] === "save"
        ? {}
        : null,
    run: async ({ request }) => {
      const body = await withBody(request);
      if (typeof body.storeName !== "string" || !body.storeName.trim()) {
        return badRequest("store name is required");
      }
      return bffProxy({ backendPath: "/api/store/save", method: "POST", body });
    },
  },
  ...["delete", "regenerate"].map((action) => ({
    method: "POST" as const,
    match: (segments: string[]) =>
      segments.length === 2 && segments[0] === "stores" && segments[1] === action
        ? {}
        : null,
    run: async ({ request }: RouteContext) => {
      const body = await withBody(request);
      const storeId = body.storeId;
      if (!Number.isInteger(storeId) || (storeId as number) <= 0) {
        return badRequest("invalid store id");
      }
      return bffProxy({
        backendPath: action === "delete" ? "/api/store/delete" : "/api/store/regenerateSecret",
        method: "POST",
        params: { store_id: String(storeId) },
      });
    },
  })),
  {
    method: "POST",
    match: (segments) =>
      segments.length === 3 && segments[0] === "apis" && segments[1] === "execute"
        ? { identifier: segments[2] }
        : null,
    run: async ({ request, params }) => execute(params.identifier, request),
  },
  {
    method: "POST",
    match: (segments) =>
      segments.length === 2 && segments[0] === "auth" && segments[1] === "login"
        ? {}
        : null,
    run: async ({ request }) => login(request),
  },
  {
    method: "POST",
    match: (segments) =>
      segments.length === 2 && segments[0] === "auth" && segments[1] === "logout"
        ? {}
        : null,
    run: logout,
  },
];

export async function dispatch(
  method: string,
  segments: string[],
  request: Request,
): Promise<NextResponse> {
  if (method !== "GET" && method !== "POST") {
    return badRequest("unsupported http method");
  }
  const route = routes.find((candidate) => candidate.method === method && candidate.match(segments));
  if (!route) {
    return NextResponse.json({ status: "error", code: "02", message: "not found" }, { status: 404 });
  }
  try {
    const params = route.match(segments) ?? {};
    return await route.run({ request, params });
  } catch (error) {
    return error instanceof Response ? (error as NextResponse) : badRequest("invalid request body");
  }
}

function validateApi(payload: SaveApiPayload): string | null {
  if (!payload) return "invalid request body";
  if (typeof payload.apiIdentifier !== "string" || !payload.apiIdentifier.trim()) return "api identifier is required";
  if (typeof payload.name !== "string" || !payload.name.trim()) return "api name is required";
  if (typeof payload.host !== "string" || !payload.host.trim()) return "api host is required";
  if (typeof payload.path !== "string" || !payload.path.startsWith("/")) return "api path must start with /";
  if (typeof payload.method !== "string" || !methods.has(payload.method.toUpperCase())) return "unsupported http method";
  if ([payload.apiIdentifier, payload.name, payload.host, payload.path].some((value) => value.length > 255)) return "field length exceeds maximum allowed";
  return null;
}

async function execute(identifier: string, request: Request): Promise<NextResponse> {
  if (!identifier || identifier.length > 255) return badRequest("invalid api identifier");
  const body = await withBody(request) as unknown as ExecuteApiRequest;
  return NextResponse.json(await forwardProxy(identifier, body));
}

async function login(request: Request): Promise<NextResponse> {
  const body = await withBody(request);
  const username = typeof body.username === "string" ? body.username.trim() : "";
  const password = typeof body.password === "string" ? body.password : "";
  if (!username || !password) return badRequest("username and password are required");
  if (username.length > 50 || password.length > 72) return badRequest("invalid credentials");
  const { httpStatus, envelope } = await callBackend("/gateway/user/login", { method: "POST", body: { username, password } });
  if (!envelope || envelope.code !== "00" || !envelope.data) {
    return NextResponse.json(envelope ?? { status: "error", code: "99", message: envelopeError(envelope, httpStatus) }, { status: httpStatus >= 400 ? httpStatus : 401 });
  }
  const data = envelope.data as { accessToken?: string; tokenLifetime?: string };
  if (!data.accessToken) return NextResponse.json({ status: "error", code: "99", message: "login failed" }, { status: 500 });
  const lifetime = Number.parseInt(data.tokenLifetime ?? "3600", 10);
  const response = NextResponse.json({ status: envelope.status, code: envelope.code, message: envelope.message });
  response.cookies.set(SESSION_COOKIE, data.accessToken, sessionCookieOptions(Number.isFinite(lifetime) && lifetime > 0 ? lifetime : 3600));
  return response;
}

async function logout(): Promise<NextResponse> {
  const token = await getSessionToken();
  if (token) await callBackend("/gateway/user/logout", { method: "POST", token });
  await clearSessionToken();
  const response = NextResponse.json({ status: "ok", code: "00", message: "success logout!" });
  response.cookies.set(SESSION_COOKIE, "", sessionCookieOptions(undefined, new Date(0)));
  return response;
}
