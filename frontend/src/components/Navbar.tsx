"use client";

import { useRouter } from "next/navigation";
import { logout } from "@/lib/api";

export function Navbar({ username }: { username?: string }) {
  const router = useRouter();

  const handleSignOut = async () => {
    try {
      await logout();
    } finally {
      router.replace("/");
      router.refresh();
    }
  };

  return (
    <nav className="navbar" style={{ padding: "1.5rem 0", maxWidth: "100%" }}>
      <span className="heading-title" style={{ margin: 0, fontSize: "1.75rem" }}>
        The Gateway
      </span>
      <div className="navbar-actions">
        {username && <span className="text-secondary text-sm">{username}</span>}
        <button className="btn btn-outline btn-sm" onClick={handleSignOut}>
          Sign Out
        </button>
      </div>
    </nav>
  );
}
