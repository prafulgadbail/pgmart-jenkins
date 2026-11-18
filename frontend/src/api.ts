import type { Address, Book, Notification, Order, PaymentStart, Placed, Session, Topic, User } from './types';

/** Error from a service: `code` is stable for the UI, `message` can be shown to the shopper as is. */
export class ApiError extends Error {
  constructor(public status: number, public code: string, message: string) {
    super(message);
  }
}

const TOKEN_KEY = 'pgmart.token';

export function getToken(): string | null {
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

export function setToken(token: string | null) {
  try {
    if (token) localStorage.setItem(TOKEN_KEY, token);
    else localStorage.removeItem(TOKEN_KEY);
  } catch {
    // private mode: the session lasts until the tab closes
  }
}

let onUnauthorized: () => void = () => {};
export function whenLoggedOut(handler: () => void) {
  onUnauthorized = handler;
}

async function call<T>(method: string, path: string, body?: unknown, headers: Record<string, string> = {}): Promise<T> {
  const token = getToken();
  let res: Response;
  try {
    res = await fetch(path, {
      method,
      headers: {
        ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...headers,
      },
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(0, 'NETWORK', 'Cannot reach the store. Check your connection and try again.');
  }
  if (res.status === 204) return undefined as T;
  const data = await res.json().catch(() => null);
  if (!res.ok) {
    if (res.status === 401 && token) onUnauthorized();
    throw new ApiError(res.status, data?.code ?? 'ERROR', data?.message ?? `Request failed (${res.status})`);
  }
  return data as T;
}

export const api = {
  // user-service
  register: (name: string, email: string, password: string) =>
    call<{ email: string; message: string }>('POST', '/api/auth/register', { name, email, password }),
  verify: (email: string, code: string) => call<Session>('POST', '/api/auth/verify', { email, code }),
  resendCode: (email: string) => call<{ message: string }>('POST', '/api/auth/resend-code', { email }),
  login: (email: string, password: string) => call<Session>('POST', '/api/auth/login', { email, password }),
  forgotPassword: (email: string) => call<{ message: string }>('POST', '/api/auth/forgot-password', { email }),
  resetPassword: (email: string, code: string, newPassword: string) =>
    call<Session>('POST', '/api/auth/reset-password', { email, code, newPassword }),
  me: () => call<User>('GET', '/api/users/me'),
  addresses: () => call<Address[]>('GET', '/api/users/me/addresses'),
  addAddress: (a: Omit<Address, 'id'>) => call<Address>('POST', '/api/users/me/addresses', a),
  deleteAddress: (id: number) => call<void>('DELETE', `/api/users/me/addresses/${id}`),

  // catalog-service
  topics: () => call<Topic[]>('GET', '/api/topics'),
  books: () => call<Book[]>('GET', '/api/books'),
  book: (id: number) => call<Book>('GET', `/api/books/${id}`),
  wishlist: () => call<number[]>('GET', '/api/wishlist'),
  addToWishlist: (id: number) => call<void>('PUT', `/api/wishlist/${id}`),
  removeFromWishlist: (id: number) => call<void>('DELETE', `/api/wishlist/${id}`),

  // order-service
  paymentOptions: () => call<{ cod: boolean; online: boolean }>('GET', '/api/payments/options'),
  placeOrder: (body: unknown, idempotencyKey: string) =>
    call<Placed>('POST', '/api/orders', body, { 'Idempotency-Key': idempotencyKey }),
  orders: () => call<Order[]>('GET', '/api/orders'),
  cancelOrder: (id: string) => call<Order>('POST', `/api/orders/${id}/cancel`),
  paymentFor: (id: string) => call<PaymentStart>('GET', `/api/orders/${id}/payment`),
  confirmPayment: (id: string, r: { razorpayOrderId: string; razorpayPaymentId: string; razorpaySignature: string }) =>
    call<Order>('POST', `/api/orders/${id}/payment`, r),
  notifications: () => call<Notification[]>('GET', '/api/notifications'),
  markNotificationsRead: () => call<void>('POST', '/api/notifications/read'),
  clearNotifications: () => call<void>('DELETE', '/api/notifications'),

  // admin
  adminBooks: () => call<Book[]>('GET', '/api/admin/books'),
  adminBookStats: () => call<{ books: number; lowStock: number }>('GET', '/api/admin/books/stats'),
  adminCreateBook: (b: unknown) => call<Book>('POST', '/api/admin/books', b),
  adminUpdateBook: (id: number, b: unknown) => call<Book>('PUT', `/api/admin/books/${id}`, b),
  adminSetStock: (id: number, stock: number) => call<Book>('PATCH', `/api/admin/books/${id}/stock`, { stock }),
  adminSetActive: (id: number, active: boolean) => call<Book>('PATCH', `/api/admin/books/${id}/active`, { active }),
  adminOrders: (status?: string) => call<Order[]>('GET', `/api/admin/orders${status ? `?status=${status}` : ''}`),
  adminOrderStats: () => call<{ orders: number; revenue: number; byStatus: Record<string, number> }>('GET', '/api/admin/orders/stats'),
  adminMoveOrder: (id: string, status: string) => call<Order>('PATCH', `/api/admin/orders/${id}/status`, { status }),
};
