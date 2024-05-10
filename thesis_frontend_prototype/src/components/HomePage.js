import React from 'react';
import { Link } from 'react-router-dom';

const Dashboard = () => {
  return (
    <div className="dashboard-container">
      <aside className="sidebar">
        <h2>Dashboard Menu</h2>
        <ul>
          <li><Link to="/profile">Profile</Link></li>
          <li><Link to="/my-containers">My Containers</Link></li> {/* Link to My Containers page */}
          <li><Link to="/settings">Settings</Link></li>
        </ul>
      </aside>
      <main className="main-content">
        <header>
          <h1>Welcome to Your Dashboard</h1>
        </header>
        {/* Content */}
      </main>
    </div>
  );
}

export default Dashboard;
