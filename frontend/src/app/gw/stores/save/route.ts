import { NextResponse } from "next/server";
import { bffProxy } from "@/lib/bffGateway";
import type { SaveStorePayload } from "@/types";

export const runtime = "nodejs";

export async function POST(request: Request) {
  let payload: SaveStorePayload;
  try {
    payload = (await request.json()) as SaveStorePayload;
  } catch {
    return NextResponse.json(
      { status: "error", code: "04", message: "invalid request body" },
      { status: 400 },
    );
  }

  if (
    !payload ||
    typeof payload.storeName !== "string" ||
    !payload.storeName.trim()
  ) {
    return NextResponse.json(
      { status: "error", code: "04", message: "store name is required" },
      { status: 400 },
    );
  }
  if (payload.storeName.length > 255) {
    return NextResponse.json(
      { status: "error", code: "04", message: "store name too long" },
      { status: 400 },
    );
  }

  return bffProxy({
    backendPath: "/api/store/save",
    method: "POST",
    body: {
      storeId: payload.storeId ?? null,
      storeName: payload.storeName.trim(),
      clientId: payload.clientId ?? null,
    },
  });
}
