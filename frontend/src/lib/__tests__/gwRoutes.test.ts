import { describe, expect, it } from "vitest";
import { dispatch } from "../gwRoutes";

function request(method: string, body?: unknown) {
  return new Request("http://localhost/gw", {
    method,
    body: body === undefined ? undefined : JSON.stringify(body),
    headers: body === undefined ? undefined : { "content-type": "application/json" },
  });
}

describe("/gw route contract", () => {
  it.each([
    ["GET", ["apis"]],
    ["GET", ["stores"]],
    ["GET", ["apis", "detail", "gateway-catapi"]],
    ["GET", ["stores", "detail", "1"]],
    ["POST", ["apis", "save"]],
    ["POST", ["apis", "delete"]],
    ["POST", ["apis", "execute", "gateway-catapi"]],
    ["POST", ["stores", "save"]],
    ["POST", ["stores", "delete"]],
    ["POST", ["stores", "regenerate"]],
    ["POST", ["auth", "login"]],
    ["POST", ["auth", "logout"]],
  ])("accepts %s /gw/%s", async (method, path) => {
    const response = await dispatch(method, path, request(method));
    expect(response.status).not.toBe(404);
  });

  it("rejects unsupported methods before routing", async () => {
    expect((await dispatch("PUT", ["apis"], request("PUT"))).status).toBe(400);
  });
});
