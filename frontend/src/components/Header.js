import React from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const Header = () => {
  const { user, logout, isAdmin } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const isActive = (path) => location.pathname === path;

  return (
    <header className="header">
      <div className="header-content">
        <div>
          <h1>Office Seat Booking</h1>
        </div>

        <nav style={{ display: 'flex', gap: '20px', alignItems: 'center' }}>
          <Link
            to="/dashboard"
            style={{
              color: 'white',
              textDecoration: 'none',
              fontWeight: isActive('/dashboard') ? 'bold' : 'normal'
            }}
          >
            Dashboard
          </Link>
          <Link
            to="/seats"
            style={{
              color: 'white',
              textDecoration: 'none',
              fontWeight: isActive('/seats') ? 'bold' : 'normal'
            }}
          >
            Book Seats
          </Link>
          <Link
            to="/my-bookings"
            style={{
              color: 'white',
              textDecoration: 'none',
              fontWeight: isActive('/my-bookings') ? 'bold' : 'normal'
            }}
          >
            My Bookings
          </Link>
          {isAdmin() && (
            <Link
              to="/admin"
              style={{
                color: 'white',
                textDecoration: 'none',
                fontWeight: isActive('/admin') ? 'bold' : 'normal'
              }}
            >
              Admin Panel
            </Link>
          )}
        </nav>

        <div className="user-info">
          <div className="user-badge">
            {user?.username} ({user?.role})
          </div>
          <button onClick={handleLogout} className="btn btn-secondary">
            Logout
          </button>
        </div>
      </div>
    </header>
  );
};

export default Header;
