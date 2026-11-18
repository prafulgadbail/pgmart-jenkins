import { Link, useNavigate } from 'react-router-dom';
import { BookCard } from '../components/BookCard';
import { Cover, coverColors } from '../components/Cover';
import { Logo } from '../components/Logo';
import { discount } from '../format';
import { useStore } from '../state/store';

export function Home() {
  const { books, topics } = useStore();
  const navigate = useNavigate();
  const shelf = [9, 8, 4].map((id) => books.find((b) => b.id === id)).filter((b) => b !== undefined);
  const deals = [...books].filter((b) => b.stock > 0).sort((a, b) => discount(b) - discount(a)).slice(0, 4);
  return (
    <>
      <section className="hero">
        <div>
          <h1>Every DevOps tool, in one book.</h1>
          <p>Cloud, CI/CD, containers, monitoring and automation. Open any book to flip through its pages before you buy.</p>
          <Link className="btn p" to="/products" style={{ display: 'inline-block' }}>Browse products</Link>
        </div>
        <div className="shelf">
          {(shelf.length ? shelf : books.slice(0, 3)).map((b) => (
            <div className="book" key={b.id} onClick={() => navigate(`/book/${b.id}`)} style={{ cursor: 'pointer' }}>
              <Cover book={b} />
            </div>
          ))}
        </div>
      </section>

      <h2>Shop by topic</h2>
      <div className="tiles">
        {topics.filter((t) => t.books > 0).map((t) => {
          const logos = books.filter((b) => b.topic === t.key).slice(0, 3);
          return (
            <button className="tile" key={t.key} onClick={() => navigate(`/products?topic=${encodeURIComponent(t.key)}`)}>
              <div className="ic">
                {logos.map((b) => (
                  <span key={b.id} style={coverColors(b)}><Logo name={b.logo} /></span>
                ))}
              </div>
              <b>{t.name}</b>
              <small>{t.books} books</small>
            </button>
          );
        })}
      </div>

      <h2>Biggest discounts today</h2>
      <div className="grid">
        {deals.map((b) => <BookCard key={b.id} book={b} />)}
      </div>
    </>
  );
}
