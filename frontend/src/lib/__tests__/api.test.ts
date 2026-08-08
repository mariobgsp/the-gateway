import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  ApiError,
  deleteApi,
  deleteStore,
  executeApi,
  getApiDetail,
  getApis,
  getStores,
  login,
  logout,
  regenerateSecret,
  saveApi,
  saveStore,
} from "../api";

const okEnvelope = (data: unknown = null) => ({
  status: "ok",
  code: "00",
  message: "success",
  data,
});

function mockFetchResponse(body: unknown, status = 200): Response {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: () => Promise.resolve(body),
  } as Response;
}

describe("api client", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("login posts credentials and returns message", async () => {
    vi.mocked(fetch).mockResolvedValue(mockFetchResponse(okEnvelope()));
    const result = await login("ario_test", "password123");

    expect(result).toBeNull();
    const [url, init] = vi.mocked(fetch).mock.calls[0] as [string, RequestInit];
    expect(url).toBe("/gw/auth/login");
    expect(init.method).toBe("POST");
    expect(JSON.parse(init.body as string)).toEqual({ username: "ario_test", password: "password123" });
  });

  it("login throws ApiError with backend message on invalid credentials", async () => {
    vi.mocked(fetch).mockResolvedValue(
      mockFetchResponse(
        { status: "UNAUTHORIZED", code: "06", message: "06:Unauthorized:invalid username or password" },
        401
      )
    );

    await expect(login("ario_test", "wrong")).rejects.toMatchObject({
      name: "ApiError",
      code: "06",
      status: 401,
    });
  });

  it("login throws ApiError on network failure", async () => {
    vi.mocked(fetch).mockRejectedValue(new TypeError("fetch failed"));
    await expect(login("a", "b")).rejects.toBeInstanceOf(ApiError);
  });

  it("getApis returns gateway list data", async () => {
    const apis = [{ id: 1, apiName: "Gateway-Cat-Api", apiIdentifier: "gateway-catapi", apiPath: "/v1", method: "GET", status: "created" }];
    vi.mocked(fetch).mockResolvedValue(mockFetchResponse(okEnvelope(apis)));

    const result = await getApis();
    expect(result).toEqual(apis);
  });

  it("getApiDetail encodes identifier", async () => {
    vi.mocked(fetch).mockResolvedValue(mockFetchResponse(okEnvelope({ apiIdentifier: "a b" })));
    await getApiDetail("a b");

    const [url] = vi.mocked(fetch).mock.calls[0] as [string];
    expect(url).toBe("/gw/apis/detail/a%20b");
  });

  it("saveApi posts payload", async () => {
    vi.mocked(fetch).mockResolvedValue(mockFetchResponse(okEnvelope()));
    await saveApi({
      apiIdentifier: "new-api",
      name: "New API",
      host: "https://example.com",
      path: "/v1",
      method: "GET",
      status: "created",
      header: "Content-Type",
      requireRequestBody: false,
      requireRequestParam: false,
      param: "",
    });

    const [, init] = vi.mocked(fetch).mock.calls[0] as [string, RequestInit];
    expect(init.method).toBe("POST");
    expect(JSON.parse(init.body as string).apiIdentifier).toBe("new-api");
  });

  it("deleteApi posts identifier", async () => {
    vi.mocked(fetch).mockResolvedValue(mockFetchResponse(okEnvelope()));
    await deleteApi("gateway-catapi");

    const [url, init] = vi.mocked(fetch).mock.calls[0] as [string, RequestInit];
    expect(url).toBe("/gw/apis/delete");
    expect(JSON.parse(init.body as string)).toEqual({ apiIdentifier: "gateway-catapi" });
  });

  it("executeApi returns formatted result", async () => {
    vi.mocked(fetch).mockResolvedValue(
      mockFetchResponse({ ok: true, httpStatus: 200, body: "{\n  \"a\": 1\n}" })
    );
    const result = await executeApi("gateway-catapi", { method: "GET", headers: {}, params: { limit: "2" }, body: "" });

    expect(result.ok).toBe(true);
    expect(result.body).toContain("a");
    const [url, init] = vi.mocked(fetch).mock.calls[0] as [string, RequestInit];
    expect(url).toBe("/gw/apis/execute/gateway-catapi");
    expect(JSON.parse(init.body as string).params).toEqual({ limit: "2" });
  });

  it("executeApi throws ApiError on failure", async () => {
    vi.mocked(fetch).mockResolvedValue(
      mockFetchResponse({ status: "error", code: "04", message: "bad request" }, 400)
    );

    await expect(
      executeApi("x", { method: "GET", headers: {}, params: {}, body: "" })
    ).rejects.toMatchObject({ message: "bad request" });
  });

  it("getStores returns store list", async () => {
    const stores = [{ id: 1, storeName: "Store A", clientId: "c1", secretKey: "s1" }];
    vi.mocked(fetch).mockResolvedValue(mockFetchResponse(okEnvelope(stores)));

    expect(await getStores()).toEqual(stores);
  });

  it("saveStore posts payload", async () => {
    vi.mocked(fetch).mockResolvedValue(mockFetchResponse(okEnvelope({ id: 1, storeName: "New", clientId: "c", secretKey: "s" })));
    await saveStore({ storeName: "New" });

    const [, init] = vi.mocked(fetch).mock.calls[0] as [string, RequestInit];
    expect(JSON.parse(init.body as string)).toEqual({ storeName: "New" });
  });

  it("deleteStore posts store id", async () => {
    vi.mocked(fetch).mockResolvedValue(mockFetchResponse(okEnvelope()));
    await deleteStore(5);

    const [url, init] = vi.mocked(fetch).mock.calls[0] as [string, RequestInit];
    expect(url).toBe("/gw/stores/delete");
    expect(JSON.parse(init.body as string)).toEqual({ storeId: 5 });
  });

  it("regenerateSecret returns updated store", async () => {
    const updated = { id: 1, storeName: "Store A", clientId: "c", secretKey: "gw_new" };
    vi.mocked(fetch).mockResolvedValue(mockFetchResponse(okEnvelope(updated)));

    expect(await regenerateSecret(1)).toEqual(updated);
  });

  it("logout posts logout", async () => {
    vi.mocked(fetch).mockResolvedValue(mockFetchResponse(okEnvelope()));
    await logout();

    const [url, init] = vi.mocked(fetch).mock.calls[0] as [string, RequestInit];
    expect(url).toBe("/gw/auth/logout");
    expect(init.method).toBe("POST");
  });

  it("throws ApiError for unknown envelope code", async () => {
    vi.mocked(fetch).mockResolvedValue(
      mockFetchResponse({ status: "NOT_FOUND", code: "02", message: "02:NotFound:api not found!" }, 404)
    );

    await expect(getApiDetail("nope")).rejects.toMatchObject({ code: "02" });
  });
});