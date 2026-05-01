import React, { useState, useEffect, useCallback } from 'react';
import { toast } from 'react-toastify';
import { seatsAPI, bookingsAPI } from '../services/api';
import { useAuth } from '../context/AuthContext';
import SeatBookingModal from './SeatBookingModal';
import CountdownTimer from './CountdownTimer';

// Helper function to adjust color brightness
const adjustColorBrightness = (color, percent) => {
  const num = parseInt(color.replace("#", ""), 16);
  const amt = Math.round(2.55 * percent);
  // const R = Math.max(0, (num >> 16) + amt);
  // const G = Math.max(0, (num >> 8 & 0x00FF) + amt);
  // const B = Math.max(0, (num & 0x0000FF) + amt);
  const R = Math.max(0, ((num >> 16) & 0xFF) + amt);
  const G = Math.max(0, ((num >> 8) & 0xFF) + amt);
  const B = Math.max(0, (num & 0xFF) + amt);
  return "#" + (0x1000000 + (R < 255 ? R < 1 ? 0 : R : 255) * 0x10000 +
    (G < 255 ? G < 1 ? 0 : G : 255) * 0x100 +
    (B < 255 ? B < 1 ? 0 : B : 255))
    .toString(16).slice(1);
};

const SeatGrid = () => {
  const [seats, setSeats] = useState([]);
  const [filteredSeats, setFilteredSeats] = useState([]);
  const [loading, setLoading] = useState(true);
  // const [isRefreshing, setIsRefreshing] = useState(false);
  const [selectedSeat, setSelectedSeat] = useState(null);
  const [lockedSeats, setLockedSeats] = useState(new Set());
  const [showBookingModal, setShowBookingModal] = useState(false);
  const [lockTimer, setLockTimer] = useState(null);
  const [filters, setFilters] = useState({
    floor: '',
    type: '',
    status: '' // Empty string means show ALL statuses (AVAILABLE, BOOKED, LOCKED, MAINTENANCE)
  });

  // const { user, isEmployee } = useAuth();
  const { isEmployee } = useAuth();

  // useEffect(() => {
  //   loadSeats(true); // Initial load

  //   // Auto-refresh seats every 5 seconds to show real-time updates
  //   // This ensures locked seats immediately show as yellow and booked seats as red
  //   // BUT: Don't refresh if booking modal is open (to prevent interference)
  //   const interval = setInterval(() => {
  //     // Only refresh if modal is NOT open
  //     if (!showBookingModal) {
  //       loadSeats(false); // Refresh without showing loading state
  //     }
  //   }, 5000);

  //   return () => clearInterval(interval);
  // }, [showBookingModal]);

  // const loadSeats = async (isInitial = false) => {
    const loadSeats = useCallback( async (isInitial = false) => {
    try {
      if (isInitial) {
        setLoading(true); // Show loading only on initial load
      } else {
        // setIsRefreshing(true); // Silent refresh for auto-updates
      }
      const response = await seatsAPI.getAllSeats();

      // Debug: Log seats with lock info
      console.log('Loaded seats from backend:', response.data);
      const lockedSeatsFromBackend = response.data.filter(seat => seat.isLocked);
      console.log('Locked seats from backend:', lockedSeatsFromBackend);

      setSeats(response.data);
    } catch (error) {
      console.error('Error loading seats:', error);
      if (isInitial) {
        toast.error('Failed to load seats', { autoClose: 12000 });
      }
    } finally {
      if (isInitial) {
        setLoading(false);
      } else {
        // setIsRefreshing(false);
      }
    }
  }, []);

  // useEffect(() => {
  //   applyFilters();
  // }, [seats, filters]);

  useEffect(() => {
    loadSeats(true);
    const interval = setInterval(() => {
      if (!showBookingModal) {
        loadSeats(false);
      }
    }, 5000);

    return () => clearInterval(interval);
  }, [showBookingModal, loadSeats]);

  // const applyFilters = () => {
  const applyFilters = useCallback( () => {
    let filtered = seats;

    if (filters.floor) {
      filtered = filtered.filter(seat => seat.floor.toString() === filters.floor);
    }

    if (filters.type) {
      filtered = filtered.filter(seat => seat.type === filters.type);
    }

    if (filters.status) {
      filtered = filtered.filter(seat => seat.status === filters.status);
    }

    setFilteredSeats(filtered);
  }, [seats, filters]);

  useEffect(() => {
    applyFilters();
  }, [applyFilters]);

  const handleFilterChange = (e) => {
    const { name, value } = e.target;
    setFilters(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const getSeatColor = useCallback((seat) => {
    // Check if seat is locked - either locally or from Redis (backend)
    if (seat.isLocked || lockedSeats.has(seat.id)) {
      return '#ffc107'; // Yellow for locked
    }

    switch (seat.status) {
      case 'AVAILABLE':
        return '#28a745'; // Green
      case 'BOOKED':
        return '#dc3545'; // Red
      case 'MAINTENANCE':
        return '#6c757d'; // Gray
      case 'LOCKED':
        return '#ffc107'; // Yellow
      default:
        return '#6c757d';
    }
  }, [lockedSeats]);

  /*

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

 */

  const canBookSeat = (seat) => {
    if (seat.status !== 'AVAILABLE') return false;
    if (isEmployee() && seat.type === 'MEETING_ROOM') return false;
    return true;
  };

  const handleSeatClick = async (seat) => {
    if (!canBookSeat(seat)) {
      if (seat.status === 'BOOKED') {
        // Show detailed booking information
        if (seat.bookedByUsername && seat.bookedFrom && seat.bookedUntil) {
          const fromTime = new Date(seat.bookedFrom).toLocaleString();
          const untilTime = new Date(seat.bookedUntil).toLocaleString();
          toast.info(
            `This seat is booked by ${seat.bookedByUsername} (${seat.bookedByRole}) from ${fromTime} until ${untilTime}`,
            { autoClose: 8000 }
          );
        } else {
          toast.info('This seat is already booked', { autoClose: 8000 });
        }
      } else if (seat.status === 'MAINTENANCE') {
        toast.info('This seat is under maintenance', { autoClose: 8000 });
      } else if (isEmployee() && seat.type === 'MEETING_ROOM') {
        toast.error('Employees cannot book meeting rooms', { autoClose: 12000 });
      }
      return;
    }

    // Check if seat is locked by Redis
    if (seat.isLocked && seat.lockedByUsername) {
      const timeLeft = seat.lockExpiresAt ? Math.max(0, (seat.lockExpiresAt - Date.now()) / 1000) : 0;
      if (timeLeft > 0) {
        const minutes = Math.floor(timeLeft / 60);
        const seconds = Math.floor(timeLeft % 60);
        const timeStr = minutes > 0 ? `${minutes}m ${seconds}s` : `${seconds}s`;
        toast.warning(
          `This seat is currently being selected by ${seat.lockedByUsername} (${seat.lockedByRole}). Available in ${timeStr}`,
          { autoClose: 5000 }
        );
      } else {
        toast.warning(
          `This seat is currently being selected by ${seat.lockedByUsername} (${seat.lockedByRole})`,
          { autoClose: 5000 }
        );
      }
      return;
    }

    if (lockedSeats.has(seat.id)) {
      toast.info('This seat is currently being booked by another user');
      return;
    }

    try {
      // Attempt to lock the seat
      const response = await bookingsAPI.lockSeat(seat.id);
      const lockData = response.data;

      console.log('Lock response:', lockData);

      if (lockData.success && lockData.locked) {
        // Successfully locked the seat
        setSelectedSeat(seat);
        setLockedSeats(prev => new Set([...prev, seat.id]));
        setShowBookingModal(true);

        // Start countdown timer
        const timer = {
          seatId: seat.id,
          expiresAt: lockData.expiresAt || (Date.now() + (2 * 60 * 1000))
        };
        setLockTimer(timer);

        if (lockData.isOwner) {
          if (lockData.message.includes('already')) {
            toast.success('You already have this seat selected. Lock extended for 2 more minutes!', { autoClose: 8000 });
          } else {
            toast.success('Seat selected successfully! You have 2 minutes to complete booking.', { autoClose: 8000 });
          }
        }

        // Auto-release lock after 2 minutes
        const timeoutDuration = lockData.expiresAt ? (lockData.expiresAt - Date.now()) : (2 * 60 * 1000);
        setTimeout(() => {
          handleLockExpiry(seat.id);
        }, timeoutDuration);

      } else {
        // Failed to lock - show detailed message
        if (lockData.lockedBy && lockData.lockedByRole) {
          // Another user has locked this seat
          const message = `This seat is currently being selected by ${lockData.lockedBy} (${lockData.lockedByRole})`;

          if (lockData.remainingTime && lockData.remainingTime > 0) {
            const minutes = Math.floor(lockData.remainingTime / 60);
            const seconds = lockData.remainingTime % 60;
            const timeStr = minutes > 0 ? `${minutes}m ${seconds}s` : `${seconds}s`;
            toast.warning(`${message}. Available in ${timeStr}`, {
              autoClose: 5000,
            });
          } else {
            toast.warning(message, {
              autoClose: 5000,
            });
          }
        } else {
          // Generic failure message
          toast.error(lockData.message || 'Failed to select seat. Please try again.', { autoClose: 12000 });
        }
      }

    } catch (error) {
      console.error('Error locking seat:', error);

      if (error.response?.data) {
        const errorData = error.response.data;

        // Handle structured error responses
        if (errorData.lockedBy && errorData.lockedByRole) {
          toast.warning(`This seat is currently being selected by ${errorData.lockedBy} (${errorData.lockedByRole})`, {
            autoClose: 5000,
          });
        } else if (errorData.error || errorData.message) {
          toast.error(errorData.error || errorData.message, { autoClose: 12000 });
        } else {
          toast.error('Failed to select seat. Please try again.', { autoClose: 12000 });
        }
      } else {
        toast.error('Failed to connect to server. Please try again.', { autoClose: 12000 });
      }
    }
  };

  const handleLockExpiry = (seatId) => {
    setLockedSeats(prev => {
      const newSet = new Set(prev);
      newSet.delete(seatId);
      return newSet;
    });

    if (lockTimer && lockTimer.seatId === seatId) {
      setLockTimer(null);
      setShowBookingModal(false);
      setSelectedSeat(null);
      toast.warning('Seat lock expired. Please try booking again.', { autoClose: 12000 });
    }
  };

  const handleBookingSuccess = () => {
    setShowBookingModal(false);
    setSelectedSeat(null);
    setLockTimer(null);
    setLockedSeats(prev => {
      const newSet = new Set(prev);
      if (selectedSeat) {
        newSet.delete(selectedSeat.id);
      }
      return newSet;
    });
    loadSeats(); // Refresh seat data
  };

  const handleBookingCancel = async () => {
    if (selectedSeat && lockedSeats.has(selectedSeat.id)) {
      try {
        // Release lock via API
        await bookingsAPI.unlockSeat(selectedSeat.id);
        console.log('Lock released for seat:', selectedSeat.id);
      } catch (error) {
        console.error('Error releasing lock:', error);
        // Continue with local cleanup even if API call fails
      }

      // Release lock locally
      setLockedSeats(prev => {
        const newSet = new Set(prev);
        newSet.delete(selectedSeat.id);
        return newSet;
      });
    }

    setShowBookingModal(false);
    setSelectedSeat(null);
    setLockTimer(null);
    toast.info('Booking cancelled. Seat is now available for others.', { autoClose: 8000 });
  };

  const getUniqueFloors = () => {
    return [...new Set(seats.map(seat => seat.floor))].sort((a, b) => a - b);
  };

  const getSeatsByFloor = (floor) => {
    return filteredSeats.filter(seat => seat.floor === floor);
  };

  if (loading) {
    return <div className="loading">Loading seats...</div>;
  }

  return (
    <div className="container">
      <div style={{ marginBottom: '30px' }}>
        <h2>Book a Seat</h2>
        <p style={{ color: '#666', marginTop: '5px' }}>
          Click on an available seat to book it
        </p>
        {showBookingModal && (
          <p style={{
            color: '#ff6b6b',
            marginTop: '10px',
            fontWeight: 'bold',
            fontSize: '0.9rem'
          }}>
            ⏸️ Real-time seat updates paused while booking...
          </p>
        )}
        {!showBookingModal && (
          <p style={{
            color: '#51cf66',
            marginTop: '10px',
            fontWeight: 'bold',
            fontSize: '0.9rem'
          }}>
            ✓ Live seat updates active (refreshes every 5 seconds)
          </p>
        )}
      </div>

      {/* Lock Timer */}
      {lockTimer && (
        <div className="card" style={{
          marginBottom: '20px',
          backgroundColor: '#fff3cd',
          border: '1px solid #ffeaa7'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div>
              <strong>Seat {selectedSeat?.seatNumber} is locked</strong>
              <p style={{ margin: '5px 0 0 0', color: '#856404' }}>
                Complete your booking before the timer expires
              </p>
            </div>
            <CountdownTimer
              expiresAt={lockTimer.expiresAt}
              onExpire={() => handleLockExpiry(lockTimer.seatId)}
            />
          </div>
        </div>
      )}

      {/* Filters */}
      <div className="card" style={{ marginBottom: '30px' }}>
        <h3 style={{ marginBottom: '20px' }}>Filters</h3>
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
          gap: '15px'
        }}>
          <div className="form-group" style={{ marginBottom: 0 }}>
            <label>Floor</label>
            <select name="floor" value={filters.floor} onChange={handleFilterChange}>
              <option value="">All Floors</option>
              {getUniqueFloors().map(floor => (
                <option key={floor} value={floor}>Floor {floor}</option>
              ))}
            </select>
          </div>

          <div className="form-group" style={{ marginBottom: 0 }}>
            <label>Seat Type</label>
            <select name="type" value={filters.type} onChange={handleFilterChange}>
              <option value="">All Types</option>
              <option value="REGULAR">Regular Seats</option>
              <option value="HOT_DESK">Hot Desks</option>
              <option value="MEETING_ROOM">Meeting Rooms</option>
            </select>
          </div>

          <div className="form-group" style={{ marginBottom: 0 }}>
            <label>Status</label>
            <select name="status" value={filters.status} onChange={handleFilterChange}>
              <option value="">All Statuses</option>
              <option value="AVAILABLE">Available</option>
              <option value="BOOKED">Booked</option>
              <option value="MAINTENANCE">Maintenance</option>
            </select>
          </div>
        </div>
      </div>

      {/* Legend */}
      <div className="card" style={{ marginBottom: '30px' }}>
        <h3 style={{ marginBottom: '15px' }}>Legend</h3>
        <div style={{ display: 'flex', gap: '20px', flexWrap: 'wrap', alignItems: 'center' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <div style={{
              width: '20px',
              height: '20px',
              backgroundColor: '#28a745',
              borderRadius: '4px'
            }} />
            <span>Available</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <div style={{
              width: '20px',
              height: '20px',
              backgroundColor: '#ffc107',
              borderRadius: '4px'
            }} />
            <span>Locked (Being Booked)</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <div style={{
              width: '20px',
              height: '20px',
              backgroundColor: '#dc3545',
              borderRadius: '4px'
            }} />
            <span>Booked</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <div style={{
              width: '20px',
              height: '20px',
              backgroundColor: '#6c757d',
              borderRadius: '4px'
            }} />
            <span>Maintenance</span>
          </div>
        </div>
      </div>

      {/* Seat Grid */}
      {getUniqueFloors().map(floor => {
        const floorSeats = getSeatsByFloor(floor);
        if (floorSeats.length === 0) return null;

        return (
          <div key={floor} className="card" style={{ marginBottom: '30px' }}>
            <h3 style={{ marginBottom: '20px' }}>Floor {floor}</h3>
            <div style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fill, minmax(140px, 1fr))',
              gap: '16px',
              padding: '10px'
            }}>
              {floorSeats.map(seat => (
                <div
                  key={seat.id}
                  onClick={() => handleSeatClick(seat)}
                  style={{
                    backgroundColor: getSeatColor(seat),
                    background: `linear-gradient(135deg, ${getSeatColor(seat)} 0%, ${adjustColorBrightness(getSeatColor(seat), -20)} 100%)`,
                    color: 'white',
                    padding: '20px 18px',
                    borderRadius: '12px',
                    cursor: canBookSeat(seat) && !lockedSeats.has(seat.id) ? 'pointer' : seat.status === 'MAINTENANCE' ? 'not-allowed' : 'default',
                    textAlign: 'center',
                    transition: 'all 0.3s ease',
                    opacity: seat.status === 'MAINTENANCE' ? 0.6 : 1,
                    boxShadow: selectedSeat?.id === seat.id
                      ? `0 0 0 4px rgba(102, 126, 234, 0.5)`
                      : `0 2px 8px rgba(0, 0, 0, 0.12)`,
                    border: selectedSeat?.id === seat.id ? '3px solid #667eea' : 'none',
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'center',
                    alignItems: 'center',
                    minHeight: '140px',
                    position: 'relative'
                  }}
                  onMouseEnter={(e) => {
                    if (canBookSeat(seat) && !lockedSeats.has(seat.id)) {
                      e.currentTarget.style.transform = 'translateY(-4px)';
                      e.currentTarget.style.boxShadow = '0 8px 20px rgba(0, 0, 0, 0.18)';
                    }
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.transform = 'translateY(0)';
                    e.currentTarget.style.boxShadow = selectedSeat?.id === seat.id
                      ? `0 0 0 4px rgba(102, 126, 234, 0.5)`
                      : `0 2px 8px rgba(0, 0, 0, 0.12)`;
                  }}
                  title={`${seat.seatNumber} (Floor ${seat.floor}) - ${seat.status}`}
                >
                  {/* Seat Icon */}
                  <div style={{ fontSize: '32px', marginBottom: '8px' }}>
                    {seat.type === 'MEETING_ROOM' ? '🏢' : seat.type === 'HOT_DESK' ? '💻' : '💺'}
                  </div>

                  {/* Seat Number */}
                  <div style={{
                    fontSize: '18px',
                    fontWeight: '700',
                    letterSpacing: '0.5px',
                    marginBottom: '5px',
                    wordBreak: 'break-word'
                  }}>
                    {seat.seatNumber}
                  </div>

                  {/* Status Badge */}
                  <div style={{
                    fontSize: '12px',
                    fontWeight: '600',
                    backgroundColor: 'rgba(255, 255, 255, 0.25)',
                    padding: '4px 10px',
                    borderRadius: '12px',
                    marginTop: '6px',
                    whiteSpace: 'nowrap'
                  }}>
                    {seat.status === 'BOOKED'
                      ? '🔴 Booked'
                      : seat.status === 'AVAILABLE'
                      ? '✓ Available'
                      : seat.status === 'MAINTENANCE'
                      ? '⚠️ Maintenance'
                      : '🔒 Locked'}
                  </div>
                </div>
              ))}
            </div>
          </div>
        );
      })}

      {filteredSeats.length === 0 && !loading && (
        <div className="card text-center">
          <p style={{ color: '#666', padding: '40px' }}>
            No seats found matching your criteria. Try adjusting the filters.
          </p>
        </div>
      )}

      {/* Booking Modal */}
      {showBookingModal && selectedSeat && (
        <SeatBookingModal
          seat={selectedSeat}
          onSuccess={handleBookingSuccess}
          onCancel={handleBookingCancel}
          timeRemaining={lockTimer ? Math.max(0, lockTimer.expiresAt - Date.now()) : 0}
        />
      )}
    </div>
  );
};

export default SeatGrid;
