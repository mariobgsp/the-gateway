"use client";

import { useState } from "react";

interface AddStoreModalProps {
  onClose: () => void;
  onAdd: (name: string) => Promise<void>;
}

export function AddStoreModal({ onClose, onAdd }: AddStoreModalProps) {
  const [name, setName] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || submitting) return;
    setSubmitting(true);
    setError("");
    try {
      await onAdd(name.trim());
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to add store");
      setSubmitting(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-box" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2 className="modal-title">Add New Store</h2>
          <button className="modal-close" onClick={onClose}>✕</button>
        </div>

        <form onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: "1.25rem" }}>
          <div className="form-group">
            <label className="form-label">Store Name</label>
            <input
              className="input-field"
              placeholder="e.g. My-Store"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
              autoFocus
            />
          </div>

          {error && <p style={{ color: "var(--clr-danger)", fontSize: "0.8rem", fontWeight: 500 }}>{error}</p>}

          <div className="modal-footer">
            <button type="button" className="btn btn-outline" onClick={onClose} disabled={submitting}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? "Adding..." : "Add Store"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
