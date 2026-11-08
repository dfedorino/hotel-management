// Calls to the system through the gateway (same origin). The JWT is kept in sessionStorage for the browser tab.

const TOKEN_KEY = 'hotel-booking.token';

export class ApiError extends Error {
  constructor(status, message, traceId) {
    super(message);
    this.status = status;
    this.traceId = traceId;
  }
}

export const session = {
  get token() {
    return sessionStorage.getItem(TOKEN_KEY);
  },
  set token(value) {
    if (value) sessionStorage.setItem(TOKEN_KEY, value);
    else sessionStorage.removeItem(TOKEN_KEY);
  },
  /** Claims of the token: sub (username), uid, roles, exp. */
  get claims() {
    const token = this.token;
    if (!token) return null;
    try {
      const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      const claims = JSON.parse(atob(payload));
      return claims.exp * 1000 > Date.now() ? claims : null;
    } catch {
      return null;
    }
  },
  hasRole(role) {
    return this.claims?.roles?.includes(role) ?? false;
  },
};

async function request(method, path, { body, query } = {}) {
  const url = new URL(path, window.location.origin);
  Object.entries(query ?? {})
    .filter(([, value]) => value !== undefined && value !== null && value !== '')
    .forEach(([key, value]) => url.searchParams.set(key, value));
  const headers = { Accept: 'application/json' };
  if (body) headers['Content-Type'] = 'application/json';
  if (session.token) headers.Authorization = `Bearer ${session.token}`;

  const response = await fetch(url, { method, headers, body: body ? JSON.stringify(body) : undefined });
  if (response.status === 204) return null;
  const data = await response.json().catch(() => null);
  if (!response.ok) {
    throw new ApiError(response.status, data?.message ?? response.statusText, response.headers.get('X-Trace-Id'));
  }
  return data;
}

export const api = {
  login: (credentials) => request('POST', '/api/bookings/users/auth', { body: credentials }),
  register: (credentials) => request('POST', '/api/bookings/users/register', { body: credentials }),
  hotels: () => request('GET', '/api/hotels'),
  room: (id) => request('GET', `/api/rooms/${id}`),
  recommendations: (query) => request('GET', '/api/bookings/rooms/recommend', { query }),
  book: (booking) => request('POST', '/api/bookings', { body: booking }),
  bookings: (page, size) => request('GET', '/api/bookings', { query: { page, size } }),
  cancel: (id) => request('DELETE', `/api/bookings/${id}`),
  occupancy: (query) => request('GET', '/api/rooms/occupancy', { query }),
};
