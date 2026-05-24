import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';

const WelcomePage: React.FC = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault();
    if (!username || !password) {
      setError('Please fill in both fields.');
      return;
    }
    setError('');
    navigate('/home');
  };

  return (
    <div className="login-wrapper">
      <div className="login-card animate-slide-up">
        {/* Logo */}
        <div className="login-logo">
          <div className="login-logo-title">The Gateway</div>
          <div className="login-logo-sub">API Management Platform</div>
        </div>

        {/* Card */}
        <div className="card-glass">
          <h2 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '0.25rem' }}>
            Welcome back
          </h2>
          <p className="text-secondary text-sm" style={{ marginBottom: '1.75rem' }}>
            Sign in to your account to continue
          </p>

          <form onSubmit={handleLogin} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            <div className="form-group">
              <label className="form-label" htmlFor="username">Username</label>
              <input
                id="username"
                type="text"
                className="input-field"
                placeholder="Enter your username"
                value={username}
                onChange={(e) => { setUsername(e.target.value); setError(''); }}
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="password">Password</label>
              <input
                id="password"
                type="password"
                className="input-field"
                placeholder="Enter your password"
                value={password}
                onChange={(e) => { setPassword(e.target.value); setError(''); }}
                required
              />
            </div>

            {error && (
              <p style={{ color: 'var(--clr-danger)', fontSize: '0.8rem', fontWeight: 500 }}>
                {error}
              </p>
            )}

            <button
              type="submit"
              className="btn btn-primary btn-lg"
              style={{ width: '100%', marginTop: '0.25rem' }}
            >
              Sign In
            </button>
          </form>

          <div style={{ marginTop: '1.5rem', textAlign: 'center' }}>
            <p className="text-secondary text-sm">
              Don't have an account?{' '}
              <span
                style={{ color: 'var(--clr-accent)', cursor: 'pointer', fontWeight: 600 }}
                onClick={() => alert('Sign-up coming soon!')}
              >
                Create one
              </span>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default WelcomePage;
