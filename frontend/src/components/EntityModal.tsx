"use client";

import { useState } from "react";
import { Modal } from "@/components/ui";
import type { SaveApiPayload } from "@/types";

export function AddApiModal({ onClose, onAdd }: { onClose: () => void; onAdd: (payload: SaveApiPayload) => Promise<void> }) {
  const [name, setName] = useState("");
  const [path, setPath] = useState("");
  const [method, setMethod] = useState("GET");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  async function submit(event: React.FormEvent) {
    event.preventDefault(); if (!name.trim() || !path.trim() || submitting) return;
    setSubmitting(true); setError("");
    try { await onAdd({ apiIdentifier: path.trim(), name: name.trim(), host: "https://api.thecatapi.com", path: `/${path.trim().replace(/^\/+/, "")}`, method, status: "created", header: "Content-Type", requireRequestBody: false, requireRequestParam: false, param: "" }); onClose(); }
    catch (e) { setError(e instanceof Error ? e.message : "Failed to add API"); setSubmitting(false); }
  }
  return <Modal title="Add New API" onClose={onClose}><form onSubmit={submit} className="stack-form">
    <label className="form-group"><span className="form-label">API Name</span><input className="input-field" placeholder="e.g. My-Awesome-API" value={name} onChange={(e) => setName(e.target.value)} required autoFocus /></label>
    <label className="form-group"><span className="form-label">API Path (identifier)</span><input className="input-field" placeholder="e.g. my-awesome-api" value={path} onChange={(e) => setPath(e.target.value)} required /></label>
    <label className="form-group"><span className="form-label">HTTP Method</span><select className="input-field" value={method} onChange={(e) => setMethod(e.target.value)}>{["GET", "POST", "PUT", "DELETE", "PATCH"].map((m) => <option key={m}>{m}</option>)}</select></label>
    {error && <p className="form-error">{error}</p>}<div className="modal-footer"><button type="button" className="btn btn-outline" onClick={onClose} disabled={submitting}>Cancel</button><button className="btn btn-primary" disabled={submitting}>{submitting ? "Adding..." : "Add API"}</button></div>
  </form></Modal>;
}

export function AddStoreModal({ onClose, onAdd }: { onClose: () => void; onAdd: (name: string) => Promise<void> }) {
  const [name, setName] = useState(""); const [error, setError] = useState(""); const [submitting, setSubmitting] = useState(false);
  async function submit(event: React.FormEvent) { event.preventDefault(); if (!name.trim() || submitting) return; setSubmitting(true); setError(""); try { await onAdd(name.trim()); onClose(); } catch (e) { setError(e instanceof Error ? e.message : "Failed to add store"); setSubmitting(false); } }
  return <Modal title="Add New Store" onClose={onClose}><form onSubmit={submit} className="stack-form"><label className="form-group"><span className="form-label">Store Name</span><input className="input-field" placeholder="e.g. My-Store" value={name} onChange={(e) => setName(e.target.value)} required autoFocus /></label>{error && <p className="form-error">{error}</p>}<div className="modal-footer"><button type="button" className="btn btn-outline" onClick={onClose} disabled={submitting}>Cancel</button><button className="btn btn-primary" disabled={submitting}>{submitting ? "Adding..." : "Add Store"}</button></div></form></Modal>;
}
