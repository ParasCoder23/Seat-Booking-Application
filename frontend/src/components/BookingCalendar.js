import React, { useEffect, useState } from 'react';
import { bookingsAPI } from '../services/api';
import FullCalendar from '@fullcalendar/react';
import dayGridPlugin from '@fullcalendar/daygrid';
import interactionPlugin from '@fullcalendar/interaction';
// import { useAuth } from '../context/AuthContext';

const BookingCalendar = () => {
  // const { user } = useAuth();
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [selectedEvent, setSelectedEvent] = useState(null);
  const [stats, setStats] = useState({ confirmed: 0, completed: 0, cancelled: 0, meetings: 0 });

  useEffect(() => { loadCalendarBookings(); }, []);

  const loadCalendarBookings = async () => {
    setLoading(true);
    try {
      const response = await bookingsAPI.getCalendarBookings();
      const bookings = response.data;
      const now = new Date();

      let confirmed = 0, completed = 0, cancelled = 0, meetings = 0;

      const mapped = bookings.map(b => {
        const isCompleted = b.endTime && new Date(b.endTime) < now;
        let title = '', color = '', borderColor = '', textColor = '#fff';

        if (b.isMeetingRoom) {
          meetings++;
          title = `🏢 ${b.seatNumber}`;
          color = '#7c3aed'; borderColor = '#5b21b6';
        } else if (b.isAdminBooked) {
          title = `💼 ${b.seatNumber}`;
          color = '#0891b2'; borderColor = '#0e7490';
        } else if (b.status === 'CANCELLED') {
          cancelled++;
          title = `✗ ${b.seatNumber}`;
          color = '#ef4444'; borderColor = '#dc2626';
        } else if (isCompleted || b.status === 'COMPLETED') {
          completed++;
          title = `✓ ${b.seatNumber}`;
          color = '#6b7280'; borderColor = '#4b5563';
        } else {
          confirmed++;
          title = `● ${b.seatNumber}`;
          color = '#059669'; borderColor = '#047857';
        }

        return {
          id: b.id,
          title,
          start: b.startTime,
          end: b.endTime,
          backgroundColor: color,
          borderColor,
          textColor,
          extendedProps: { ...b, isCompleted }
        };
      });

      setEvents(mapped);
      setStats({ confirmed, completed, cancelled, meetings });
    } catch (err) {
      console.error('Failed to load calendar', err);
    } finally {
      setLoading(false);
    }
  };

  const handleEventClick = (info) => {
    setSelectedEvent(info.event.extendedProps);
    setShowModal(true);
  };

  const fmt = (d) => d ? new Date(d).toLocaleString('en-US', { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : '-';
  const fmtTime = (d) => d ? new Date(d).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }) : '-';

  const getStatusBadge = (status, isCompleted) => {
    const s = isCompleted ? 'COMPLETED' : status;
    const map = {
      CONFIRMED: { bg: '#dcfce7', color: '#15803d', icon: '✓', label: 'Confirmed' },
      CANCELLED: { bg: '#fee2e2', color: '#dc2626', icon: '✗', label: 'Cancelled' },
      COMPLETED: { bg: '#f3f4f6', color: '#6b7280', icon: '◎', label: 'Completed' },
    };
    const cfg = map[s] || { bg: '#dbeafe', color: '#1d4ed8', icon: '●', label: s };
    return (
      <span style={{ display: 'inline-flex', alignItems: 'center', gap: '5px', padding: '4px 12px', borderRadius: '999px', backgroundColor: cfg.bg, color: cfg.color, fontWeight: 600, fontSize: '0.8rem' }}>
        {cfg.icon} {cfg.label}
      </span>
    );
  };

  return (
    <div style={{ padding: '0 0 40px 0' }}>
      {/* Header */}
      <div style={{ background: 'linear-gradient(135deg, #1e3a5f 0%, #2d6a9f 100%)', borderRadius: '16px', padding: '28px 32px', marginBottom: '24px', color: 'white' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
          <div>
            <h2 style={{ margin: 0, fontSize: '1.6rem', fontWeight: 700, letterSpacing: '-0.3px' }}>📅 My Booking Calendar</h2>
            <p style={{ margin: '6px 0 0', opacity: 0.8, fontSize: '0.9rem' }}>Track all your seat and meeting room bookings in one place</p>
          </div>
          <button onClick={loadCalendarBookings} style={{ display: 'flex', alignItems: 'center', gap: '8px', background: 'rgba(255,255,255,0.15)', border: '1px solid rgba(255,255,255,0.3)', color: 'white', padding: '9px 18px', borderRadius: '8px', cursor: 'pointer', fontSize: '0.85rem', fontWeight: 500, backdropFilter: 'blur(4px)' }}>
            🔄 Refresh
          </button>
        </div>

        {/* Stats Row */}
        {!loading && (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '12px', marginTop: '20px' }}>
            {[
              { label: 'Confirmed', value: stats.confirmed, icon: '✓', bg: 'rgba(16,185,129,0.2)', border: 'rgba(16,185,129,0.4)' },
              { label: 'Meetings', value: stats.meetings, icon: '🏢', bg: 'rgba(124,58,237,0.2)', border: 'rgba(124,58,237,0.4)' },
              { label: 'Completed', value: stats.completed, icon: '◎', bg: 'rgba(156,163,175,0.2)', border: 'rgba(156,163,175,0.4)' },
              { label: 'Cancelled', value: stats.cancelled, icon: '✗', bg: 'rgba(239,68,68,0.2)', border: 'rgba(239,68,68,0.4)' },
            ].map(s => (
              <div key={s.label} style={{ background: s.bg, border: `1px solid ${s.border}`, borderRadius: '10px', padding: '12px 16px', textAlign: 'center' }}>
                <div style={{ fontSize: '1.5rem', fontWeight: 700 }}>{s.value}</div>
                <div style={{ fontSize: '0.75rem', opacity: 0.85, marginTop: '2px' }}>{s.icon} {s.label}</div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Legend */}
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '10px', marginBottom: '16px', padding: '14px 18px', background: '#f8fafc', borderRadius: '10px', border: '1px solid #e2e8f0' }}>
        <span style={{ fontSize: '0.78rem', color: '#64748b', fontWeight: 600, marginRight: '4px', alignSelf: 'center' }}>LEGEND:</span>
        {[
          { color: '#059669', label: 'Confirmed Booking' },
          { color: '#7c3aed', label: 'Meeting Room' },
          { color: '#0891b2', label: 'Admin Booking' },
          { color: '#6b7280', label: 'Completed' },
          { color: '#ef4444', label: 'Cancelled' },
        ].map(l => (
          <span key={l.label} style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', padding: '4px 12px', borderRadius: '999px', background: l.color + '18', border: `1px solid ${l.color}40`, fontSize: '0.78rem', color: l.color, fontWeight: 500 }}>
            <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: l.color, display: 'inline-block' }} />
            {l.label}
          </span>
        ))}
      </div>

      {/* Calendar Card */}
      <div style={{ backgroundColor: 'white', borderRadius: '16px', boxShadow: '0 1px 3px rgba(0,0,0,0.06), 0 4px 16px rgba(0,0,0,0.06)', border: '1px solid #e2e8f0', overflow: 'hidden' }}>
        {loading ? (
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '80px 20px', gap: '16px' }}>
            <div style={{ width: '48px', height: '48px', border: '4px solid #e2e8f0', borderTopColor: '#2d6a9f', borderRadius: '50%', animation: 'spin 0.8s linear infinite' }} />
            <p style={{ color: '#94a3b8', margin: 0, fontSize: '0.95rem' }}>Loading your bookings...</p>
            <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
          </div>
        ) : (
          <div style={{ padding: '20px' }}>
            <FullCalendar
              plugins={[dayGridPlugin, interactionPlugin]}
              initialView="dayGridMonth"
              events={events}
              height={620}
              headerToolbar={{ left: 'prev,next today', center: 'title', right: 'dayGridMonth,dayGridWeek,dayGridDay' }}
              eventClick={handleEventClick}
              eventMouseEnter={(info) => { info.el.style.transform = 'scale(1.03)'; info.el.style.zIndex = '10'; info.el.style.transition = 'transform 0.15s ease'; }}
              eventMouseLeave={(info) => { info.el.style.transform = 'scale(1)'; info.el.style.zIndex = ''; }}
              eventDisplay="block"
              dayMaxEvents={3}
              moreLinkText={(n) => `+${n} more`}
              dayCellDidMount={(info) => {
                const today = new Date(); today.setHours(0,0,0,0);
                const cell = new Date(info.date); cell.setHours(0,0,0,0);
                if (cell.getTime() === today.getTime()) {
                  info.el.style.backgroundColor = '#eff6ff';
                }
              }}
            />
          </div>
        )}
      </div>

      {/* Help text */}
      <p style={{ textAlign: 'center', color: '#94a3b8', fontSize: '0.8rem', marginTop: '12px' }}>
        💡 Click on any event to view full details
      </p>

      {/* Modal */}
      {showModal && selectedEvent && (
        <div
          onClick={(e) => { if (e.target === e.currentTarget) setShowModal(false); }}
          style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(15,23,42,0.55)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 2000, padding: '16px', backdropFilter: 'blur(4px)', animation: 'fadeIn 0.15s ease' }}
        >
          <style>{`
            @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
            @keyframes slideUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
          `}</style>
          <div style={{ backgroundColor: 'white', borderRadius: '20px', width: '100%', maxWidth: '480px', boxShadow: '0 25px 60px rgba(0,0,0,0.25)', overflow: 'hidden', animation: 'slideUp 0.2s ease', maxHeight: '90vh', display: 'flex', flexDirection: 'column' }}>

            {/* Modal Header */}
            <div style={{ background: selectedEvent.isMeetingRoom ? 'linear-gradient(135deg, #7c3aed, #5b21b6)' : selectedEvent.isAdminBooked ? 'linear-gradient(135deg, #0891b2, #0e7490)' : 'linear-gradient(135deg, #1e3a5f, #2d6a9f)', padding: '24px 28px', color: 'white', position: 'relative' }}>
              <button onClick={() => setShowModal(false)} style={{ position: 'absolute', top: '16px', right: '16px', background: 'rgba(255,255,255,0.2)', border: 'none', color: 'white', width: '32px', height: '32px', borderRadius: '50%', cursor: 'pointer', fontSize: '16px', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 700 }}>✕</button>
              <div style={{ fontSize: '2rem', marginBottom: '8px' }}>
                {selectedEvent.isMeetingRoom ? '🏢' : selectedEvent.isAdminBooked ? '💼' : '💺'}
              </div>
              <h3 style={{ margin: '0 0 4px', fontSize: '1.2rem', fontWeight: 700 }}>
                {selectedEvent.isMeetingRoom ? 'Meeting Room Booking' : selectedEvent.isAdminBooked ? 'Admin Booking' : 'Seat Booking'}
              </h3>
              <div style={{ fontSize: '1.4rem', fontWeight: 700, opacity: 0.95 }}>{selectedEvent.seatNumber}</div>
            </div>

            {/* Modal Body */}
            <div style={{ padding: '24px 28px', overflowY: 'auto', flex: 1 }}>

              {/* Status */}
              <div style={{ marginBottom: '20px' }}>
                {getStatusBadge(selectedEvent.status, selectedEvent.isCompleted)}
              </div>

              {/* Info Grid */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px', marginBottom: '20px' }}>
                <div style={{ background: '#f8fafc', borderRadius: '10px', padding: '14px' }}>
                  <div style={{ fontSize: '0.7rem', color: '#94a3b8', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: '6px' }}>📅 Start</div>
                  <div style={{ fontSize: '0.85rem', fontWeight: 600, color: '#1e293b' }}>{fmt(selectedEvent.startTime)}</div>
                </div>
                <div style={{ background: '#f8fafc', borderRadius: '10px', padding: '14px' }}>
                  <div style={{ fontSize: '0.7rem', color: '#94a3b8', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: '6px' }}>🏁 End</div>
                  <div style={{ fontSize: '0.85rem', fontWeight: 600, color: '#1e293b' }}>{fmt(selectedEvent.endTime)}</div>
                </div>
              </div>

              {/* Time Duration */}
              {selectedEvent.startTime && selectedEvent.endTime && (
                <div style={{ background: '#f0fdf4', border: '1px solid #bbf7d0', borderRadius: '10px', padding: '12px 16px', marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <span style={{ fontSize: '1.2rem' }}>⏱</span>
                  <div>
                    <div style={{ fontSize: '0.7rem', color: '#166534', fontWeight: 600 }}>DURATION</div>
                    <div style={{ fontSize: '0.9rem', fontWeight: 600, color: '#15803d' }}>
                      {fmtTime(selectedEvent.startTime)} → {fmtTime(selectedEvent.endTime)}
                    </div>
                  </div>
                </div>
              )}

              {/* Organizer / Booked By */}
              {(selectedEvent.isMeetingRoom || selectedEvent.isAdminBooked) && selectedEvent.meetingOrganizerName && (
                <div style={{ background: '#faf5ff', border: '1px solid #e9d5ff', borderRadius: '10px', padding: '14px 16px', marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '12px' }}>
                  <div style={{ width: '40px', height: '40px', borderRadius: '50%', background: 'linear-gradient(135deg, #7c3aed, #a855f7)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'white', fontSize: '1rem', fontWeight: 700, flexShrink: 0 }}>
                    {selectedEvent.meetingOrganizerName[0].toUpperCase()}
                  </div>
                  <div>
                    <div style={{ fontSize: '0.7rem', color: '#7e22ce', fontWeight: 600, textTransform: 'uppercase' }}>
                      {selectedEvent.isMeetingRoom ? 'Organizer' : 'Booked By'}
                    </div>
                    <div style={{ fontSize: '0.95rem', fontWeight: 600, color: '#1e293b' }}>{selectedEvent.meetingOrganizerName}</div>
                  </div>
                </div>
              )}

              {/* Regular booking user */}
              {!selectedEvent.isMeetingRoom && !selectedEvent.isAdminBooked && selectedEvent.username && (
                <div style={{ background: '#f0f9ff', border: '1px solid #bae6fd', borderRadius: '10px', padding: '14px 16px', marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '12px' }}>
                  <div style={{ width: '40px', height: '40px', borderRadius: '50%', background: 'linear-gradient(135deg, #0891b2, #38bdf8)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'white', fontSize: '1rem', fontWeight: 700, flexShrink: 0 }}>
                    {selectedEvent.username[0].toUpperCase()}
                  </div>
                  <div>
                    <div style={{ fontSize: '0.7rem', color: '#0369a1', fontWeight: 600, textTransform: 'uppercase' }}>Booked By</div>
                    <div style={{ fontSize: '0.95rem', fontWeight: 600, color: '#1e293b' }}>{selectedEvent.username}
                      {selectedEvent.bookedForUsername && selectedEvent.bookedForUsername !== selectedEvent.username && (
                        <span style={{ fontSize: '0.8rem', color: '#64748b', fontWeight: 400 }}> → for {selectedEvent.bookedForUsername}</span>
                      )}
                    </div>
                  </div>
                </div>
              )}

              {/* Attendees */}
              {(selectedEvent.isMeetingRoom || selectedEvent.isAdminBooked) && selectedEvent.meetingAttendees && selectedEvent.meetingAttendees.length > 0 && (
                <div>
                  <div style={{ fontSize: '0.75rem', color: '#64748b', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: '10px' }}>
                    📋 {selectedEvent.isMeetingRoom ? 'Attendees' : 'Booked For'} ({selectedEvent.meetingAttendees.length})
                  </div>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
                    {selectedEvent.meetingAttendees.map((a, i) => (
                      <div key={i} style={{ display: 'flex', alignItems: 'center', gap: '7px', padding: '6px 12px 6px 8px', background: selectedEvent.isMeetingRoom ? '#f5f3ff' : '#ecfeff', border: `1px solid ${selectedEvent.isMeetingRoom ? '#ddd6fe' : '#a5f3fc'}`, borderRadius: '999px' }}>
                        <div style={{ width: '24px', height: '24px', borderRadius: '50%', background: selectedEvent.isMeetingRoom ? 'linear-gradient(135deg, #7c3aed, #a855f7)' : 'linear-gradient(135deg, #0891b2, #38bdf8)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'white', fontSize: '0.7rem', fontWeight: 700, flexShrink: 0 }}>
                          {a[0].toUpperCase()}
                        </div>
                        <span style={{ fontSize: '0.82rem', fontWeight: 500, color: '#1e293b' }}>{a}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>

            {/* Modal Footer */}
            <div style={{ padding: '16px 28px', borderTop: '1px solid #f1f5f9', background: '#fafbfc' }}>
              <button onClick={() => setShowModal(false)} style={{ width: '100%', padding: '11px', borderRadius: '10px', border: 'none', background: 'linear-gradient(135deg, #1e3a5f, #2d6a9f)', color: 'white', fontWeight: 600, cursor: 'pointer', fontSize: '0.9rem', letterSpacing: '0.3px' }}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default BookingCalendar;

