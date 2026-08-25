import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { register } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { apiError } from '../utils/errors';

const s = {
  wrap: { maxWidth: 400, margin: '80px auto', background: '#fff', padding: '2rem', borderRadius: 8, boxShadow: '0 2px 12px rgba(0,0,0,0.1)' },
  h2: { marginBottom: '1.5rem', color: '#1a1a2e' },
  input: { width: '100%', padding: '10px', marginBottom: '1rem', border: '1px solid #ddd', borderRadius: 4, fontSize: '1rem' },
  btn: { width: '100%', padding: '10px', background: '#e94560', color: '#fff', border: 'none', borderRadius: 4, fontSize: '1rem' },
  err: { color: 'red', marginBottom: '1rem', fontSize: '0.9rem' },
};

export default function Register() {
  const [form, setForm] = useState({ name: '', email: '', password: '', phone: '' });
  const [error, setError] = useState('');
  const { login: authLogin } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      const { data } = await register(form);
      authLogin(data);
      navigate('/products');
    } catch (err) {
      setError(apiError(err, 'Registration failed'));
    }
  };

  return (
    <div style={s.wrap}>
      <h2 style={s.h2}>Create Account</h2>
      {error && <p style={s.err}>{error}</p>}
      <form onSubmit={handleSubmit}>
        <input style={s.input} placeholder="Full Name" value={form.name}
          onChange={e => setForm({ ...form, name: e.target.value })} required />
        <input style={s.input} type="email" placeholder="Email" value={form.email}
          onChange={e => setForm({ ...form, email: e.target.value })} required />
        <input style={s.input} type="password" placeholder="Password (min 6 chars)" value={form.password}
          onChange={e => setForm({ ...form, password: e.target.value })} required minLength={6} />
        <input style={s.input} placeholder="Phone (optional)" value={form.phone}
          onChange={e => setForm({ ...form, phone: e.target.value })} />
        <button style={s.btn} type="submit">Register</button>
      </form>
      <p style={{ marginTop: '1rem', textAlign: 'center' }}>
        Have an account? <Link to="/login" style={{ color: '#e94560' }}>Login</Link>
      </p>
    </div>
  );
}
