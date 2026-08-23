import React, { useState, useEffect, useCallback } from 'react';
import { getCart, updateCartItem, removeFromCart, checkout, verifyPayment } from '../services/api';
import { useNavigate } from 'react-router-dom';
import { apiError } from '../utils/errors';

const s = {
  page: { padding: '2rem', maxWidth: 700, margin: '0 auto' },
  row: { display: 'flex', alignItems: 'center', gap: '1rem', background: '#fff', padding: '1rem', borderRadius: 8, marginBottom: '1rem', boxShadow: '0 1px 4px rgba(0,0,0,0.07)' },
  name: { flex: 1, fontWeight: 600 },
  qty: { display: 'flex', alignItems: 'center', gap: '0.5rem' },
  qtyBtn: { width: 28, height: 28, border: '1px solid #ddd', background: '#f5f5f5', borderRadius: 4, fontWeight: 700 },
  removeBtn: { background: 'none', border: 'none', color: '#e94560', fontWeight: 600 },
  total: { textAlign: 'right', fontSize: '1.2rem', fontWeight: 700, margin: '1rem 0' },
  checkoutBtn: { display: 'block', width: '100%', padding: '12px', background: '#e94560', color: '#fff', border: 'none', borderRadius: 6, fontSize: '1rem' },
  err: { background: '#fdecea', color: '#c0392b', padding: '0.75rem 1rem', borderRadius: 6, marginBottom: '1rem', fontSize: '0.9rem' },
};

export default function Cart() {
  const [items, setItems] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const navigate = useNavigate();

  const loadCart = useCallback(async () => {
    try {
      const { data } = await getCart();
      setItems(data);
    } catch (err) {
      setError(apiError(err, 'Could not load your cart'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadCart(); }, [loadCart]);

  const handleQty = async (item, delta) => {
    setError('');
    const newQty = item.quantity + delta;
    try {
      if (newQty < 1) await removeFromCart(item.id);
      else await updateCartItem(item.id, newQty);
      await loadCart();
    } catch (err) {
      // e.g. the requested quantity exceeds available stock
      setError(apiError(err, 'Could not update that item'));
    }
  };

  const handleRemove = async (itemId) => {
    setError('');
    try {
      await removeFromCart(itemId);
      await loadCart();
    } catch (err) {
      setError(apiError(err, 'Could not remove that item'));
    }
  };

  const total = items.reduce((sum, i) => sum + i.product.price * i.quantity, 0);

  const handleCheckout = async () => {
    setError('');
    if (!window.Razorpay) {
      setError('Payment library failed to load. Check your connection and refresh.');
      return;
    }
    setBusy(true);
    try {
      const { data } = await checkout();
      const rzp = new window.Razorpay({
        key: data.keyId,
        amount: data.amount,
        currency: data.currency,
        order_id: data.razorpayOrderId,
        name: 'GUVI Shop',
        description: 'Order Payment',
        handler: async (response) => {
          try {
            await verifyPayment({
              razorpayOrderId: response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature,
            });
            navigate('/orders');
          } catch (err) {
            setError(apiError(err, 'Payment could not be verified. Please contact support.'));
          } finally {
            setBusy(false);
          }
        },
        modal: {
          ondismiss: () => setBusy(false),
        },
      });
      rzp.on('payment.failed', (response) => {
        setError(response?.error?.description || 'Payment failed. Please try again.');
        setBusy(false);
      });
      rzp.open();
    } catch (err) {
      // e.g. the cart is empty, or stock ran out between adding and checking out
      setError(apiError(err, 'Could not start checkout'));
      setBusy(false);
    }
  };

  if (loading) return <div style={{ padding: '2rem', textAlign: 'center' }}>Loading your cart…</div>;

  if (items.length === 0) {
    return (
      <div style={{ padding: '2rem', textAlign: 'center' }}>
        {error && <p style={{ ...s.err, maxWidth: 700, margin: '0 auto 1rem' }}>{error}</p>}
        Your cart is empty.
      </div>
    );
  }

  return (
    <div style={s.page}>
      <h2 style={{ marginBottom: '1.5rem' }}>Shopping Cart</h2>
      {error && <p style={s.err}>{error}</p>}
      {items.map(item => (
        <div key={item.id} style={s.row}>
          <div style={s.name}>{item.product.name}</div>
          <div style={{ color: '#e94560', fontWeight: 700 }}>₹{item.product.price}</div>
          <div style={s.qty}>
            <button style={s.qtyBtn} onClick={() => handleQty(item, -1)}>-</button>
            <span>{item.quantity}</span>
            <button style={s.qtyBtn} onClick={() => handleQty(item, 1)}
              disabled={item.quantity >= item.product.stock}>+</button>
          </div>
          <div style={{ fontWeight: 600 }}>₹{(item.product.price * item.quantity).toFixed(2)}</div>
          <button style={s.removeBtn} onClick={() => handleRemove(item.id)}>Remove</button>
        </div>
      ))}
      <div style={s.total}>Total: ₹{total.toFixed(2)}</div>
      <button style={s.checkoutBtn} onClick={handleCheckout} disabled={busy}>
        {busy ? 'Processing…' : 'Proceed to Checkout'}
      </button>
    </div>
  );
}
