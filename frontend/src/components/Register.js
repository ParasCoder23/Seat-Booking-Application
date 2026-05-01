import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import { authAPI } from '../services/api';
import { useAuth } from '../context/AuthContext';

const Register = () => {
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    role: 'EMPLOYEE'
  });
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const { login } = useAuth();

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      const response = await authAPI.register(formData);
      const { token, username, email, role, userId, id } = response.data;

      // Extract user ID - try userId first, then id
      const extractedId = userId || id;

      if (!extractedId) {
        console.error('No user ID in response:', response.data);
        toast.error('Registration failed - no user ID received from server');
        setLoading(false);
        return;
      }

      // Create user object with all necessary fields
      const userObject = {
        id: extractedId,
        username: username || '',
        email: email || '',
        role: role || 'EMPLOYEE'
      };

      login(userObject, token);
      toast.success('Registration successful!');
      navigate('/dashboard');
    } catch (error) {
      console.error('Registration error:', error);
      toast.error(error.response?.data?.message || 'Registration failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
    }}>
      <div className="card" style={{ width: '400px', maxWidth: '90vw' }}>
        <div className="text-center mb-4">
          <h2 style={{ color: '#333', marginBottom: '10px' }}>Create Account</h2>
          <p style={{ color: '#666' }}>Join our office seat booking system</p>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="username">Username</label>
            <input
              type="text"
              id="username"
              name="username"
              value={formData.username}
              onChange={handleChange}
              required
              placeholder="Enter your username"
            />
          </div>

          <div className="form-group">
            <label htmlFor="email">Email</label>
            <input
              type="email"
              id="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              required
              placeholder="Enter your email"
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">Password</label>
            <input
              type="password"
              id="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              required
              placeholder="Enter your password"
            />
          </div>

          <div className="form-group">
            <label htmlFor="role">Role</label>
            <select
              id="role"
              name="role"
              value={formData.role}
              onChange={handleChange}
              required
            >
              <option value="EMPLOYEE">Employee</option>
              <option value="MANAGER">Manager</option>
            </select>
          </div>

          <button
            type="submit"
            className="btn w-100 mb-3"
            disabled={loading}
          >
            {loading ? 'Creating account...' : 'Create Account'}
          </button>
        </form>

        <div className="text-center">
          <p style={{ color: '#666' }}>
            Already have an account?{' '}
            <Link to="/login" style={{ color: '#007bff', textDecoration: 'none' }}>
              Sign in
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
};

export default Register;
