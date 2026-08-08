import { NextResponse, type NextRequest } from "next/server";
import { SESSION_COOKIE } from "@/lib/session";

export function middleware(request: NextRequest) {
  const token = request.cookies.get(SESSION_COOKIE)?.value;
  const { pathname } = request.nextUrl;

  const isProtected =
    pathname === "/home" || pathname.startsWith("/api/") || pathname.startsWith("/store/");

  if (isProtected && !token) {
    const url = request.nextUrl.clone();
    url.pathname = "/";
    url.search = "";
    return NextResponse.redirect(url);
  }

  if (pathname === "/" && token) {
    return NextResponse.redirect(new URL("/home", request.url));
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/", "/home/:path*", "/api/:path*", "/store/:path*"],
};
