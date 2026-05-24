import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';

type Tab = 'api' | 'store';

// ─── Mock Data ─────────────────────────────────────────────────────────────────

const INITIAL_APIS = [
  { id: '1', name: 'Gateway-Example-1',  path: 'gateway-example-1',  method: 'GET',  status: 'created' },
  { id: '2', name: 'Gateway-Example-2',  path: 'gateway-example-2',  method: 'POST', status: 'published' },
  { id: '3', name: 'Gateway-Example-3',  path: 'gateway-example-3',  method: 'PUT',  status: 'deprecated' },
  { id: '4', name: 'Gateway-Cat-Api',    path: 'gateway-catapi',      method: 'GET',  status: 'created' },
  { id: '5', name: 'Gateway-Example-4', path: 'gateway-example-4',  method: 'GET',  status: 'created' },
];

const INITIAL_STORES = [
  { id: '1', name: 'Store-Example-Satu',     status: 'created' },
  { id: '2', name: 'Store-Example-DuaLima',  status: 'active' },
  { id: '3', name: 'Store-Example-3',         status: 'deactive' },
  { id: '4', name: 'Store-Example-4',         status: 'created' },
  { id: '5', name: 'Store-Example-5',         status: 'created' },
];

// ─── Add API Modal ─────────────────────────────────────────────────────────────

interface AddApiModalProps {
  onClose: () => void;
  onAdd: (api: { name: string; path: string; method: string }) => void;
}

const AddApiModal: React.FC<AddApiModalProps> = ({ onClose, onAdd }) => {
  const [name, setName]     = useState('');
  const [path, setPath]     = useState('');
  const [method, setMethod] = useState('GET');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !path.trim()) return;
    onAdd({ name: name.trim(), path: path.trim(), method });
    onClose();
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-box" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2 className="modal-title">Add New API</h2>
          <button className="modal-close" onClick={onClose}>✕</button>
        </div>

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
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
            <label className="form-label">API Path</label>
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
            <select className="input-field" value={method} onChange={(e) => setMethod(e.target.value)}>
              <option value="GET">GET</option>
              <option value="POST">POST</option>
              <option value="PUT">PUT</option>
              <option value="DELETE">DELETE</option>
              <option value="PATCH">PATCH</option>
            </select>
          </div>

          <div className="modal-footer">
            <button type="button" className="btn btn-outline" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn btn-primary">Add API</button>
          </div>
        </form>
      </div>
    </div>
  );
};

// ─── Add Store Modal ───────────────────────────────────────────────────────────

interface AddStoreModalProps {
  onClose: () => void;
  onAdd: (store: { name: string }) => void;
}

const AddStoreModal: React.FC<AddStoreModalProps> = ({ onClose, onAdd }) => {
  const [name, setName] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    onAdd({ name: name.trim() });
    onClose();
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-box" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2 className="modal-title">Add New Store</h2>
          <button className="modal-close" onClick={onClose}>✕</button>
        </div>

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
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

          <div className="modal-footer">
            <button type="button" className="btn btn-outline" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn btn-primary">Add Store</button>
          </div>
        </form>
      </div>
    </div>
  );
};

// ─── Main HomePage ─────────────────────────────────────────────────────────────

