import React, { useState, useEffect } from 'react';
import { getMyOrders } from '../services/api';
import { apiError } from '../utils/errors';

const statusColor = { CREATED: '#f39c12', PAID: '#27ae60', FAILED: '#e74c3c', CANCELLED: '#95a5a6' };

const s = {
  page: { padding: '2rem', maxWidth: 800, margin: '0 auto' },
  card: { background: '#fff', borderRadius: 8, padding: '1.5rem', marginBottom: '1rem', boxShadow: '0 1px 4px rgba(0,0,0,0.07)' },
  header: { display: 'flex', justifyContent: 'space-between', marginBottom: '1rem' },
  badge: (status) => ({ background: statusColor[status] || '#888', color: '#fff', padding: '3px 10px', borderRadius: 12, fontSize: '0.8rem' }),
  item: { display: 'flex', justifyContent: 'space-between', padding: '0.4rem 0', borderBottom: '1px solid #f0f0f0', fontSize: '0.9rem' },
};

export default function Orders() {
  const [orders, setOrders] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getMyOrders()
      .then(r => setOrders(r.data))
      .catch(err => setError(apiError(err, 'Could not load your orders')))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div style={{ padding: '2rem', textAlign: 'center' }}>Loading your orders…</div>;
  if (error) return <div style={{ padding: '2rem', textAlign: 'center', color: '#c0392b' }}>{error}</div>;
  if (orders.length === 0) return <div style={{ padding: '2rem', textAlign: 'center' }}>No orders yet.</div>;

  return (
    <div style={s.page}>
      <h2 style={{ marginBottom: '1.5rem' }}>My Orders</h2>
      {orders.map(order => (
        <div key={order.id} style={s.card}>
          <div style={s.header}>
            <div>
              <strong>Order #{order.id}</strong>
              <div style={{ color: '#888', fontSize: '0.85rem' }}>{new Date(order.createdAt).toLocaleDateString()}</div>
            </div>
            <span style={s.badge(order.status)}>{order.status}</span>
          </div>
          {order.items?.map(item => (
            <div key={item.id} style={s.item}>
              <span>{item.product.name} × {item.quantity}</span>
              <span>₹{(item.price * item.quantity).toFixed(2)}</span>
            </div>
          ))}
          <div style={{ textAlign: 'right', fontWeight: 700, marginTop: '0.75rem' }}>
            Total: ₹{order.totalAmount}
          </div>
        </div>
      ))}
    </div>
  );
}
