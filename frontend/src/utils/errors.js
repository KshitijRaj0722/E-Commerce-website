// The backend's GlobalExceptionHandler always returns { timestamp, status, error, message },
// so prefer that message and only fall back when the request never reached the server.
export function apiError(err, fallback = 'Something went wrong. Please try again.') {
  return err?.response?.data?.message || err?.message || fallback;
}
