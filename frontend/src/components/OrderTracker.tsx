import { useEffect, useState } from 'react';
import { STATUS_LABEL, TRACK_STEPS, trackStep } from '../format';
import type { OrderStatus } from '../types';

// Road from the store to the customer, drawn as two cubic curves
type P = [number, number];
const P0: P = [40, 130], C1: P = [160, 130], C2: P = [180, 50], P1: P = [300, 70], C3: P = [420, 90], C4: P = [470, 130], P2: P = [590, 60];
const cubic = (a: P, b: P, c: P, d: P, t: number): P =>
  [0, 1].map((i) => (1 - t) ** 3 * a[i] + 3 * (1 - t) ** 2 * t * b[i] + 3 * (1 - t) * t * t * c[i] + t ** 3 * d[i]) as P;
const along = (f: number): P => (f < 0.5 ? cubic(P0, C1, C2, P1, f * 2) : cubic(P1, C3, C4, P2, (f - 0.5) * 2));
const ROAD = 'M40 130C160 130 180 50 300 70S470 130 590 60';

function Scooter() {
  return (
    <g transform="translate(0,-16)">
      <g className="dust"><line x1="-26" y1="4" x2="-36" y2="4" /><line x1="-26" y1="9" x2="-40" y2="9" /><line x1="-26" y1="14" x2="-34" y2="14" /></g>
      <rect x="-24" y="-20" width="17" height="14" rx="2" fill="#F97316" /><rect x="-24" y="-15" width="17" height="3" fill="#FED7AA" />
      <path d="M-14 10L-8 -2H8L12 4L14 10" fill="none" stroke="#38BDF8" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" />
      <rect x="-9" y="-9" width="15" height="6" rx="3" fill="#38BDF8" />
      <path d="M11 3L9 -11M5 -12H13" stroke="#E2E8F0" strokeWidth="2.4" strokeLinecap="round" fill="none" />
      <path d="M-3 -9L-2 -19" stroke="#FB923C" strokeWidth="6" strokeLinecap="round" /><path d="M-2 -17L8 -11" stroke="#FB923C" strokeWidth="3" strokeLinecap="round" />
      <circle cx="-1" cy="-26" r="5" fill="#FBBF24" /><path d="M-6 -27a5 5 0 0 1 10 0z" fill="#EF4444" />
      <g className="wh"><circle cx="-14" cy="10" r="6.5" fill="#0F172A" stroke="#E2E8F0" strokeWidth="2" /><line x1="-14" y1="4" x2="-14" y2="16" stroke="#E2E8F0" /></g>
      <g className="wh"><circle cx="14" cy="10" r="6.5" fill="#0F172A" stroke="#E2E8F0" strokeWidth="2" /><line x1="14" y1="4" x2="14" y2="16" stroke="#E2E8F0" /></g>
    </g>
  );
}

const ETA: Partial<Record<OrderStatus, string>> = {
  PAYMENT_PENDING: 'Waiting for your payment.',
  PLACED: 'Order received. The store is preparing it.',
  PACKED: 'Packed. A rider is being assigned.',
  OUT_FOR_DELIVERY: 'Out for delivery. The rider is on the way.',
  DELIVERED: 'Delivered. Enjoy your books!',
  CANCELLED: 'This order was cancelled.',
};

/** Delivery map: the rider moves while the order is out for delivery. */
export function OrderTracker({ status, cod }: { status: OrderStatus; cod: boolean }) {
  const moving = status === 'OUT_FOR_DELIVERY';
  const [f, setF] = useState(0);
  useEffect(() => {
    if (!moving) return;
    let start = performance.now();
    let raf = 0;
    const tick = (now: number) => {
      const t = (now - start) / 14000;
      if (t >= 1) start = now;
      setF(0.05 + Math.min(t, 1) * 0.85);
      raf = requestAnimationFrame(tick);
    };
    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [moving]);

  const pos = status === 'DELIVERED' ? 1 : moving ? f : 0;
  const [x, y] = along(pos);
  const step = trackStep(status);
  return (
    <>
      {status !== 'CANCELLED' && (
        <svg className="trk" viewBox="0 0 640 170" role="img" aria-label="Delivery route">
          <path d={ROAD} fill="none" stroke="#334155" strokeWidth="12" strokeLinecap="round" />
          <path d={ROAD} fill="none" stroke="#94A3B8" strokeWidth="2" strokeDasharray="8 10" />
          <g transform="translate(40,130)"><rect x="-22" y="-34" width="44" height="30" rx="4" fill="#475569" /><path d="M-26 -34L0 -50L26 -34z" fill="#38BDF8" /><rect x="-6" y="-22" width="12" height="18" fill="#0F172A" /></g>
          <text x="40" y="158" textAnchor="middle" className="tl">PGMart</text>
          <g transform="translate(590,60)"><path d="M-20 -4V-30L0 -46L20 -30V-4z" fill="#FB923C" /><rect x="-5" y="-20" width="10" height="16" fill="#0F172A" /></g>
          <text x="590" y="86" textAnchor="middle" className="tl">You</text>
          <g className={`sc${moving ? ' mv' : ''}`} style={{ transform: `translate(${x}px,${y}px)` }}><Scooter /></g>
        </svg>
      )}
      <div className="eta">{ETA[status]}{status === 'OUT_FOR_DELIVERY' && cod ? ' Please keep the cash ready.' : ''}</div>
      {status !== 'CANCELLED' && status !== 'PAYMENT_PENDING' && (
        <div className="steps">
          {TRACK_STEPS.map((s, i) => <div key={s} className={`stp${i <= step ? ' on' : ''}`}>{STATUS_LABEL[s]}</div>)}
        </div>
      )}
    </>
  );
}
