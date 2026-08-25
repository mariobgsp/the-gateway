import { bffProxy } from "@/lib/bffGateway";

export const runtime = "nodejs";

export async function GET() {
  return bffProxy({ backendPath: "/api/store/getList" });
}
