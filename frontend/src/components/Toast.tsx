"use client";

import { useEffect } from "react";

interface ToastProps {
  message: string;
  type: "success" | "error";
  onDone: () => void;
}

export function Toast({ message, type, onDone }: ToastProps) {
  useEffect(() => {
    const timer = setTimeout(onDone, 2500);
    return () => clearTimeout(timer);
  }, [onDone]);

  return (
    <div className={`toast toast-${type}`}>
      {type === "success" ? "✓" : "✕"} {message}
    </div>
  );
}
