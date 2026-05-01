import React, { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import { Navigate } from 'react-router-dom';
import { seatsAPI, adminAPI, bookingsAPI } from '../services/api';
import { useAuth } from '../context/AuthContext';

const AdminPanel = () => {
  const { isAdmin } = useAuth();
  const [activeTab, setActiveTab] = useState('seats');
  const [seats, setSeats] = useState([]);
  const [allBookings, setAllBookings] = useState([]);
  const [loading, setLoading] = useState(false);
  const [showCreateSeat, setShowCreateSeat] = useState(false);
  const [showMeetingModal, setShowMeetingModal] = useState(false);
  const [selectedMeeting, setSelectedMeeting] = useState(null);
  const [newSeat, setNewSeat] = useState({
    floor: '',
    seatNumber: '',
    type: 'REGULAR'
  });

  useEffect(() => {
    if (activeTab === 'seats') {
      loadSeats();
    } else if (activeTab === 'bookings') {
      loadAllBookings();
    }
  }, [activeTab]);

  if (!isAdmin()) {
    return <Navigate to="/dashboard" replace />;
  }

  const loadSeats = async () => {
    try {
      setLoading(true);
      const response = await seatsAPI.getAllSeats();
      setSeats(response.data);
    } catch (error) {
      console.error('Error loading seats:', error);
      toast.error('Failed to load seats');
    } finally {
      setLoading(false);
    }
  };

  const loadAllBookings = async () => {
    try {
      setLoading(true);
      const response = await bookingsAPI.getAllBookings();
      // Sort by booking ID descending (newest first)
      const sorted = (response.data || []).sort((a, b) => b.id - a.id);
      setAllBookings(sorted);
    } catch (error) {
      console.error('Error loading bookings:', error);
      toast.error('Failed to load bookings');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateSeat = async (e) => {
    e.preventDefault();
    try {
      await adminAPI.createSeat(newSeat);
      toast.success('Seat created successfully');
      setNewSeat({ floor: '', seatNumber: '', type: 'REGULAR' });
      setShowCreateSeat(false);
      loadSeats();
    } catch (error) {
      console.error('Error creating seat:', error);
      toast.error(error.response?.data?.message || 'Failed to create seat');
    }
  };

  const handleUpdateSeatStatus = async (seatId, status) => {
    try {
      await adminAPI.updateSeatStatus(seatId, status);
      toast.success('Seat status updated successfully');
      loadSeats();
    } catch (error) {
      console.error('Error updating seat status:', error);
      toast.error('Failed to update seat status');
    }
  };

  const handleDeleteSeat = async (seatId) => {
    if (!window.confirm('Are you sure you want to delete this seat?')) {
      return;
    }

    try {
      await adminAPI.deleteSeat(seatId);
      toast.success('Seat deleted successfully');
      loadSeats();
    } catch (error) {
      console.error('Error deleting seat:', error);
      toast.error('Failed to delete seat');
    }
  };

  const formatDateTime = (dateString) => {
    return new Date(dateString).toLocaleString();
  };

  const handleViewMeetingDetails = (booking) => {
    setSelectedMeeting(booking);
    setShowMeetingModal(true);
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'AVAILABLE': return '#28a745';
      case 'BOOKED': return '#dc3545';
      case 'MAINTENANCE': return '#ffc107';
      case 'CONFIRMED': return '#28a745';
      case 'CANCELLED': return '#dc3545';
      case 'COMPLETED': return '#6c757d';
      default: return '#007bff';
    }
  };

  return (
    <div className="container">
      <div style={{ marginBottom: '30px' }}>
        <h2>Admin Panel</h2>
        <p style={{ color: '#666', marginTop: '5px' }}>
          Manage seats and view all bookings
        </p>
      </div>

      {/* Tabs */}
      <div className="card" style={{ marginBottom: '30px' }}>
        <div style={{
          display: 'flex',
          gap: '0',
          borderBottom: '1px solid #e1e5e9',
          marginBottom: '0'
        }}>
          <button
            onClick={() => setActiveTab('seats')}
            style={{
              padding: '15px 25px',
              border: 'none',
              background: activeTab === 'seats' ? '#007bff' : 'transparent',
              color: activeTab === 'seats' ? 'white' : '#666',
              borderRadius: '6px 6px 0 0',
              cursor: 'pointer',
              fontWeight: activeTab === 'seats' ? 'bold' : 'normal'
            }}
          >
            Seat Management
          </button>
          <button
            onClick={() => setActiveTab('bookings')}
            style={{
              padding: '15px 25px',
              border: 'none',
              background: activeTab === 'bookings' ? '#007bff' : 'transparent',
              color: activeTab === 'bookings' ? 'white' : '#666',
              borderRadius: '6px 6px 0 0',
              cursor: 'pointer',
              fontWeight: activeTab === 'bookings' ? 'bold' : 'normal'
            }}
          >
            All Bookings
          </button>
        </div>
      </div>

      {/* Seat Management Tab */}
      {activeTab === 'seats' && (
        <div>
          <div className="card" style={{ marginBottom: '30px' }}>
            <div style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              marginBottom: '20px'
            }}>
              <h3>Seats ({seats.length})</h3>
              <button
                onClick={() => setShowCreateSeat(true)}
                className="btn btn-success"
              >
                + Add New Seat
              </button>
            </div>

            {/* Create Seat Form */}
            {showCreateSeat && (
              <div style={{
                padding: '20px',
                backgroundColor: '#f8f9fa',
                borderRadius: '8px',
                marginBottom: '20px'
              }}>
                <h4 style={{ marginBottom: '15px' }}>Create New Seat</h4>
                <form onSubmit={handleCreateSeat}>
                  <div style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
                    gap: '15px',
                    marginBottom: '15px'
                  }}>
                    <div className="form-group" style={{ marginBottom: 0 }}>
                      <label>Floor</label>
                      <input
                        type="number"
                        value={newSeat.floor}
                        onChange={(e) => setNewSeat({...newSeat, floor: e.target.value})}
                        required
                        min="1"
                      />
                    </div>
                    <div className="form-group" style={{ marginBottom: 0 }}>
                      <label>Seat Number</label>
                      <input
                        type="text"
                        value={newSeat.seatNumber}
                        onChange={(e) => setNewSeat({...newSeat, seatNumber: e.target.value})}
                        required
                        placeholder="e.g., 1-01"
                      />
                    </div>
                    <div className="form-group" style={{ marginBottom: 0 }}>
                      <label>Type</label>
                      <select
                        value={newSeat.type}
                        onChange={(e) => setNewSeat({...newSeat, type: e.target.value})}
                        required
                      >
                        <option value="REGULAR">Regular</option>
                        <option value="HOT_DESK">Hot Desk</option>
                        <option value="MEETING_ROOM">Meeting Room</option>
                      </select>
                    </div>
                  </div>
                  <div style={{ display: 'flex', gap: '10px' }}>
                    <button type="submit" className="btn btn-success">
                      Create Seat
                    </button>
                    <button
                      type="button"
                      onClick={() => setShowCreateSeat(false)}
                      className="btn btn-secondary"
                    >
                      Cancel
                    </button>
                  </div>
                </form>
              </div>
            )}

            {loading ? (
              <div className="loading">Loading seats...</div>
            ) : (
              <div style={{ overflowX: 'auto' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                  <thead>
                    <tr style={{ borderBottom: '2px solid #e1e5e9' }}>
                      <th style={{ padding: '12px', textAlign: 'left' }}>Seat Number</th>
                      <th style={{ padding: '12px', textAlign: 'left' }}>Floor</th>
                      <th style={{ padding: '12px', textAlign: 'left' }}>Type</th>
                      <th style={{ padding: '12px', textAlign: 'left' }}>Status</th>
                      <th style={{ padding: '12px', textAlign: 'left' }}>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {seats.map(seat => (
                      <tr key={seat.id} style={{ borderBottom: '1px solid #e1e5e9' }}>
                        <td style={{ padding: '12px' }}>{seat.seatNumber}</td>
                        <td style={{ padding: '12px' }}>{seat.floor}</td>
                        <td style={{ padding: '12px' }}>{seat.type}</td>
                        <td style={{ padding: '12px' }}>
                          <span style={{
                            padding: '4px 8px',
                            borderRadius: '12px',
                            fontSize: '0.8rem',
                            backgroundColor: getStatusColor(seat.status) + '20',
                            color: getStatusColor(seat.status),
                            fontWeight: '500'
                          }}>
                            {seat.status}
                          </span>
                        </td>
                        <td style={{ padding: '12px' }}>
                          <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                            <select
                              value={seat.status}
                              onChange={(e) => handleUpdateSeatStatus(seat.id, e.target.value)}
                              style={{
                                padding: '4px 8px',
                                borderRadius: '4px',
                                border: '1px solid #ccc',
                                fontSize: '0.8rem'
                              }}
                            >
                              <option value="AVAILABLE">Available</option>
                              <option value="MAINTENANCE">Maintenance</option>
                            </select>
                            <button
                              onClick={() => handleDeleteSeat(seat.id)}
                              className="btn btn-danger"
                              style={{
                                fontSize: '0.7rem',
                                padding: '4px 8px',
                                minWidth: 'auto'
                              }}
                            >
                              Delete
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}

      {/* All Bookings Tab */}
      {activeTab === 'bookings' && (
        <div className="card">
          <h3 style={{ marginBottom: '20px' }}>All Bookings ({allBookings.length})</h3>

          {loading ? (
            <div className="loading">Loading bookings...</div>
          ) : allBookings.length === 0 ? (
            <div className="text-center" style={{ padding: '40px', color: '#666' }}>
              No bookings found
            </div>
          ) : (
            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                <thead>
                  <tr style={{ borderBottom: '2px solid #e1e5e9' }}>
                    <th style={{ padding: '12px', textAlign: 'left' }}>Booking ID</th>
                    <th style={{ padding: '12px', textAlign: 'left' }}>User</th>
                    <th style={{ padding: '12px', textAlign: 'left' }}>Seat</th>
                    <th style={{ padding: '12px', textAlign: 'left' }}>Start Time</th>
                    <th style={{ padding: '12px', textAlign: 'left' }}>End Time</th>
                    <th style={{ padding: '12px', textAlign: 'left' }}>Status</th>
                    <th style={{ padding: '12px', textAlign: 'left' }}>Created</th>
                  </tr>
                </thead>
                <tbody>
                  {allBookings.map(booking => (
                    <tr key={booking.id} style={{ borderBottom: '1px solid #e1e5e9' }}>
                      <td style={{ padding: '12px' }}>#{booking.id}</td>
                      <td style={{ padding: '12px' }}>
                        {booking.isMeetingRoom || booking.isAdminBooked ? (
                          <strong>{booking.meetingOrganizerName}</strong>
                        ) : (
                          booking.username
                        )}
                      </td>
                      <td style={{ padding: '12px' }}>
                        {booking.isMeetingRoom || booking.isAdminBooked ? (
                          <span  onClick={() => handleViewMeetingDetails(booking)}
                            style={{
                              cursor: 'pointer',
                              color: '#007bff',
                              textDecoration: 'underline',
                              fontWeight: '500'
                            }}>
                            {booking.isMeetingRoom ? '🏢' : '💼'} {booking.seatNumber}
                          </span>
                        ) : (
                          booking.seatNumber
                        )}
                      </td>
                      <td style={{ padding: '12px' }}>
                        {formatDateTime(booking.startTime)}
                      </td>
                      <td style={{ padding: '12px' }}>
                        {formatDateTime(booking.endTime)}
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
                      <td style={{ padding: '12px' }}>
                        {formatDateTime(booking.createdAt)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* Meeting/Admin Booking Details Modal */}
      {showMeetingModal && selectedMeeting && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: 'rgba(0, 0, 0, 0.5)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000
        }}>
          <div style={{
            backgroundColor: 'white',
            borderRadius: '12px',
            padding: '30px',
            maxWidth: '500px',
            width: '90%',
            boxShadow: '0 10px 40px rgba(0, 0, 0, 0.3)',
            maxHeight: '80vh',
            overflowY: 'auto'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
              <h2 style={{ margin: 0, color: '#333' }}>
                {selectedMeeting.isMeetingRoom ? '🏢 Meeting Room Details' : '💼 Booking Details'}
              </h2>
              <button
                onClick={() => setShowMeetingModal(false)}
                style={{
                  background: 'none',
                  border: 'none',
                  fontSize: '24px',
                  cursor: 'pointer',
                  color: '#666'
                }}
              >
                ✕
              </button>
            </div>

            <div style={{ borderBottom: '1px solid #e1e5e9', marginBottom: '20px', paddingBottom: '20px' }}>
              <div style={{ marginBottom: '15px' }}>
                <strong style={{ color: '#666' }}>{selectedMeeting.isMeetingRoom ? 'Meeting Room' : 'Seat'}:</strong>
                <div style={{ fontSize: '1.1rem', marginTop: '5px', color: '#333' }}>{selectedMeeting.seatNumber}</div>
              </div>

              <div style={{ marginBottom: '15px' }}>
                <strong style={{ color: '#666' }}>{selectedMeeting.isMeetingRoom ? 'Organizer' : 'Booked By'}:</strong>
                <div style={{ fontSize: '0.95rem', marginTop: '5px', color: '#333' }}>👤 {selectedMeeting.meetingOrganizerName}</div>
              </div>

              <div style={{ marginBottom: '15px' }}>
                <strong style={{ color: '#666' }}>Time:</strong>
                <div style={{ fontSize: '0.9rem', marginTop: '5px', color: '#333' }}>
                  <div>Start: {formatDateTime(selectedMeeting.startTime)}</div>
                  <div>End: {formatDateTime(selectedMeeting.endTime)}</div>
                </div>
              </div>

              <div>
                <strong style={{ color: '#666' }}>Status:</strong>
                <div style={{ marginTop: '5px' }}>
                  <span style={{
                    padding: '6px 12px',
                    borderRadius: '12px',
                    fontSize: '0.85rem',
                    backgroundColor: getStatusColor(selectedMeeting.status) + '30',
                    color: getStatusColor(selectedMeeting.status),
                    fontWeight: '600'
                  }}>
                    {selectedMeeting.status}
                  </span>
                </div>
              </div>
            </div>

            <div style={{ marginBottom: '20px' }}>
              <strong style={{ color: '#666', display: 'block', marginBottom: '10px' }}>
                📋 {selectedMeeting.isMeetingRoom ? 'Attendees' : 'Booked For'} ({selectedMeeting.meetingAttendees?.length || 0})
              </strong>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
                {selectedMeeting.meetingAttendees && selectedMeeting.meetingAttendees.length > 0 ? (
                  selectedMeeting.meetingAttendees.map((attendee, idx) => (
                    <span key={idx} style={{
                      padding: '6px 12px',
                      backgroundColor: '#e7f3ff',
                      color: '#0066cc',
                      borderRadius: '20px',
                      fontSize: '0.9rem',
                      fontWeight: '500'
                    }}>
                      👤 {attendee}
                    </span>
                  ))
                ) : (
                  <span style={{ color: '#666' }}>No attendees listed</span>
                )}
              </div>
            </div>

            <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end' }}>
              <button
                onClick={() => setShowMeetingModal(false)}
                className="btn btn-primary"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminPanel;