const HomePage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<Tab>('api');
  const [showAddApi, setShowAddApi]     = useState(false);
  const [showAddStore, setShowAddStore] = useState(false);
  const [apis, setApis]     = useState(INITIAL_APIS);
  const [stores, setStores] = useState(INITIAL_STORES);
  const navigate = useNavigate();

  const handleAddApi = ({ name, path, method }: { name: string; path: string; method: string }) => {
    const id = String(Date.now());
    setApis((prev) => [...prev, { id, name, path, method, status: 'created' }]);
  };

  const handleAddStore = ({ name }: { name: string }) => {
    const id = String(Date.now());
    setStores((prev) => [...prev, { id, name, status: 'created' }]);
  };

  return (
    <div className="app-container animate-fade-in">
      {/* ── Navbar ────────────────────────────────────────────── */}
      <nav className="navbar" style={{ padding: '1.5rem 0', maxWidth: '100%' }}>
        <span className="heading-title" style={{ margin: 0, fontSize: '1.75rem' }}>The Gateway</span>
        <div className="navbar-actions">
          <span className="text-secondary text-sm">Admin</span>
          <button
            className="btn btn-outline btn-sm"
            onClick={() => navigate('/')}
          >
            Sign Out
          </button>
        </div>
      </nav>

      {/* ── Tabs ──────────────────────────────────────────────── */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.25rem' }}>
        <div className="tabs-bar">
          <button
            className={`tab-btn ${activeTab === 'api' ? 'active' : ''}`}
            onClick={() => setActiveTab('api')}
          >
            API List
          </button>
          <button
            className={`tab-btn ${activeTab === 'store' ? 'active' : ''}`}
            onClick={() => setActiveTab('store')}
          >
            Store Account
          </button>
        </div>

        <button
          className="btn btn-primary"
          onClick={() => activeTab === 'api' ? setShowAddApi(true) : setShowAddStore(true)}
        >
          + {activeTab === 'api' ? 'Add API' : 'Add Store'}
        </button>
      </div>

      {/* ── Table ─────────────────────────────────────────────── */}
      <div className="card-glass" style={{ padding: 0, overflow: 'hidden' }}>

        {/* Table Header */}
        {activeTab === 'api' ? (
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
            <div className="table-cell table-cell-center">Status</div>
            <div className="table-cell table-cell-center">Actions</div>
          </div>
        )}

        {/* Table Rows */}
        {activeTab === 'api' && apis.map((api) => (
          <div
            key={api.id}
            className="table-row"
            onClick={() => navigate(`/api/${api.id}`)}
            title="Click to view / edit"
          >
            <div className="table-cell" style={{ flex: 2, fontWeight: 600 }}>{api.name}</div>
            <div className="table-cell table-cell-center text-secondary">{api.path}</div>
            <div className="table-cell table-cell-center">
              <span className={`method-badge method-${api.method}`}>{api.method}</span>
            </div>
            <div className="table-cell table-cell-center">
              <span className={`status-pill status-${api.status}`}>{api.status}</span>
            </div>
            <div
              className="table-cell table-cell-center"
              onClick={(e) => e.stopPropagation()}
            >
              <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'center' }}>
                <button
                  className="btn btn-outline btn-sm"
                  onClick={() => navigate(`/api/${api.id}`)}
                >
                  Edit
                </button>
                <button
                  className="btn btn-danger btn-sm"
                  onClick={() => {
                    if (window.confirm(`Delete "${api.name}"?`)) {
                      setApis((prev) => prev.filter((a) => a.id !== api.id));
                    }
                  }}
                >
                  Delete
                </button>
              </div>
            </div>
          </div>
        ))}

        {activeTab === 'store' && stores.map((store) => (
          <div
            key={store.id}
            className="table-row"
            onClick={() => navigate(`/store/${store.id}`)}
            title="Click to edit"
          >
            <div className="table-cell" style={{ flex: 2, fontWeight: 600 }}>{store.name}</div>
            <div className="table-cell table-cell-center">
              <span className={`status-pill status-${store.status}`}>{store.status}</span>
            </div>
            <div
              className="table-cell table-cell-center"
              onClick={(e) => e.stopPropagation()}
            >
              <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'center' }}>
                <button
                  className="btn btn-outline btn-sm"
                  onClick={() => navigate(`/store/${store.id}`)}
                >
                  Edit
                </button>
                <button
                  className="btn btn-danger btn-sm"
                  onClick={() => {
                    if (window.confirm(`Delete "${store.name}"?`)) {
                      setStores((prev) => prev.filter((s) => s.id !== store.id));
                    }
                  }}
                >
                  Delete
                </button>
              </div>
            </div>
          </div>
        ))}

        {/* Empty state */}
        {activeTab === 'api' && apis.length === 0 && (
          <div style={{ padding: '4rem 2rem', textAlign: 'center', color: 'var(--txt-secondary)' }}>
            <div style={{ fontSize: '2.5rem', marginBottom: '0.5rem' }}>🔌</div>
            <p className="font-semibold">No APIs yet</p>
            <p className="text-sm mt-1">Click "+ Add API" to get started</p>
          </div>
        )}
        {activeTab === 'store' && stores.length === 0 && (
          <div style={{ padding: '4rem 2rem', textAlign: 'center', color: 'var(--txt-secondary)' }}>
            <div style={{ fontSize: '2.5rem', marginBottom: '0.5rem' }}>🏪</div>
            <p className="font-semibold">No stores yet</p>
            <p className="text-sm mt-1">Click "+ Add Store" to get started</p>
          </div>
        )}
      </div>

      {/* ── Pagination ────────────────────────────────────────── */}
      <div className="pagination">
        <button className="page-btn active">1</button>
        <button className="page-btn">2</button>
        <button className="page-btn">3</button>
        <span className="text-muted text-sm" style={{ padding: '0 0.25rem' }}>…</span>
        <button className="page-btn" style={{ padding: '0 0.875rem' }}>Next →</button>
      </div>

      {/* ── Modals ────────────────────────────────────────────── */}
      {showAddApi   && <AddApiModal   onClose={() => setShowAddApi(false)}   onAdd={handleAddApi} />}
      {showAddStore && <AddStoreModal onClose={() => setShowAddStore(false)} onAdd={handleAddStore} />}
    </div>
  );
};

export default HomePage;
