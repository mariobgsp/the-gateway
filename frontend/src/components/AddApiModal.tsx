"use client";

import { useState } from "react";
import type { SaveApiPayload } from "@/types";

export interface AddApiForm {
  name: string;
  path: string;
  method: string;
}

interface AddApiModalProps {
  onClose: () => void;
  onAdd: (api: SaveApiPayload) => Promise<void>;
}

export function AddApiModal({ onClose, onAdd }: AddApiModalProps) {
  const [name, setName] = useState("");
  const [path, setPath] = useState("");
  const [method, setMethod] = useState("GET");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !path.trim() || submitting) return;
    setSubmitting(true);
    setError("");
    try {
      await onAdd({
        apiIdentifier: path.trim(),
        name: name.trim(),
        host: "https://api.thecatapi.com",
        path: `/${path.trim().replace(/^\/+/, "")}`,
        method,
        status: "created",
        header: "Content-Type",
        requireRequestBody: false,
        requireRequestParam: false,
        param: "",
      });
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to add API");
      setSubmitting(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-box" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2 className="modal-title">Add New API</h2>
          <button className="modal-close" onClick={onClose}>
            ✕
          </button>
        </div>

        <form
          onSubmit={handleSubmit}
          style={{ display: "flex", flexDirection: "column", gap: "1.25rem" }}
        >
          <div className="form-group">
            <label className="form-label">API Name</label>
            <input
              className="input-field"
              placeholder="e.g. My-Awesome-API"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
              autoFocus
            />
          </div>

          <div className="form-group">
            <label className="form-label">API Path (identifier)</label>
            <input
              className="input-field"
              placeholder="e.g. my-awesome-api"
              value={path}
              onChange={(e) => setPath(e.target.value)}
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">HTTP Method</label>
            <select
              className="input-field"
              value={method}
              onChange={(e) => setMethod(e.target.value)}
            >
              <option value="GET">GET</option>
              <option value="POST">POST</option>
              <option value="PUT">PUT</option>
              <option value="DELETE">DELETE</option>
              <option value="PATCH">PATCH</option>
            </select>
          </div>

          {error && (
            <p
              style={{
                color: "var(--clr-danger)",
                fontSize: "0.8rem",
                fontWeight: 500,
              }}
            >
              {error}
            </p>
          )}

          <div className="modal-footer">
            <button
              type="button"
              className="btn btn-outline"
              onClick={onClose}
              disabled={submitting}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={submitting}
            >
              {submitting ? "Adding..." : "Add API"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
