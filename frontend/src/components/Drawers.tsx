import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { inr, timeAgo } from '../format';
import { useStore } from '../state/store';
import { Thumb } from './Cover';

const icon = (title: string) =>
  /delivered/i.test(title) ? '✅' : /out for delivery/i.test(title) ? '🛵' : /packed/i.test(title) ? '📦'
    : /cancel/i.test(title) ? '✖️' : /placed/i.test(title) ? '🧾' : '🔔';

export function Drawers() {
  const { drawer, setDrawer, cart, bookById, addToCart, removeFromCart, toggleWishlist, user,
    notifications, clearNotifications, toast } = useStore();
  const navigate = useNavigate();

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && setDrawer(null);
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [setDrawer]);

  const ids = Object.keys(cart).map(Number).filter((id) => bookById(id));
  let mrp = 0;
  let total = 0;
  ids.forEach((id) => {
    const b = bookById(id)!;
    mrp += b.mrp * cart[id];
    total += b.price * cart[id];
  });

  return (
    <>
      <div className={`ov${drawer ? ' show' : ''}`} style={{ zIndex: 45 }} onClick={() => setDrawer(null)} />
      <aside className={`drawer${drawer ? ' show' : ''}`} aria-hidden={!drawer}>
        {drawer === 'cart' && (
          <>
            <div className="row"><b>Your cart</b><button className="btn" onClick={() => setDrawer(null)}>Close</button></div>
            <div style={{ flex: 1, overflow: 'auto' }}>
              {ids.length === 0 && <div className="empty">Your cart is empty. Add a book to start.</div>}
              {ids.map((id) => {
                const b = bookById(id)!;
                return (
                  <div className="ci" key={id}>
                    <Thumb book={b} />
                    <div>
                      <b>{b.title}</b>
                      <div className="pr"><b>{inr(b.price * cart[id])}</b>{b.mrp > b.price && <s>{inr(b.mrp * cart[id])}</s>}</div>
                      <div className="row" style={{ justifyContent: 'flex-start', gap: 14, marginTop: 6 }}>
                        <div className="qty" style={{ width: 110 }}>
                          <button onClick={() => addToCart(id, -1)}>−</button><b>{cart[id]}</b><button onClick={() => addToCart(id, 1)}>+</button>
                        </div>
                        {user && (
                          <button className="lnk" onClick={async () => {
                            removeFromCart(id);
                            await toggleWishlist(id);
                            toast(`Saved ${b.title} for later`);
                          }}>Save for later</button>
                        )}
                        <button className="lnk r" onClick={() => removeFromCart(id)}>Delete</button>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
            {ids.length > 0 && (
              <>
                <div className="row"><span className="mut">Price (MRP)</span><s className="mut">{inr(mrp)}</s></div>
                <div className="row"><span style={{ color: 'var(--ok)' }}>You save</span><b style={{ color: 'var(--ok)' }}>{inr(mrp - total)}</b></div>
                <div className="row"><b>Total</b><b style={{ color: 'var(--a)' }}>{inr(total)}</b></div>
                <button className="btn p" onClick={() => { setDrawer(null); navigate('/checkout'); }}>Proceed to checkout</button>
              </>
            )}
          </>
        )}
        {drawer === 'notes' && (
          <>
            <div className="row">
              <b>Notifications</b>
              <span>
                {notifications.length > 0 && <button className="lnk" onClick={clearNotifications}>Clear all</button>}{' '}
                <button className="btn" onClick={() => setDrawer(null)}>Close</button>
              </span>
            </div>
            <div style={{ flex: 1, overflow: 'auto' }}>
              {notifications.length === 0 && <div className="empty">No notifications yet. Order updates show up here.</div>}
              {notifications.map((n) => (
                <div className={`nt${n.read ? '' : ' nw'}`} key={n.id} style={{ cursor: n.orderId ? 'pointer' : undefined }}
                  onClick={() => { if (n.orderId) { setDrawer(null); navigate('/orders'); } }}>
                  <span className="ni">{icon(n.title)}</span>
                  <div>
                    <div><b>{n.title}</b></div>
                    <div className="mut" style={{ fontSize: 14 }}>{n.body}</div>
                    <small className="mut">{timeAgo(n.createdAt)}</small>
                  </div>
                </div>
              ))}
            </div>
          </>
        )}
      </aside>
    </>
  );
}
