import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { inr } from '../format';
import { useStore } from '../state/store';
import { Thumb } from './Cover';

/** Ctrl+K search over title, subtitle, topic and tool. */
export function SearchPalette() {
  const { paletteOpen, setPaletteOpen, books } = useStore();
  const [q, setQ] = useState('');
  const input = useRef<HTMLInputElement>(null);
  const navigate = useNavigate();

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault();
        setPaletteOpen(true);
      }
      if (e.key === 'Escape') setPaletteOpen(false);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [setPaletteOpen]);

  useEffect(() => {
    if (paletteOpen) {
      setQ('');
      window.setTimeout(() => input.current?.focus(), 10);
    }
  }, [paletteOpen]);

  const t = q.trim().toLowerCase();
  const hits = books.filter((b) => `${b.title} ${b.subtitle} ${b.topic} ${b.logo}`.toLowerCase().includes(t)).slice(0, 12);

  return (
    <div className={`ov${paletteOpen ? ' show' : ''}`} onClick={(e) => e.target === e.currentTarget && setPaletteOpen(false)}>
      <div className="pal">
        <input ref={input} value={q} onChange={(e) => setQ(e.target.value)} placeholder="Search books or tools..." autoComplete="off" />
        <div>
          {hits.map((b) => (
            <button key={b.id} onClick={() => { setPaletteOpen(false); navigate(`/book/${b.id}`); }}>
              <Thumb book={b} />
              <span>{b.title} <small className="mut">{inr(b.price)}</small></span>
            </button>
          ))}
          {hits.length === 0 && <p className="mut" style={{ padding: 10 }}>No match. Try a tool name like docker.</p>}
        </div>
      </div>
    </div>
  );
}
