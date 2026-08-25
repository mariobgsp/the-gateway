import { NextResponse } from "next/server";
import { bffProxy } from "@/lib/bffGateway";

export const runtime = "nodejs";

interface RouteContext {
  params: Promise<{ id: string }>;
}

export async function GET(_request: Request, context: RouteContext) {
  const { id } = await context.params;
  const storeId = Number.parseInt(id, 10);
  if (!Number.isInteger(storeId) || storeId <= 0) {
    return NextResponse.json(
      { status: "error", code: "04", message: "invalid store id" },
      { status: 400 },
    );
  }

  return bffProxy({
    backendPath: "/api/store/getDetail",
    method: "POST",
    params: { store_id: String(storeId) },
  });
}
