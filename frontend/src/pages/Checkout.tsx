import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api, ApiError } from '../api';
import { Thumb } from '../components/Cover';
import { OrderConfirmed } from '../components/OrderConfirmed';
import { deliveryFor, inr } from '../format';
import { payWithRazorpay } from '../razorpay';
import { useStore } from '../state/store';
import type { Address } from '../types';

const EMPTY = { fullName: '', mobile: '', line1: '', city: '', state: '', pincode: '' };

export function Checkout() {
  const { user, cart, bookById, clearCart, reloadBooks, refreshNotifications, toast } = useStore();
  const navigate = useNavigate();
  const [addresses, setAddresses] = useState<Address[]>([]);
  const [chosen, setChosen] = useState<number | 'new'>('new');
  const [form, setForm] = useState({ ...EMPTY, fullName: user?.name ?? '' });
  const [save, setSave] = useState(true);
  const [online, setOnline] = useState(false);
  const [method, setMethod] = useState<'COD' | 'RAZORPAY'>('COD');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState<string | null>(null);
  const attempt = useRef<string>(crypto.randomUUID());

  useEffect(() => {
    api.addresses().then((a) => {
      setAddresses(a);
      if (a.length) setChosen(a[0].id);
    }).catch(() => {});
    api.paymentOptions().then((o) => setOnline(o.online)).catch(() => {});
  }, []);

  const ids = Object.keys(cart).map(Number).filter((id) => bookById(id));
  if (!ids.length && !done) {
    return (
      <div className="empty">
        Your cart is empty.<br />
        <Link className="btn p" to="/products" style={{ display: 'inline-block', marginTop: 12 }}>Browse books</Link>
      </div>
    );
  }

  let mrp = 0;
  let items = 0;
  ids.forEach((id) => {
    const b = bookById(id)!;
    mrp += b.mrp * cart[id];
    items += b.price * cart[id];
  });
  const delivery = deliveryFor(items);

  const field = (key: keyof typeof EMPTY, placeholder: string, extra: Record<string, unknown> = {}) => (
    <input className={`in2${key === 'line1' ? ' w2' : ''}`} placeholder={placeholder} value={form[key]}
      onChange={(e) => setForm({ ...form, [key]: e.target.value })} {...extra} />
  );

  function addressToUse() {
    if (chosen !== 'new') {
      const a = addresses.find((x) => x.id === chosen)!;
      return { name: a.fullName, mobile: a.mobile, line1: a.line1, city: a.city, state: a.state, pincode: a.pincode };
    }
    const f = Object.fromEntries(Object.entries(form).map(([k, v]) => [k, v.trim()])) as typeof EMPTY;
    if (!f.fullName || !f.line1 || !f.city || !f.state) throw new Error('Please fill the full address');
    if (!/^\d{10}$/.test(f.mobile)) throw new Error('Enter a 10 digit mobile number');
    if (!/^\d{6}$/.test(f.pincode)) throw new Error('Enter a 6 digit pincode');
    return { name: f.fullName, mobile: f.mobile, line1: f.line1, city: f.city, state: f.state, pincode: f.pincode };
  }

  async function placeOrder() {
    setError(null);
    let address;
    try {
      address = addressToUse();
    } catch (e) {
      setError((e as Error).message);
      return;
    }
    setBusy(true);
    try {
      if (chosen === 'new' && save) {
        await api.addAddress({ fullName: address.name, mobile: address.mobile, line1: address.line1, city: address.city,
          state: address.state, pincode: address.pincode, isDefault: addresses.length === 0 }).catch(() => {});
      }
      const placed = await api.placeOrder(
        { items: ids.map((id) => ({ bookId: id, qty: cart[id] })), address, paymentMethod: method },
        attempt.current,
      );
      // The order exists and its stock is reserved: the cart is done either way
      clearCart();
      attempt.current = crypto.randomUUID();
      void reloadBooks();

      if (!placed.payment) {
        setDone(`Order ${placed.order.number} · ${inr(placed.order.total)} · Pay cash when it arrives.`);
        refreshNotifications();
        return;
      }
      const result = await payWithRazorpay(placed.payment, placed.order.number, user!, address.mobile);
      if (!result.paid) {
        toast(`${result.reason}. You can pay from Your orders within 30 minutes.`);
        navigate('/orders');
        return;
      }
      const paid = await api.confirmPayment(placed.order.id, {
        razorpayOrderId: result.response.razorpay_order_id,
        razorpayPaymentId: result.response.razorpay_payment_id,
        razorpaySignature: result.response.razorpay_signature,
      });
      setDone(`Order ${paid.number} · ${inr(paid.total)} · Paid online.`);
      refreshNotifications();
    } catch (e) {
      setError(e instanceof ApiError || e instanceof Error ? e.message : 'Could not place the order');
      void reloadBooks();
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <div className="crumb">
        <button className="btn" onClick={() => navigate('/products')}>← Continue shopping</button>
        <b>Checkout</b>
      </div>
      <div className="co">
        <div>
          <h3>Delivery address</h3>
          {addresses.map((a) => (
            <label className={`rad${chosen === a.id ? '' : ' off'}`} key={a.id} style={{ marginBottom: 8 }}>
              <input type="radio" checked={chosen === a.id} onChange={() => setChosen(a.id)} />
              <span><b>{a.fullName}</b>, {a.line1}, {a.city}, {a.state} - {a.pincode} · {a.mobile}</span>
            </label>
          ))}
          {addresses.length > 0 && (
            <label className={`rad${chosen === 'new' ? '' : ' off'}`}>
              <input type="radio" checked={chosen === 'new'} onChange={() => setChosen('new')} /> Deliver to a new address
            </label>
          )}
          {chosen === 'new' && (
            <>
              <div className="fg">
                {field('fullName', 'Full name', { autoComplete: 'name' })}
                {field('mobile', 'Mobile number (10 digits)', { inputMode: 'numeric', autoComplete: 'tel' })}
                {field('line1', 'House no, street, area', { autoComplete: 'street-address' })}
                {field('city', 'City', { autoComplete: 'address-level2' })}
                {field('state', 'State', { autoComplete: 'address-level1' })}
                {field('pincode', 'Pincode (6 digits)', { inputMode: 'numeric', autoComplete: 'postal-code' })}
              </div>
              <label className="mut" style={{ display: 'flex', gap: 8, marginTop: -8 }}>
                <input type="checkbox" checked={save} onChange={(e) => setSave(e.target.checked)} /> Save this address for next time
              </label>
            </>
          )}

          <h3>Payment</h3>
          <label className={`rad${method === 'COD' ? '' : ' off'}`} style={{ marginBottom: 8 }}>
            <input type="radio" checked={method === 'COD'} onChange={() => setMethod('COD')} />
            Cash on Delivery <span className="mut">Pay when your order arrives</span>
          </label>
          <label className={`rad${method === 'RAZORPAY' ? '' : ' off'}${online ? '' : ' disabled'}`}>
            <input type="radio" disabled={!online} checked={method === 'RAZORPAY'} onChange={() => setMethod('RAZORPAY')} />
            Card / UPI / Netbanking <span className="mut">{online ? 'Razorpay' : 'Not available right now'}</span>
          </label>
        </div>

        <div className="sum">
          <h3 style={{ marginTop: 0 }}>Order summary</h3>
          {ids.map((id) => {
            const b = bookById(id)!;
            return (
              <div className="ci" key={id}>
                <Thumb book={b} />
                <div><b>{b.title}</b><div className="mut">Qty {cart[id]} · {inr(b.price * cart[id])}</div></div>
              </div>
            );
          })}
          <div className="row"><span className="mut">MRP total</span><s className="mut">{inr(mrp)}</s></div>
          <div className="row"><span style={{ color: 'var(--ok)' }}>You save</span><b style={{ color: 'var(--ok)' }}>{inr(mrp - items)}</b></div>
          <div className="row"><span className="mut">Delivery</span><b>{delivery ? inr(delivery) : 'Free'}</b></div>
          <div className="row"><b>Total</b><b style={{ color: 'var(--a)' }}>{inr(items + delivery)}</b></div>
          {error && <p className="err">{error}</p>}
          <button className="btn p" style={{ width: '100%', marginTop: 12 }} disabled={busy} onClick={placeOrder}>
            {busy ? 'Placing order…' : method === 'COD' ? 'Place order (Cash on Delivery)' : `Pay ${inr(items + delivery)}`}
          </button>
          <p className="mut" style={{ fontSize: 13 }}>Prices and stock are checked again when you place the order.</p>
        </div>
      </div>
      {done && (
        <OrderConfirmed text={done} onTrack={() => navigate('/orders')} onShop={() => navigate('/products')} />
      )}
    </>
  );
}
