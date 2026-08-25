import type { ApiEnvelope } from "@/types";

const BACKEND_API_URL = process.env.BACKEND_API_URL ?? "http://localhost:8080";

export interface BackendResponse {
  httpStatus: number;
  envelope: ApiEnvelope | null;
}

interface CallOptions {
  method?: "GET" | "POST" | "PUT" | "DELETE" | "PATCH";
  token?: string | null;
  body?: unknown;
  params?: Record<string, string>;
  headers?: Record<string, string>;
  timeoutMs?: number;
}

export async function callBackend(
  path: string,
  options: CallOptions = {},
): Promise<BackendResponse> {
  const {
    method = "GET",
    token,
    body,
    params,
    headers: extraHeaders,
    timeoutMs = 20000,
  } = options;

  const url = new URL(path, BACKEND_API_URL);
  if (params) {
    for (const [key, value] of Object.entries(params)) {
      url.searchParams.set(key, value);
    }
  }

  const headers: Record<string, string> = {
    ...(extraHeaders ?? {}),
  };
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }
  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
  }

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);

  try {
    const res = await fetch(url, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
      signal: controller.signal,
      cache: "no-store",
    });

    let envelope: ApiEnvelope | null = null;
    try {
      envelope = (await res.json()) as ApiEnvelope;
    } catch {
      envelope = null;
    }

    return { httpStatus: res.status, envelope };
  } finally {
    clearTimeout(timeout);
  }
}

export function envelopeError(
  envelope: ApiEnvelope | null,
  httpStatus: number,
): string {
  if (envelope?.message) return envelope.message;
  if (envelope?.errorMessage) return envelope.errorMessage;
  return `Request failed with status ${httpStatus}`;
}
