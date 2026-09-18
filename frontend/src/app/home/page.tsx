"use client";

import { useCallback, useState } from "react";
import { useRouter } from "next/navigation";
import { AddApiModal, AddStoreModal } from "@/components/EntityModal";
import { AppShell, StateBlock, Tabs } from "@/components/ui";
import { useResource } from "@/hooks/useResource";
import { useToast } from "@/hooks/useToast";
import { deleteApi, deleteStore, getApis, getStores, saveApi, saveStore } from "@/lib/api";
import type { GatewayListRs, SaveApiPayload, StoreRs } from "@/types";

type Tab = "api" | "store";

export default function HomePage() {
  const router = useRouter();
  const [active, setActive] = useState<Tab>("api");
  const [modal, setModal] = useState<Tab | null>(null);
  const { showToast, view: toast } = useToast();
  const load = useCallback(async () => Promise.all([getApis(), getStores()]), []);
  const { data: [apis, stores], loading, error, reload } = useResource(load, [[], []] as [GatewayListRs[], StoreRs[]]);

  async function addApi(payload: SaveApiPayload) { await saveApi(payload); await reload(); showToast("API created successfully!"); }
  async function addStore(name: string) { await saveStore({ storeName: name }); await reload(); showToast("Store created successfully!"); }
  async function removeApi(item: GatewayListRs) { if (!window.confirm(`Delete "${item.apiName}"?`)) return; try { await deleteApi(item.apiIdentifier); await reload(); showToast("API deleted."); } catch (e) { showToast(e instanceof Error ? e.message : "Delete failed", "error"); } }
  async function removeStore(item: StoreRs) { if (!window.confirm(`Delete "${item.storeName}"?`)) return; try { await deleteStore(item.id); await reload(); showToast("Store deleted."); } catch (e) { showToast(e instanceof Error ? e.message : "Delete failed", "error"); } }

  const isApi = active === "api";
  return <AppShell username="Admin"><div className="dashboard-toolbar"><Tabs active={active} onChange={setActive} /><button className="btn btn-primary" onClick={() => setModal(active)}>+ {isApi ? "Add API" : "Add Store"}</button></div>
    <div className="card-glass table-card"><div className="table-header">{(isApi ? ["API Name", "Path", "Method", "Status", "Actions"] : ["Store Name", "Client ID", "Actions"]).map((label, index) => <div key={label} className={`table-cell ${index > 0 ? "table-cell-center" : ""}`} style={index === 0 ? { flex: 2 } : undefined}>{label}</div>)}</div>
      {loading && <StateBlock title="Loading..." />}
      {!loading && error && <StateBlock title="Failed to load data" detail={error} error />}
      {!loading && !error && (isApi ? apis.map((item) => <div className="table-row" key={item.id} onClick={() => router.push(`/api/${item.apiIdentifier}`)} title="Click to view / edit"><div className="table-cell" style={{ flex: 2, fontWeight: 600 }}>{item.apiName}</div><div className="table-cell table-cell-center text-secondary">{item.apiIdentifier}</div><div className="table-cell table-cell-center"><span className={`method-badge method-${item.method}`}>{item.method}</span></div><div className="table-cell table-cell-center"><span className={`status-pill status-${item.status}`}>{item.status}</span></div><Actions onEdit={() => router.push(`/api/${item.apiIdentifier}`)} onDelete={() => removeApi(item)} /></div>) : stores.map((item) => <div className="table-row" key={item.id} onClick={() => router.push(`/store/${item.id}`)} title="Click to edit"><div className="table-cell" style={{ flex: 2, fontWeight: 600 }}>{item.storeName}</div><div className="table-cell table-cell-center text-secondary">{item.clientId}</div><Actions onEdit={() => router.push(`/store/${item.id}`)} onDelete={() => removeStore(item)} /></div>))}
      {!loading && !error && (isApi ? apis.length === 0 : stores.length === 0) && <StateBlock title={isApi ? "No APIs yet" : "No stores yet"} detail={`Click + Add ${isApi ? "API" : "Store"} to get started`} empty={<div className="empty-icon">{isApi ? "🔌" : "🏪"}</div>} />}
    </div>
    {modal === "api" && <AddApiModal onClose={() => setModal(null)} onAdd={addApi} />}{modal === "store" && <AddStoreModal onClose={() => setModal(null)} onAdd={addStore} />}{toast}</AppShell>;
}

function Actions({ onEdit, onDelete }: { onEdit: () => void; onDelete: () => void }) {
  return <div className="table-cell table-cell-center" onClick={(e) => e.stopPropagation()}><div className="row-actions"><button className="btn btn-outline btn-sm" onClick={onEdit}>Edit</button><button className="btn btn-danger btn-sm" onClick={onDelete}>Delete</button></div></div>;
}
