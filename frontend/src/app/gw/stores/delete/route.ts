import { NextResponse } from "next/server";
import { callBackend, envelopeError } from "@/lib/backend";
import { getSessionToken } from "@/lib/session";

export const runtime = "nodejs";

export async function POST(request: Request) {
  const token = await getSessionToken();
  if (!token) {
    return NextResponse.json({ status: "error", code: "98", message: "unauthorized" }, { status: 401 });
  }

  let body: { storeId?: number };
  try {
    body = await request.json();
  } catch {
    return NextResponse.json({ status: "error", code: "04", message: "invalid request body" }, { status: 400 });
  }

  if (!Number.isInteger(body?.storeId) || (body.storeId as number) <= 0) {
    return NextResponse.json({ status: "error", code: "04", message: "invalid store id" }, { status: 400 });
  }

  const { httpStatus, envelope } = await callBackend("/api/store/delete", {
    method: "POST",
    token,
    params: { store_id: String(body.storeId) },
  });

  if (!envelope || envelope.code !== "00") {
    return NextResponse.json(
      envelope ?? { status: "error", code: "99", message: envelopeError(envelope, httpStatus) },
      { status: httpStatus >= 400 ? httpStatus : 500 }
    );
  }
  return NextResponse.json(envelope, { status: 200 });
}
