import React, { useState, useEffect } from 'react';
import './../component/Home.css';

function HomePage() {
  const [apiList, setApiList] = useState([]);
  const [message, setMessage] = useState('');

  useEffect(() => {
    const fetchApiList = async () => {
      try {
        const token = localStorage.getItem('accessToken');
        const response = await fetch('http://localhost:8080/config/getApiList', {
          method: 'GET',
          headers: {
            'Authorization': `Bearer ${token}`,
          },
        });
        const data = await response.json();
        if (data.status === 'ok') {
          setApiList(data.data);
        } else {
          setMessage(data.message);
        }
      } catch (error) {
        setMessage('Failed to fetch API list.');
      }
    };

    fetchApiList();
  }, []);

  return (
    <div className="home-container">
      <div className="home-box">
        <h1>The Gateway</h1>
        <div className="tab-container">
          <button className="tab-button active">API List</button>
          <button className="tab-button">Store Account</button>
        </div>
        {message && <p className="error-message">{message}</p>}
        <table className="api-table">
          <thead>
            <tr>
              <th>API Name</th>
              <th>API Path</th>
              <th>Method</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {apiList.map((api) => (
              <tr key={api.id}>
                <td>{api.apiName}</td>
                <td>{api.apiIdentifier}</td>
                <td>{api.method}</td> {/* Assume GET method for simplicity */}
                <td>{api.status || 'CREATED'}</td> {/* Default to CREATED if status is null */}
              </tr>
            ))}
          </tbody>
        </table>
        <div className="pagination">
          <button className="pagination-button active">1</button>
          <button className="pagination-button">2</button>
          <button className="pagination-button">3</button>
          <button className="pagination-button">...</button>
          <button className="pagination-button">NEXT</button>
        </div>
        <button className="add-api-button">Add API</button>
      </div>
    </div>
  );
}

export default HomePage;
