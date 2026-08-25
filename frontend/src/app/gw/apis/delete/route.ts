import { NextResponse } from "next/server";
import { bffProxy } from "@/lib/bffGateway";

export const runtime = "nodejs";

export async function POST(request: Request) {
  let body: { apiIdentifier?: string };
  try {
    body = await request.json();
  } catch {
    return NextResponse.json(
      { status: "error", code: "04", message: "invalid request body" },
      { status: 400 },
    );
  }

  const identifier =
    typeof body.apiIdentifier === "string" ? body.apiIdentifier.trim() : "";
  if (!identifier) {
    return NextResponse.json(
      { status: "error", code: "04", message: "api identifier is required" },
      { status: 400 },
    );
  }

  return bffProxy({
    backendPath: "/api/gateway/deleteApi",
    method: "POST",
    params: { api_identifier: identifier },
  });
}
