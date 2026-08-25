import { NextResponse } from "next/server";
import { callBackend, envelopeError } from "./backend";
import { getSessionToken } from "./session";

type HttpMethod = "GET" | "POST" | "PUT" | "DELETE" | "PATCH";

interface BffProxyOptions {
  backendPath: string;
  method?: HttpMethod;
  body?: unknown;
  params?: Record<string, string>;
  headers?: Record<string, string>;
  timeoutMs?: number;
}

/**
 * Deep Module: BffGateway
 * Hides getSessionToken → 401, callBackend, envelopeError → NextResponse mapping.
 * Each /gw/* route is a thin adapter declaring {backendPath, method, validate}.
 * Sanitizers (sanitizeHeaders/sanitizeParams) stay private in BFF — not exposed.
 */
export async function bffProxy(
  options: BffProxyOptions,
): Promise<NextResponse> {
  const {
    backendPath,
    method = "GET",
    body,
    params,
    headers,
    timeoutMs,
  } = options;

  const token = await getSessionToken();
  if (!token) {
    return NextResponse.json(
      { status: "error", code: "98", message: "unauthorized" },
      { status: 401 },
    );
  }

  const { httpStatus, envelope } = await callBackend(backendPath, {
    method,
    token,
    body,
    params,
    headers,
    timeoutMs,
  });

  if (!envelope || envelope.code !== "00") {
    return NextResponse.json(
      envelope ?? {
        status: "error",
        code: "99",
        message: envelopeError(envelope, httpStatus),
      },
      { status: httpStatus >= 400 ? httpStatus : 500 },
    );
  }
  return NextResponse.json(envelope, { status: 200 });
}
