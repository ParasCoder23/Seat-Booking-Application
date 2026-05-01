import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import { authAPI } from '../services/api';
import { useAuth } from '../context/AuthContext';

const Login = () => {
  const [formData, setFormData] = useState({
    username: '',
    password: ''
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
      const response = await authAPI.login(formData);
      console.log('Full login response:', response.data);

      const { token, username, email, role, userId, id } = response.data;

      // Extract user ID - try userId first, then id
      const extractedId = userId || id;

      if (!extractedId) {
        console.error('No user ID in response:', response.data);
        toast.error('Login failed - no user ID received from server');
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

      console.log('Created user object for storage:', userObject);

      login(userObject, token);
      toast.success('Login successful!');
      navigate('/dashboard');
    } catch (error) {
      console.error('Login error:', error);
      toast.error(error.response?.data?.message || 'Login failed');
    } finally {
      setLoading(false);
    }
  };

  // const fillDemoCredentials = (role) => {
  //   const credentials = {
  //     'admin': { username: 'admin', password: 'admin123' },
  //     'manager': { username: 'manager', password: 'manager123' },
  //     'employee': { username: 'employee', password: 'employee123' }
  //   };
  //   setFormData(credentials[role]);
  // };

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
          <h2 style={{ color: '#333', marginBottom: '10px' }}>Welcome Back</h2>
          <p style={{ color: '#666' }}>Sign in to your account</p>
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

          <button
            type="submit"
            className="btn w-100 mb-3"
            disabled={loading}
          >
            {loading ? 'Signing in...' : 'Sign In'}
          </button>
        </form>

        {/* Demo Accounts */}
        {/* <div style={{ marginBottom: '20px' }}>
          <p style={{ textAlign: 'center', marginBottom: '10px', color: '#666', fontSize: '0.9rem' }}>
            Demo Accounts:
          </p>
          <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
            <button
              type="button"
              onClick={() => fillDemoCredentials('admin')}
              className="btn"
              style={{ flex: 1, fontSize: '0.8rem', padding: '8px' }}
            >
              Admin
            </button>
            <button
              type="button"
              onClick={() => fillDemoCredentials('manager')}
              className="btn"
              style={{ flex: 1, fontSize: '0.8rem', padding: '8px' }}
            >
              Manager
            </button>
            <button
              type="button"
              onClick={() => fillDemoCredentials('employee')}
              className="btn"
              style={{ flex: 1, fontSize: '0.8rem', padding: '8px' }}
            >
              Employee
            </button>
          </div>
        </div> */}

        <div className="text-center">
          <p style={{ color: '#666' }}>
            Don't have an account?{' '}
            <Link to="/register" style={{ color: '#007bff', textDecoration: 'none' }}>
              Sign up
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
};

export default Login;
