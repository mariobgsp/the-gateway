import { dispatch } from "@/lib/gwRoutes";

export const runtime = "nodejs";

type Context = { params: Promise<{ path: string[] }> };

async function route(request: Request, context: Context) {
  const { path } = await context.params;
  return dispatch(request.method, path, request);
}

export const GET = route;
export const POST = route;
