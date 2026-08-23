import axios from 'axios';

// In production set REACT_APP_API_URL to your backend URL, e.g. https://ecommerce-backend.onrender.com/api
// In development it falls back to '/api', which is proxied to localhost:8080 (see package.json "proxy").
const api = axios.create({ baseURL: process.env.REACT_APP_API_URL || '/api' });

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// A 401 means the stored token is missing, expired, or invalid — tokens last 24h,
// so this is mostly expiry. Clear the dead session and send the user to log in
// again rather than letting every subsequent page fail. Login/register replies
// are excluded: a 401 there just means bad credentials.
api.interceptors.response.use(
  response => response,
  error => {
    const status = error?.response?.status;
    const url = error?.config?.url || '';
    if (status === 401 && !url.includes('/auth/')) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      if (window.location.pathname !== '/login') {
        window.location.assign('/login');
      }
    }
    return Promise.reject(error);
  }
);

// Auth
export const register = (data) => api.post('/auth/register', data);
export const login = (data) => api.post('/auth/login', data);

// Profile
export const getProfile = () => api.get('/users/me');
export const updateProfile = (data) => api.put('/users/me', data);

// Products
export const getProducts = () => api.get('/products');
export const getProduct = (id) => api.get(`/products/${id}`);
export const searchProducts = (query) => api.get('/products/search', { params: { query } });
export const createProduct = (data) => api.post('/products', data);
export const updateProduct = (id, data) => api.put(`/products/${id}`, data);
export const deleteProduct = (id) => api.delete(`/products/${id}`);

// Cart
export const getCart = () => api.get('/cart');
export const addToCart = (data) => api.post('/cart', data);
export const updateCartItem = (itemId, quantity) => api.put(`/cart/${itemId}?quantity=${quantity}`);
export const removeFromCart = (itemId) => api.delete(`/cart/${itemId}`);
export const clearCart = () => api.delete('/cart');

// Orders
export const getMyOrders = () => api.get('/orders');
export const checkout = () => api.post('/orders/checkout');
export const verifyPayment = (data) => api.post('/orders/verify-payment', data);
export const getAllOrders = () => api.get('/orders/admin/all');
export const updateOrderStatus = (id, status) => api.put(`/orders/admin/${id}/status?status=${status}`);

export default api;
