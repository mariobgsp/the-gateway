import { NextResponse } from "next/server";
import { callBackend, envelopeError } from "@/lib/backend";
import { getSessionToken } from "@/lib/session";
import type { SaveApiPayload } from "@/types";

export const runtime = "nodejs";

const ALLOWED_METHODS = new Set(["GET", "POST", "PUT", "DELETE", "PATCH"]);

export async function POST(request: Request) {
  const token = await getSessionToken();
  if (!token) {
    return NextResponse.json({ status: "error", code: "98", message: "unauthorized" }, { status: 401 });
  }

  let payload: SaveApiPayload;
  try {
    payload = (await request.json()) as SaveApiPayload;
  } catch {
    return NextResponse.json({ status: "error", code: "04", message: "invalid request body" }, { status: 400 });
  }

  const validationError = validate(payload);
  if (validationError) {
    return NextResponse.json({ status: "error", code: "04", message: validationError }, { status: 400 });
  }

  const { httpStatus, envelope } = await callBackend("/api/gateway/saveApi", {
    method: "POST",
    token,
    body: payload,
  });

  if (!envelope || envelope.code !== "00") {
    return NextResponse.json(
      envelope ?? { status: "error", code: "99", message: envelopeError(envelope, httpStatus) },
      { status: httpStatus >= 400 ? httpStatus : 500 }
    );
  }
  return NextResponse.json(envelope, { status: 200 });
}

function validate(p: SaveApiPayload): string | null {
  if (!p) return "invalid request body";
  if (typeof p.apiIdentifier !== "string" || !p.apiIdentifier.trim()) return "api identifier is required";
  if (typeof p.name !== "string" || !p.name.trim()) return "api name is required";
  if (typeof p.host !== "string" || !/^https?:\/\//i.test(p.host)) return "api host must be a valid http(s) url";
  if (typeof p.path !== "string" || !p.path.startsWith("/")) return "api path must start with /";
  if (typeof p.method !== "string" || !ALLOWED_METHODS.has(p.method.toUpperCase())) return "unsupported http method";
  if (p.apiIdentifier.length > 255 || p.name.length > 255 || p.host.length > 255 || p.path.length > 255) {
    return "field length exceeds maximum allowed";
  }
  return null;
}
