export interface ApiEnvelope<T = unknown> {
  status?: string;
  code?: string;
  message?: string;
  data?: T;
  errorMessage?: string;
}

export interface GatewayListRs {
  id: number;
  apiName: string;
  apiIdentifier: string;
  apiPath: string;
  method: string;
  status: string;
}

export interface ApiGateway {
  id: number;
  apiName: string;
  apiIdentifier: string;
  apiHost: string;
  apiPath: string;
  method: string;
  status: string;
  header: string;
  requireRequestBody: boolean;
  requireRequestParam: boolean;
  param: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface SaveApiPayload {
  apiIdentifier: string;
  name: string;
  host: string;
  path: string;
  method: string;
  status: string;
  header: string;
  requireRequestBody: boolean;
  requireRequestParam: boolean;
  param: string;
}

export interface StoreRs {
  id: number;
  storeName: string;
  clientId: string;
  secretKey: string;
}

export interface SaveStorePayload {
  storeId?: number;
  storeName: string;
  clientId?: string;
}

export interface ExecuteApiRequest {
  method: string;
  headers: Record<string, string>;
  params: Record<string, string>;
  body: string;
}

export interface ExecuteApiResult {
  ok: boolean;
  httpStatus: number;
  body: string;
}

export interface LoginResponse {
  message?: string;
}
