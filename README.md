# 🏢 Office Seat Booking System

> A modern, full-stack web application for managing office seat bookings, meeting room reservations, and employee scheduling with real-time lock management.

![Version](https://img.shields.io/badge/version-2.0.0-blue)
![License](https://img.shields.io/badge/license-MIT-green)
![Status](https://img.shields.io/badge/status-Production%20Ready-brightgreen)

---

## 📋 Table of Contents

- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Architecture](#-architecture)
- [Installation](#-installation--setup)
- [Configuration](#-configuration)
- [Running Locally](#-running-locally)
- [Deployment](#-deployment)
- [Environment Variables](#-environment-variables)
- [API Documentation](#-api-documentation)
- [Project Structure](#-project-structure)
- [Contributing](#-contributing)
- [License](#-license)

---

## ✨ Features

### Core Features
- ✅ **Seat Booking System** - Reserve office seats with time slots
- ✅ **Meeting Room Booking** - Book meeting rooms for 1-6 employees
- ✅ **Real-time Seat Locking** - 2-minute seat lock to prevent double-booking
- ✅ **Calendar View** - Visual calendar for bookings and availability
- ✅ **Admin Management** - Full admin panel for seat and booking management
- ✅ **Role-based Access** - Employee, Manager, and Admin roles
- ✅ **User Authentication** - Secure login with JWT tokens
- ✅ **Booking History** - Track all past and upcoming bookings

### Advanced Features
- 🔄 **Meeting Grouping** - Shows meetings as single records with all attendees
- 📱 **Responsive Design** - Works on desktop, tablet, and mobile
- 🎨 **Modern UI** - Beautiful, interactive interface with smooth animations
- 🔒 **Redis Caching** - Fast seat lock management and session handling
- 🗄️ **MySQL Database** - Reliable data persistence
- 🚀 **Scalable Architecture** - Ready for production and expansion

---

## 🛠 Tech Stack

### Backend
- **Java 17** - Spring Boot 3.x
- **MySQL** - Relational database
- **Redis** - In-memory cache & session store
- **Maven** - Build automation
- **Spring Security** - Authentication & Authorization
- **JPA/Hibernate** - ORM framework

### Frontend
- **React 18** - UI library
- **Axios** - HTTP client
- **FullCalendar** - Calendar component
- **React Toastify** - Notifications
- **CSS3** - Modern styling

### DevOps & Deployment
- **Docker** - Containerization
- **Git** - Version control
- **Vercel** - Frontend hosting
- **Render** - Backend hosting
- **Railway/Upstash** - Database & Cache

---

## 🏗 Architecture

```
┌─────────────────────────────────────────────────────┐
│                  User's Browser                      │
└──────────────────────┬──────────────────────────────┘
                       │
        ┌──────────────┴──────────────┐
        │                             │
┌───────▼────────────┐      ┌─────────▼────────────┐
│  Vercel (Frontend) │      │ Render (Backend)     │
│  React SPA         │◄────►│ Spring Boot API      │
│  (CDN Global)      │      │ (Docker Container)   │
└────────────────────┘      └─────────┬────────────┘
                                      │
                    ┌─────────────────┼─────────────────┐
                    │                 │                 │
            ┌───────▼────────┐  ┌─────▼────────┐  ┌────▼──────────┐
            │  Railway MySQL │  │  Upstash     │  │ Render Logs   │
            │  Database      │  │  Redis Cache │  │               │
            └────────────────┘  └──────────────┘  └───────────────┘
```

---

## 📦 Installation & Setup

### Prerequisites
- **Node.js** 16+ (for frontend)
- **Java 17+** (for backend)
- **Maven 3.9+** (for backend)
- **MySQL 8+** (for database)
- **Redis 6+** (for caching) - **OR** use Upstash (free)
- **Git** (for version control)

### 1. Clone Repository
```bash
git clone https://github.com/YOUR_USERNAME/office-seat-booking.git
cd office-seat-booking
```

### 2. Setup Backend

```bash
# Navigate to backend directory
cd backend  # or stay in root if not in subdirectory

# Install dependencies
mvn clean install

# Create .env file from template
cp .env.example .env

# Edit .env with your local database credentials
# See Configuration section below
```

### 3. Setup Frontend

```bash
# Navigate to frontend directory
cd frontend

# Install dependencies
npm install

# Create .env file from template
cp .env.example .env.local

# (Keep default REACT_APP_API_URL=http://localhost:8080 for local dev)
```

---

## ⚙️ Configuration

### Backend Configuration

Create `.env` file in project root:

```env
# Database Configuration (Local Development)
DB_HOST=localhost
DB_PORT=3306
DB_NAME=seat_booking_db
DB_USER=root
DB_PASSWORD=your_password

# Redis Configuration (Local Development)
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_SSL_ENABLED=false

# JWT Configuration
JWT_SECRET=your-super-secret-key-at-least-256-bits-long
JWT_EXPIRATION=86400000

# Server Configuration
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev
```

### Frontend Configuration

Create `frontend/.env.local` file:

```env
# Local Development
REACT_APP_API_URL=http://localhost:8080
NODE_ENV=development

# Production (update after deployment)
# REACT_APP_API_URL=https://your-backend-url.onrender.com
```

### Environment-Specific Profiles

The application supports multiple profiles:
- **dev** - Development with verbose logging
- **prod** - Production with minimal logging and Upstash Redis

Switch profiles with:
```env
SPRING_PROFILES_ACTIVE=prod
```

---

## 🚀 Running Locally

### Start MySQL Database
```bash
# Using Docker (recommended)
docker run --name mysql-seat-booking \
  -e MYSQL_ROOT_PASSWORD=your_password \
  -e MYSQL_DATABASE=seat_booking_db \
  -d -p 3306:3306 mysql:8.0

# OR use your local MySQL installation
mysql -u root -p
CREATE DATABASE seat_booking_db;
```

### Start Redis
```bash
# Using Docker (recommended)
docker run --name redis-seat-booking \
  -d -p 6379:6379 redis:7-alpine

# OR use local Redis installation
redis-server
```

### Start Backend
```bash
# From project root
mvn spring-boot:run

# Or compile and run jar
mvn clean package
java -Dspring.profiles.active=dev -jar target/office-seat-booking-1.0.0.jar
```

Backend will start at: `http://localhost:8080`

### Start Frontend
```bash
# In frontend directory
npm start

# Application opens at http://localhost:3000
```

### Test Application
1. Open http://localhost:3000
2. Register a new account
3. Login with your credentials
4. Explore features:
   - Book a seat
   - View calendar
   - Admin panel (if admin role)

---

## 🌐 Deployment

### Complete Deployment Guide

See [DEPLOYMENT_GUIDE.md](./DEPLOYMENT_GUIDE.md) for detailed step-by-step instructions.

### Quick Deployment Summary

#### 1. Prepare Code for Production
```bash
# Update frontend API URL in .env
REACT_APP_API_URL=https://your-backend.onrender.com

# Push to GitHub
git add .
git commit -m "Prepare for production"
git push origin main
```

#### 2. Deploy Backend (Render)
```
1. Sign up at render.com
2. New Web Service → Select your repository
3. Configure:
   - Runtime: Docker
   - Instance: Free tier
   - Environment Variables: (see below)
4. Deploy
```

#### 3. Deploy Frontend (Vercel)
```
1. Sign up at vercel.com
2. Import GitHub repository
3. Root Directory: frontend
4. Environment Variables: REACT_APP_API_URL=...
5. Deploy
```

#### 4. Setup Database (Railway)
```
1. Sign up at railway.app
2. New Project → Add MySQL
3. Copy connection details to backend env vars
4. Create database: CREATE DATABASE seat_booking_db;
```

#### 5. Setup Redis (Upstash)
```
1. Sign up at upstash.com
2. Create Redis Database
3. Copy credentials to backend env vars
4. Enable SSL for Redis
```

### Production Environment Variables

**Backend (Render)**
```env
DB_HOST=your-railway-mysql-host
DB_PORT=12345
DB_NAME=seat_booking_db
DB_USER=root
DB_PASSWORD=your-railway-password

REDIS_HOST=your-upstash-host.upstash.io
REDIS_PORT=6379
REDIS_PASSWORD=your-upstash-token
REDIS_SSL_ENABLED=true

JWT_SECRET=generate-secure-256-bit-key
JWT_EXPIRATION=86400000

SPRING_PROFILES_ACTIVE=prod
```

**Frontend (Vercel)**
```env
REACT_APP_API_URL=https://your-backend.onrender.com
NODE_ENV=production
```

---

## 🔐 Environment Variables

All sensitive configuration is stored in environment variables. Never hardcode secrets!

### Backend Variables
| Variable | Description | Example |
|----------|-------------|---------|
| `DB_HOST` | MySQL host | `localhost` or `railway.proxy.rlwy.net` |
| `DB_PORT` | MySQL port | `3306` or `12345` |
| `DB_NAME` | Database name | `seat_booking_db` |
| `DB_USER` | Database user | `root` |
| `DB_PASSWORD` | Database password | `secure_password` |
| `REDIS_HOST` | Redis host | `localhost` or `your-redis.upstash.io` |
| `REDIS_PORT` | Redis port | `6379` |
| `REDIS_PASSWORD` | Redis password | (empty for local, token for Upstash) |
| `REDIS_SSL_ENABLED` | Enable SSL for Redis | `false` (local) or `true` (Upstash) |
| `JWT_SECRET` | JWT signing key | (256-bit minimum) |
| `JWT_EXPIRATION` | Token expiry (ms) | `86400000` (24 hours) |
| `SERVER_PORT` | Application port | `8080` |
| `SPRING_PROFILES_ACTIVE` | Active profile | `dev` or `prod` |

### Frontend Variables
| Variable | Description | Example |
|----------|-------------|---------|
| `REACT_APP_API_URL` | Backend API URL | `http://localhost:8080` or `https://api.onrender.com` |
| `NODE_ENV` | Environment | `development` or `production` |

---

## 📚 API Documentation

### Base URL
- **Local**: `http://localhost:8080/api`
- **Production**: `https://your-api.onrender.com/api`

### Authentication
All requests (except login/register) require JWT token in header:
```
Authorization: Bearer <token>
```

### Main Endpoints

#### Auth
- `POST /auth/login` - Login
- `POST /auth/register` - Register

#### Seats
- `GET /seats` - Get all seats
- `GET /seats/available` - Get available seats
- `GET /seats/type/{type}` - Get seats by type

#### Bookings
- `POST /bookings/lock/{seatId}` - Lock a seat (2 min)
- `POST /bookings` - Create booking
- `GET /bookings/my` - Get user bookings
- `GET /bookings` - Get all bookings (admin only)
- `GET /bookings/calendar` - Get calendar bookings
- `POST /bookings/meeting-room` - Book meeting room
- `PUT /bookings/{id}/cancel` - Cancel booking

#### Admin
- `POST /admin/seats` - Create new seat
- `PUT /admin/seats/{id}/status` - Update seat status
- `GET /bookings` - All bookings

For complete API details, see [API Documentation](./API_DOCUMENTATION.md)

---

## 📁 Project Structure

```
office-seat-booking/
├── src/main/java/com/officeseatbooking/
│   ├── controller/          # REST API endpoints
│   ├── service/             # Business logic
│   ├── repository/          # Database access
│   ├── entity/              # JPA entities
│   ├── dto/                 # Data transfer objects
│   ├── security/            # JWT & Auth
│   ├── config/              # Spring & Redis config
│   └── exception/           # Custom exceptions
│
├── src/main/resources/
│   ├── application.properties         # Dev config
│   ├── application-prod.properties    # Production config (uses env vars)
│   └── data.sql                       # Initial data
│
├── frontend/
│   ├── src/
│   │   ├── components/      # React components
│   │   ├── services/        # API client
│   │   ├── context/         # React context
│   │   ├── App.js           # Main app
│   │   └── index.js         # Entry point
│   ├── public/              # Static assets
│   ├── package.json         # Dependencies
│   ├── .env.example         # Env template
│   └── netlify.toml         # Netlify config
│
├── .env.example             # Backend env template
├── Dockerfile               # Backend Docker image
├── pom.xml                  # Maven dependencies
├── DEPLOYMENT_GUIDE.md      # Detailed deployment steps
└── README.md               # This file
```

---

## 🔄 Development Workflow

### Branch Strategy
```
main
├── production (merge releases here)
└── develop
    ├── feature/seat-management
    ├── feature/calendar-ui
    └── bugfix/lock-timeout
```

### Making Changes
```bash
# Create feature branch
git checkout -b feature/your-feature

# Make changes
git add .
git commit -m "Add your feature"

# Push and create PR
git push origin feature/your-feature
```

### Code Quality
- Follow Java naming conventions (camelCase, PascalCase for classes)
- Follow React best practices (functional components, hooks)
- Add comments for complex logic
- Test locally before pushing

---

## 🧪 Testing

### Backend Testing
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=BookingServiceTest

# Run with coverage
mvn clean test jacoco:report
```

### Frontend Testing
```bash
# Run tests
npm test

# Run with coverage
npm test -- --coverage
```

---

## 🐛 Troubleshooting

### Common Issues

**Issue**: Backend won't start
- Check MySQL is running: `mysql -u root -p`
- Check Redis is running: `redis-cli ping`
- Check env variables are set correctly
- Review logs in `logs/` directory

**Issue**: Frontend can't connect to API
- Verify `REACT_APP_API_URL` is correct
- Check backend is running and accessible
- Check CORS is enabled on backend
- Check browser console for network errors

**Issue**: Database connection failed
- Verify MySQL is running
- Verify credentials in `.env`
- Verify database exists: `CREATE DATABASE seat_booking_db;`

**Issue**: Redis connection failed
- Verify Redis is running
- For Upstash: Verify SSL is enabled in config
- Check password is correct

### Get Help
1. Check logs: `docker logs container-name`
2. Check database: `mysql -u root -p seat_booking_db`
3. Test API: `curl http://localhost:8080/api/health`
4. Check frontend console: Browser DevTools F12

---

## 📊 Performance & Scaling

### Optimization Tips
- Use Redis for frequent queries
- Enable database connection pooling (configured in prod)
- Gzip compression for frontend assets
- CDN for static files (Vercel handles this)
- Database indexing on frequently queried columns

### Monitoring
- Backend logs in Render dashboard
- Frontend errors in Vercel analytics
- Database performance in Railway dashboard
- Redis stats in Upstash dashboard

### Scaling
- Horizontal scaling: Add more Render instances
- Database: Upgrade Railway to higher tier
- Cache: Upgrade Upstash to higher tier
- Content: Vercel automatically scales

---

## 🤝 Contributing

We welcome contributions! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Standards
- Follow existing code style
- Add comments for complex logic
- Test your changes locally
- Update documentation if needed

---

## 📄 License

This project is licensed under the MIT License - see [LICENSE](LICENSE) file for details.

---

## 👥 Team & Support

### Developers
- Lead Developer: [Your Name]
- Contributors: [List contributors]

### Support Channels
- 📧 Email: support@example.com
- 💬 Discord: [Join server]
- 📝 Issues: [GitHub Issues](https://github.com/your-username/office-seat-booking/issues)

---

## 🎯 Roadmap

### Version 2.5 (Q3 2026)
- [ ] Mobile app (React Native)
- [ ] Email notifications
- [ ] Booking analytics
- [ ] Advanced reporting

### Version 3.0 (Q4 2026)
- [ ] AI-powered recommendations
- [ ] Integration with Google Calendar
- [ ] Video meeting room integration
- [ ] Workplace analytics

---

## 📞 Contact & Questions

Have questions? 
- Open an issue: [GitHub Issues](https://github.com/)
- Email: support@example.com
- Documentation: [Full Docs](./DOCUMENTATION_INDEX.md)

---

## 🙏 Acknowledgments

- Spring Boot & Spring Security teams
- React community
- Open source contributors
- All users and testers

---

**Last Updated**: May 1, 2026  
**Version**: 2.0.0  
**Status**: ✅ Production Ready  

**🚀 Ready to deploy? [Start Here](./DEPLOYMENT_GUIDE.md)**

