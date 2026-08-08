"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { login } from "@/lib/api";

export default function LoginPage() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const router = useRouter();

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username || !password) {
      setError("Please fill in both fields.");
      return;
    }
    setError("");
    setSubmitting(true);
    try {
      await login(username, password);
      router.replace("/home");
      router.refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Login failed. Please try again.");
      setSubmitting(false);
    }
  };

  return (
    <div className="login-wrapper">
      <div className="login-card animate-slide-up">
        <div className="login-logo">
          <div className="login-logo-title">The Gateway</div>
          <div className="login-logo-sub">API Management Platform</div>
        </div>

        <div className="card-glass">
          <h2 style={{ fontSize: "1.25rem", fontWeight: 700, marginBottom: "0.25rem" }}>
            Welcome back
          </h2>
          <p className="text-secondary text-sm" style={{ marginBottom: "1.75rem" }}>
            Sign in to your account to continue
          </p>

          <form onSubmit={handleLogin} style={{ display: "flex", flexDirection: "column", gap: "1.25rem" }}>
            <div className="form-group">
              <label className="form-label" htmlFor="username">Username</label>
              <input
                id="username"
                type="text"
                className="input-field"
                placeholder="Enter your username"
                value={username}
                onChange={(e) => { setUsername(e.target.value); setError(""); }}
                required
                autoComplete="username"
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="password">Password</label>
              <input
                id="password"
                type="password"
                className="input-field"
                placeholder="Enter your password"
                value={password}
                onChange={(e) => { setPassword(e.target.value); setError(""); }}
                required
                autoComplete="current-password"
              />
            </div>

            {error && (
              <p style={{ color: "var(--clr-danger)", fontSize: "0.8rem", fontWeight: 500 }} role="alert">
                {error}
              </p>
            )}

            <button
              type="submit"
              className="btn btn-primary btn-lg"
              style={{ width: "100%", marginTop: "0.25rem" }}
              disabled={submitting}
            >
              {submitting ? "Signing in..." : "Sign In"}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
