import type {
  ApiEnvelope,
  ApiGateway,
  ExecuteApiRequest,
  ExecuteApiResult,
  GatewayListRs,
  LoginResponse,
  SaveApiPayload,
  SaveStorePayload,
  StoreRs,
} from "@/types";

export class ApiError extends Error {
  constructor(
    message: string,
    readonly code?: string,
    readonly status?: number,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

type HttpMethod = "GET" | "POST" | "PUT" | "DELETE" | "PATCH";

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  let response: Response;
  try {
    response = await fetch(path, {
      ...init,
      headers: { "Content-Type": "application/json", ...init.headers },
    });
  } catch {
    throw new ApiError("Network error - please try again", "99", 0);
  }

  let envelope: ApiEnvelope | null = null;
  try {
    envelope = (await response.json()) as ApiEnvelope;
  } catch {
    // Keep the response error below useful even when the server sent no JSON.
  }
  if (!response.ok || envelope?.code !== "00") {
    throw new ApiError(
      envelope?.message ?? envelope?.errorMessage ?? `Request failed (${response.status})`,
      envelope?.code,
      response.status,
    );
  }
  return envelope.data as T;
}

function call<T>(path: string, method: HttpMethod = "GET", body?: unknown): Promise<T> {
  return request<T>(path, {
    method,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}

export const login = (username: string, password: string): Promise<LoginResponse> =>
  call<LoginResponse>("/gw/auth/login", "POST", { username, password });
export const logout = (): Promise<unknown> => call("/gw/auth/logout", "POST");
export const getApis = (): Promise<GatewayListRs[]> => call("/gw/apis");
export const getApiDetail = (identifier: string): Promise<ApiGateway> =>
  call(`/gw/apis/detail/${encodeURIComponent(identifier)}`);
export const saveApi = (payload: SaveApiPayload): Promise<unknown> =>
  call("/gw/apis/save", "POST", payload);
export const deleteApi = (identifier: string): Promise<unknown> =>
  call("/gw/apis/delete", "POST", { apiIdentifier: identifier });
export const getStores = (): Promise<StoreRs[]> => call("/gw/stores");
export const getStoreDetail = (id: number): Promise<StoreRs> =>
  call(`/gw/stores/detail/${id}`);
export const saveStore = (payload: SaveStorePayload): Promise<StoreRs> =>
  call("/gw/stores/save", "POST", payload);
export const deleteStore = (id: number): Promise<unknown> =>
  call("/gw/stores/delete", "POST", { storeId: id });
export const regenerateSecret = (id: number): Promise<StoreRs> =>
  call("/gw/stores/regenerate", "POST", { storeId: id });

export async function executeApi(
  identifier: string,
  input: ExecuteApiRequest,
): Promise<ExecuteApiResult> {
  let response: Response;
  try {
    response = await fetch(`/gw/apis/execute/${encodeURIComponent(identifier)}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(input),
    });
  } catch {
    throw new ApiError("Network error - please try again", "99", 0);
  }
  if (!response.ok) {
    let message = `Request failed (${response.status})`;
    try { message = ((await response.json()) as ApiEnvelope).message ?? message; } catch { /* non-JSON error */ }
    throw new ApiError(message, undefined, response.status);
  }
  return (await response.json()) as ExecuteApiResult;
}
