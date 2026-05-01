# Office Seat Booking System - Frontend

A modern React frontend application for the office seat booking system with real-time seat status, countdown timers, and role-based UI.

## Features

### 🎨 **Visual Seat Grid**
- **Color-coded seats**: Green (available), Yellow (locked), Red (booked), Gray (maintenance)
- **Interactive grid layout** organized by floors
- **Seat type icons**: 💺 Regular, 💻 Hot Desk, 🏢 Meeting Room
- **Real-time status updates**

### ⏱️ **Booking System**
- **2-minute countdown timer** for seat locks
- **Two-step booking process**: Lock → Confirm
- **Modal-based booking form** with date/time selection
- **Automatic lock release** after timeout

### 👥 **Role-based UI**
- **Employee**: Book regular seats only, max 1 booking
- **Manager**: Book all seat types including meeting rooms
- **Admin**: Full admin panel with seat management

### 🔐 **Authentication**
- **JWT-based authentication** with auto token management
- **Demo account buttons** for easy testing
- **Role-based navigation** and permissions

### 📱 **Responsive Design**
- **Mobile-friendly** grid layout
- **Adaptive components** for all screen sizes
- **Modern UI/UX** with smooth animations

## Tech Stack

- **React 18** - Frontend framework
- **React Router** - Client-side routing
- **Axios** - HTTP client for API calls
- **React Toastify** - Toast notifications
- **CSS Grid & Flexbox** - Modern layouts

## Prerequisites

- Node.js 16+
- npm or yarn
- Backend server running on http://localhost:8080

## Installation & Setup

### 1. Navigate to frontend directory
```bash
cd frontend
```

### 2. Install dependencies
```bash
npm install
```

### 3. Start development server
```bash
npm start
```

The application will open at `http://localhost:3000`

## Project Structure

```
frontend/
├── public/
│   └── index.html              # HTML template
├── src/
│   ├── components/             # React components
│   │   ├── AdminPanel.js       # Admin management interface
│   │   ├── CountdownTimer.js   # 2-minute countdown timer
│   │   ├── Dashboard.js        # User dashboard with stats
│   │   ├── Header.js           # Navigation header
│   │   ├── Login.js            # Login form with demo accounts
│   │   ├── MyBookings.js       # User booking management
│   │   ├── ProtectedRoute.js   # Route protection
│   │   ├── Register.js         # User registration
│   │   ├── SeatBookingModal.js # Booking confirmation modal
│   │   └── SeatGrid.js         # Main seat selection interface
│   ├── context/
│   │   └── AuthContext.js      # Authentication state management
│   ├── services/
│   │   └── api.js              # API service layer
│   ├── App.js                  # Main app component
│   ├── index.js                # App entry point
│   └── index.css               # Global styles
├── package.json                # Dependencies and scripts
└── README.md                   # This file
```

## Key Components

### SeatGrid Component
The main interface for seat booking with:
- **Interactive seat grid** organized by floors
- **Color-coded status** indicators
- **Filter options** by floor, type, and status
- **Click-to-lock** functionality
- **Real-time lock status** updates

### CountdownTimer Component
- **Visual countdown** with progress bar
- **Color transitions** (green → yellow → red)
- **Auto-expiry handling**
- **Time formatting** (MM:SS)

### SeatBookingModal Component
- **Seat lock confirmation** modal
- **Date/time picker** for booking period
- **Embedded countdown timer**
- **Form validation**
- **Auto-close on timer expiry**

### Dashboard Component
- **Statistics overview** (total, available, booked seats)
- **Recent bookings** table
- **Quick action buttons**
- **Role-based content**

## API Integration

The frontend integrates with the Spring Boot backend through:

### Authentication Endpoints
- `POST /api/auth/login` - User login
- `POST /api/auth/register` - User registration

### Seat Management
- `GET /api/seats` - Get all seats
- `GET /api/seats/available` - Get available seats
- `GET /api/seats/floor/{floor}` - Get seats by floor

### Booking Operations
- `POST /api/bookings/lock/{seatId}` - Lock seat for 2 minutes
- `POST /api/bookings` - Confirm booking
- `GET /api/bookings/my` - Get user bookings
- `PUT /api/bookings/{id}/cancel` - Cancel booking

### Admin Operations (Admin only)
- `GET /api/bookings` - Get all bookings
- `POST /api/admin/seats` - Create seat
- `PUT /api/admin/seats/{id}/status` - Update seat status
- `DELETE /api/admin/seats/{id}` - Delete seat

## User Flows

### Employee Booking Flow
1. **Login** with employee credentials
2. **View seat grid** → only regular seats and hot desks available
3. **Click available seat** → seat locks for 2 minutes (yellow)
4. **Fill booking form** → select date/time range
5. **Confirm booking** → seat turns red, booking confirmed
6. **View in "My Bookings"** → can cancel if not started

### Manager Booking Flow
1. **Login** with manager credentials
2. **Access all seat types** including meeting rooms
3. **Book multiple seats** without restrictions
4. **Same lock/confirm process** as employee

### Admin Management Flow
1. **Login** with admin credentials
2. **Access admin panel** → seat management tab
3. **Create new seats** → specify floor, number, type
4. **Update seat status** → set to maintenance/available
5. **Delete seats** → remove from system
6. **View all bookings** → system-wide booking overview

## Seat Status Colors

| Color | Status | Description |
|-------|---------|-------------|
| 🟢 Green | Available | Ready for booking |
| 🟡 Yellow | Locked | Being booked (2min timer) |
| 🔴 Red | Booked | Occupied/Reserved |
| ⚫ Gray | Maintenance | Under maintenance |

## Responsive Design

The application is fully responsive with:
- **Mobile-first approach**
- **Flexible grid layouts**
- **Touch-friendly buttons**
- **Adaptive navigation**
- **Scalable components**

## Development Features

### Hot Reloading
Changes to components automatically refresh the browser

### Error Boundaries
Graceful error handling with user-friendly messages

### Toast Notifications
Real-time feedback for all user actions

### Protected Routes
Automatic redirect to login for unauthenticated users

### Token Management
Automatic token refresh and logout on expiry

## Demo Accounts

The application includes demo account buttons for easy testing:

| Role | Username | Password | Permissions |
|------|----------|----------|-------------|
| Admin | admin | admin123 | Full system access |
| Manager | manager | manager123 | All seats + multiple bookings |
| Employee | employee | employee123 | Regular seats only, 1 booking max |

## Building for Production

```bash
npm run build
```

Creates optimized production build in `build/` directory.

## Environment Variables

Create `.env` file for custom configuration:

```env
REACT_APP_API_URL=http://localhost:8080/api
```

## Browser Support

- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)

## Contributing

1. Fork the repository
2. Create feature branch
3. Make changes
4. Test thoroughly
5. Submit pull request

## Troubleshooting

### Backend Connection Issues
- Ensure Spring Boot server is running on port 8080
- Check CORS configuration in backend
- Verify API endpoints are accessible

### Authentication Problems
- Clear browser localStorage
- Check JWT token expiry
- Verify backend authentication endpoints

### Seat Grid Issues
- Check WebSocket connections for real-time updates
- Verify seat data format from backend
- Clear browser cache

## Future Enhancements

- **WebSocket integration** for real-time updates
- **Push notifications** for booking reminders
- **Seat map visualization** with floor plans
- **Calendar integration** with external systems
- **Booking analytics** and reporting
- **Dark mode** theme support
- **Progressive Web App** features
