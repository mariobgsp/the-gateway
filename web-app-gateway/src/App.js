import './App.css';

import React from 'react';
import { useRoutes } from 'react-router-dom';

import Login from './component/Login.js';
import HomePage from './component/Home.js';

const AppRoutes = () => {
  const routes = useRoutes([
    { path: '/login', element: <Login /> },
    { path: '/home', element: <HomePage /> },
  ]);

  return routes;
};

function App() {
  return (
    <div>
      <AppRoutes />
    </div>
  );
}

export default App;

