import React, { useState, useEffect } from 'react';
import { getProducts, createProduct, updateProduct, deleteProduct, getAllOrders, updateOrderStatus } from '../services/api';
import { apiError } from '../utils/errors';

const s = {
  page: { padding: '2rem' },
  tabs: { display: 'flex', gap: '1rem', marginBottom: '2rem' },
  tab: (active) => ({ padding: '8px 20px', border: 'none', borderRadius: 4, background: active ? '#e94560' : '#ddd', color: active ? '#fff' : '#333', fontWeight: 600, cursor: 'pointer' }),
  form: { background: '#fff', padding: '1.5rem', borderRadius: 8, marginBottom: '2rem', boxShadow: '0 1px 4px rgba(0,0,0,0.07)', maxWidth: 500 },
  input: { width: '100%', padding: '8px', marginBottom: '0.75rem', border: '1px solid #ddd', borderRadius: 4 },
  btn: { padding: '8px 20px', background: '#e94560', color: '#fff', border: 'none', borderRadius: 4, marginRight: '0.5rem' },
  table: { width: '100%', borderCollapse: 'collapse', background: '#fff', borderRadius: 8, overflow: 'hidden', boxShadow: '0 1px 4px rgba(0,0,0,0.07)' },
  th: { background: '#1a1a2e', color: '#fff', padding: '10px 14px', textAlign: 'left', fontSize: '0.9rem' },
  td: { padding: '10px 14px', borderBottom: '1px solid #f0f0f0', fontSize: '0.9rem' },
  delBtn: { background: '#e74c3c', color: '#fff', border: 'none', borderRadius: 4, padding: '4px 10px', marginLeft: '0.5rem' },
  err: { background: '#fdecea', color: '#c0392b', padding: '0.75rem 1rem', borderRadius: 6, marginBottom: '1rem', fontSize: '0.9rem', maxWidth: 700 },
};

const emptyForm = { name: '', description: '', price: '', stock: '', imageUrl: '', category: '' };

export default function Admin() {
  const [tab, setTab] = useState('products');
  const [products, setProducts] = useState([]);
  const [orders, setOrders] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [editId, setEditId] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    getProducts()
      .then(r => setProducts(r.data))
      .catch(err => setError(apiError(err, 'Could not load products')));
    getAllOrders()
      .then(r => setOrders(r.data))
      .catch(err => setError(apiError(err, 'Could not load orders')));
  }, []);

  const handleSave = async (e) => {
    e.preventDefault();
    setError('');
    const price = parseFloat(form.price);
    const stock = parseInt(form.stock, 10);
    if (Number.isNaN(price) || price < 0) { setError('Price must be a number of 0 or more'); return; }
    if (Number.isNaN(stock) || stock < 0) { setError('Stock must be a whole number of 0 or more'); return; }

    const payload = { ...form, price, stock };
    try {
      if (editId) {
        const r = await updateProduct(editId, payload);
        setProducts(products.map(p => p.id === editId ? r.data : p));
      } else {
        const r = await createProduct(payload);
        setProducts([...products, r.data]);
      }
      setForm(emptyForm);
      setEditId(null);
    } catch (err) {
      setError(apiError(err, 'Could not save the product'));
    }
  };

  const handleEdit = (p) => {
    setForm({ name: p.name, description: p.description || '', price: p.price, stock: p.stock, imageUrl: p.imageUrl || '', category: p.category || '' });
    setEditId(p.id);
  };

  const handleDelete = async (id) => {
    setError('');
    if (!window.confirm('Delete this product? This cannot be undone.')) return;
    try {
      await deleteProduct(id);
      setProducts(products.filter(p => p.id !== id));
    } catch (err) {
      setError(apiError(err, 'Could not delete the product'));
    }
  };

  const handleStatusChange = async (orderId, status) => {
    setError('');
    try {
      const r = await updateOrderStatus(orderId, status);
      setOrders(orders.map(o => o.id === orderId ? r.data : o));
    } catch (err) {
      setError(apiError(err, 'Could not update the order status'));
    }
  };

  return (
    <div style={s.page}>
      <h2 style={{ marginBottom: '1.5rem' }}>Admin Panel</h2>
      {error && <p style={s.err}>{error}</p>}
      <div style={s.tabs}>
        <button style={s.tab(tab === 'products')} onClick={() => setTab('products')}>Products</button>
        <button style={s.tab(tab === 'orders')} onClick={() => setTab('orders')}>Orders</button>
      </div>

      {tab === 'products' && (
        <>
          <form style={s.form} onSubmit={handleSave}>
            <h3 style={{ marginBottom: '1rem' }}>{editId ? 'Edit Product' : 'Add Product'}</h3>
            {['name', 'description', 'price', 'stock', 'imageUrl', 'category'].map(field => (
              <input key={field} style={s.input} placeholder={field.charAt(0).toUpperCase() + field.slice(1)}
                type={field === 'price' || field === 'stock' ? 'number' : 'text'}
                min={field === 'price' || field === 'stock' ? '0' : undefined}
                step={field === 'price' ? '0.01' : field === 'stock' ? '1' : undefined}
                value={form[field]} onChange={e => setForm({ ...form, [field]: e.target.value })}
                required={['name', 'price', 'stock'].includes(field)} />
            ))}
            <button style={s.btn} type="submit">{editId ? 'Update' : 'Add'}</button>
            {editId && <button style={{ ...s.btn, background: '#888' }} type="button" onClick={() => { setForm(emptyForm); setEditId(null); }}>Cancel</button>}
          </form>
          <table style={s.table}>
            <thead><tr>{['ID', 'Name', 'Price', 'Stock', 'Category', 'Actions'].map(h => <th key={h} style={s.th}>{h}</th>)}</tr></thead>
            <tbody>
              {products.map(p => (
                <tr key={p.id}>
                  <td style={s.td}>{p.id}</td>
                  <td style={s.td}>{p.name}</td>
                  <td style={s.td}>₹{p.price}</td>
                  <td style={s.td}>{p.stock}</td>
                  <td style={s.td}>{p.category}</td>
                  <td style={s.td}>
                    <button style={s.btn} onClick={() => handleEdit(p)}>Edit</button>
                    <button style={s.delBtn} onClick={() => handleDelete(p.id)}>Delete</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}

      {tab === 'orders' && (
        <table style={s.table}>
          <thead><tr>{['Order ID', 'User', 'Total', 'Status', 'Date', 'Action'].map(h => <th key={h} style={s.th}>{h}</th>)}</tr></thead>
          <tbody>
            {orders.map(o => (
              <tr key={o.id}>
                <td style={s.td}>{o.id}</td>
                <td style={s.td}>{o.user?.email}</td>
                <td style={s.td}>₹{o.totalAmount}</td>
                <td style={s.td}>{o.status}</td>
                <td style={s.td}>{new Date(o.createdAt).toLocaleDateString()}</td>
                <td style={s.td}>
                  <select value={o.status} onChange={e => handleStatusChange(o.id, e.target.value)}
                    style={{ padding: '4px', borderRadius: 4, border: '1px solid #ddd' }}>
                    {['CREATED', 'PAID', 'FAILED', 'CANCELLED'].map(st => <option key={st}>{st}</option>)}
                  </select>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
