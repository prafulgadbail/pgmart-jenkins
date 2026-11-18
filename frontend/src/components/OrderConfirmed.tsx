import { useMemo } from 'react';

const COLORS = ['#38BDF8', '#FB923C', '#34D399', '#FBBF24', '#FB7185', '#F97316'];

/** Big tick and confetti after a successful order. */
export function OrderConfirmed({ text, onTrack, onShop }: { text: string; onTrack: () => void; onShop: () => void }) {
  const confetti = useMemo(
    () =>
      Array.from({ length: 80 }, (_, i) => ({
        x: Math.random() * 100,
        d: Math.random() * 0.9,
        t: 2.2 + Math.random() * 1.6,
        c: COLORS[i % COLORS.length],
        r: Math.round(Math.random() * 720 - 360),
      })),
    [],
  );
  return (
    <div className="ov show" style={{ zIndex: 70 }}>
      <div className="cf">
        {confetti.map((p, i) => (
          <i key={i} style={{ ['--x' as string]: `${p.x}vw`, ['--d' as string]: `${p.d}s`, ['--t' as string]: `${p.t}s`, ['--c' as string]: p.c, ['--r' as string]: `${p.r}deg` }} />
        ))}
      </div>
      <div className="okb">
        <svg className="ck" viewBox="0 0 52 52">
          <circle cx="26" cy="26" r="24" fill="none" stroke="#34D399" strokeWidth="3" />
          <path d="M14 27l8 8 16-17" fill="none" stroke="#34D399" strokeWidth="4" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
        <h2>Order confirmed!</h2>
        <p className="mut">{text}</p>
        <div className="row" style={{ justifyContent: 'center', gap: 10 }}>
          <button className="btn p" onClick={onTrack}>Track order</button>
          <button className="btn" onClick={onShop}>Keep shopping</button>
        </div>
      </div>
    </div>
  );
}
