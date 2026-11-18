import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, ApiError } from '../api';
import { OrderTracker } from '../components/OrderTracker';
import { inr, PAYMENT_LABEL, STATUS_LABEL } from '../format';
import { payWithRazorpay } from '../razorpay';
import { useStore } from '../state/store';
import type { Order } from '../types';

const CAN_CANCEL = ['PAYMENT_PENDING', 'PLACED', 'PACKED'];

export function Orders() {
  const { user, toast, reloadBooks, refreshNotifications } = useStore();
  const [orders, setOrders] = useState<Order[] | null>(null);
  const [busy, setBusy] = useState<string | null>(null);
  const [confirmCancel, setConfirmCancel] = useState<string | null>(null);

  const load = useCallback(() => {
    api.orders().then(setOrders).catch((e) => toast(e instanceof ApiError ? e.message : 'Could not load orders'));
  }, [toast]);

  // Status changes made by the store show up without reloading the page
  useEffect(() => {
    load();
    const t = window.setInterval(load, 15_000);
    return () => window.clearInterval(t);
  }, [load]);

  async function cancel(o: Order) {
    if (confirmCancel !== o.id) {
      setConfirmCancel(o.id);
      return;
    }
    setBusy(o.id);
    try {
      await api.cancelOrder(o.id);
      toast(`Order ${o.number} cancelled`);
      void reloadBooks();
      refreshNotifications();
      load();
    } catch (e) {
      toast(e instanceof ApiError ? e.message : 'Could not cancel');
    } finally {
      setBusy(null);
      setConfirmCancel(null);
    }
  }

  async function pay(o: Order) {
    setBusy(o.id);
    try {
      const start = await api.paymentFor(o.id);
      const r = await payWithRazorpay(start, o.number, user!, o.address.mobile);
      if (r.paid) {
        await api.confirmPayment(o.id, {
          razorpayOrderId: r.response.razorpay_order_id,
          razorpayPaymentId: r.response.razorpay_payment_id,
          razorpaySignature: r.response.razorpay_signature,
        });
        toast(`Payment received for ${o.number}`);
        refreshNotifications();
      } else {
        toast(r.reason);
      }
      load();
    } catch (e) {
      toast(e instanceof Error ? e.message : 'Payment failed');
    } finally {
      setBusy(null);
    }
  }

  return (
    <>
      <h1 style={{ fontSize: 42 }}>Your orders</h1>
      {orders === null && <div className="empty">Loading…</div>}
      {orders?.length === 0 && (
        <div className="empty">
          No orders yet. Add books to your cart and place an order.<br />
          <Link className="btn p" to="/products" style={{ display: 'inline-block', marginTop: 12 }}>Browse books</Link>
        </div>
      )}
      {orders?.map((o) => (
        <div className="ord" key={o.id}>
          <div className="row" style={{ flexWrap: 'wrap' }}>
            <b>Order {o.number} <span className={`badge s-${o.status}`}>{STATUS_LABEL[o.status]}</span></b>
            <span style={{ color: 'var(--a)', fontWeight: 700 }}>
              {inr(o.total)} · {o.paymentMethod === 'COD' ? 'Cash on Delivery' : 'Online'} · {PAYMENT_LABEL[o.paymentStatus]}
            </span>
          </div>
          <p className="mut" style={{ margin: '6px 0 0' }}>
            {o.items.map((i) => `${i.title} × ${i.qty}`).join(', ')} · placed {new Date(o.createdAt).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })}
          </p>
          <OrderTracker status={o.status} cod={o.paymentMethod === 'COD'} />
          <details style={{ marginTop: 12 }}>
            <summary>Details</summary>
            <p className="mut" style={{ margin: '8px 0' }}>
              Deliver to {o.address.name}, {o.address.line1}, {o.address.city}, {o.address.state} - {o.address.pincode} · {o.address.mobile}
            </p>
            <table>
              <tbody>
                {o.items.map((i) => <tr key={i.bookId}><td>{i.title} × {i.qty}</td><td>{inr(i.price * i.qty)}</td></tr>)}
                <tr><td>Delivery</td><td>{o.delivery ? inr(o.delivery) : 'Free'}</td></tr>
                <tr><td><b>Total</b></td><td><b>{inr(o.total)}</b></td></tr>
              </tbody>
            </table>
            <ol className="timeline">
              {o.events.map((e, i) => (
                <li key={i}><b>{STATUS_LABEL[e.status]}</b> · {e.note} <span className="mut">{new Date(e.at).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })}</span></li>
              ))}
            </ol>
          </details>
          <div className="row" style={{ justifyContent: 'flex-start', gap: 10, marginTop: 10 }}>
            {o.status === 'PAYMENT_PENDING' && (
              <button className="btn p" disabled={busy === o.id} onClick={() => pay(o)}>Pay {inr(o.total)} now</button>
            )}
            {CAN_CANCEL.includes(o.status) && (
              <button className="btn" disabled={busy === o.id} onClick={() => cancel(o)}>
                {confirmCancel === o.id ? 'Yes, cancel this order' : 'Cancel order'}
              </button>
            )}
          </div>
        </div>
      ))}
    </>
  );
}
