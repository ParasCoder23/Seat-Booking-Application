import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Header from './components/Header';
import Login from './components/Login';
import Register from './components/Register';
import Dashboard from './components/Dashboard';
import SeatGrid from './components/SeatGrid';
import MyBookings from './components/MyBookings';
import AdminPanel from './components/AdminPanel';
import BookingCalendar from './components/BookingCalendar';

function App() {
  return (
    <AuthProvider>
      <div className="App">
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route
            path="/*"
            element={
              <ProtectedRoute>
                <Header />
                <Routes>
                  <Route path="/" element={<Navigate to="/dashboard" replace />} />
                  <Route path="/dashboard" element={<Dashboard />} />
                  <Route path="/seats" element={<SeatGrid />} />
                  <Route path="/my-bookings" element={<MyBookings />} />
                  <Route path="/admin" element={<AdminPanel />} />
                  <Route path="/calendar" element={<BookingCalendar />} />
                </Routes>
              </ProtectedRoute>
            }
          />
        </Routes>
      </div>
    </AuthProvider>
  );
}

export default App;
