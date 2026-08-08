"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AddApiModal } from "@/components/AddApiModal";
import { AddStoreModal } from "@/components/AddStoreModal";
import { Navbar } from "@/components/Navbar";
import { Toast } from "@/components/Toast";
import { deleteApi, deleteStore, getApis, getStores, saveApi, saveStore } from "@/lib/api";
import type { GatewayListRs, SaveApiPayload, StoreRs } from "@/types";

type Tab = "api" | "store";

export default function HomePage() {
  const [activeTab, setActiveTab] = useState<Tab>("api");
  const [showAddApi, setShowAddApi] = useState(false);
  const [showAddStore, setShowAddStore] = useState(false);
  const [apis, setApis] = useState<GatewayListRs[]>([]);
  const [stores, setStores] = useState<StoreRs[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [toast, setToast] = useState<{ msg: string; type: "success" | "error" } | null>(null);
  const navigate = useRouter();

  const loadData = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [apiList, storeList] = await Promise.all([getApis(), getStores()]);
      setApis(apiList);
      setStores(storeList);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load data");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  const handleAddApi = async (api: SaveApiPayload) => {
    await saveApi(api);
    await loadData();
    setToast({ msg: "API created successfully!", type: "success" });
  };

  const handleAddStore = async (name: string) => {
    await saveStore({ storeName: name });
    await loadData();
    setToast({ msg: "Store created successfully!", type: "success" });
  };

  const handleDeleteApi = async (api: GatewayListRs) => {
    if (!window.confirm(`Delete "${api.apiName}"?`)) return;
    try {
      await deleteApi(api.apiIdentifier);
      await loadData();
      setToast({ msg: "API deleted.", type: "success" });
    } catch (err) {
      setToast({ msg: err instanceof Error ? err.message : "Delete failed", type: "error" });
    }
  };

  const handleDeleteStore = async (store: StoreRs) => {
    if (!window.confirm(`Delete "${store.storeName}"?`)) return;
    try {
      await deleteStore(store.id);
      await loadData();
      setToast({ msg: "Store deleted.", type: "success" });
    } catch (err) {
      setToast({ msg: err instanceof Error ? err.message : "Delete failed", type: "error" });
    }
  };

  return (
    <div className="app-container animate-fade-in">
      <Navbar username="Admin" />

      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "1.25rem" }}>
        <div className="tabs-bar">
          <button
            className={`tab-btn ${activeTab === "api" ? "active" : ""}`}
            onClick={() => setActiveTab("api")}
          >
            API List
          </button>
          <button
            className={`tab-btn ${activeTab === "store" ? "active" : ""}`}
            onClick={() => setActiveTab("store")}
          >
            Store Account
          </button>
        </div>

        <button
          className="btn btn-primary"
          onClick={() => (activeTab === "api" ? setShowAddApi(true) : setShowAddStore(true))}
        >
          + {activeTab === "api" ? "Add API" : "Add Store"}
        </button>
      </div>

      <div className="card-glass" style={{ padding: 0, overflow: "hidden" }}>
        {activeTab === "api" ? (
          <div className="table-header">
            <div className="table-cell" style={{ flex: 2 }}>API Name</div>
            <div className="table-cell table-cell-center">Path</div>
            <div className="table-cell table-cell-center">Method</div>
            <div className="table-cell table-cell-center">Status</div>
            <div className="table-cell table-cell-center">Actions</div>
          </div>
        ) : (
          <div className="table-header">
            <div className="table-cell" style={{ flex: 2 }}>Store Name</div>
            <div className="table-cell table-cell-center">Client ID</div>
            <div className="table-cell table-cell-center">Actions</div>
          </div>
        )}

        {loading && (
          <div style={{ padding: "4rem 2rem", textAlign: "center", color: "var(--txt-secondary)" }}>
            <p className="font-semibold">Loading...</p>
          </div>
        )}

        {!loading && error && (
          <div style={{ padding: "4rem 2rem", textAlign: "center", color: "var(--clr-danger)" }}>
            <p className="font-semibold">Failed to load data</p>
            <p className="text-sm mt-1">{error}</p>
          </div>
        )}

        {!loading && !error && activeTab === "api" && apis.map((api) => (
          <div
            key={api.id}
            className="table-row"
            onClick={() => navigate.push(`/api/${api.apiIdentifier}`)}
            title="Click to view / edit"
          >
            <div className="table-cell" style={{ flex: 2, fontWeight: 600 }}>{api.apiName}</div>
            <div className="table-cell table-cell-center text-secondary">{api.apiIdentifier}</div>
            <div className="table-cell table-cell-center">
              <span className={`method-badge method-${api.method}`}>{api.method}</span>
            </div>
            <div className="table-cell table-cell-center">
              <span className={`status-pill status-${api.status}`}>{api.status}</span>
            </div>
            <div className="table-cell table-cell-center" onClick={(e) => e.stopPropagation()}>
              <div style={{ display: "flex", gap: "0.5rem", justifyContent: "center" }}>
                <button className="btn btn-outline btn-sm" onClick={() => navigate.push(`/api/${api.apiIdentifier}`)}>
                  Edit
                </button>
                <button className="btn btn-danger btn-sm" onClick={() => handleDeleteApi(api)}>
                  Delete
                </button>
              </div>
            </div>
          </div>
        ))}

        {!loading && !error && activeTab === "store" && stores.map((store) => (
          <div
            key={store.id}
            className="table-row"
            onClick={() => navigate.push(`/store/${store.id}`)}
            title="Click to edit"
          >
            <div className="table-cell" style={{ flex: 2, fontWeight: 600 }}>{store.storeName}</div>
            <div className="table-cell table-cell-center text-secondary">{store.clientId}</div>
            <div className="table-cell table-cell-center" onClick={(e) => e.stopPropagation()}>
              <div style={{ display: "flex", gap: "0.5rem", justifyContent: "center" }}>
                <button className="btn btn-outline btn-sm" onClick={() => navigate.push(`/store/${store.id}`)}>
                  Edit
                </button>
                <button className="btn btn-danger btn-sm" onClick={() => handleDeleteStore(store)}>
                  Delete
                </button>
              </div>
            </div>
          </div>
        ))}

        {!loading && !error && activeTab === "api" && apis.length === 0 && (
          <div style={{ padding: "4rem 2rem", textAlign: "center", color: "var(--txt-secondary)" }}>
            <div style={{ fontSize: "2.5rem", marginBottom: "0.5rem" }}>🔌</div>
            <p className="font-semibold">No APIs yet</p>
            <p className="text-sm mt-1">Click + Add API to get started</p>
          </div>
        )}
        {!loading && !error && activeTab === "store" && stores.length === 0 && (
          <div style={{ padding: "4rem 2rem", textAlign: "center", color: "var(--txt-secondary)" }}>
            <div style={{ fontSize: "2.5rem", marginBottom: "0.5rem" }}>🏪</div>
            <p className="font-semibold">No stores yet</p>
            <p className="text-sm mt-1">Click + Add Store to get started</p>
          </div>
        )}
      </div>

      {showAddApi && <AddApiModal onClose={() => setShowAddApi(false)} onAdd={handleAddApi} />}
      {showAddStore && <AddStoreModal onClose={() => setShowAddStore(false)} onAdd={handleAddStore} />}

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
