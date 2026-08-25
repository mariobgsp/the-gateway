import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const mockGetSessionToken = vi.fn();
const mockCallBackend = vi.fn();
const mockEnvelopeError = vi.fn((e, s) => e?.message ?? `fail ${s}`);

vi.mock("../session", () => ({
  getSessionToken: (...args: unknown[]) => mockGetSessionToken(...args),
}));

vi.mock("../backend", () => ({
  callBackend: (...args: unknown[]) =>
    (mockCallBackend as (...a: unknown[]) => unknown)(...args),
  envelopeError: (e: unknown, s: unknown) => mockEnvelopeError(e, s),
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

describe("bffProxy deep module", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });
  afterEach(() => vi.clearAllMocks());

  it("returns 401 when unauthenticated", async () => {
    mockGetSessionToken.mockResolvedValue(null);
    const res = (await bffProxy({
      backendPath: "/api/gateway/getApiList",
    })) as unknown as {
      status: number;
      body: { code: string };
    };
    expect(res.status).toBe(401);
    expect(res.body.code).toBe("98");
    expect(mockCallBackend).not.toHaveBeenCalled();
  });

  it("calls backend with token and returns 200 on code 00", async () => {
    mockGetSessionToken.mockResolvedValue("tok123");
    mockCallBackend.mockResolvedValue({
      httpStatus: 200,
      envelope: { code: "00", data: [{ id: 1 }], status: "ok" },
    });
    const res = (await bffProxy({
      backendPath: "/api/gateway/getApiList",
      method: "GET",
    })) as unknown as { status: number; body: { code: string } };
    expect(mockCallBackend).toHaveBeenCalledWith(
      "/api/gateway/getApiList",
      expect.objectContaining({ token: "tok123", method: "GET" }),
    );
    expect(res.status).toBe(200);
    expect(res.body.code).toBe("00");
  });

  it("maps envelope error to 500 when httpStatus not 4xx", async () => {
    mockGetSessionToken.mockResolvedValue("tok");
    mockCallBackend.mockResolvedValue({ httpStatus: 200, envelope: null });
    const res = (await bffProxy({
      backendPath: "/api/gateway/getApiList",
    })) as unknown as {
      status: number;
    };
    expect(res.status).toBe(500);
  });

  it("preserves 404 when envelope code !=00 and httpStatus 404", async () => {
    mockGetSessionToken.mockResolvedValue("tok");
    mockCallBackend.mockResolvedValue({
      httpStatus: 404,
      envelope: { code: "02", message: "02:NotFound", status: "error" },
    });
    const res = (await bffProxy({
      backendPath: "/api/gateway/getApiList",
    })) as unknown as { status: number; body: { code: string } };
    expect(res.status).toBe(404);
    expect(res.body.code).toBe("02");
  });

  it("maps 200 httpStatus with envelope code 04 to 500", async () => {
    mockGetSessionToken.mockResolvedValue("tok");
    mockCallBackend.mockResolvedValue({
      httpStatus: 200,
      envelope: { code: "04", message: "04:BadRequest", status: "error" },
    });
    const res = (await bffProxy({
      backendPath: "/api/gateway/getApiList",
    })) as unknown as { status: number };
    expect(res.status).toBe(500);
  });
});
