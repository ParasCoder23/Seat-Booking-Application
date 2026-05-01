import React, { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import { bookingsAPI, usersAPI } from '../services/api';
import { useAuth } from '../context/AuthContext';
import CountdownTimer from './CountdownTimer';

const SeatBookingModal = ({ seat, onSuccess, onCancel, timeRemaining }) => {
  const { user } = useAuth();
  const [bookingData, setBookingData] = useState({
    startTime: '',
    endTime: '',
    bookedForUserId: null,
    bookedForUserIds: [] // for meeting room
  });
  const [loading, setLoading] = useState(false);
  const [employees, setEmployees] = useState([]);
  const [showEmployeeSelect, setShowEmployeeSelect] = useState(false);

  useEffect(() => {
    // Load employees if user is admin or manager
    if (user && (user.role === 'ADMIN' || user.role === 'MANAGER')) {
      loadEmployees();
    }
  }, [user]);

  const loadEmployees = async () => {
    try {
      const response = await usersAPI.getEmployees();
      setEmployees(response.data);
    } catch (error) {
      console.error('Error loading employees:', error);
    }
  };

  const handleChange = (e) => {
    setBookingData({
      ...bookingData,
      [e.target.name]: e.target.value
    });
  };

  const handleEmployeeChange = (e) => {
    const selectedUserId = e.target.value;
    setBookingData({
      ...bookingData,
      bookedForUserId: selectedUserId === '' ? null : parseInt(selectedUserId),
      bookedForUserIds: [] // clear multi-select if switching to single
    });
  };

  // When toggling to meeting room, clear single user selection
  const handleShowEmployeeSelect = (checked) => {
    setShowEmployeeSelect(checked);
    if (!checked) {
      setBookingData({...bookingData, bookedForUserId: null, bookedForUserIds: []});
    } else if (seat.type === 'MEETING_ROOM') {
      setBookingData({...bookingData, bookedForUserId: null});
    } else {
      setBookingData({...bookingData, bookedForUserIds: []});
    }
  };

  const handleMultiSelectChange = (e) => {
    const selectedOptions = Array.from(e.target.selectedOptions);
    const selectedIds = selectedOptions.map(option => parseInt(option.value));
    setBookingData({
      ...bookingData,
      bookedForUserIds: selectedIds
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      // Validate times
      if (!bookingData.startTime || !bookingData.endTime) {
        toast.error('Please select both start and end time', { autoClose: 5000 });
        setLoading(false);
        return;
      }

      // Debug: Check user object
      console.log('Current user object:', user);
      console.log('User ID:', user?.id);

      // Validate user ID
      if (!user?.id) {
        console.error('User ID missing. User object:', user);
        toast.error('User ID not available. Please logout and login again.', { autoClose: 5000 });
        setLoading(false);
        return;
      }

      let bookingRequest = {
        seatId: seat.id,
        startTime: bookingData.startTime,
        endTime: bookingData.endTime,
        bookedByUserId: user.id, // always include the current user as the booker
        seatType: seat.type // always include seatType for backend logic
      };

      // Handle booking for specific user (regular seat or admin/manager booking for someone else)
      let isMeetingRoom = false;
      if (seat.type === 'MEETING_ROOM') {
        // For meeting room - multiple employees
        isMeetingRoom = true;
        const validUserIds = (bookingData.bookedForUserIds || []).filter(id => id != null && !isNaN(id));
        if (!validUserIds.length) {
          toast.error('Please select at least one employee for the meeting.', { autoClose: 5000 });
          setLoading(false);
          return;
        }
        bookingRequest.bookedForUserIds = validUserIds;
        console.log('Meeting room booking request:', bookingRequest);
      } else {
        // For regular seat - single employee
        if (bookingData.bookedForUserId != null && !isNaN(bookingData.bookedForUserId)) {
          // Booking for someone else
          bookingRequest.bookedForUserId = bookingData.bookedForUserId;
        } else {
          // Booking for self
          bookingRequest.bookedForUserId = user.id;
        }
        console.log('Regular seat booking request:', bookingRequest);
      }

      // Make the booking request
      await bookingsAPI.createBooking(bookingRequest);

      // Show success message based on booking type
      if (isMeetingRoom) {
        const selectedEmployees = bookingData.bookedForUserIds.map(id => employees.find(emp => emp.id === id));
        const employeeNames = selectedEmployees.map(emp => emp.username).join(', ');
        toast.success(`Meeting room booking created successfully for ${employeeNames}!`, { autoClose: 5000 });
      } else if (bookingData.bookedForUserId && bookingData.bookedForUserId !== user.id) {
        const selectedEmployee = employees.find(emp => emp.id === bookingData.bookedForUserId);
        if (selectedEmployee) {
          toast.success(`Seat booking created successfully for ${selectedEmployee.username}!`, { autoClose: 5000 });
        } else {
          toast.success('Seat booking created successfully!', { autoClose: 5000 });
        }
      } else {
        toast.success('Your seat booking created successfully!', { autoClose: 5000 });
      }

      onSuccess();
    } catch (error) {
      console.error('Error creating booking:', error);

      if (error.response?.data) {
        const errorData = error.response.data;

        if (typeof errorData === 'string') {
          // Handle string error responses
          if (errorData.includes('lock') || errorData.includes('expired')) {
            toast.error('Your seat selection has expired. Please select the seat again.', {
              autoClose: 5000,
            });
            onCancel(); // Close modal to force re-selection
          } else {
            toast.error(errorData);
          }
        } else if (errorData.error || errorData.message) {
          const errorMsg = errorData.error || errorData.message;

          if (errorMsg.includes('lock') || errorMsg.includes('expired')) {
            toast.error('Your seat selection has expired. Please select the seat again.', {
              autoClose: 5000,
            });
            onCancel(); // Close modal to force re-selection
          } else if (errorMsg.includes('already booked')) {
            toast.error('This seat was just booked by someone else. Please select another seat.', {
              autoClose: 5000,
            });
            onCancel(); // Close modal
          } else if (errorMsg.includes('cannot deserialize')) {
            toast.error('Invalid time format. Please check your start and end times.', {
              autoClose: 5000,
            });
          } else {
            toast.error(errorMsg);
          }
        } else {
          toast.error('Failed to create booking. Please try again.');
        }
      } else {
        toast.error('Failed to connect to server. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  const getMinDateTime = () => {
    const now = new Date();
    const year = now.getFullYear();
    const month = (now.getMonth() + 1).toString().padStart(2, '0');
    const date = now.getDate().toString().padStart(2, '0');
    const hours = now.getHours().toString().padStart(2, '0');
    const minutes = now.getMinutes().toString().padStart(2, '0');
    return `${year}-${month}-${date}T${hours}:${minutes}`;
  };

  const getSeatIcon = (seatType) => {
    switch (seatType) {
      case 'MEETING_ROOM':
        return '🏢';
      case 'HOT_DESK':
        return '💻';
      default:
        return '💺';
    }
  };

  return (
    <div style={{
      position: 'fixed',
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      backgroundColor: 'rgba(0, 0, 0, 0.65)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 1000,
      padding: '20px',
      overflowY: 'auto'
    }}>
      <div style={{
        backgroundColor: 'white',
        borderRadius: '16px',
        padding: '45px',
        maxWidth: '550px',
        width: '100%',
        minWidth: '300px',
        maxHeight: '95vh',
        overflowY: 'auto',
        boxShadow: '0 15px 50px rgba(0, 0, 0, 0.25)',
        animation: 'slideUp 0.3s ease'
      }}>
        {/* Header */}
        <div style={{ marginBottom: '35px', borderBottom: '3px solid #f0f0f0', paddingBottom: '25px', textAlign: 'center' }}>
          <div style={{ fontSize: '48px', marginBottom: '15px' }}>
            {getSeatIcon(seat.type)}
          </div>
          <h1 style={{
            margin: '0 0 12px 0',
            fontSize: '28px',
            fontWeight: '700',
            color: '#1a1a1a'
          }}>
            Book {seat.type === 'MEETING_ROOM' ? 'Meeting Room' : 'Seat'}
          </h1>
          <p style={{
            margin: '5px 0',
            fontSize: '20px',
            fontWeight: '600',
            color: '#667eea',
            letterSpacing: '0.5px'
          }}>
            {seat.seatNumber}
          </p>
          <p style={{
            margin: '8px 0 0 0',
            fontSize: '15px',
            color: '#666'
          }}>
            Floor {seat.floor} • {seat.type.replace('_', ' ')}
          </p>

          {/* Timer */}
          {timeRemaining && (
            <div style={{
              marginTop: '18px',
              padding: '15px',
              backgroundColor: '#fff3cd',
              borderRadius: '10px',
              border: '2px solid #ffc107'
            }}>
              <p style={{ margin: '0 0 8px 0', fontSize: '14px', fontWeight: '600', color: '#856404' }}>
                ⏱️ Lock expires in:
              </p>
              <CountdownTimer expiresAt={Date.now() + timeRemaining} onExpire={onCancel} />
            </div>
          )}
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>

          {/* Admin/Manager Selection */}
          {(user?.role === 'ADMIN' || user?.role === 'MANAGER') && (
            <div style={{
              padding: '18px',
              backgroundColor: '#f8f9fa',
              borderRadius: '10px',
              border: '2px solid #e1e5e9'
            }}>
              <label style={{
                display: 'flex',
                alignItems: 'center',
                gap: '12px',
                fontSize: '16px',
                fontWeight: '600',
                color: '#333',
                cursor: 'pointer',
                margin: '0'
              }}>
                <input
                  type="checkbox"
                  checked={showEmployeeSelect}
                  onChange={(e) => handleShowEmployeeSelect(e.target.checked)}
                  style={{
                    width: '22px',
                    height: '22px',
                    cursor: 'pointer',
                    accentColor: '#667eea'
                  }}
                />
                <span>Book for someone else</span>
              </label>

              {showEmployeeSelect && (
                <div style={{ marginTop: '16px' }}>
                  <label style={{
                    display: 'block',
                    marginBottom: '10px',
                    fontSize: '14px',
                    fontWeight: '600',
                    color: '#555'
                  }}>
                    {seat.type === 'MEETING_ROOM' ? 'Select Employees' : 'Select Employee'}
                  </label>
                  <select
                    {... (seat.type === 'MEETING_ROOM' ? { multiple: true } : {})}
                    value={seat.type === 'MEETING_ROOM'
                      ? bookingData.bookedForUserIds.map(String)
                      : (bookingData.bookedForUserId || '')}
                    onChange={seat.type === 'MEETING_ROOM' ? handleMultiSelectChange : handleEmployeeChange}
                    style={{
                      width: '100%',
                      padding: '14px',
                      fontSize: '15px',
                      border: '2px solid #e1e5e9',
                      borderRadius: '8px',
                      fontFamily: 'inherit',
                      minHeight: seat.type === 'MEETING_ROOM' ? '130px' : 'auto',
                      boxSizing: 'border-box',
                      backgroundColor: 'white'
                    }}
                  >
                    {!seat.type === 'MEETING_ROOM' && <option value="">-- Select Employee --</option>}
                    {employees.map(emp => (
                      <option key={emp.id} value={emp.id}>
                        {emp.username} ({emp.role})
                      </option>
                    ))}
                  </select>
                  <small style={{ display: 'block', marginTop: '8px', color: '#666', fontSize: '13px' }}>
                    {seat.type === 'MEETING_ROOM'
                      ? '👉 Hold Ctrl/Cmd to select multiple • Max 6 employees'
                      : '👉 Choose who will use this seat'}
                  </small>
                </div>
              )}
            </div>
          )}

          {/* Start Time */}
          <div>
            <label style={{
              display: 'block',
              marginBottom: '10px',
              fontSize: '16px',
              fontWeight: '600',
              color: '#1a1a1a'
            }}>
              📍 Start Time
            </label>
            <input
              type="datetime-local"
              name="startTime"
              value={bookingData.startTime}
              onChange={handleChange}
              min={getMinDateTime()}
              required
              style={{
                width: '100%',
                padding: '14px 16px',
                fontSize: '16px',
                border: '2px solid #e1e5e9',
                borderRadius: '10px',
                fontFamily: 'inherit',
                transition: 'all 0.3s ease',
                boxSizing: 'border-box',
                backgroundColor: '#fafafa'
              }}
              onFocus={(e) => {
                e.target.style.borderColor = '#667eea';
                e.target.style.backgroundColor = 'white';
                e.target.style.boxShadow = '0 0 0 3px rgba(102, 126, 234, 0.1)';
              }}
              onBlur={(e) => {
                e.target.style.borderColor = '#e1e5e9';
                e.target.style.backgroundColor = '#fafafa';
                e.target.style.boxShadow = 'none';
              }}
            />
          </div>

          {/* End Time */}
          <div>
            <label style={{
              display: 'block',
              marginBottom: '10px',
              fontSize: '16px',
              fontWeight: '600',
              color: '#1a1a1a'
            }}>
              📍 End Time
            </label>
            <input
              type="datetime-local"
              name="endTime"
              value={bookingData.endTime}
              onChange={handleChange}
              min={bookingData.startTime || getMinDateTime()}
              required
              style={{
                width: '100%',
                padding: '14px 16px',
                fontSize: '16px',
                border: '2px solid #e1e5e9',
                borderRadius: '10px',
                fontFamily: 'inherit',
                transition: 'all 0.3s ease',
                boxSizing: 'border-box',
                backgroundColor: '#fafafa'
              }}
              onFocus={(e) => {
                e.target.style.borderColor = '#667eea';
                e.target.style.backgroundColor = 'white';
                e.target.style.boxShadow = '0 0 0 3px rgba(102, 126, 234, 0.1)';
              }}
              onBlur={(e) => {
                e.target.style.borderColor = '#e1e5e9';
                e.target.style.backgroundColor = '#fafafa';
                e.target.style.boxShadow = 'none';
              }}
            />
          </div>

          {/* Buttons */}
          <div style={{
            display: 'flex',
            gap: '14px',
            marginTop: '15px'
          }}>
            <button
              type="submit"
              disabled={loading}
              style={{
                flex: 1,
                padding: '16px 24px',
                fontSize: '16px',
                fontWeight: '700',
                background: loading
                  ? 'linear-gradient(135deg, #ccc 0%, #999 100%)'
                  : 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                color: 'white',
                border: 'none',
                borderRadius: '10px',
                cursor: loading ? 'not-allowed' : 'pointer',
                transition: 'all 0.3s ease',
                opacity: loading ? 0.7 : 1,
                boxShadow: loading ? 'none' : '0 4px 14px rgba(102, 126, 234, 0.35)',
                letterSpacing: '0.5px'
              }}
              onMouseOver={(e) => !loading && (e.target.style.boxShadow = '0 6px 20px rgba(102, 126, 234, 0.45)')}
              onMouseOut={(e) => !loading && (e.target.style.boxShadow = '0 4px 14px rgba(102, 126, 234, 0.35)')}
            >
              {loading ? '⏳ Confirming...' : '✓ Confirm Booking'}
            </button>
            <button
              type="button"
              onClick={onCancel}
              disabled={loading}
              style={{
                flex: 1,
                padding: '16px 24px',
                fontSize: '16px',
                fontWeight: '700',
                background: '#f0f0f0',
                color: '#333',
                border: '2px solid #e0e0e0',
                borderRadius: '10px',
                cursor: loading ? 'not-allowed' : 'pointer',
                transition: 'all 0.3s ease',
                opacity: loading ? 0.7 : 1,
                letterSpacing: '0.5px'
              }}
              onMouseOver={(e) => !loading && (e.target.style.backgroundColor = '#e0e0e0')}
              onMouseOut={(e) => !loading && (e.target.style.backgroundColor = '#f0f0f0')}
            >
              ✕ Cancel
            </button>
          </div>
        </form>

        {/* Info Box */}
        <div style={{
          marginTop: '25px',
          padding: '18px',
          backgroundColor: '#e7f3ff',
          borderLeft: '5px solid #2196F3',
          borderRadius: '8px',
          fontSize: '14px',
          color: '#1565c0',
          lineHeight: '1.6'
        }}>
          <strong>ℹ️ Important:</strong> Your seat selection will expire in 2 minutes. Please complete your booking to secure the seat.
        </div>
      </div>
    </div>
  );
};

export default SeatBookingModal;
