import React, { useState, useEffect } from 'react';
import { getProducts, searchProducts, addToCart } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import { apiError } from '../utils/errors';

const s = {
  page: { padding: '2rem' },
  search: { width: '100%', maxWidth: 400, padding: '10px', marginBottom: '1.5rem', border: '1px solid #ddd', borderRadius: 4, fontSize: '1rem' },
  grid: { display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))', gap: '1.5rem' },
  card: { background: '#fff', borderRadius: 8, overflow: 'hidden', boxShadow: '0 2px 8px rgba(0,0,0,0.08)' },
  img: { width: '100%', height: 180, objectFit: 'cover', background: '#eee' },
  info: { padding: '1rem' },
  name: { fontWeight: 600, marginBottom: '0.5rem' },
  price: { color: '#e94560', fontWeight: 700, fontSize: '1.1rem', marginBottom: '0.5rem' },
  stock: { color: '#888', fontSize: '0.85rem', marginBottom: '0.75rem' },
  btn: { width: '100%', padding: '8px', background: '#e94560', color: '#fff', border: 'none', borderRadius: 4 },
  msg: { color: 'green', fontSize: '0.85rem', marginTop: '0.5rem' },
  itemErr: { color: '#c0392b', fontSize: '0.85rem', marginTop: '0.5rem' },
  err: { background: '#fdecea', color: '#c0392b', padding: '0.75rem 1rem', borderRadius: 6, marginBottom: '1rem', fontSize: '0.9rem' },
};

export default function Products() {
  const [products, setProducts] = useState([]);
  const [query, setQuery] = useState('');
  const [added, setAdded] = useState({});
  const [itemErrors, setItemErrors] = useState({});
  const [error, setError] = useState('');
  const { user } = useAuth();
  const navigate = useNavigate();

  // Also performs the initial load (empty query). Debounced so a fast typist
  // doesn't fire a request per keystroke, and so a slow earlier response can't
  // overwrite the results of a later query.
  useEffect(() => {
    const handle = setTimeout(async () => {
      try {
        const r = query.trim() ? await searchProducts(query.trim()) : await getProducts();
        setProducts(r.data);
        setError('');
      } catch (err) {
        setError(apiError(err, query.trim() ? 'Search failed' : 'Could not load products'));
      }
    }, query ? 300 : 0);
    return () => clearTimeout(handle);
  }, [query]);

  const handleAddToCart = async (productId) => {
    if (!user) { navigate('/login'); return; }
    setItemErrors(prev => ({ ...prev, [productId]: '' }));
    try {
      await addToCart({ productId, quantity: 1 });
      setAdded(prev => ({ ...prev, [productId]: true }));
      setTimeout(() => setAdded(prev => ({ ...prev, [productId]: false })), 2000);
    } catch (err) {
      // e.g. requesting more units than are in stock
      setItemErrors(prev => ({ ...prev, [productId]: apiError(err, 'Could not add to cart') }));
    }
  };

  return (
    <div style={s.page}>
      <h2 style={{ marginBottom: '1rem' }}>Products</h2>
      {error && <p style={s.err}>{error}</p>}
      <input style={s.search} placeholder="Search products..." value={query}
        onChange={e => setQuery(e.target.value)} />
      {products.length === 0 && !error && (
        <p style={{ color: '#888' }}>No products found.</p>
      )}
      <div style={s.grid}>
        {products.map(p => (
          <div key={p.id} style={s.card}>
            {p.imageUrl ? (
              <img src={p.imageUrl} alt={p.name} style={s.img} />
            ) : (
              <div style={{ ...s.img, display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#aaa' }}>No Image</div>
            )}
            <div style={s.info}>
              <div style={s.name}>{p.name}</div>
              <div style={{ fontSize: '0.85rem', color: '#666', marginBottom: '0.5rem' }}>{p.description}</div>
              <div style={s.price}>₹{p.price}</div>
              <div style={s.stock}>Stock: {p.stock}</div>
              <button style={s.btn} onClick={() => handleAddToCart(p.id)} disabled={p.stock === 0}>
                {p.stock === 0 ? 'Out of Stock' : 'Add to Cart'}
              </button>
              {added[p.id] && <div style={s.msg}>Added to cart!</div>}
              {itemErrors[p.id] && <div style={s.itemErr}>{itemErrors[p.id]}</div>}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
