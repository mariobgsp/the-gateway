import React, { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

// ─── Mock data ─────────────────────────────────────────────────────────────────
const STORES: Record<string, { name: string; status: string; description: string; webhookUrl: string; apiKeys: string[] }> = {
  '1': { name: 'Store-Example-Satu',    status: 'created',  description: 'First example store',  webhookUrl: '', apiKeys: [] },
  '2': { name: 'Store-Example-DuaLima', status: 'active',   description: 'Second example store', webhookUrl: 'https://webhook.example.com/store2', apiKeys: ['key_abc123'] },
  '3': { name: 'Store-Example-3',       status: 'deactive', description: 'Third example store',  webhookUrl: '', apiKeys: [] },
  '4': { name: 'Store-Example-4',       status: 'created',  description: '',                     webhookUrl: '', apiKeys: [] },
  '5': { name: 'Store-Example-5',       status: 'created',  description: '',                     webhookUrl: '', apiKeys: [] },
};

type SidebarSection = 'general' | 'webhook' | 'apikeys' | 'danger';

// ─── Toast ─────────────────────────────────────────────────────────────────────
const Toast: React.FC<{ message: string; type: 'success' | 'error'; onDone: () => void }> = ({ message, type, onDone }) => {
  React.useEffect(() => {
    const t = setTimeout(onDone, 2500);
    return () => clearTimeout(t);
  }, [onDone]);
  return <div className={`toast toast-${type}`}>{type === 'success' ? '✓' : '✕'}  {message}</div>;
};

// ─── EditStoreAccountPage ──────────────────────────────────────────────────────
const EditStoreAccountPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const source = STORES[id || ''] ?? STORES['1'];

  const [activeSection, setActiveSection] = useState<SidebarSection>('general');
  const [storeName,    setStoreName]    = useState(source.name);
  const [storeStatus,  setStoreStatus]  = useState(source.status);
  const [description,  setDescription]  = useState(source.description);
  const [webhookUrl,   setWebhookUrl]   = useState(source.webhookUrl);
  const [apiKeys,      setApiKeys]      = useState<string[]>(source.apiKeys);
  const [toast, setToast] = useState<{ msg: string; type: 'success' | 'error' } | null>(null);

  const showToast = (msg: string, type: 'success' | 'error' = 'success') =>
    setToast({ msg, type });

  const handleSave = () => showToast('Store account updated successfully!');

  const handleDelete = () => {
    if (window.confirm(`Delete "${storeName}"? This cannot be undone.`)) {
      showToast('Store deleted.', 'error');
      setTimeout(() => navigate('/home'), 1200);
    }
  };

  const handleAddKey = () => {
    const key = `key_${Math.random().toString(36).slice(2, 10)}`;
    setApiKeys((prev) => [...prev, key]);
    showToast('New API key generated!');
  };

  const handleRevokeKey = (key: string) => {
    if (window.confirm(`Revoke key "${key}"?`)) {
      setApiKeys((prev) => prev.filter((k) => k !== key));
      showToast('API key revoked.', 'error');
    }
  };

  const sidebarItems: { key: SidebarSection; icon: string; label: string }[] = [
    { key: 'general', icon: '⚙️', label: 'General' },
    { key: 'webhook', icon: '🔗', label: 'Webhook' },
    { key: 'apikeys', icon: '🔑', label: 'API Keys' },
    { key: 'danger',  icon: '⚠️', label: 'Danger Zone' },
  ];

  return (
    <div className="app-container animate-fade-in">
      {/* ── Navbar ────────────────────────────────────────────── */}
      <nav className="navbar" style={{ padding: '1.5rem 0', maxWidth: '100%' }}>
        <span className="heading-title" style={{ margin: 0, fontSize: '1.75rem' }}>The Gateway</span>
        <div className="navbar-actions">
          <button className="btn btn-outline btn-sm" onClick={() => navigate('/')}>Sign Out</button>
        </div>
      </nav>

      {/* ── Breadcrumb ────────────────────────────────────────── */}
      <div className="breadcrumb">
        <span className="breadcrumb-link" onClick={() => navigate('/home')}>Dashboard</span>
        <span className="breadcrumb-sep">›</span>
        <span className="breadcrumb-link" onClick={() => navigate('/home')}>Store Account</span>
        <span className="breadcrumb-sep">›</span>
        <span className="breadcrumb-current">{storeName}</span>
      </div>

      {/* ── Tab bar ───────────────────────────────────────────── */}
      <div style={{ marginBottom: '1.5rem' }}>
        <div className="tabs-bar">
          <button className="tab-btn" onClick={() => navigate('/home')}>API List</button>
          <button className="tab-btn active">Store Account</button>
        </div>
      </div>

      {/* ── Body: sidebar + content ───────────────────────────── */}
      <div style={{ display: 'grid', gridTemplateColumns: '220px 1fr', gap: '1.5rem', alignItems: 'start' }}>

        {/* ── Sidebar ─────────────────────────────────────────── */}
        <div className="card-glass" style={{ padding: '1rem' }}>
          <div style={{ marginBottom: '1rem', padding: '0.25rem 0.5rem' }}>
            <div style={{ fontWeight: 700, fontSize: '0.95rem', color: 'var(--txt-primary)' }}>{storeName}</div>
            <span className={`status-pill status-${storeStatus}`} style={{ marginTop: '0.375rem' }}>{storeStatus}</span>
          </div>
          <div className="section-divider" />
          <nav className="sidebar-menu">
            {sidebarItems.map((item) => (
              <button
                key={item.key}
                className={`sidebar-item ${activeSection === item.key ? 'active' : ''}`}
                onClick={() => setActiveSection(item.key)}
              >
                <span className="sidebar-icon">{item.icon}</span>
                {item.label}
              </button>
            ))}
          </nav>
        </div>

        {/* ── Content ─────────────────────────────────────────── */}
        <div className="card-glass animate-slide-up" key={activeSection}>

          {/* ── GENERAL ─────────────────────────────────────────── */}
          {activeSection === 'general' && (
            <>
              <div className="page-header">
                <div>
                  <div className="page-title">General Settings</div>
                  <div className="page-subtitle">Update the name, status and description of your store.</div>
                </div>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
                <div className="form-group">
                  <label className="form-label">Store Name</label>
                  <input
                    className="input-field"
                    value={storeName}
                    onChange={(e) => setStoreName(e.target.value)}
                    placeholder="My Store Name"
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Status</label>
                  <select
                    className="input-field"
                    value={storeStatus}
                    onChange={(e) => setStoreStatus(e.target.value)}
                  >
                    <option value="created">Created</option>
                    <option value="active">Active</option>
                    <option value="deactive">Deactive</option>
                  </select>
                </div>

                <div className="form-group">
                  <label className="form-label">Description</label>
                  <textarea
                    className="input-field"
                    rows={4}
                    placeholder="Describe what this store account is used for..."
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                  />
                </div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1.75rem' }}>
                <button className="btn btn-ghost" onClick={() => navigate('/home')}>Cancel</button>
                <button className="btn btn-primary" onClick={handleSave}>Save Changes</button>
              </div>
            </>
          )}

          {/* ── WEBHOOK ─────────────────────────────────────────── */}
          {activeSection === 'webhook' && (
            <>
              <div className="page-header">
                <div>
                  <div className="page-title">Webhook Configuration</div>
                  <div className="page-subtitle">Configure webhook endpoints to receive real-time events.</div>
                </div>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
                <div className="form-group">
                  <label className="form-label">Webhook URL</label>
                  <input
                    className="input-field"
                    type="url"
                    placeholder="https://your-server.com/webhook"
                    value={webhookUrl}
                    onChange={(e) => setWebhookUrl(e.target.value)}
                  />
                </div>

                <div style={{
                  padding: '1rem',
                  background: 'rgba(56,189,248,0.07)',
                  border: '1px solid rgba(56,189,248,0.2)',
                  borderRadius: 'var(--r-md)',
                  fontSize: '0.8rem',
                  color: 'var(--clr-info)',
                  lineHeight: 1.7
                }}>
                  ℹ️  We will send a POST request with an <code>application/json</code> body to this URL whenever an API event occurs.
                </div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1.75rem' }}>
                <button className="btn btn-ghost" onClick={() => setWebhookUrl('')}>Clear</button>
                <button className="btn btn-primary" onClick={() => showToast('Webhook URL saved!')}>Save Webhook</button>
              </div>
            </>
          )}

          {/* ── API KEYS ─────────────────────────────────────────── */}
          {activeSection === 'apikeys' && (
            <>
              <div className="page-header">
                <div>
                  <div className="page-title">API Keys</div>
                  <div className="page-subtitle">Manage keys for this store account. Treat them like passwords.</div>
                </div>
                <button className="btn btn-primary btn-sm" onClick={handleAddKey}>+ Generate Key</button>
              </div>

              {apiKeys.length === 0 ? (
                <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--txt-secondary)' }}>
                  <div style={{ fontSize: '2.5rem', marginBottom: '0.5rem' }}>🔑</div>
                  <p className="font-semibold">No API keys yet</p>
                  <p className="text-sm mt-1">Click "Generate Key" to create one.</p>
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                  {apiKeys.map((key) => (
                    <div key={key} style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '0.875rem 1.25rem',
                      background: 'rgba(255,255,255,0.03)',
                      border: '1px solid var(--clr-border)',
                      borderRadius: 'var(--r-md)'
                    }}>
                      <code style={{ fontSize: '0.85rem', color: 'var(--txt-secondary)', fontFamily: 'monospace' }}>
                        {key.slice(0, 8)}{'•'.repeat(12)}
                      </code>
                      <div style={{ display: 'flex', gap: '0.5rem' }}>
                        <button
                          className="btn btn-ghost btn-sm"
                          onClick={() => { navigator.clipboard.writeText(key); showToast('Key copied!'); }}
                        >
                          Copy
                        </button>
                        <button className="btn btn-danger btn-sm" onClick={() => handleRevokeKey(key)}>
                          Revoke
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </>
          )}

          {/* ── DANGER ZONE ─────────────────────────────────────── */}
          {activeSection === 'danger' && (
            <>
              <div className="page-header">
                <div>
                  <div className="page-title" style={{ color: 'var(--clr-danger)' }}>Danger Zone</div>
                  <div className="page-subtitle">Irreversible and destructive actions.</div>
                </div>
              </div>

              <div style={{
                padding: '1.5rem',
                background: 'rgba(239,68,68,0.06)',
                border: '1px solid rgba(239,68,68,0.25)',
                borderRadius: 'var(--r-lg)',
              }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1rem' }}>
                  <div>
                    <p style={{ fontWeight: 700, color: 'var(--clr-danger)' }}>Delete Store Account</p>
                    <p className="text-secondary text-sm" style={{ marginTop: '0.25rem' }}>
                      Once deleted, all data associated with this store will be permanently removed.
                    </p>
                  </div>
                  <button className="btn btn-danger" onClick={handleDelete}>
                    Delete Store
                  </button>
                </div>
              </div>
            </>
          )}
        </div>
      </div>

      {/* ── Toast ─────────────────────────────────────────────── */}
      {toast && (
        <Toast
          message={toast.msg}
          type={toast.type}
          onDone={() => setToast(null)}
        />
      )}
    </div>
  );
};

export default EditStoreAccountPage;
