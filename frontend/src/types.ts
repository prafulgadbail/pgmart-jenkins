// Shapes returned by the three backend services. Money is always in paise (₹1 = 100 paise).

export interface Topic {
  key: string;
  name: string;
  description: string;
  books: number;
}

export interface Book {
  id: number;
  title: string;
  subtitle: string;
  topic: string;
  logo: string;
  color1: string;
  color2: string;
  mrp: number;
  price: number;
  stock: number;
  pages: number;
  highlights: string[];
  imageUrl: string | null;
  active: boolean;
}

export type Role = 'CUSTOMER' | 'ADMIN';

export interface User {
  id: number;
  name: string;
  email: string;
  phone: string | null;
  role: Role;
}

export interface Session {
  token: string;
  user: User;
}

export interface Address {
  id: number;
  fullName: string;
  mobile: string;
  line1: string;
  city: string;
  state: string;
  pincode: string;
  isDefault: boolean;
}

export type OrderStatus = 'PAYMENT_PENDING' | 'PLACED' | 'PACKED' | 'OUT_FOR_DELIVERY' | 'DELIVERED' | 'CANCELLED';
export type PaymentStatus = 'COD_DUE' | 'PENDING' | 'PAID' | 'REFUND_PENDING' | 'REFUNDED' | 'NOT_PAID';

export interface OrderItem {
  bookId: number;
  title: string;
  qty: number;
  price: number;
  mrp: number;
}

export interface OrderEvent {
  status: OrderStatus;
  note: string;
  at: string;
}

export interface Order {
  id: string;
  number: string;
  userId: number;
  customerName: string;
  customerEmail: string;
  status: OrderStatus;
  paymentMethod: 'COD' | 'RAZORPAY';
  paymentStatus: PaymentStatus;
  itemsTotal: number;
  mrpTotal: number;
  delivery: number;
  total: number;
  address: { name: string; mobile: string; line1: string; city: string; state: string; pincode: string };
  createdAt: string;
  items: OrderItem[];
  events: OrderEvent[];
}

export interface PaymentStart {
  keyId: string;
  razorpayOrderId: string;
  amount: number;
}

export interface Placed {
  order: Order;
  payment: PaymentStart | null;
}

export interface Notification {
  id: number;
  orderId: string | null;
  title: string;
  body: string;
  read: boolean;
  createdAt: string;
}

/** Rich "look inside" content shipped with the frontend (src/content/books.json), keyed by book id. */
export interface BookContent {
  why?: string[];
  flow?: string[];
  code?: string;
  intro?: string;
  cc?: string[];
  file?: { n: string; c: string; k: [string, string][] };
}
