import { NextResponse } from "next/server";
import { callBackend, envelopeError } from "@/lib/backend";
import { getSessionToken } from "@/lib/session";

export const runtime = "nodejs";

interface RouteContext {
  params: Promise<{ identifier: string }>;
}

export async function GET(_request: Request, context: RouteContext) {
  const token = await getSessionToken();
  if (!token) {
    return NextResponse.json({ status: "error", code: "98", message: "unauthorized" }, { status: 401 });
  }

  const { identifier } = await context.params;
  const { httpStatus, envelope } = await callBackend("/api/gateway/getDetailedApi", {
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
