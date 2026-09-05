import { NextResponse } from "next/server";
import { callBackend, envelopeError } from "@/lib/backend";
import { SESSION_COOKIE, sessionCookieOptions } from "@/lib/session";

export const runtime = "nodejs";

export async function POST(request: Request) {
  let body: { username?: string; password?: string };
  try {
    body = await request.json();
  } catch {
    return NextResponse.json(
      { status: "error", code: "04", message: "invalid request body" },
      { status: 400 },
    );
  }

  const username =
    typeof body.username === "string" ? body.username.trim() : "";
  const password = typeof body.password === "string" ? body.password : "";

  if (!username || !password) {
    return NextResponse.json(
      {
        status: "error",
        code: "04",
        message: "username and password are required",
      },
      { status: 400 },
    );
  }
  if (username.length > 50 || password.length > 72) {
    return NextResponse.json(
      { status: "error", code: "04", message: "invalid credentials" },
      { status: 400 },
    );
  }

  const { httpStatus, envelope } = await callBackend("/gateway/user/login", {
    method: "POST",
    body: { username, password },
  });

  if (!envelope || envelope.code !== "00" || !envelope.data) {
    return NextResponse.json(
      envelope ?? {
        status: "error",
        code: "99",
        message: envelopeError(envelope, httpStatus),
      },
      { status: httpStatus >= 400 ? httpStatus : 401 },
    );
  }

  const data = envelope.data as {
    accessToken?: string;
    tokenLifetime?: string;
  };
  const token = data.accessToken;
  if (!token) {
    return NextResponse.json(
      { status: "error", code: "99", message: "login failed" },
      { status: 500 },
    );
  }

  const lifetime = Number.parseInt(data.tokenLifetime ?? "3600", 10);
  const maxAge = Number.isFinite(lifetime) && lifetime > 0 ? lifetime : 3600;

  const response = NextResponse.json(
    { status: envelope.status, code: envelope.code, message: envelope.message },
    { status: 200 },
  );
  response.cookies.set(SESSION_COOKIE, token, sessionCookieOptions(maxAge));
  return response;
}
