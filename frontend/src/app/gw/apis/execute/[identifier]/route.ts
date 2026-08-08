import { NextResponse } from "next/server";
import { callBackend } from "@/lib/backend";
import { getSessionToken } from "@/lib/session";
import type { ExecuteApiRequest } from "@/types";

export const runtime = "nodejs";

const ALLOWED_METHODS = new Set(["GET", "POST", "PUT", "DELETE", "PATCH"]);

interface RouteContext {
  params: Promise<{ identifier: string }>;
}

export async function POST(request: Request, context: RouteContext) {
  const token = await getSessionToken();
  if (!token) {
    return NextResponse.json({ status: "error", code: "98", message: "unauthorized" }, { status: 401 });
  }

  const { identifier } = await context.params;
  if (!identifier || identifier.length > 255) {
    return NextResponse.json({ status: "error", code: "04", message: "invalid api identifier" }, { status: 400 });
  }

  let body: ExecuteApiRequest;
  try {
    body = await request.json();
  } catch {
    return NextResponse.json({ status: "error", code: "04", message: "invalid request body" }, { status: 400 });
  }

  const method = typeof body.method === "string" ? body.method.toUpperCase() : "GET";
  if (!ALLOWED_METHODS.has(method)) {
    return NextResponse.json({ status: "error", code: "04", message: "unsupported http method" }, { status: 400 });
  }

  const headers = sanitizeHeaders(body.headers);
  const params = sanitizeParams(body.params);

  const query = Object.entries(params)
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
    .join("&");

  const encodedPath = `${encodeURIComponent(identifier)}${query ? `%3F${query}` : ""}`;

  const forwardHeaders: Record<string, string> = {
    ...headers,
  };
  if (body.body) {
    forwardHeaders["Content-Type"] = "application/json";
  }

  const { httpStatus, envelope } = await callBackend(`/api/gateway/${encodedPath}`, {
    method: method as "GET" | "POST" | "PUT" | "DELETE",
    token,
    headers: forwardHeaders,
    body: body.body ? parseJsonBody(body.body) : undefined,
  });

  if (envelope && envelope.code === "00") {
    const pretty = envelope.data !== undefined ? JSON.stringify(envelope.data, null, 2) : "OK";
    return NextResponse.json({ ok: true, httpStatus: 200, body: pretty }, { status: 200 });
  }

  const message = envelope?.message ?? `Request failed with status ${httpStatus}`;
  return NextResponse.json({ ok: false, httpStatus, body: message }, { status: 200 });
}

function sanitizeHeaders(raw: unknown): Record<string, string> {
  const result: Record<string, string> = {};
  if (!raw || typeof raw !== "object") return result;
  for (const [key, value] of Object.entries(raw)) {
    const name = key.trim();
    if (!name || name.length > 100) continue;
    if (/^(host|content-length|cookie|authorization|connection|accept-encoding|transfer-encoding)$/i.test(name)) continue;
    if (typeof value === "string" && value.length <= 1000) {
      result[name] = value;
    }
  }
  return result;
}

function sanitizeParams(raw: unknown): Record<string, string> {
  const result: Record<string, string> = {};
  if (!raw || typeof raw !== "object") return result;
  for (const [key, value] of Object.entries(raw)) {
    if (key.length > 100) continue;
    if (typeof value === "string" && value.length <= 1000) {
      result[key] = value;
    }
  }
  return result;
}

function parseJsonBody(raw: string): unknown {
  if (!raw.trim()) return undefined;
  try {
    return JSON.parse(raw);
  } catch {
    return raw;
  }
}
