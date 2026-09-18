"use client";

import { useState } from "react";
import { Modal } from "@/components/ui";
import { executeApi } from "@/lib/api";

function parseKeyValue(text: string) { return Object.fromEntries(text.split("\n").flatMap((line) => { const i = line.indexOf(":"); return i > 0 ? [[line.slice(0, i).trim(), line.slice(i + 1).trim()]] : []; })); }

export function TryItModal({ name, host, path, method, identifier, onClose }: { name: string; host: string; path: string; method: string; identifier: string; onClose: () => void }) {
  const [headers, setHeaders] = useState(""); const [params, setParams] = useState(""); const [body, setBody] = useState(""); const [response, setResponse] = useState("");
  async function send() { setResponse("⏳ Sending request..."); try { const result = await executeApi(identifier, { method, headers: parseKeyValue(headers), params: parseKeyValue(params), body }); setResponse(result.body); } catch (e) { setResponse(`❌ Request failed: ${e instanceof Error ? e.message : "Unknown error"}`); } }
  return <Modal title={`Try it Now — ${name}`} onClose={onClose} wide><div className="try-target"><span className={`method-badge method-${method}`}>{method}</span><code>{host}{path}</code></div><div className="grid-3"><label className="form-group"><span className="form-label">Headers (key: value)</span><textarea className="input-field" rows={4} placeholder="x-api-key: your-key" value={headers} onChange={(e) => setHeaders(e.target.value)} /></label><label className="form-group"><span className="form-label">Query Params (key: value)</span><textarea className="input-field" rows={4} placeholder="limit: 10" value={params} onChange={(e) => setParams(e.target.value)} /></label><label className="form-group"><span className="form-label">Request Body (JSON)</span><textarea className="input-field" rows={4} placeholder={'{\n  "key": "value"\n}'} value={body} onChange={(e) => setBody(e.target.value)} /></label></div><pre className="try-response">{response}</pre><div className="modal-footer"><button className="btn btn-outline" onClick={onClose}>Close</button><button className="btn btn-success" onClick={send}>Send Request</button></div></Modal>;
}
