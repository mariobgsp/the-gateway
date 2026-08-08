"use client";

import { useCallback, useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { Navbar } from "@/components/Navbar";
import { Toast } from "@/components/Toast";
import { deleteApi, executeApi, getApiDetail, saveApi } from "@/lib/api";
import type { ApiGateway } from "@/types";

const METHODS = ["GET", "POST", "PUT", "DELETE", "PATCH"];
const STATUSES = ["created", "published", "deprecated"];

export default function ApiDetailPage() {
  const { identifier } = useParams<{ identifier: string }>();
  const navigate = useRouter();

  const [api, setApi] = useState<ApiGateway | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");

  const [name, setName] = useState("");
  const [apiHost, setApiHost] = useState("");
  const [apiPath, setApiPath] = useState("");
  const [method, setMethod] = useState("GET");
  const [status, setStatus] = useState("created");
  const [headerContent, setHeaderContent] = useState("");
  const [hasReqParam, setHasReqParam] = useState(false);
  const [reqParamContent, setReqParamContent] = useState("");
  const [hasReqBody, setHasReqBody] = useState(false);
  const [saving, setSaving] = useState(false);
  const [toast, setToast] = useState<{ msg: string; type: "success" | "error" } | null>(null);

  const [tryOpen, setTryOpen] = useState(false);
  const [tryHeaders, setTryHeaders] = useState("");
  const [tryParams, setTryParams] = useState("");
  const [tryBody, setTryBody] = useState("");
  const [tryResponse, setTryResponse] = useState("");

  const load = useCallback(async () => {
    if (!identifier) return;
    setLoading(true);
    setLoadError("");
    try {
      const detail = await getApiDetail(identifier);
      setApi(detail);
      setName(detail.apiName);
      setApiHost(detail.apiHost);
      setApiPath(detail.apiPath);
      setMethod(detail.method);
      setStatus(detail.status ?? "created");
      setHeaderContent((detail.header ?? "").split(";").filter(Boolean).join("\n"));
      setHasReqParam(detail.requireRequestParam ?? false);
      setReqParamContent((detail.param ?? "").split(";").filter(Boolean).join("\n"));
      setHasReqBody(detail.requireRequestBody ?? false);
      setTryHeaders("");
      setTryParams("");
      setTryBody("");
    } catch (err) {
      setLoadError(err instanceof Error ? err.message : "Failed to load API");
    } finally {
      setLoading(false);
    }
  }, [identifier]);

  useEffect(() => {
    void load();
  }, [load]);

  const handleSave = async () => {
    if (!api) return;
    setSaving(true);
    try {
      await saveApi({
        apiIdentifier: api.apiIdentifier,
        name: name.trim(),
        host: apiHost.trim(),
        path: apiPath.trim(),
        method,
        status,
        header: headerContent.split("\n").map((s) => s.trim()).filter(Boolean).join(";"),
        requireRequestBody: hasReqBody,
        requireRequestParam: hasReqParam,
        param: reqParamContent.split("\n").map((s) => s.trim()).filter(Boolean).join(";"),
      });
      setToast({ msg: "API changes saved successfully!", type: "success" });
      await load();
    } catch (err) {
      setToast({ msg: err instanceof Error ? err.message : "Save failed", type: "error" });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!api) return;
    if (!window.confirm(`Delete "${api.apiName}"? This action cannot be undone.`)) return;
    try {
      await deleteApi(api.apiIdentifier);
      navigate.replace("/home");
    } catch (err) {
      setToast({ msg: err instanceof Error ? err.message : "Delete failed", type: "error" });
    }
  };

  const handleTryIt = async () => {
    if (!api) return;
    setTryResponse("⏳ Sending request...");
    try {
      const result = await executeApi(api.apiIdentifier, {
        method,
        headers: parseKeyValue(tryHeaders),
        params: parseKeyValue(tryParams),
        body: tryBody,
      });
      setTryResponse(result.body);
    } catch (err) {
      setTryResponse(`❌ Request failed: ${err instanceof Error ? err.message : "Unknown error"}`);
    }
  };

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

  if (loadError || !api) {
    return (
      <div className="app-container animate-fade-in">
        <Navbar />
        <div style={{ padding: "4rem 2rem", textAlign: "center", color: "var(--clr-danger)" }}>
          <p className="font-semibold">API not found</p>
          <p className="text-sm mt-1">{loadError}</p>
          <button className="btn btn-outline mt-1" onClick={() => navigate.push("/home")} style={{ marginTop: "1rem" }}>
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
        <span className="breadcrumb-link" onClick={() => navigate.push("/home")}>API List</span>
        <span className="breadcrumb-sep">›</span>
        <span className="breadcrumb-current">{name}</span>
      </div>

      <div style={{ marginBottom: "1.5rem" }}>
        <div className="tabs-bar">
          <button className="tab-btn active">API List</button>
          <button className="tab-btn" onClick={() => navigate.push("/home")}>Store Account</button>
        </div>
      </div>

      <div className="card-glass" style={{ padding: "2rem" }}>
        <div style={{
          display: "flex",
          alignItems: "center",
          gap: "1.25rem",
          padding: "1rem 1.5rem",
          background: "rgba(124,58,237,0.08)",
          border: "1px solid rgba(124,58,237,0.2)",
          borderRadius: "var(--r-lg)",
          marginBottom: "2rem",
          flexWrap: "wrap",
        }}>
          <div style={{ flex: "2 1 160px" }}>
            <div className="form-label">API Name</div>
            <input
              className="input-field"
              placeholder="API name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              style={{ fontWeight: 700 }}
            />
          </div>
          <div style={{ flex: "1 1 120px" }}>
            <div className="form-label">Method</div>
            <select className="input-field" value={method} onChange={(e) => setMethod(e.target.value)}>
              {METHODS.map((m) => <option key={m} value={m}>{m}</option>)}
            </select>
          </div>
          <div style={{ flex: "1 1 120px" }}>
            <div className="form-label">Status</div>
            <select className="input-field" value={status} onChange={(e) => setStatus(e.target.value)}>
              {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
            </select>
          </div>
        </div>

        <div className="grid-3" style={{ marginBottom: "2rem" }}>
          <div className="form-group">
            <label className="form-label">API Host</label>
            <input
              className="input-field"
              placeholder="https://api.example.com"
              value={apiHost}
              onChange={(e) => setApiHost(e.target.value)}
            />
          </div>
          <div className="form-group">
            <label className="form-label">API Path</label>
            <input
              className="input-field"
              placeholder="/v1/endpoint"
              value={apiPath}
              onChange={(e) => setApiPath(e.target.value)}
            />
          </div>
          <div className="form-group">
            <label className="form-label">API Identifier</label>
            <input className="input-field" value={api.apiIdentifier} disabled />
          </div>
        </div>

        <div className="section-divider" />

        <div className="grid-3">
          <div className="form-group">
            <label className="toggle-wrapper form-label" style={{ marginBottom: "0.75rem" }}>
              <input type="checkbox" checked readOnly />
              <span>Allowed Request Headers</span>
            </label>
            <textarea
              className="input-field"
              rows={5}
              placeholder={"x-api-key\nContent-Type"}
              value={headerContent}
              onChange={(e) => setHeaderContent(e.target.value)}
            />
          </div>

          <div className="form-group">
            <label className="toggle-wrapper form-label" style={{ marginBottom: "0.75rem" }}>
              <input
                type="checkbox"
                checked={hasReqParam}
                onChange={(e) => setHasReqParam(e.target.checked)}
              />
              <span>Require Request Parameters</span>
            </label>
            {hasReqParam ? (
              <textarea
                className="input-field"
                rows={5}
                placeholder={"limit\nsize"}
                value={reqParamContent}
                onChange={(e) => setReqParamContent(e.target.value)}
              />
            ) : (
              <div style={{
                minHeight: "120px",
                background: "rgba(255,255,255,0.02)",
                border: "1px dashed var(--clr-border)",
                borderRadius: "var(--r-md)",
                display: "flex", alignItems: "center", justifyContent: "center",
              }}>
                <span className="text-muted text-sm">(no required parameters)</span>
              </div>
            )}
          </div>

          <div className="form-group">
            <label className="toggle-wrapper form-label" style={{ marginBottom: "0.75rem" }}>
              <input
                type="checkbox"
                checked={hasReqBody}
                onChange={(e) => setHasReqBody(e.target.checked)}
              />
              <span>Require Request Body</span>
            </label>
            <div style={{
              minHeight: "120px",
              background: "rgba(255,255,255,0.02)",
              border: "1px dashed var(--clr-border)",
              borderRadius: "var(--r-md)",
              display: "flex", alignItems: "center", justifyContent: "center",
            }}>
              <span className="text-muted text-sm">
                {hasReqBody ? "Request body will be mandatory" : "(no required body)"}
              </span>
            </div>
          </div>
        </div>
      </div>

      <div style={{ display: "flex", justifyContent: "flex-end", gap: "0.75rem", marginTop: "1.5rem" }}>
        <button className="btn btn-ghost" onClick={() => navigate.push("/home")}>← Back</button>
        <button className="btn btn-success" onClick={() => setTryOpen(true)}>▶ Try it Now!</button>
        <button className="btn btn-primary" onClick={handleSave} disabled={saving}>
          {saving ? "Saving..." : "Save Changes"}
        </button>
        <button className="btn btn-danger" onClick={handleDelete}>Delete API</button>
      </div>

      {tryOpen && (
        <div className="modal-overlay" onClick={() => setTryOpen(false)}>
          <div className="modal-box" style={{ maxWidth: "700px" }} onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2 className="modal-title">Try it Now — {name}</h2>
              <button className="modal-close" onClick={() => setTryOpen(false)}>✕</button>
            </div>

            <div style={{ marginBottom: "0.75rem" }}>
              <span className={`method-badge method-${method}`}>{method}</span>
              <code style={{ marginLeft: "0.75rem", fontSize: "0.85rem", color: "var(--txt-secondary)" }}>
                {apiHost}{apiPath}
              </code>
            </div>

            <div className="grid-3" style={{ marginBottom: "0.75rem" }}>
              <div className="form-group">
                <label className="form-label">Headers (key: value)</label>
                <textarea
                  className="input-field"
                  rows={4}
                  placeholder={"x-api-key: your-key"}
                  value={tryHeaders}
                  onChange={(e) => setTryHeaders(e.target.value)}
                />
              </div>
              <div className="form-group">
                <label className="form-label">Query Params (key: value)</label>
                <textarea
                  className="input-field"
                  rows={4}
                  placeholder={"limit: 10"}
                  value={tryParams}
                  onChange={(e) => setTryParams(e.target.value)}
                />
              </div>
              <div className="form-group">
                <label className="form-label">Request Body (JSON)</label>
                <textarea
                  className="input-field"
                  rows={4}
                  placeholder={'{\n  "key": "value"\n}'}
                  value={tryBody}
                  onChange={(e) => setTryBody(e.target.value)}
                />
              </div>
            </div>

            <pre style={{
              background: "var(--clr-bg)",
              border: "1px solid var(--clr-border)",
              borderRadius: "var(--r-md)",
              padding: "1rem",
              fontSize: "0.8rem",
              overflowX: "auto",
              whiteSpace: "pre-wrap",
              wordBreak: "break-all",
              color: "var(--txt-primary)",
              maxHeight: "320px",
              overflowY: "auto",
            }}>
              {tryResponse}
            </pre>

            <div className="modal-footer">
              <button className="btn btn-outline" onClick={() => setTryOpen(false)}>Close</button>
              <button className="btn btn-success" onClick={handleTryIt}>Send Request</button>
            </div>
          </div>
        </div>
      )}

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

function parseKeyValue(text: string): Record<string, string> {
  const result: Record<string, string> = {};
  for (const line of text.split("\n")) {
    const index = line.indexOf(":");
    if (index <= 0) continue;
    const key = line.slice(0, index).trim();
    const value = line.slice(index + 1).trim();
    if (key) result[key] = value;
  }
  return result;
}
