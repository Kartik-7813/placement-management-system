import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import './styles/index.css';

/**
 * Main Application Entry Point
 * Initializes the React 18 Virtual DOM root and renders the App component
 * inside strict mode for best-practice development lifecycle checks.
 */
ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
