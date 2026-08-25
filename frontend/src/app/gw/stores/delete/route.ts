import { NextResponse } from "next/server";
import { bffProxy } from "@/lib/bffGateway";

export const runtime = "nodejs";

export async function POST(request: Request) {
  let body: { storeId?: number };
  try {
    body = await request.json();
  } catch {
    return NextResponse.json(
      { status: "error", code: "04", message: "invalid request body" },
      { status: 400 },
    );
  }

  if (!Number.isInteger(body?.storeId) || (body.storeId as number) <= 0) {
    return NextResponse.json(
      { status: "error", code: "04", message: "invalid store id" },
      { status: 400 },
    );
  }

  return bffProxy({
    backendPath: "/api/store/delete",
    method: "POST",
    params: { store_id: String(body.storeId) },
  });
}
