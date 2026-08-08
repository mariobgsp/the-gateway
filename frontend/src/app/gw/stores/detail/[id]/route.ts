import { NextResponse } from "next/server";
import { callBackend, envelopeError } from "@/lib/backend";
import { getSessionToken } from "@/lib/session";

export const runtime = "nodejs";

interface RouteContext {
  params: Promise<{ id: string }>;
}

export async function GET(_request: Request, context: RouteContext) {
  const token = await getSessionToken();
  if (!token) {
    return NextResponse.json({ status: "error", code: "98", message: "unauthorized" }, { status: 401 });
  }

  const { id } = await context.params;
  const storeId = Number.parseInt(id, 10);
  if (!Number.isInteger(storeId) || storeId <= 0) {
    return NextResponse.json({ status: "error", code: "04", message: "invalid store id" }, { status: 400 });
  }

  const { httpStatus, envelope } = await callBackend("/api/store/getDetail", {
    method: "POST",
    token,
    params: { store_id: String(storeId) },
  });

  if (!envelope || envelope.code !== "00") {
    return NextResponse.json(
      envelope ?? { status: "error", code: "99", message: envelopeError(envelope, httpStatus) },
      { status: httpStatus >= 400 ? httpStatus : 500 }
    );
  }
  return NextResponse.json(envelope, { status: 200 });
}
