import type { Book, OrderStatus, PaymentStatus } from './types';

/** ₹799 from 79900 paise; Indian digit grouping (₹1,84,500). */
export function inr(paise: number): string {
  const rupees = paise / 100;
  return '₹' + rupees.toLocaleString('en-IN', { maximumFractionDigits: rupees % 1 ? 2 : 0, minimumFractionDigits: rupees % 1 ? 2 : 0 });
}

export function discount(b: Pick<Book, 'mrp' | 'price'>): number {
  return Math.round((1 - b.price / b.mrp) * 100);
}

export const STATUS_LABEL: Record<OrderStatus, string> = {
  PAYMENT_PENDING: 'Waiting for payment',
  PLACED: 'Placed',
  PACKED: 'Packed',
  OUT_FOR_DELIVERY: 'Out for delivery',
  DELIVERED: 'Delivered',
  CANCELLED: 'Cancelled',
};

export const PAYMENT_LABEL: Record<PaymentStatus, string> = {
  COD_DUE: 'to pay on delivery',
  PENDING: 'Payment pending',
  PAID: 'Paid',
  REFUND_PENDING: 'Refund in progress',
  REFUNDED: 'Refunded',
  NOT_PAID: 'Not paid',
};

/** The four steps of the tracking bar. */
export const TRACK_STEPS: OrderStatus[] = ['PLACED', 'PACKED', 'OUT_FOR_DELIVERY', 'DELIVERED'];

export function trackStep(status: OrderStatus): number {
  return TRACK_STEPS.indexOf(status);
}

export function timeAgo(iso: string, now = Date.now()): string {
  const s = Math.max(0, Math.round((now - new Date(iso).getTime()) / 1000));
  if (s < 60) return 'just now';
  if (s < 3600) return `${Math.floor(s / 60)} min ago`;
  if (s < 86400) return `${Math.floor(s / 3600)} h ago`;
  return new Date(iso).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' });
}

export const DELIVERY_FREE_FROM = 499_00;
export const DELIVERY_FEE = 40_00;

export function deliveryFor(itemsTotal: number): number {
  return itemsTotal >= DELIVERY_FREE_FROM ? 0 : DELIVERY_FEE;
}
