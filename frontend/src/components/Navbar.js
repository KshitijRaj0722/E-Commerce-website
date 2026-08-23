import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const styles = {
  nav: { background: '#1a1a2e', padding: '0 2rem', display: 'flex', alignItems: 'center', justifyContent: 'space-between', height: 60 },
  brand: { color: '#e94560', fontWeight: 700, fontSize: '1.3rem' },
  links: { display: 'flex', gap: '1.5rem', alignItems: 'center' },
  link: { color: '#ccc', fontSize: '0.9rem' },
  btn: { background: '#e94560', color: '#fff', border: 'none', padding: '6px 14px', borderRadius: 4, fontSize: '0.9rem' },
};

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => { logout(); navigate('/login'); };

  return (
    <nav style={styles.nav}>
      <Link to="/" style={styles.brand}>GUVI Shop</Link>
      <div style={styles.links}>
        <Link to="/products" style={styles.link}>Products</Link>
        {user ? (
          <>
            <Link to="/cart" style={styles.link}>Cart</Link>
            <Link to="/orders" style={styles.link}>Orders</Link>
            <Link to="/profile" style={styles.link}>Profile</Link>
            {user.role === 'ADMIN' && <Link to="/admin" style={styles.link}>Admin</Link>}
            <button onClick={handleLogout} style={styles.btn}>Logout</button>
          </>
        ) : (
          <>
            <Link to="/login" style={styles.link}>Login</Link>
            <Link to="/register" style={{ ...styles.btn, display: 'inline-block' }}>Register</Link>
          </>
        )}
      </div>
    </nav>
  );
}
