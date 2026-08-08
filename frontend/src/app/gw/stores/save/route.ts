import { NextResponse } from "next/server";
import { callBackend, envelopeError } from "@/lib/backend";
import { getSessionToken } from "@/lib/session";
import type { SaveStorePayload } from "@/types";

export const runtime = "nodejs";

export async function POST(request: Request) {
  const token = await getSessionToken();
  if (!token) {
    return NextResponse.json({ status: "error", code: "98", message: "unauthorized" }, { status: 401 });
  }

  let payload: SaveStorePayload;
  try {
    payload = (await request.json()) as SaveStorePayload;
  } catch {
    return NextResponse.json({ status: "error", code: "04", message: "invalid request body" }, { status: 400 });
  }

  if (!payload || typeof payload.storeName !== "string" || !payload.storeName.trim()) {
    return NextResponse.json({ status: "error", code: "04", message: "store name is required" }, { status: 400 });
  }
  if (payload.storeName.length > 255) {
    return NextResponse.json({ status: "error", code: "04", message: "store name too long" }, { status: 400 });
  }

  const { httpStatus, envelope } = await callBackend("/api/store/save", {
    method: "POST",
    token,
    body: {
      storeId: payload.storeId ?? null,
      storeName: payload.storeName.trim(),
      clientId: payload.clientId ?? null,
    },
  });

  if (!envelope || envelope.code !== "00") {
    return NextResponse.json(
      envelope ?? { status: "error", code: "99", message: envelopeError(envelope, httpStatus) },
      { status: httpStatus >= 400 ? httpStatus : 500 }
    );
  }
  return NextResponse.json(envelope, { status: 200 });
}
