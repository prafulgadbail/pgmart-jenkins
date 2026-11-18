import { useSearchParams } from 'react-router-dom';
import { BookCard } from '../components/BookCard';
import { useStore } from '../state/store';

export function Products() {
  const { books, topics } = useStore();
  const [params, setParams] = useSearchParams();
  const topic = params.get('topic') ?? 'All';
  const q = params.get('q') ?? '';

  const set = (key: string, value: string) => {
    const next = new URLSearchParams(params);
    if (value && value !== 'All') next.set(key, value);
    else next.delete(key);
    setParams(next, { replace: true });
  };

  const t = q.trim().toLowerCase();
  const match = (b: (typeof books)[number]) =>
    (topic === 'All' || b.topic === topic) && `${b.title} ${b.subtitle} ${b.topic} ${b.logo}`.toLowerCase().includes(t);
  const sections = topics
    .map((tp) => ({ tp, list: books.filter((b) => b.topic === tp.key && match(b)) }))
    .filter((s) => s.list.length);

  return (
    <>
      <h1 style={{ fontSize: 42 }}>Products</h1>
      <input className="srch" value={q} onChange={(e) => set('q', e.target.value)} placeholder="Search any book: docker, jenkins, aws..." />
      <div className="chips">
        {['All', ...topics.map((x) => x.key)].map((k) => (
          <button key={k} className={`chip${k === topic ? ' on' : ''}`} onClick={() => set('topic', k)}>{k}</button>
        ))}
      </div>
      {sections.map(({ tp, list }) => (
        <section key={tp.key}>
          <h2>{tp.name}</h2>
          <p className="mut" style={{ margin: 0 }}>{tp.description}</p>
          <div className="grid">
            {list.map((b) => <BookCard key={b.id} book={b} />)}
          </div>
        </section>
      ))}
      {sections.length === 0 && <div className="empty">No book matches “{q}”. Try a tool name like docker.</div>}
    </>
  );
}
