import type { PaymentStart, User } from './types';

/** Razorpay Checkout (the popup with card / UPI / netbanking). Loaded only when someone pays online. */
interface RazorpayResponse {
  razorpay_order_id: string;
  razorpay_payment_id: string;
  razorpay_signature: string;
}

interface RazorpayInstance {
  open(): void;
  on(event: 'payment.failed', handler: (r: { error: { description: string } }) => void): void;
}

declare global {
  interface Window {
    Razorpay?: new (options: Record<string, unknown>) => RazorpayInstance;
  }
}

function loadScript(): Promise<void> {
  if (window.Razorpay) return Promise.resolve();
  return new Promise((resolve, reject) => {
    const s = document.createElement('script');
    s.src = 'https://checkout.razorpay.com/v1/checkout.js';
    s.onload = () => resolve();
    s.onerror = () => reject(new Error('Could not load Razorpay. Check your connection.'));
    document.body.appendChild(s);
  });
}

export type PayResult = { paid: true; response: RazorpayResponse } | { paid: false; reason: string };

export async function payWithRazorpay(p: PaymentStart, orderNumber: string, user: User, mobile?: string): Promise<PayResult> {
  await loadScript();
  return new Promise((resolve) => {
    const rzp = new window.Razorpay!({
      key: p.keyId,
      order_id: p.razorpayOrderId,
      amount: p.amount,
      currency: 'INR',
      name: 'PGMart',
      description: `Order ${orderNumber}`,
      prefill: { name: user.name, email: user.email, contact: mobile ?? '' },
      theme: { color: '#F97316' },
      handler: (response: RazorpayResponse) => resolve({ paid: true, response }),
      modal: { ondismiss: () => resolve({ paid: false, reason: 'Payment window closed' }) },
    });
    rzp.on('payment.failed', (r) => resolve({ paid: false, reason: r.error.description }));
    rzp.open();
  });
}
