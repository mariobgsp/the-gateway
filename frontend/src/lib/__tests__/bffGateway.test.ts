import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const mockGetSessionToken = vi.fn();

vi.mock("../session", () => ({
  getSessionToken: (...args: unknown[]) => mockGetSessionToken(...args),
}));

vi.mock("next/server", () => ({
  NextResponse: {
    json: (body: unknown, init?: { status?: number }) => ({
      status: init?.status ?? 200,
      json: async () => body,
      body,
    }),
  },
}));

import { bffProxy } from "../bffGateway";

function response(body: unknown, status = 200): Response {
  return { ok: status >= 200 && status < 300, status, json: () => Promise.resolve(body) } as Response;
}

describe("bffProxy deep module", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.stubGlobal("fetch", vi.fn());
  });
  afterEach(() => vi.unstubAllGlobals());

  it("returns 401 when unauthenticated", async () => {
    mockGetSessionToken.mockResolvedValue(null);
    const res = await bffProxy({ backendPath: "/api/gateway/getApiList" }) as unknown as { status: number; body: { code: string } };
    expect(res.status).toBe(401);
    expect(res.body.code).toBe("98");
    expect(fetch).not.toHaveBeenCalled();
  });

  it("calls backend with token and returns 200 on code 00", async () => {
    mockGetSessionToken.mockResolvedValue("tok123");
    vi.mocked(fetch).mockResolvedValue(response({ code: "00", data: [{ id: 1 }], status: "ok" }));
    const res = await bffProxy({ backendPath: "/api/gateway/getApiList", method: "GET" }) as unknown as { status: number; body: { code: string } };
    expect(res.status).toBe(200);
    expect(res.body.code).toBe("00");
    expect(vi.mocked(fetch).mock.calls[0][1]).toEqual(expect.objectContaining({ headers: { Authorization: "Bearer tok123" } }));
  });

  it("maps envelope error to 500 when httpStatus not 4xx", async () => {
    mockGetSessionToken.mockResolvedValue("tok");
    vi.mocked(fetch).mockResolvedValue(response(null, 200));
    const res = await bffProxy({ backendPath: "/api/gateway/getApiList" }) as unknown as { status: number };
    expect(res.status).toBe(500);
  });

  it("preserves 404 when envelope code !=00 and httpStatus 404", async () => {
    mockGetSessionToken.mockResolvedValue("tok");
    vi.mocked(fetch).mockResolvedValue(response({ code: "02", message: "02:NotFound", status: "error" }, 404));
    const res = await bffProxy({ backendPath: "/api/gateway/getApiList" }) as unknown as { status: number; body: { code: string } };
    expect(res.status).toBe(404);
    expect(res.body.code).toBe("02");
  });

  it("maps 200 httpStatus with envelope code 04 to 500", async () => {
    mockGetSessionToken.mockResolvedValue("tok");
    vi.mocked(fetch).mockResolvedValue(response({ code: "04", message: "04:BadRequest", status: "error" }, 200));
    const res = await bffProxy({ backendPath: "/api/gateway/getApiList" }) as unknown as { status: number };
    expect(res.status).toBe(500);
  });
});
