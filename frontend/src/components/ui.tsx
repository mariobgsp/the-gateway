import type { ReactNode } from "react";
import { Navbar } from "@/components/Navbar";

export function AppShell({ children, username }: { children: ReactNode; username?: string }) {
  return <div className="app-container animate-fade-in"><Navbar username={username} />{children}</div>;
}

export function StateBlock({ title, detail, error = false, empty }: { title: string; detail?: string; error?: boolean; empty?: ReactNode }) {
  return <div className={`state-block${error ? " state-error" : ""}`}>
    {empty}
    <p className="font-semibold">{title}</p>
    {detail && <p className="text-sm mt-1">{detail}</p>}
  </div>;
}

export function Modal({ title, children, onClose, wide = false }: { title: string; children: ReactNode; onClose: () => void; wide?: boolean }) {
  return <div className="modal-overlay" onClick={onClose}><div className={`modal-box${wide ? " modal-wide" : ""}`} onClick={(e) => e.stopPropagation()}>
    <div className="modal-header"><h2 className="modal-title">{title}</h2><button className="modal-close" onClick={onClose}>✕</button></div>
    {children}
  </div></div>;
}

export function Breadcrumb({ section, current, onHome }: { section: string; current: string; onHome: () => void }) {
  return <div className="breadcrumb"><span className="breadcrumb-link" onClick={onHome}>Dashboard</span><span className="breadcrumb-sep">›</span><span className="breadcrumb-link" onClick={onHome}>{section}</span><span className="breadcrumb-sep">›</span><span className="breadcrumb-current">{current}</span></div>;
}

export function Tabs({ active, onChange }: { active: "api" | "store"; onChange: (tab: "api" | "store") => void }) {
  return <div className="tabs-bar"><button className={`tab-btn ${active === "api" ? "active" : ""}`} onClick={() => onChange("api")}>API List</button><button className={`tab-btn ${active === "store" ? "active" : ""}`} onClick={() => onChange("store")}>Store Account</button></div>;
}

export function Button({ children, variant = "primary", ...props }: React.ButtonHTMLAttributes<HTMLButtonElement> & { variant?: "primary" | "outline" | "danger" | "success" | "ghost" }) {
  return <button className={`btn btn-${variant}`} {...props}>{children}</button>;
}

export function Field({ label, children }: { label: string; children: ReactNode }) {
  return <div className="form-group"><label className="form-label">{label}</label>{children}</div>;
}
