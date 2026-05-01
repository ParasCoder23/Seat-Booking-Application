import React, { useState, useEffect } from 'react';
import { seatsAPI, bookingsAPI } from '../services/api';
import { useAuth } from '../context/AuthContext';

const Dashboard = () => {
  const [stats, setStats] = useState({
    totalSeats: 0,
    availableSeats: 0,
    bookedSeats: 0,
    myBookings: 0
  });
  const [recentBookings, setRecentBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const { user } = useAuth();

  useEffect(() => {
    loadDashboardData();
  }, []);

  const loadDashboardData = async () => {
    try {
      const [seatsResponse, availableSeatsResponse, userBookingsResponse] = await Promise.all([
        seatsAPI.getAllSeats(),
        seatsAPI.getAvailableSeats(),
        bookingsAPI.getUserBookings()
      ]);

      const totalSeats = seatsResponse.data.length;
      const availableSeats = availableSeatsResponse.data.length;
      const userBookings = userBookingsResponse.data;

      setStats({
        totalSeats,
        availableSeats,
        bookedSeats: totalSeats - availableSeats,
        myBookings: userBookings.filter(b => b.status === 'CONFIRMED').length
      });

      // Get recent bookings (last 5)
      const sortedBookings = userBookings
        .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
        .slice(0, 5);
      setRecentBookings(sortedBookings);

    } catch (error) {
      console.error('Error loading dashboard data:', error);
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'CONFIRMED': return '#28a745';
      case 'CANCELLED': return '#dc3545';
      case 'COMPLETED': return '#6c757d';
      default: return '#007bff';
    }
  };

  if (loading) {
    return <div className="loading">Loading dashboard...</div>;
  }

  return (
    <div className="container">
      <div style={{ marginBottom: '30px' }}>
        <h2>Welcome back, {user?.username}!</h2>
        <p style={{ color: '#666', marginTop: '5px' }}>
          Here's your booking overview
        </p>
      </div>

      {/* Stats Cards */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
        gap: '20px',
        marginBottom: '30px'
      }}>
        <div className="card text-center">
          <h3 style={{ color: '#007bff', fontSize: '2rem', marginBottom: '10px' }}>
            {stats.totalSeats}
          </h3>
          <p style={{ color: '#666' }}>Total Seats</p>
        </div>

        <div className="card text-center">
          <h3 style={{ color: '#28a745', fontSize: '2rem', marginBottom: '10px' }}>
            {stats.availableSeats}
          </h3>
          <p style={{ color: '#666' }}>Available Seats</p>
        </div>

        <div className="card text-center">
          <h3 style={{ color: '#dc3545', fontSize: '2rem', marginBottom: '10px' }}>
            {stats.bookedSeats}
          </h3>
          <p style={{ color: '#666' }}>Booked Seats</p>
        </div>

        <div className="card text-center">
          <h3 style={{ color: '#17a2b8', fontSize: '2rem', marginBottom: '10px' }}>
            {stats.myBookings}
          </h3>
          <p style={{ color: '#666' }}>My Active Bookings</p>
        </div>
      </div>

      {/* Quick Actions */}
      <div className="card" style={{ marginBottom: '30px' }}>
        <h3 style={{ marginBottom: '20px' }}>Quick Actions</h3>
        <div style={{ display: 'flex', gap: '15px', flexWrap: 'wrap' }}>
          <a href="/seats" className="btn">
            Book a Seat
          </a>
          <a href="/my-bookings" className="btn btn-secondary">
            View My Bookings
          </a>
          {user?.role === 'ADMIN' && (
            <a href="/admin" className="btn btn-success">
              Admin Panel
            </a>
          )}
        </div>
      </div>

      {/* Calendar View Button */}
      <div style={{ marginBottom: '30px', textAlign: 'right' }}>
        <a href="/calendar" style={{
          background: '#007bff',
          color: '#fff',
          padding: '10px 20px',
          borderRadius: '5px',
          textDecoration: 'none',
          fontWeight: 'bold',
          boxShadow: '0 2px 8px rgba(0,0,0,0.08)'
        }}>View My Booking Calendar</a>
      </div>

      {/* Color Code Legend */}
      <div style={{ marginBottom: '20px', padding: '10px', background: '#f8f9fa', borderRadius: '5px' }}>
        <strong>Calendar Color Codes:</strong>
        <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', gap: '20px' }}>
          <li><span style={{ background: '#28a745', display: 'inline-block', width: 16, height: 16, borderRadius: 3, marginRight: 6 }}></span> Confirmed</li>
          <li><span style={{ background: '#dc3545', display: 'inline-block', width: 16, height: 16, borderRadius: 3, marginRight: 6 }}></span> Cancelled</li>
          <li><span style={{ background: '#007bff', display: 'inline-block', width: 16, height: 16, borderRadius: 3, marginRight: 6 }}></span> Other</li>
        </ul>
        <div style={{ fontSize: '0.95em', color: '#666', marginTop: 6 }}>
          Click the calendar button above to see all your seat and meeting bookings, including those made by admin/manager for you.
        </div>
      </div>

      {/* Recent Bookings */}
      <div className="card">
        <h3 style={{ marginBottom: '20px' }}>Recent Bookings</h3>
        {recentBookings.length === 0 ? (
          <p style={{ color: '#666', textAlign: 'center', padding: '20px' }}>
            No bookings yet. <a href="/seats">Book your first seat!</a>
          </p>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr style={{ borderBottom: '2px solid #e1e5e9' }}>
                  <th style={{ padding: '12px', textAlign: 'left' }}>Seat</th>
                  <th style={{ padding: '12px', textAlign: 'left' }}>Date</th>
                  <th style={{ padding: '12px', textAlign: 'left' }}>Time</th>
                  <th style={{ padding: '12px', textAlign: 'left' }}>Status</th>
                </tr>
              </thead>
              <tbody>
                {recentBookings.map((booking) => (
                  <tr key={booking.id} style={{ borderBottom: '1px solid #e1e5e9' }}>
                    <td style={{ padding: '12px' }}>{booking.seatNumber}</td>
                    <td style={{ padding: '12px' }}>
                      {new Date(booking.startTime).toLocaleDateString()}
                    </td>
                    <td style={{ padding: '12px' }}>
                      {formatDate(booking.startTime)} - {formatDate(booking.endTime)}
                    </td>
                    <td style={{ padding: '12px' }}>
                      <span style={{
                        padding: '4px 8px',
                        borderRadius: '12px',
                        fontSize: '0.8rem',
                        backgroundColor: getStatusColor(booking.status) + '20',
                        color: getStatusColor(booking.status),
                        fontWeight: '500'
                      }}>
                        {booking.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default Dashboard;
