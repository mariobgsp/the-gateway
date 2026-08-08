"use client";

import { useCallback, useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { Navbar } from "@/components/Navbar";
import { Toast } from "@/components/Toast";
import { deleteStore, getStoreDetail, regenerateSecret, saveStore } from "@/lib/api";
import type { StoreRs } from "@/types";

type SidebarSection = "general" | "apikeys" | "danger";

function maskKey(key: string): string {
  if (key.length <= 10) return key;
  return `${key.slice(0, 10)}${"•".repeat(12)}`;
}

export default function EditStoreAccountPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useRouter();

  const [store, setStore] = useState<StoreRs | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [activeSection, setActiveSection] = useState<SidebarSection>("general");
  const [storeName, setStoreName] = useState("");
  const [saving, setSaving] = useState(false);
  const [regenerating, setRegenerating] = useState(false);
  const [toast, setToast] = useState<{ msg: string; type: "success" | "error" } | null>(null);

  const showToast = (msg: string, type: "success" | "error" = "success") =>
    setToast({ msg, type });

  const load = useCallback(async () => {
    const storeId = Number.parseInt(id ?? "", 10);
    if (!Number.isInteger(storeId) || storeId <= 0) {
      setLoadError("Invalid store id");
      setLoading(false);
      return;
    }
    setLoading(true);
    setLoadError("");
    try {
      const detail = await getStoreDetail(storeId);
      setStore(detail);
      setStoreName(detail.storeName);
    } catch (err) {
      setLoadError(err instanceof Error ? err.message : "Failed to load store");
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    void load();
  }, [load]);

  const handleSave = async () => {
    if (!store) return;
    setSaving(true);
    try {
      const updated = await saveStore({ storeId: store.id, storeName: storeName.trim() });
      setStore(updated);
      showToast("Store account updated successfully!");
    } catch (err) {
      showToast(err instanceof Error ? err.message : "Save failed", "error");
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!store) return;
    if (!window.confirm(`Delete "${store.storeName}"? This cannot be undone.`)) return;
    try {
      await deleteStore(store.id);
      navigate.replace("/home");
    } catch (err) {
      showToast(err instanceof Error ? err.message : "Delete failed", "error");
    }
  };

  const handleRegenerate = async () => {
    if (!store) return;
    if (!window.confirm("Regenerate the API key for this store? The old key will stop working.")) return;
    setRegenerating(true);
    try {
      const updated = await regenerateSecret(store.id);
      setStore(updated);
      showToast("New API key generated!");
    } catch (err) {
      showToast(err instanceof Error ? err.message : "Regeneration failed", "error");
    } finally {
      setRegenerating(false);
    }
  };

  const handleCopyKey = async () => {
    if (!store?.secretKey) return;
    try {
      await navigator.clipboard.writeText(store.secretKey);
      showToast("Key copied!");
    } catch {
      showToast("Could not copy key", "error");
    }
  };

  const sidebarItems: { key: SidebarSection; icon: string; label: string }[] = [
    { key: "general", icon: "⚙️", label: "General" },
    { key: "apikeys", icon: "🔑", label: "API Keys" },
    { key: "danger", icon: "⚠️", label: "Danger Zone" },
  ];

  if (loading) {
    return (
      <div className="app-container animate-fade-in">
        <Navbar />
        <div style={{ padding: "4rem 2rem", textAlign: "center", color: "var(--txt-secondary)" }}>
          <p className="font-semibold">Loading...</p>
        </div>
      </div>
    );
  }

  if (loadError || !store) {
    return (
      <div className="app-container animate-fade-in">
        <Navbar />
        <div style={{ padding: "4rem 2rem", textAlign: "center", color: "var(--clr-danger)" }}>
          <p className="font-semibold">Store not found</p>
          <p className="text-sm mt-1">{loadError}</p>
          <button className="btn btn-outline" onClick={() => navigate.push("/home")} style={{ marginTop: "1rem" }}>
            ← Back to Dashboard
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="app-container animate-fade-in">
      <Navbar />

      <div className="breadcrumb">
        <span className="breadcrumb-link" onClick={() => navigate.push("/home")}>Dashboard</span>
        <span className="breadcrumb-sep">›</span>
        <span className="breadcrumb-link" onClick={() => navigate.push("/home")}>Store Account</span>
        <span className="breadcrumb-sep">›</span>
        <span className="breadcrumb-current">{store.storeName}</span>
      </div>

      <div style={{ marginBottom: "1.5rem" }}>
        <div className="tabs-bar">
          <button className="tab-btn" onClick={() => navigate.push("/home")}>API List</button>
          <button className="tab-btn active">Store Account</button>
        </div>
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "220px 1fr", gap: "1.5rem", alignItems: "start" }}>
        <div className="card-glass" style={{ padding: "1rem" }}>
          <div style={{ marginBottom: "1rem", padding: "0.25rem 0.5rem" }}>
            <div style={{ fontWeight: 700, fontSize: "0.95rem", color: "var(--txt-primary)" }}>{store.storeName}</div>
            <span className="text-muted text-sm" style={{ display: "block", marginTop: "0.25rem" }}>{store.clientId}</span>
          </div>
          <div className="section-divider" />
          <nav className="sidebar-menu">
            {sidebarItems.map((item) => (
              <button
                key={item.key}
                className={`sidebar-item ${activeSection === item.key ? "active" : ""}`}
                onClick={() => setActiveSection(item.key)}
              >
                <span className="sidebar-icon">{item.icon}</span>
                {item.label}
              </button>
            ))}
          </nav>
        </div>

        <div className="card-glass animate-slide-up" key={activeSection}>
          {activeSection === "general" && (
            <>
              <div className="page-header">
                <div>
                  <div className="page-title">General Settings</div>
                  <div className="page-subtitle">Update the name of your store account.</div>
                </div>
              </div>

              <div style={{ display: "flex", flexDirection: "column", gap: "1.25rem" }}>
                <div className="form-group">
                  <label className="form-label">Store Name</label>
                  <input
                    className="input-field"
                    value={storeName}
                    onChange={(e) => setStoreName(e.target.value)}
                    placeholder="My Store Name"
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Client ID</label>
                  <input className="input-field" value={store.clientId ?? ""} disabled />
                </div>
              </div>

              <div style={{ display: "flex", justifyContent: "flex-end", gap: "0.75rem", marginTop: "1.75rem" }}>
                <button className="btn btn-ghost" onClick={() => navigate.push("/home")}>Cancel</button>
                <button className="btn btn-primary" onClick={handleSave} disabled={saving}>
                  {saving ? "Saving..." : "Save Changes"}
                </button>
              </div>
            </>
          )}

          {activeSection === "apikeys" && (
            <>
              <div className="page-header">
                <div>
                  <div className="page-title">API Keys</div>
                  <div className="page-subtitle">Treat this key like a password. Share it only with your server.</div>
                </div>
                <button className="btn btn-primary btn-sm" onClick={handleRegenerate} disabled={regenerating}>
                  {regenerating ? "Generating..." : "+ Regenerate Key"}
                </button>
              </div>

              {store.secretKey ? (
                <div style={{ display: "flex", flexDirection: "column", gap: "0.75rem" }}>
                  <div style={{
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "space-between",
                    padding: "0.875rem 1.25rem",
                    background: "rgba(255,255,255,0.03)",
                    border: "1px solid var(--clr-border)",
                    borderRadius: "var(--r-md)",
                  }}>
                    <code style={{ fontSize: "0.85rem", color: "var(--txt-secondary)", fontFamily: "monospace" }}>
                      {maskKey(store.secretKey)}
                    </code>
                    <div style={{ display: "flex", gap: "0.5rem" }}>
                      <button className="btn btn-ghost btn-sm" onClick={handleCopyKey}>
                        Copy
                      </button>
                    </div>
                  </div>
                  <p className="text-muted text-sm">
                    Regenerating the key immediately invalidates the previous one.
                  </p>
                </div>
              ) : (
                <div style={{ padding: "3rem", textAlign: "center", color: "var(--txt-secondary)" }}>
                  <div style={{ fontSize: "2.5rem", marginBottom: "0.5rem" }}>🔑</div>
                  <p className="font-semibold">No API key yet</p>
                  <p className="text-sm mt-1">Click Regenerate Key to create one.</p>
                </div>
              )}
            </>
          )}

          {activeSection === "danger" && (
            <>
              <div className="page-header">
                <div>
                  <div className="page-title" style={{ color: "var(--clr-danger)" }}>Danger Zone</div>
                  <div className="page-subtitle">Irreversible and destructive actions.</div>
                </div>
              </div>

              <div style={{
                padding: "1.5rem",
                background: "rgba(239,68,68,0.06)",
                border: "1px solid rgba(239,68,68,0.25)",
                borderRadius: "var(--r-lg)",
              }}>
                <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", flexWrap: "wrap", gap: "1rem" }}>
                  <div>
                    <p style={{ fontWeight: 700, color: "var(--clr-danger)" }}>Delete Store Account</p>
                    <p className="text-secondary text-sm" style={{ marginTop: "0.25rem" }}>
                      Once deleted, all data associated with this store will be permanently removed.
                    </p>
                  </div>
                  <button className="btn btn-danger" onClick={handleDelete}>
                    Delete Store
                  </button>
                </div>
              </div>
            </>
          )}
        </div>
      </div>

      {toast && (
        <Toast
          message={toast.msg}
          type={toast.type}
          onDone={() => setToast(null)}
        />
      )}
    </div>
  );
}
