import { NextResponse } from "next/server";
import { callBackend, envelopeError } from "@/lib/backend";
import { getSessionToken } from "@/lib/session";

export const runtime = "nodejs";

export async function POST(request: Request) {
  const token = await getSessionToken();
  if (!token) {
    return NextResponse.json({ status: "error", code: "98", message: "unauthorized" }, { status: 401 });
  }

  let body: { apiIdentifier?: string };
  try {
    body = await request.json();
  } catch {
    return NextResponse.json({ status: "error", code: "04", message: "invalid request body" }, { status: 400 });
  }

  const identifier = typeof body.apiIdentifier === "string" ? body.apiIdentifier.trim() : "";
  if (!identifier) {
    return NextResponse.json({ status: "error", code: "04", message: "api identifier is required" }, { status: 400 });
  }

  const { httpStatus, envelope } = await callBackend("/api/gateway/deleteApi", {
    method: "POST",
    token,
    params: { api_identifier: identifier },
  });

  if (!envelope || envelope.code !== "00") {
    return NextResponse.json(
      envelope ?? { status: "error", code: "99", message: envelopeError(envelope, httpStatus) },
      { status: httpStatus >= 400 ? httpStatus : 500 }
    );
  }
  return NextResponse.json(envelope, { status: 200 });
}
