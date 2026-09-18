import { NextResponse } from "next/server";
import type { ExecuteApiRequest, ExecuteApiResult } from "@/types";
import type { ApiEnvelope } from "@/types";
import { getSessionToken } from "./session";

const BACKEND_API_URL = process.env.BACKEND_API_URL ?? "http://localhost:8080";
const METHODS = new Set(["GET", "POST", "PUT", "DELETE", "PATCH"]);

type HttpMethod = "GET" | "POST" | "PUT" | "DELETE" | "PATCH";

type ProxyOptions = {
  backendPath: string;
  method?: HttpMethod;
  body?: unknown;
  params?: Record<string, string>;
  headers?: Record<string, string>;
};

type BackendResponse = { httpStatus: number; envelope: ApiEnvelope | null };

type BackendOptions = {
  method?: HttpMethod;
  token?: string | null;
  body?: unknown;
  params?: Record<string, string>;
  headers?: Record<string, string>;
};

/**
 * BffGateway hides session lookup, backend fetches, timeout handling, and
 * envelope-to-NextResponse translation behind the route adapter seam.
 */
export async function bffProxy(options: ProxyOptions): Promise<NextResponse> {
  const token = await getSessionToken();
  if (!token) return unauthorized();
  const result = await callBackend(options.backendPath, { ...options, token });
  return responseFor(result);
}

/** Executes a configured upstream API without exposing sanitization details. */
export async function forwardProxy(
  identifier: string,
  input: ExecuteApiRequest,
): Promise<ExecuteApiResult> {
  const method = typeof input.method === "string" ? input.method.toUpperCase() : "GET";
  if (!METHODS.has(method)) {
    return { ok: false, httpStatus: 400, body: "unsupported http method" };
  }
  const params = sanitizeParams(input.params);
  const query = Object.entries(params)
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
    .join("&");
  const path = `${encodeURIComponent(identifier)}${query ? `%3F${query}` : ""}`;
  const body = input.body ? parseJsonBody(input.body) : undefined;
  const token = await getSessionToken();
  if (!token) return { ok: false, httpStatus: 401, body: "unauthorized" };
  const { httpStatus, envelope } = await callBackend(`/api/gateway/${path}`, {
    method: method as HttpMethod,
    token,
    headers: input.body ? { ...sanitizeHeaders(input.headers), "Content-Type": "application/json" } : sanitizeHeaders(input.headers),
    body,
  });
  if (envelope?.code === "00") {
    return {
      ok: true,
      httpStatus: 200,
      body: envelope.data === undefined ? "OK" : JSON.stringify(envelope.data, null, 2),
    };
  }
  return { ok: false, httpStatus, body: envelope?.message ?? envelopeError(envelope, httpStatus) };
}

export async function callBackend(path: string, options: BackendOptions = {}): Promise<BackendResponse> {
  const { method = "GET", token, body, params, headers: extraHeaders } = options;
  const url = new URL(path, BACKEND_API_URL);
  for (const [key, value] of Object.entries(params ?? {})) url.searchParams.set(key, value);
  const headers: Record<string, string> = { ...(extraHeaders ?? {}) };
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers["Content-Type"] = "application/json";
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 20000);
  try {
    const res = await fetch(url, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal: controller.signal,
      cache: "no-store",
    });
    let envelope: ApiEnvelope | null = null;
    try {
      envelope = (await res.json()) as ApiEnvelope;
    } catch {
      // Non-envelope upstream responses are represented by null.
    }
    return { httpStatus: res.status, envelope };
  } finally {
    clearTimeout(timeout);
  }
}

function responseFor({ httpStatus, envelope }: BackendResponse): NextResponse {
  if (!envelope || envelope.code !== "00") {
    return NextResponse.json(
      envelope ?? { status: "error", code: "99", message: envelopeError(envelope, httpStatus) },
      { status: httpStatus >= 400 ? httpStatus : 500 },
    );
  }
  return NextResponse.json(envelope, { status: 200 });
}

export function envelopeError(envelope: ApiEnvelope | null, status: number): string {
  return envelope?.message ?? envelope?.errorMessage ?? `Request failed with status ${status}`;
}

function unauthorized(): NextResponse {
  return NextResponse.json({ status: "error", code: "98", message: "unauthorized" }, { status: 401 });
}

function sanitizeHeaders(raw: unknown): Record<string, string> {
  if (!raw || typeof raw !== "object") return {};
  return Object.fromEntries(Object.entries(raw).filter(([key, value]) => {
    const name = key.trim();
    return name && name.length <= 100 && !/^(host|content-length|cookie|authorization|connection|accept-encoding|transfer-encoding)$/i.test(name) && typeof value === "string" && value.length <= 1000;
  }));
}

function sanitizeParams(raw: unknown): Record<string, string> {
  if (!raw || typeof raw !== "object") return {};
  return Object.fromEntries(Object.entries(raw).filter(([key, value]) => key.length <= 100 && typeof value === "string" && value.length <= 1000));
}

function parseJsonBody(raw: string): unknown {
  if (!raw.trim()) return undefined;
  try { return JSON.parse(raw); } catch { return raw; }
}
