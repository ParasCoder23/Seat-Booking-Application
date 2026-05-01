import React, { useState, useEffect } from 'react';

const CountdownTimer = ({ expiresAt, onExpire }) => {
  const [timeLeft, setTimeLeft] = useState(0);

  useEffect(() => {
    const updateTimer = () => {
      const now = Date.now();
      const remaining = Math.max(0, expiresAt - now);
      setTimeLeft(remaining);

      if (remaining <= 0 && onExpire) {
        onExpire();
      }
    };

    updateTimer();
    const interval = setInterval(updateTimer, 1000);

    return () => clearInterval(interval);
  }, [expiresAt, onExpire]);

  const formatTime = (milliseconds) => {
    const totalSeconds = Math.floor(milliseconds / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  };

  const getTimerColor = () => {
    const totalTime = 2 * 60 * 1000; // 2 minutes in milliseconds
    const percentage = timeLeft / totalTime;

    if (percentage > 0.5) return '#28a745'; // Green
    if (percentage > 0.25) return '#ffc107'; // Yellow
    return '#dc3545'; // Red
  };

  return (
    <div style={{
      display: 'flex',
      alignItems: 'center',
      gap: '10px',
      padding: '10px 15px',
      backgroundColor: 'white',
      borderRadius: '8px',
      border: `2px solid ${getTimerColor()}`,
      fontWeight: 'bold',
      fontSize: '1.2rem'
    }}>
      <div style={{ color: getTimerColor() }}>
        ⏰ {formatTime(timeLeft)}
      </div>
      <div style={{
        width: '100px',
        height: '8px',
        backgroundColor: '#e1e5e9',
        borderRadius: '4px',
        overflow: 'hidden'
      }}>
        <div
          style={{
            width: `${Math.max(0, (timeLeft / (2 * 60 * 1000)) * 100)}%`,
            height: '100%',
            backgroundColor: getTimerColor(),
            transition: 'width 1s ease'
          }}
        />
      </div>
    </div>
  );
};

export default CountdownTimer;
