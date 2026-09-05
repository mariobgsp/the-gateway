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
  code?: string;
  status?: number;

  constructor(message: string, code?: string, status?: number) {
    super(message);
    this.name = "ApiError";
    this.code = code;
    this.status = status;
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let res: Response;
  try {
    res = await fetch(path, {
      ...init,
      headers: {
        "Content-Type": "application/json",
        ...(init?.headers ?? {}),
      },
    });
  } catch {
    throw new ApiError("Network error - please try again", "99", 0);
  }

  let envelope: ApiEnvelope | null = null;
  try {
    envelope = (await res.json()) as ApiEnvelope;
  } catch {
    envelope = null;
  }

  if (!res.ok || !envelope || envelope.code !== "00") {
    throw new ApiError(
      envelope?.message ??
        envelope?.errorMessage ??
        `Request failed (${res.status})`,
      envelope?.code,
      res.status,
    );
  }
  return envelope.data as T;
}

export function login(
  username: string,
  password: string,
): Promise<LoginResponse> {
  return request<LoginResponse>("/gw/auth/login", {
    method: "POST",
    body: JSON.stringify({ username, password }),
  });
}

export function logout(): Promise<unknown> {
  return request<unknown>("/gw/auth/logout", { method: "POST" });
}

export function getApis(): Promise<GatewayListRs[]> {
  return request<GatewayListRs[]>("/gw/apis");
}

export function getApiDetail(identifier: string): Promise<ApiGateway> {
  return request<ApiGateway>(
    `/gw/apis/detail/${encodeURIComponent(identifier)}`,
  );
}

export function saveApi(payload: SaveApiPayload): Promise<unknown> {
  return request<unknown>("/gw/apis/save", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function deleteApi(identifier: string): Promise<unknown> {
  return request<unknown>("/gw/apis/delete", {
    method: "POST",
    body: JSON.stringify({ apiIdentifier: identifier }),
  });
}

export async function executeApi(
  identifier: string,
  req: ExecuteApiRequest,
): Promise<ExecuteApiResult> {
  let res: Response;
  try {
    res = await fetch(`/gw/apis/execute/${encodeURIComponent(identifier)}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(req),
    });
  } catch {
    throw new ApiError("Network error - please try again", "99", 0);
  }

  if (!res.ok) {
    let message = `Request failed (${res.status})`;
    try {
      const envelope = (await res.json()) as ApiEnvelope;
      if (envelope?.message) message = envelope.message;
    } catch {
      // ignore
    }
    throw new ApiError(message, undefined, res.status);
  }
  return (await res.json()) as ExecuteApiResult;
}

export function getStores(): Promise<StoreRs[]> {
  return request<StoreRs[]>("/gw/stores");
}

export function getStoreDetail(id: number): Promise<StoreRs> {
  return request<StoreRs>(`/gw/stores/detail/${id}`);
}

export function saveStore(payload: SaveStorePayload): Promise<StoreRs> {
  return request<StoreRs>("/gw/stores/save", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function deleteStore(id: number): Promise<unknown> {
  return request<unknown>("/gw/stores/delete", {
    method: "POST",
    body: JSON.stringify({ storeId: id }),
  });
}

export function regenerateSecret(id: number): Promise<StoreRs> {
  return request<StoreRs>("/gw/stores/regenerate", {
    method: "POST",
    body: JSON.stringify({ storeId: id }),
  });
}
