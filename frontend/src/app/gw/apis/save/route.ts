import { NextResponse } from "next/server";
import { bffProxy } from "@/lib/bffGateway";
import type { SaveApiPayload } from "@/types";

export const runtime = "nodejs";

const ALLOWED_METHODS = new Set(["GET", "POST", "PUT", "DELETE", "PATCH"]);

export async function POST(request: Request) {
  let payload: SaveApiPayload;
  try {
    payload = (await request.json()) as SaveApiPayload;
  } catch {
    return NextResponse.json(
      { status: "error", code: "04", message: "invalid request body" },
      { status: 400 },
    );
  }

  const validationError = validate(payload);
  if (validationError) {
    return NextResponse.json(
      { status: "error", code: "04", message: validationError },
      { status: 400 },
    );
  }

  return bffProxy({
    backendPath: "/api/gateway/saveApi",
    method: "POST",
    body: payload,
  });
}

function validate(p: SaveApiPayload): string | null {
  if (!p) return "invalid request body";
  if (typeof p.apiIdentifier !== "string" || !p.apiIdentifier.trim())
    return "api identifier is required";
  if (typeof p.name !== "string" || !p.name.trim())
    return "api name is required";
  // Host format validated only in GatewayForward (backend) — BFF just checks required (decision: Backend only)
  if (typeof p.host !== "string" || !p.host.trim())
    return "api host is required";
  if (typeof p.path !== "string" || !p.path.startsWith("/"))
    return "api path must start with /";
  if (
    typeof p.method !== "string" ||
    !ALLOWED_METHODS.has(p.method.toUpperCase())
  )
    return "unsupported http method";
  if (
    p.apiIdentifier.length > 255 ||
    p.name.length > 255 ||
    p.host.length > 255 ||
    p.path.length > 255
  ) {
    return "field length exceeds maximum allowed";
  }
  return null;
}
