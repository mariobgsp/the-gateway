import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './../component/Login.css';

function Login() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [message, setMessage] = useState('');
  const [accessToken, setAccessToken] = useState('');
  const navigate = useNavigate();

  const handleLogin = async (e) => {
    e.preventDefault();

    const response = await fetch('http://localhost:8080/user/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        username: username,
        password: password,
      }),
    });

    const data = await response.json();

    if (data.status === 'ok') {
      setMessage(data.data.loginMessage);
      setAccessToken(data.data.accessToken);
      // Store the token in local storage or handle it as needed
      localStorage.setItem('accessToken', data.data.accessToken);
      navigate('/home');
    } else {
      setMessage(data.message);
    }
  };

  return (
    <div className="login-container">
      <div className="login-box">
        <h1>The Gateway</h1>
        <form onSubmit={handleLogin}>
          <input
            type="text"
            placeholder="username"
            className="login-input"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
          />
          <input
            type="password"
            placeholder="password"
            className="login-input"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
          <button type="submit" className="login-button">
            login
          </button>
        </form>
        <div className="signup-link">
          <p>
            create account? <a href="#">Sign Up</a>
          </p>
        </div>
      </div>
    </div>
  );
}

export default Login;