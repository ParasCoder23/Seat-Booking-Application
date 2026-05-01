import axios from 'axios';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

// Create axios instance
const api = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add request interceptor to include auth token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Add response interceptor to handle errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// Auth API
export const authAPI = {
  login: (credentials) => api.post('/auth/login', credentials),
  register: (userData) => api.post('/auth/register', userData),
};

// Seats API
export const seatsAPI = {
  getAllSeats: () => api.get('/seats'),
  getAvailableSeats: () => api.get('/seats/available'),
  getSeatsByFloor: (floor) => api.get(`/seats/floor/${floor}`),
  getAvailableSeatsByFloor: (floor) => api.get(`/seats/floor/${floor}/available`),
  getSeatsByType: (type) => api.get(`/seats/type/${type}`),
  getSeatById: (id) => api.get(`/seats/${id}`),
};

// Bookings API
export const bookingsAPI = {
  lockSeat: (seatId) => api.post(`/bookings/lock/${seatId}`),
  unlockSeat: (seatId) => api.post(`/bookings/unlock/${seatId}`),
  createBooking: (bookingData) => api.post('/bookings', bookingData),
  getUserBookings: () => api.get('/bookings/my'),
  getAllBookings: () => api.get('/bookings'),
  getBookingById: (id) => api.get(`/bookings/${id}`),
  cancelBooking: (id) => api.put(`/bookings/${id}/cancel`),
  getCalendarBookings: () => api.get('/bookings/calendar'),
  bookMeetingRoom: (bookingData) => api.post('/bookings/meeting-room', bookingData),
};

// Admin API
export const adminAPI = {
  createSeat: (seatData) => api.post('/admin/seats', seatData),
  updateSeatStatus: (id, status) => api.put(`/admin/seats/${id}/status`, { status }),
  deleteSeat: (id) => api.delete(`/admin/seats/${id}`),
};

// Users API
export const usersAPI = {
  getEmployees: () => api.get('/users/employees'),
  getAllUsers: () => api.get('/users'),
  searchEmployees: (query) => api.get(`/users/employees/search?query=${query}`),
};

export default api;
// No changes needed here for Toastify duration. Update toast calls in components instead.
