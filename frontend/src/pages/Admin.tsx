import { useCallback, useEffect, useState } from 'react';
import { api, ApiError } from '../api';
import { Cover, Thumb } from '../components/Cover';
import { LOGO_KEYS } from '../content';
import { inr, PAYMENT_LABEL, STATUS_LABEL } from '../format';
import { useStore } from '../state/store';
import type { Book, Order, OrderStatus } from '../types';

/** What the admin can do next with an order (same rules as order-service). */
const NEXT: Partial<Record<OrderStatus, [OrderStatus, string][]>> = {
  PLACED: [['PACKED', 'Mark packed'], ['CANCELLED', 'Cancel']],
  PACKED: [['OUT_FOR_DELIVERY', 'Hand to rider'], ['CANCELLED', 'Cancel']],
  OUT_FOR_DELIVERY: [['DELIVERED', 'Mark delivered']],
};

const FILTERS: (OrderStatus | '')[] = ['', 'PLACED', 'PACKED', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED', 'PAYMENT_PENDING'];

interface BookForm {
  title: string; subtitle: string; topic: string; logo: string; color1: string; color2: string;
  mrp: string; price: string; stock: string; pages: string; highlights: string; imageUrl: string;
}

const blank = (topic: string): BookForm => ({
  title: '', subtitle: '', topic, logo: 'k8s', color1: '#326CE5', color2: '#12206b',
  mrp: '', price: '', stock: '', pages: '', highlights: '', imageUrl: '',
});

const fromBook = (b: Book): BookForm => ({
  title: b.title, subtitle: b.subtitle, topic: b.topic, logo: b.logo, color1: b.color1, color2: b.color2,
  mrp: String(b.mrp / 100), price: String(b.price / 100), stock: String(b.stock), pages: String(b.pages),
  highlights: b.highlights.join('\n'), imageUrl: b.imageUrl ?? '',
});

export function Admin() {
  const { topics, toast, reloadBooks } = useStore();
  const [tab, setTab] = useState<'orders' | 'books'>('orders');
  const [orders, setOrders] = useState<Order[]>([]);
  const [filter, setFilter] = useState<OrderStatus | ''>('');
  const [books, setBooks] = useState<Book[]>([]);
  const [stats, setStats] = useState({ books: 0, lowStock: 0, orders: 0, revenue: 0, byStatus: {} as Record<string, number> });
  const [form, setForm] = useState<BookForm | null>(null);
  const [editing, setEditing] = useState<number | null>(null);
  const [stockEdits, setStockEdits] = useState<Record<number, string>>({});
  const [busy, setBusy] = useState(false);

  const fail = useCallback((e: unknown) => toast(e instanceof ApiError ? e.message : 'Something went wrong'), [toast]);

  const load = useCallback(async () => {
    try {
      const [o, b, bs, os] = await Promise.all([api.adminOrders(filter || undefined), api.adminBooks(), api.adminBookStats(), api.adminOrderStats()]);
      setOrders(o);
      setBooks(b);
      setStats({ ...bs, ...os });
    } catch (e) {
      fail(e);
    }
  }, [filter, fail]);

  useEffect(() => {
    void load();
    const t = window.setInterval(load, 20_000);
    return () => window.clearInterval(t);
  }, [load]);

  async function move(o: Order, status: OrderStatus) {
    if (status === 'CANCELLED' && !window.confirm(`Cancel order ${o.number}? Stock goes back and an online payment is refunded.`)) return;
    try {
      await api.adminMoveOrder(o.id, status);
      toast(`${o.number}: ${STATUS_LABEL[status]}. The customer gets an email.`);
      await load();
      if (status === 'CANCELLED') void reloadBooks();
    } catch (e) {
      fail(e);
    }
  }

  async function saveBook() {
    if (!form) return;
    const body = {
      title: form.title, subtitle: form.subtitle || form.title, topic: form.topic, logo: form.logo,
      color1: form.color1, color2: form.color2,
      mrp: Math.round(Number(form.mrp) * 100), price: Math.round(Number(form.price) * 100),
      stock: Number(form.stock), pages: Number(form.pages || 0),
      highlights: form.highlights.split('\n').map((h) => h.trim()).filter(Boolean),
      imageUrl: form.imageUrl.trim(), active: editing ? books.find((b) => b.id === editing)?.active ?? true : true,
    };
    if (!body.title || !(body.mrp > 0) || !(body.price > 0) || form.stock === '') return toast('Fill name, MRP, price and stock');
    if (body.price > body.mrp) return toast('Selling price cannot be above the MRP');
    setBusy(true);
    try {
      if (editing) await api.adminUpdateBook(editing, body);
      else await api.adminCreateBook(body);
      toast(editing ? 'Book updated' : 'Book added. It is in the store now.');
      setForm(null);
      setEditing(null);
      await load();
      void reloadBooks();
    } catch (e) {
      fail(e);
    } finally {
      setBusy(false);
    }
  }

  async function setStock(b: Book) {
    const v = Number(stockEdits[b.id]);
    if (!(v >= 0)) return;
    try {
      await api.adminSetStock(b.id, v);
      toast(`Stock of ${b.title} is now ${v}`);
      setStockEdits((s) => ({ ...s, [b.id]: '' }));
      await load();
      void reloadBooks();
    } catch (e) {
      fail(e);
    }
  }

  async function toggleActive(b: Book) {
    try {
      await api.adminSetActive(b.id, !b.active);
      toast(b.active ? `${b.title} is hidden from the store` : `${b.title} is back in the store`);
      await load();
      void reloadBooks();
    } catch (e) {
      fail(e);
    }
  }

  const f = form;
  const set = (k: keyof BookForm, v: string) => setForm((x) => (x ? { ...x, [k]: v } : x));

  return (
    <>
      <h1 style={{ fontSize: 36, marginTop: 24 }}>Dashboard</h1>
      <div className="stats">
        <div><b>{stats.books}</b><span>Books in the store</span></div>
        <div><b>{stats.orders}</b><span>Orders</span></div>
        <div><b>{inr(stats.revenue)}</b><span>Order value (not cancelled)</span></div>
        <div><b>{stats.byStatus.PLACED ?? 0}</b><span>To pack</span></div>
        <div><b>{stats.lowStock}</b><span>Low stock (5 or fewer)</span></div>
      </div>
      <div className="tabs">
        <button className={`tab${tab === 'orders' ? ' on' : ''}`} onClick={() => setTab('orders')}>Orders</button>
        <button className={`tab${tab === 'books' ? ' on' : ''}`} onClick={() => setTab('books')}>Books and stock</button>
      </div>

      {tab === 'orders' && (
        <>
          <div className="chips">
            {FILTERS.map((s) => (
              <button key={s || 'all'} className={`chip${filter === s ? ' on' : ''}`} onClick={() => setFilter(s)}>
                {s ? STATUS_LABEL[s] : 'All'}{s && stats.byStatus[s] ? ` (${stats.byStatus[s]})` : ''}
              </button>
            ))}
          </div>
          <div className="tw">
            <table className="at">
              <thead>
                <tr><th>Order</th><th>Customer</th><th>Items</th><th>Total</th><th>Deliver to</th><th>Status</th><th>Next step</th></tr>
              </thead>
              <tbody>
                {orders.map((o) => (
                  <tr key={o.id}>
                    <td><b>{o.number}</b><br /><small className="mut">{new Date(o.createdAt).toLocaleString('en-IN', { dateStyle: 'short', timeStyle: 'short' })}</small></td>
                    <td>{o.customerName}<br /><small className="mut">{o.customerEmail}</small></td>
                    <td>{o.items.map((i) => `${i.title} × ${i.qty}`).join(', ')}</td>
                    <td>{inr(o.total)}<br /><small className="mut">{o.paymentMethod === 'COD' ? 'COD' : 'Online'} · {PAYMENT_LABEL[o.paymentStatus]}</small></td>
                    <td><small>{o.address.name}, {o.address.line1}, {o.address.city}, {o.address.state} {o.address.pincode} · {o.address.mobile}</small></td>
                    <td><span className={`badge s-${o.status}`}>{STATUS_LABEL[o.status]}</span></td>
                    <td>
                      <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                        {(NEXT[o.status] ?? []).map(([s, label]) => (
                          <button key={s} className={`btn${s === 'CANCELLED' ? '' : ' p'}`} style={{ padding: '5px 10px', fontSize: 13 }} onClick={() => move(o, s)}>{label}</button>
                        ))}
                      </div>
                    </td>
                  </tr>
                ))}
                {orders.length === 0 && <tr><td colSpan={7} className="mut">No orders here yet.</td></tr>}
              </tbody>
            </table>
          </div>
        </>
      )}

      {tab === 'books' && (
        <>
          <div className="row" style={{ margin: '12px 0' }}>
            <b>{books.length} books ({books.filter((b) => !b.active).length} hidden)</b>
            {!f && <button className="btn p" onClick={() => { setEditing(null); setForm(blank(topics[0]?.key ?? 'CI/CD')); }}>+ Add book</button>}
          </div>

          {f && (
            <div className="ord admin-form">
              <div>
                <h3 style={{ marginTop: 0 }}>{editing ? 'Edit book' : 'Add book'}</h3>
                <div className="fg">
                  <input className="in2" placeholder="Book name" value={f.title} onChange={(e) => set('title', e.target.value)} />
                  <input className="in2" placeholder="Subtitle" value={f.subtitle} onChange={(e) => set('subtitle', e.target.value)} />
                  <select className="in2" value={f.topic} onChange={(e) => set('topic', e.target.value)}>
                    {topics.map((t) => <option key={t.key} value={t.key}>{t.name}</option>)}
                  </select>
                  <select className="in2" value={f.logo} onChange={(e) => set('logo', e.target.value)} aria-label="Tool logo">
                    {LOGO_KEYS.map((k) => <option key={k} value={k}>Logo: {k}</option>)}
                  </select>
                  <label className="mut">Cover colours <input type="color" value={f.color1} onChange={(e) => set('color1', e.target.value)} /> <input type="color" value={f.color2} onChange={(e) => set('color2', e.target.value)} /></label>
                  <input className="in2" placeholder="Image URL (optional, https://)" value={f.imageUrl} onChange={(e) => set('imageUrl', e.target.value)} />
                  <input className="in2" type="number" min="1" placeholder="MRP ₹" value={f.mrp} onChange={(e) => set('mrp', e.target.value)} />
                  <input className="in2" type="number" min="1" placeholder="Selling price ₹" value={f.price} onChange={(e) => set('price', e.target.value)} />
                  <input className="in2" type="number" min="0" placeholder="Stock" value={f.stock} onChange={(e) => set('stock', e.target.value)} />
                  <input className="in2" type="number" min="0" placeholder="Pages" value={f.pages} onChange={(e) => set('pages', e.target.value)} />
                  <textarea className="in2 w2" rows={3} placeholder="Why buy this book? One point per line" value={f.highlights} onChange={(e) => set('highlights', e.target.value)} />
                </div>
                <div className="row" style={{ justifyContent: 'flex-start', gap: 10 }}>
                  <button className="btn p" disabled={busy} onClick={saveBook}>{busy ? 'Saving…' : 'Save'}</button>
                  <button className="btn" onClick={() => { setForm(null); setEditing(null); }}>Cancel</button>
                </div>
              </div>
              <div>
                <p className="mut" style={{ marginTop: 0, fontSize: 13 }}>Cover preview (made from the fields, nothing to upload)</p>
                <div className="book" style={{ maxWidth: 220 }}>
                  <Cover book={{ title: f.title || 'Book name', subtitle: f.subtitle, logo: f.logo, color1: f.color1, color2: f.color2, imageUrl: f.imageUrl || null }} />
                </div>
              </div>
            </div>
          )}

          <div className="tw">
            <table className="at">
              <thead>
                <tr><th></th><th>Book</th><th>Topic</th><th>MRP</th><th>Price</th><th>Stock</th><th></th></tr>
              </thead>
              <tbody>
                {books.map((b) => (
                  <tr key={b.id} style={{ opacity: b.active ? 1 : 0.5 }}>
                    <td><Thumb book={b} /></td>
                    <td>{b.title}{!b.active && <small className="mut"> (hidden)</small>}</td>
                    <td>{b.topic}</td>
                    <td>{inr(b.mrp)}</td>
                    <td>{inr(b.price)}</td>
                    <td style={{ whiteSpace: 'nowrap' }}>
                      <input className="in2 sm" type="number" min="0" placeholder={String(b.stock)} value={stockEdits[b.id] ?? ''}
                        onChange={(e) => setStockEdits((s) => ({ ...s, [b.id]: e.target.value }))} />{' '}
                      <button className="lnk" onClick={() => setStock(b)}>Update</button>
                      {b.stock <= 5 && <small className="lw2" style={{ display: 'block', margin: 0 }}>{b.stock === 0 ? 'Out of stock' : 'Low'}</small>}
                    </td>
                    <td style={{ whiteSpace: 'nowrap' }}>
                      <button className="lnk" onClick={() => { setEditing(b.id); setForm(fromBook(b)); window.scrollTo({ top: 0, behavior: 'smooth' }); }}>Edit</button>{' '}
                      <button className={`lnk${b.active ? ' r' : ''}`} onClick={() => toggleActive(b)}>{b.active ? 'Hide' : 'Show'}</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </>
  );
}
