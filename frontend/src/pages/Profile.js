import React, { useState, useEffect } from 'react';
import { getProfile, updateProfile } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { apiError } from '../utils/errors';

const s = {
  wrap: { maxWidth: 480, margin: '48px auto', background: '#fff', padding: '2rem', borderRadius: 8, boxShadow: '0 2px 12px rgba(0,0,0,0.1)' },
  h2: { marginBottom: '0.5rem', color: '#1a1a2e' },
  sub: { color: '#888', fontSize: '0.85rem', marginBottom: '1.5rem' },
  label: { display: 'block', fontSize: '0.8rem', color: '#666', marginBottom: '0.25rem' },
  input: { width: '100%', padding: '10px', marginBottom: '1rem', border: '1px solid #ddd', borderRadius: 4, fontSize: '1rem' },
  readonly: { width: '100%', padding: '10px', marginBottom: '1rem', border: '1px solid #eee', borderRadius: 4, fontSize: '1rem', background: '#f7f7f7', color: '#777' },
  btn: { width: '100%', padding: '10px', background: '#e94560', color: '#fff', border: 'none', borderRadius: 4, fontSize: '1rem' },
  divider: { border: 'none', borderTop: '1px solid #eee', margin: '1.5rem 0' },
  err: { color: '#e74c3c', marginBottom: '1rem', fontSize: '0.9rem' },
  ok: { color: '#27ae60', marginBottom: '1rem', fontSize: '0.9rem' },
  legend: { fontSize: '0.95rem', fontWeight: 600, marginBottom: '0.75rem', color: '#1a1a2e' },
};

export default function Profile() {
  const { updateUser } = useAuth();
  const [form, setForm] = useState({ name: '', phone: '', currentPassword: '', newPassword: '' });
  const [email, setEmail] = useState('');
  const [role, setRole] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    getProfile()
      .then(({ data }) => {
        setForm(f => ({ ...f, name: data.name || '', phone: data.phone || '' }));
        setEmail(data.email || '');
        setRole(data.role || '');
      })
      .catch(err => setError(apiError(err, 'Could not load your profile')))
      .finally(() => setLoading(false));
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setSaving(true);
    try {
      const payload = { name: form.name, phone: form.phone };
      if (form.newPassword) {
        payload.currentPassword = form.currentPassword;
        payload.newPassword = form.newPassword;
      }
      const { data } = await updateProfile(payload);
      updateUser({ name: data.name });
      setForm(f => ({ ...f, currentPassword: '', newPassword: '' }));
      setSuccess('Profile updated.');
    } catch (err) {
      setError(apiError(err, 'Could not update your profile'));
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <div style={{ padding: '2rem', textAlign: 'center' }}>Loading profile…</div>;

  return (
    <div style={s.wrap}>
      <h2 style={s.h2}>My Profile</h2>
      <p style={s.sub}>Signed in as {role === 'ADMIN' ? 'an administrator' : 'a customer'}</p>

      {error && <p style={s.err}>{error}</p>}
      {success && <p style={s.ok}>{success}</p>}

      <form onSubmit={handleSubmit}>
        <label style={s.label} htmlFor="profile-email">Email (cannot be changed)</label>
        <input id="profile-email" style={s.readonly} value={email} readOnly disabled />

        <label style={s.label} htmlFor="profile-name">Full name</label>
        <input id="profile-name" style={s.input} value={form.name} required
          onChange={e => setForm({ ...form, name: e.target.value })} />

        <label style={s.label} htmlFor="profile-phone">Phone</label>
        <input id="profile-phone" style={s.input} value={form.phone}
          onChange={e => setForm({ ...form, phone: e.target.value })} />

        <hr style={s.divider} />
        <div style={s.legend}>Change password</div>
        <p style={s.sub}>Leave blank to keep your current password.</p>

        <label style={s.label} htmlFor="profile-current">Current password</label>
        <input id="profile-current" style={s.input} type="password" value={form.currentPassword}
          required={!!form.newPassword}
          onChange={e => setForm({ ...form, currentPassword: e.target.value })} />

        <label style={s.label} htmlFor="profile-new">New password (min 6 chars)</label>
        <input id="profile-new" style={s.input} type="password" value={form.newPassword} minLength={6}
          onChange={e => setForm({ ...form, newPassword: e.target.value })} />

        <button style={s.btn} type="submit" disabled={saving}>
          {saving ? 'Saving…' : 'Save changes'}
        </button>
      </form>
    </div>
  );
}
