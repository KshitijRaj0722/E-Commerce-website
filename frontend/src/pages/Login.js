import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { login } from '../services/api';
import { useAuth } from '../context/AuthContext';

const s = {
  wrap: { maxWidth: 400, margin: '80px auto', background: '#fff', padding: '2rem', borderRadius: 8, boxShadow: '0 2px 12px rgba(0,0,0,0.1)' },
  h2: { marginBottom: '1.5rem', color: '#1a1a2e' },
  input: { width: '100%', padding: '10px', marginBottom: '1rem', border: '1px solid #ddd', borderRadius: 4, fontSize: '1rem' },
  btn: { width: '100%', padding: '10px', background: '#e94560', color: '#fff', border: 'none', borderRadius: 4, fontSize: '1rem' },
  err: { color: 'red', marginBottom: '1rem', fontSize: '0.9rem' },
};

export default function Login() {
  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const { login: authLogin } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      const { data } = await login(form);
      authLogin(data);
      navigate(data.role === 'ADMIN' ? '/admin' : '/products');
    } catch (err) {
      setError(err.response?.data?.message || 'Login failed');
    }
  };

  return (
    <div style={s.wrap}>
      <h2 style={s.h2}>Login</h2>
      {error && <p style={s.err}>{error}</p>}
      <form onSubmit={handleSubmit}>
        <input style={s.input} type="email" placeholder="Email" value={form.email}
          onChange={e => setForm({ ...form, email: e.target.value })} required />
        <input style={s.input} type="password" placeholder="Password" value={form.password}
          onChange={e => setForm({ ...form, password: e.target.value })} required />
        <button style={s.btn} type="submit">Login</button>
      </form>
      <p style={{ marginTop: '1rem', textAlign: 'center' }}>
        No account? <Link to="/register" style={{ color: '#e94560' }}>Register</Link>
      </p>
    </div>
  );
}
