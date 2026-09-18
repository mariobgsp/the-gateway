"use client";

import { useState } from "react";
import { Toast } from "@/components/Toast";

type ToastState = { msg: string; type: "success" | "error" } | null;

export function useToast() {
  const [toast, setToast] = useState<ToastState>(null);
  const showToast = (msg: string, type: "success" | "error" = "success") => setToast({ msg, type });
  const view = toast ? <Toast message={toast.msg} type={toast.type} onDone={() => setToast(null)} /> : null;
  return { showToast, view };
}
