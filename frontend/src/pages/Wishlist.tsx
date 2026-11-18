import { Link } from 'react-router-dom';
import { BookCard } from '../components/BookCard';
import { useStore } from '../state/store';

export function Wishlist() {
  const { user, wishlist, bookById } = useStore();
  const list = [...wishlist].map((id) => bookById(id)).filter((b) => b !== undefined);
  return (
    <>
      <h1 style={{ fontSize: 42 }}>Wishlist</h1>
      <p className="mut">Books you saved to buy later.</p>
      {!user ? (
        <div className="empty">
          Log in to see your wishlist.<br />
          <Link className="btn p" to="/login?next=/wishlist" style={{ display: 'inline-block', marginTop: 12 }}>Login</Link>
        </div>
      ) : (
        <div className="grid">
          {list.map((b) => <BookCard key={b.id} book={b} />)}
          {list.length === 0 && (
            <div className="empty" style={{ gridColumn: '1/-1' }}>Your wishlist is empty. Tap the heart on a book to save it.</div>
          )}
        </div>
      )}
    </>
  );
}
