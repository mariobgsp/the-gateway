import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

// ─── Mock data (would come from API in real app) ───────────────────────────────
const APIS: Record<string, {
  name: string; path: string; method: string; status: string;
  apiHost: string; apiPath: string; storeSubscription: string;
  hasHeader: boolean; headerContent: string;
  hasReqParam: boolean; reqParamContent: string;
  hasReqBody: boolean; reqBodyContent: string;
}> = {
  '1': { name: 'Gateway-Example-1', path: 'gateway-example-1', method: 'GET',  status: 'created',    apiHost: 'https://api.example.com', apiPath: '/v1/resource',       storeSubscription: 'default', hasHeader: false, headerContent: '',              hasReqParam: false, reqParamContent: '', hasReqBody: false, reqBodyContent: '' },
  '2': { name: 'Gateway-Example-2', path: 'gateway-example-2', method: 'POST', status: 'published',  apiHost: 'https://api.example.com', apiPath: '/v1/create',         storeSubscription: 'default', hasHeader: true,  headerContent: 'Content-Type',  hasReqParam: false, reqParamContent: '', hasReqBody: true,  reqBodyContent: '{\n  "key": "value"\n}' },
  '3': { name: 'Gateway-Example-3', path: 'gateway-example-3', method: 'PUT',  status: 'deprecated', apiHost: 'https://api.example.com', apiPath: '/v1/update',         storeSubscription: 'default', hasHeader: true,  headerContent: 'Authorization', hasReqParam: false, reqParamContent: '', hasReqBody: true,  reqBodyContent: '{\n  "id": 1\n}' },
  '4': { name: 'Gateway-Cat-Api',   path: 'gateway-catapi',    method: 'GET',  status: 'created',    apiHost: 'https://api.thecatapi.com',apiPath: '/v1/images/search',  storeSubscription: 'default', hasHeader: true,  headerContent: 'x-api-key\nContent-Type', hasReqParam: false, reqParamContent: '', hasReqBody: false, reqBodyContent: '' },
  '5': { name: 'Gateway-Example-4', path: 'gateway-example-4', method: 'GET',  status: 'created',    apiHost: 'https://api.example.com', apiPath: '/v1/resource/search', storeSubscription: 'store-example-1', hasHeader: true, headerContent: 'x-api-key\nContent-Type', hasReqParam: true, reqParamContent: 'Content-Type', hasReqBody: false, reqBodyContent: '' },
};

// ─── Toast ─────────────────────────────────────────────────────────────────────
const Toast: React.FC<{ message: string; type: 'success' | 'error'; onDone: () => void }> = ({ message, type, onDone }) => {
  useEffect(() => {
    const t = setTimeout(onDone, 2500);
    return () => clearTimeout(t);
  }, [onDone]);
  return (
    <div className={`toast toast-${type}`}>
      {type === 'success' ? '✓' : '✕'}  {message}
    </div>
  );
};

