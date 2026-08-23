import React from 'react';
import { Link } from 'react-router-dom';

const s = {
  hero: { background: 'linear-gradient(135deg, #1a1a2e 0%, #16213e 100%)', color: '#fff', padding: '5rem 2rem', textAlign: 'center' },
  h1: { fontSize: '2.5rem', marginBottom: '1rem' },
  subtitle: { fontSize: '1.1rem', color: '#ccc', marginBottom: '2rem' },
  btn: { display: 'inline-block', padding: '12px 32px', background: '#e94560', color: '#fff', borderRadius: 6, fontWeight: 700, fontSize: '1rem' },
  features: { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '2rem', padding: '3rem 2rem', maxWidth: 900, margin: '0 auto' },
  feature: { textAlign: 'center', padding: '1.5rem', background: '#fff', borderRadius: 8, boxShadow: '0 2px 8px rgba(0,0,0,0.08)' },
  icon: { fontSize: '2.5rem', marginBottom: '0.75rem' },
};

export default function Home() {
  return (
    <>
      <div style={s.hero}>
        <h1 style={s.h1}>Welcome to GUVI Shop</h1>
        <p style={s.subtitle}>Browse thousands of products with secure Razorpay payments</p>
        <Link to="/products" style={s.btn}>Shop Now</Link>
      </div>
      <div style={s.features}>
        {[
          { icon: '🛍️', title: 'Wide Selection', desc: 'Thousands of products across categories' },
          { icon: '🔒', title: 'Secure Payments', desc: 'Razorpay-powered secure checkout' },
          { icon: '📦', title: 'Order Tracking', desc: 'Track your orders in real-time' },
          { icon: '👤', title: 'Easy Account', desc: 'Simple registration and login' },
        ].map(f => (
          <div key={f.title} style={s.feature}>
            <div style={s.icon}>{f.icon}</div>
            <h3 style={{ marginBottom: '0.5rem' }}>{f.title}</h3>
            <p style={{ color: '#666', fontSize: '0.9rem' }}>{f.desc}</p>
          </div>
        ))}
      </div>
    </>
  );
}
