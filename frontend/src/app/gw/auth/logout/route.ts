import { NextResponse } from "next/server";
import { callBackend } from "@/lib/backend";
import {
  SESSION_COOKIE,
  clearSessionToken,
  getSessionToken,
  sessionCookieOptions,
} from "@/lib/session";

export const runtime = "nodejs";

export async function POST() {
  const token = await getSessionToken();

  if (token) {
    await callBackend("/gateway/user/logout", {
      method: "POST",
      token,
    });
  }

  await clearSessionToken();

  const response = NextResponse.json(
    { status: "ok", code: "00", message: "success logout!" },
    { status: 200 },
  );
  response.cookies.set(
    SESSION_COOKIE,
    "",
    sessionCookieOptions(undefined, new Date(0)),
  );
  return response;
}
