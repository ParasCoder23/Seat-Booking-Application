import React, { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import { bookingsAPI } from '../services/api';

const MyBookings = () => {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('all'); // all, active, past, cancelled
  const [expandedMeetings, setExpandedMeetings] = useState(new Set()); // Track expanded meetings

  useEffect(() => {
    loadBookings();
  }, []);

  const loadBookings = async () => {
    try {
      setLoading(true);
      const response = await bookingsAPI.getUserBookings();
      // Sort by startTime descending (most recent first)
      const sorted = (response.data || []).sort((a, b) => {
        return new Date(b.startTime) - new Date(a.startTime);
      });
      setBookings(sorted);
    } catch (error) {
      console.error('Error loading bookings:', error);
      toast.error('Failed to load bookings', { autoClose: 12000 });
    } finally {
      setLoading(false);
    }
  };

  // Group bookings by meeting group ID (meetings or admin bookings) or individual bookings
  const groupBookings = (bookingsList) => {
    const grouped = {};
    const individual = [];

    bookingsList.forEach(booking => {
      // Group meeting rooms and admin bookings
      if ((booking.isMeetingRoom || booking.isAdminBooked) && booking.meetingGroupId) {
        // This is a meeting or admin booking
        const groupId = booking.meetingGroupId;
        if (!grouped[groupId]) {
          grouped[groupId] = {
            id: groupId,
            seatNumber: booking.seatNumber,
            startTime: booking.startTime,
            endTime: booking.endTime,
            status: booking.status,
            createdAt: booking.createdAt,
            isMeetingRoom: booking.isMeetingRoom,
            isAdminBooked: booking.isAdminBooked,
            meetingOrganizerName: booking.meetingOrganizerName,
            attendees: booking.meetingAttendees || [],
            bookings: []
          };
        }
        grouped[groupId].bookings.push(booking);
      } else if (booking.isAdminBooked && !booking.meetingGroupId) {
        // Admin booking without meetingGroupId - group by seat + time
        const groupKey = booking.seatNumber + '_' + booking.startTime + '_' + booking.endTime;
        if (!grouped[groupKey]) {
          grouped[groupKey] = {
            id: booking.id,
            seatNumber: booking.seatNumber,
            startTime: booking.startTime,
            endTime: booking.endTime,
            status: booking.status,
            createdAt: booking.createdAt,
            isMeetingRoom: false,
            isAdminBooked: true,
            meetingOrganizerName: booking.meetingOrganizerName,
            attendees: booking.meetingAttendees || [],
            bookings: []
          };
        }
        grouped[groupKey].bookings.push(booking);
      } else {
        // Regular booking
        individual.push(booking);
      }
    });

    return { grouped, individual };
  };

  const handleCancelBooking = async (bookingId, seatNumber, startTime) => {
    const startDateTime = new Date(startTime).toLocaleString();

    if (!window.confirm(`Are you sure you want to cancel your booking for seat ${seatNumber} starting at ${startDateTime}?`)) {
      return;
    }

    try {
      await bookingsAPI.cancelBooking(bookingId);
      toast.success(`Booking for seat ${seatNumber} cancelled successfully. The seat is now available for others to book.`, { autoClose: 8000 });
      loadBookings(); // Refresh the list
    } catch (error) {
      console.error('Error cancelling booking:', error);
      if (error.response?.data?.error) {
        toast.error(error.response.data.error, { autoClose: 12000 });
      } else if (error.response?.data?.message) {
        toast.error(error.response.data.message, { autoClose: 12000 });
      } else {
        toast.error('Failed to cancel booking. Please try again.', { autoClose: 12000 });
      }
    }
  };

  const formatDateTime = (dateString) => {
    return new Date(dateString).toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
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

  const getStatusIcon = (status) => {
    switch (status) {
      case 'CONFIRMED': return '✅';
      case 'CANCELLED': return '❌';
      case 'COMPLETED': return '✔️';
      default: return '📅';
    }
  };

  const isBookingCancellable = (booking) => {
    if (booking.status !== 'CONFIRMED') return false;
    return new Date(booking.startTime) > new Date();
  };

  const isBookingActive = (booking) => {
    const now = new Date();
    const start = new Date(booking.startTime);
    const end = new Date(booking.endTime);
    return booking.status === 'CONFIRMED' && start <= now && end >= now;
  };

  const isBookingPast = (booking) => {
    return new Date(booking.endTime) < new Date();
  };

  const getStatusLabel = (booking) => {
    const now = new Date();
    if (booking.status === 'COMPLETED' || (booking.endTime && new Date(booking.endTime) < now)) {
      return <span style={{ color: '#6c757d', fontWeight: 600 }}>COMPLETED ✓</span>;
    }
    if (booking.status === 'CONFIRMED') {
      return <span style={{ color: '#28a745', fontWeight: 600 }}>CONFIRMED</span>;
    }
    if (booking.status === 'CANCELLED') {
      return <span style={{ color: '#dc3545', fontWeight: 600 }}>CANCELLED</span>;
    }
    return <span>{booking.status}</span>;
  };

  const getFilteredBookings = () => {
    const now = new Date();

    switch (filter) {
      case 'active':
        return bookings.filter(booking =>
          booking.status === 'CONFIRMED' && new Date(booking.endTime) > now
        );
      case 'past':
        return bookings.filter(booking => {
          const endTime = new Date(booking.endTime);
          return booking.status === 'COMPLETED' ||
                 (booking.status === 'CONFIRMED' && endTime < now) ||
                 booking.status === 'CANCELLED';
        });
      case 'cancelled':
        return bookings.filter(booking => booking.status === 'CANCELLED');
      default:
        return bookings;
    }
  };

  const filteredBookings = getFilteredBookings();

  if (loading) {
    return <div className="loading">Loading your bookings...</div>;
  }

  return (
    <div className="container">
      <div style={{ marginBottom: '30px' }}>
        <h2>My Bookings</h2>
        <p style={{ color: '#666', marginTop: '5px' }}>
          Manage your seat bookings
        </p>
      </div>

      {/* Filter Tabs */}
      <div className="card" style={{ marginBottom: '30px' }}>
        <div style={{
          display: 'flex',
          gap: '10px',
          borderBottom: '1px solid #e1e5e9',
          marginBottom: '0'
        }}>
          {[
            { key: 'all', label: 'All Bookings' },
            { key: 'active', label: 'Active' },
            { key: 'past', label: 'Past' },
            { key: 'cancelled', label: 'Cancelled' }
          ].map(tab => (
            <button
              key={tab.key}
              onClick={() => setFilter(tab.key)}
              style={{
                padding: '10px 20px',
                border: 'none',
                background: filter === tab.key ? '#007bff' : 'transparent',
                color: filter === tab.key ? 'white' : '#666',
                borderRadius: '6px 6px 0 0',
                cursor: 'pointer',
                fontWeight: filter === tab.key ? 'bold' : 'normal',
                transition: 'all 0.3s ease'
              }}
            >
              {tab.label}
            </button>
          ))}
        </div>

        <div style={{ padding: '20px 0 0 0' }}>
          <p style={{ margin: 0, color: '#666' }}>
            Showing {filteredBookings.length} booking(s)
          </p>
        </div>
      </div>

      {/* Bookings List */}
      {filteredBookings.length === 0 ? (
        <div className="card text-center">
          <div style={{ padding: '40px' }}>
            <div style={{ fontSize: '3rem', marginBottom: '20px' }}>📅</div>
            <h3 style={{ marginBottom: '10px' }}>No bookings found</h3>
            <p style={{ color: '#666', marginBottom: '20px' }}>
              {filter === 'all'
                ? "You haven't made any bookings yet."
                : `No ${filter} bookings found.`}
            </p>
            <a href="/seats" className="btn">
              Book a Seat
            </a>
          </div>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          {filteredBookings.map(booking => {
            // Backend already returns grouped meetings with meetingAttendees array
            const isMeeting = booking.isMeetingRoom || booking.isAdminBooked;
            const attendeesList = booking.meetingAttendees || booking.attendees || [];
            const meetingKey = booking.meetingGroupId || (booking.seatNumber + '_' + booking.startTime + '_' + booking.endTime);
            const isExpanded = expandedMeetings.has(meetingKey);

            const toggleMeetingExpand = () => {
              const newExpanded = new Set(expandedMeetings);
              if (isExpanded) {
                newExpanded.delete(meetingKey);
              } else {
                newExpanded.add(meetingKey);
              }
              setExpandedMeetings(newExpanded);
            };

            return (
            <div key={booking.id} className="card">
              <div style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'flex-start',
                gap: '20px',
                flexWrap: 'wrap'
              }}>
                <div style={{ flex: 1, minWidth: '300px' }}>
                  <div style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '10px',
                    marginBottom: '15px'
                  }}>
                    <div style={{ fontSize: '1.5rem' }}>
                      {isMeeting ? (booking.isMeetingRoom ? '🏢' : '💼') : '💺'}
                    </div>
                    <div>
                      <h3 style={{ margin: 0, marginBottom: '5px' }}>
                        {isMeeting ? 
                          (booking.isMeetingRoom ? 
                            `Meeting Room: ${booking.seatNumber}` : 
                            `Booking for: ${booking.seatNumber}`)
                          : `Seat ${booking.seatNumber}`
                        }
                        {isMeeting && (
                          <span style={{
                            fontSize: '0.8rem',
                            color: '#666',
                            fontWeight: 'normal',
                            marginLeft: '8px'
                          }}>
                            (Created by: {booking.meetingOrganizerName || booking.bookedByUsername})
                          </span>
                        )}
                        {!isMeeting && booking.bookedForUsername && booking.bookedForUsername !== booking.username && (
                          <span style={{
                            fontSize: '0.8rem',
                            color: '#666',
                            fontWeight: 'normal',
                            marginLeft: '8px'
                          }}>
                            (for {booking.bookedForUsername})
                          </span>
                        )}
                      </h3>
                      <div style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: '8px',
                        flexWrap: 'wrap'
                      }}>
                        {getStatusLabel(booking)}
                        {isBookingActive(booking) && (
                          <span style={{
                            padding: '4px 8px',
                            borderRadius: '12px',
                            fontSize: '0.8rem',
                            backgroundColor: '#17a2b8',
                            color: 'white',
                            fontWeight: '500'
                          }}>
                            🟢 ACTIVE NOW
                          </span>
                        )}
                        {booking.bookedByUsername && booking.bookedByUsername !== booking.username && (
                          <span style={{
                            padding: '4px 8px',
                            borderRadius: '12px',
                            fontSize: '0.8rem',
                            backgroundColor: '#6f42c1',
                            color: 'white',
                            fontWeight: '500'
                          }}>
                            👨‍💼 Booked by {booking.bookedByUsername}
                          </span>
                        )}
                      </div>
                    </div>
                  </div>

                  <div style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
                    gap: '15px',
                    color: '#666'
                  }}>
                    <div>
                      <strong>Start Time:</strong><br />
                      {formatDateTime(booking.startTime)}
                    </div>
                    <div>
                      <strong>End Time:</strong><br />
                      {formatDateTime(booking.endTime)}
                    </div>
                    <div>
                      <strong>Booked On:</strong><br />
                      {formatDateTime(booking.createdAt)}
                    </div>
                  </div>

                  {/* Meeting/Admin Booking Attendees Section */}
                  {isMeeting && (
                    <div style={{
                      marginTop: '20px',
                      padding: '15px',
                      backgroundColor: '#f8f9fa',
                      borderRadius: '8px',
                      borderLeft: booking.isMeetingRoom ? '4px solid #667eea' : '4px solid #6f42c1'
                    }}>
                      {/* Who Booked */}
                      <div style={{ marginBottom: '10px', color: '#555', fontSize: '0.9rem' }}>
                        <strong>Booked By:</strong> 👨‍💼 {booking.meetingOrganizerName || booking.bookedByUsername}
                      </div>

                      {/* Attendees / Booked For - expandable */}
                      {attendeesList.length > 0 && (
                        <>
                          <div style={{
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'center',
                            cursor: 'pointer',
                            marginBottom: isExpanded ? '10px' : '0'
                          }} onClick={toggleMeetingExpand}>
                            <div style={{ fontWeight: 600, color: '#333' }}>
                              📋 {booking.isMeetingRoom ? 'Attendees' : 'Booked For'} ({attendeesList.length})
                            </div>
                            <div style={{ fontSize: '0.8rem', color: booking.isMeetingRoom ? '#667eea' : '#6f42c1' }}>
                              {isExpanded ? '▼ Hide' : '▶ View More'}
                            </div>
                          </div>

                          {isExpanded && (
                            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
                              {attendeesList.map((attendee, idx) => (
                                <span key={idx} style={{
                                  padding: '6px 12px',
                                  backgroundColor: booking.isMeetingRoom ? '#e7f3ff' : '#f0e6ff',
                                  color: booking.isMeetingRoom ? '#0066cc' : '#6f42c1',
                                  borderRadius: '20px',
                                  fontSize: '0.9rem',
                                  fontWeight: '500'
                                }}>
                                  👤 {attendee}
                                </span>
                              ))}
                            </div>
                          )}
                        </>
                      )}
                    </div>
                  )}
                </div>

                <div style={{
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '10px',
                  alignItems: 'flex-end'
                }}>
                  {isBookingCancellable(booking) && (
                    <button
                      onClick={() => handleCancelBooking(booking.id, booking.seatNumber, booking.startTime)}
                      className="btn btn-danger"
                      style={{ fontSize: '0.9rem' }}
                    >
                      Cancel Booking
                    </button>
                  )}

                  <div style={{
                    fontSize: '0.8rem',
                    color: '#666',
                    textAlign: 'right'
                  }}>
                    Booking ID: #{booking.id}
                  </div>
                </div>
              </div>
            </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default MyBookings;