// ─── ApiDetailPage ─────────────────────────────────────────────────────────────
const ApiDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const source = APIS[id || ''] ?? APIS['4'];

  const [name,              setName]              = useState(source.name);
  const [apiHost,           setApiHost]           = useState(source.apiHost);
  const [apiPath,           setApiPath]           = useState(source.apiPath);
  const [storeSubscription, setStoreSubscription] = useState(source.storeSubscription);
  const [hasHeader,         setHasHeader]         = useState(source.hasHeader);
  const [headerContent,     setHeaderContent]     = useState(source.headerContent);
  const [hasReqParam,       setHasReqParam]       = useState(source.hasReqParam);
  const [reqParamContent,   setReqParamContent]   = useState(source.reqParamContent);
  const [hasReqBody,        setHasReqBody]        = useState(source.hasReqBody);
  const [reqBodyContent,    setReqBodyContent]    = useState(source.reqBodyContent);
  const [toast, setToast] = useState<{ msg: string; type: 'success' | 'error' } | null>(null);
  const [tryOpen, setTryOpen] = useState(false);
  const [tryResponse, setTryResponse] = useState('');

  const handleSave = () => {
    setToast({ msg: 'API changes saved successfully!', type: 'success' });
  };

  const handleDelete = () => {
    if (window.confirm(`Delete "${name}"? This action cannot be undone.`)) {
      navigate('/home');
    }
  };

  const handleTryIt = async () => {
    setTryOpen(true);
    setTryResponse('⏳ Sending request...');
    try {
      const res  = await fetch(`${apiHost}${apiPath}`);
      const text = await res.text();
      try {
        setTryResponse(JSON.stringify(JSON.parse(text), null, 2));
      } catch {
        setTryResponse(text);
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Unknown error';
      setTryResponse(`❌ Request failed: ${msg}`);
    }
  };

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
        <span className="breadcrumb-link" onClick={() => navigate('/home')}>API List</span>
        <span className="breadcrumb-sep">›</span>
        <span className="breadcrumb-current">{name}</span>
      </div>

      {/* ── Tab bar ───────────────────────────────────────────── */}
      <div style={{ marginBottom: '1.5rem' }}>
        <div className="tabs-bar">
          <button className="tab-btn active" onClick={() => navigate('/home')}>API List</button>
          <button className="tab-btn" onClick={() => navigate('/home')}>Store Account</button>
        </div>
      </div>

      {/* ── Main card ─────────────────────────────────────────── */}
      <div className="card-glass" style={{ padding: '2rem' }}>

        {/* ── Row summary ─────────────────────────────────────── */}
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '1.25rem',
          padding: '1rem 1.5rem',
          background: 'rgba(124,58,237,0.08)',
          border: '1px solid rgba(124,58,237,0.2)',
          borderRadius: 'var(--r-lg)',
          marginBottom: '2rem',
          flexWrap: 'wrap'
        }}>
          <div style={{ flex: '2 1 160px' }}>
            <div className="form-label">API Name</div>
            <input
              className="input-field"
              value={name}
              onChange={(e) => setName(e.target.value)}
              style={{ fontWeight: 700 }}
            />
          </div>
          <div style={{ flex: '1 1 120px' }}>
            <div className="form-label">Method</div>
            <span className={`method-badge method-${source.method}`} style={{ fontSize: '0.85rem', padding: '0.3rem 0.9rem' }}>
              {source.method}
            </span>
          </div>
          <div style={{ flex: '1 1 120px' }}>
            <div className="form-label">Status</div>
            <span className={`status-pill status-${source.status}`}>{source.status}</span>
          </div>
        </div>

        {/* ── Grid: Host / Path / Subscription ─────────────────── */}
        <div className="grid-3" style={{ marginBottom: '2rem' }}>
          <div className="form-group">
            <label className="form-label">API Host</label>
            <input
              className="input-field"
              placeholder="https://api.example.com"
              value={apiHost}
              onChange={(e) => setApiHost(e.target.value)}
            />
          </div>
          <div className="form-group">
            <label className="form-label">API Path</label>
            <input
              className="input-field"
              placeholder="/v1/endpoint"
              value={apiPath}
              onChange={(e) => setApiPath(e.target.value)}
            />
          </div>
          <div className="form-group">
            <label className="form-label">Store Subscription</label>
            <select
              className="input-field"
              value={storeSubscription}
              onChange={(e) => setStoreSubscription(e.target.value)}
            >
              <option value="default">Default</option>
              <option value="store-example-1">Store Example 1</option>
              <option value="store-example-2">Store Example 2</option>
            </select>
          </div>
        </div>

        <div className="section-divider" />

        {/* ── Grid: Header / Request Param / Request Body ───────── */}
        <div className="grid-3">
          {/* Header */}
          <div className="form-group">
            <label className="toggle-wrapper form-label" style={{ marginBottom: '0.75rem' }}>
              <input
                type="checkbox"
                checked={hasHeader}
                onChange={(e) => setHasHeader(e.target.checked)}
              />
              <span>Header</span>
            </label>
            {hasHeader ? (
              <textarea
                className="input-field"
                rows={5}
                placeholder="key: value&#10;key2: value2"
                value={headerContent}
                onChange={(e) => setHeaderContent(e.target.value)}
              />
            ) : (
              <div style={{
                minHeight: '120px',
                background: 'rgba(255,255,255,0.02)',
                border: '1px dashed var(--clr-border)',
                borderRadius: 'var(--r-md)',
                display: 'flex', alignItems: 'center', justifyContent: 'center'
              }}>
                <span className="text-muted text-sm">Disabled</span>
              </div>
            )}
          </div>

          {/* Request Param */}
          <div className="form-group">
            <label className="toggle-wrapper form-label" style={{ marginBottom: '0.75rem' }}>
              <input
                type="checkbox"
                checked={hasReqParam}
                onChange={(e) => setHasReqParam(e.target.checked)}
              />
              <span>Request Parameters</span>
            </label>
            {hasReqParam ? (
              <textarea
                className="input-field"
                rows={5}
                placeholder="param1=value1&#10;param2=value2"
                value={reqParamContent}
                onChange={(e) => setReqParamContent(e.target.value)}
              />
            ) : (
              <div style={{
                minHeight: '120px',
                background: 'rgba(255,255,255,0.02)',
                border: '1px dashed var(--clr-border)',
                borderRadius: 'var(--r-md)',
                display: 'flex', alignItems: 'center', justifyContent: 'center'
              }}>
                <span className="text-muted text-sm">(no request parameters)</span>
              </div>
            )}
          </div>

          {/* Request Body */}
          <div className="form-group">
            <label className="toggle-wrapper form-label" style={{ marginBottom: '0.75rem' }}>
              <input
                type="checkbox"
                checked={hasReqBody}
                onChange={(e) => setHasReqBody(e.target.checked)}
              />
              <span>Request Body</span>
            </label>
            {hasReqBody ? (
              <textarea
                className="input-field"
                rows={5}
                placeholder='{\n  "key": "value"\n}'
                value={reqBodyContent}
                onChange={(e) => setReqBodyContent(e.target.value)}
              />
            ) : (
              <div style={{
                minHeight: '120px',
                background: 'rgba(255,255,255,0.02)',
                border: '1px dashed var(--clr-border)',
                borderRadius: 'var(--r-md)',
                display: 'flex', alignItems: 'center', justifyContent: 'center'
              }}>
                <span className="text-muted text-sm">(no request body)</span>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* ── Action bar ────────────────────────────────────────── */}
      <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1.5rem' }}>
        <button className="btn btn-ghost" onClick={() => navigate('/home')}>← Back</button>
        <button className="btn btn-success" onClick={handleTryIt}>▶ Try it Now!</button>
        <button className="btn btn-primary" onClick={handleSave}>Save Changes</button>
        <button className="btn btn-danger" onClick={handleDelete}>Delete API</button>
      </div>

      {/* ── "Try It" Modal ────────────────────────────────────── */}
      {tryOpen && (
        <div className="modal-overlay" onClick={() => setTryOpen(false)}>
          <div className="modal-box" style={{ maxWidth: '700px' }} onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2 className="modal-title">Try it Now — {name}</h2>
              <button className="modal-close" onClick={() => setTryOpen(false)}>✕</button>
            </div>
            <div style={{ marginBottom: '0.75rem' }}>
              <span className={`method-badge method-${source.method}`}>{source.method}</span>
              <code style={{ marginLeft: '0.75rem', fontSize: '0.85rem', color: 'var(--txt-secondary)' }}>
                {apiHost}{apiPath}
              </code>
            </div>
            <pre style={{
              background: 'var(--clr-bg)',
              border: '1px solid var(--clr-border)',
              borderRadius: 'var(--r-md)',
              padding: '1rem',
              fontSize: '0.8rem',
              overflowX: 'auto',
              whiteSpace: 'pre-wrap',
              wordBreak: 'break-all',
              color: 'var(--txt-primary)',
              maxHeight: '320px',
              overflowY: 'auto'
            }}>
              {tryResponse}
            </pre>
            <div className="modal-footer">
              <button className="btn btn-outline" onClick={() => setTryOpen(false)}>Close</button>
              <button className="btn btn-success" onClick={handleTryIt}>Retry</button>
            </div>
          </div>
        </div>
      )}

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

export default ApiDetailPage;
