import { bffProxy } from "@/lib/bffGateway";

export const runtime = "nodejs";

interface RouteContext {
  params: Promise<{ identifier: string }>;
}

export async function GET(_request: Request, context: RouteContext) {
  const { identifier } = await context.params;
  return bffProxy({
    backendPath: "/api/gateway/getDetailedApi",
    method: "POST",
    params: { api_identifier: identifier },
  });
}
