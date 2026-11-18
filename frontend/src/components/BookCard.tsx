import { useNavigate } from 'react-router-dom';
import { contentFor } from '../content';
import { discount, inr } from '../format';
import { useStore } from '../state/store';
import type { Book } from '../types';
import { Cover } from './Cover';
import { useWishlistToggle } from './useWishlistToggle';

/** Add to cart button, or − qty + once the book is in the cart. */
export function CartControl({ book }: { book: Book }) {
  const { cart, addToCart } = useStore();
  const q = cart[book.id] ?? 0;
  if (book.stock < 1) return <button className="btn" disabled>Out of stock</button>;
  if (!q)
    return (
      <button className="btn p" onClick={() => addToCart(book.id, 1)}>
        Add to cart
      </button>
    );
  return (
    <div className="qty">
      <button onClick={() => addToCart(book.id, -1)} aria-label="Remove one">{q === 1 ? '🗑' : '−'}</button>
      <b>{q}</b>
      <button onClick={() => addToCart(book.id, 1)} aria-label="Add one">+</button>
    </div>
  );
}

/** Card with the cover that opens on hover to show the chapters inside. */
export function BookCard({ book }: { book: Book }) {
  const navigate = useNavigate();
  const { wishlist } = useStore();
  const toggleWish = useWishlistToggle();
  const flow = contentFor(book.id).flow ?? book.highlights.map((h) => `${h}|`);
  const saved = wishlist.has(book.id);
  const open = () => navigate(`/book/${book.id}`);
  return (
    <article className="cd">
      <div className="cw" onClick={open}>
        <div className="book">
          <div className="in">
            <b>{book.title}</b>
            <span className="m2">Inside this book</span>
            {flow.slice(0, 6).map((f, i) => (
              <span className="ch2" key={i}>
                {i + 1}. {f.split('|')[0]}
              </span>
            ))}
          </div>
          <div className="ocv">
            <Cover book={book} />
            <div className="back" />
          </div>
        </div>
        {discount(book) > 0 && <span className="bd">{discount(book)}% off</span>}
        <button
          className={`hr${saved ? ' on' : ''}`}
          aria-label={`${saved ? 'Remove' : 'Save'} ${book.title} ${saved ? 'from' : 'to'} wishlist`}
          onClick={(e) => {
            e.stopPropagation();
            void toggleWish(book.id);
          }}
        >
          {saved ? '♥' : '♡'}
        </button>
        {book.stock > 0 && book.stock <= 5 && <span className="lw">Only {book.stock} left</span>}
      </div>
      <h4 onClick={open}>{book.title}</h4>
      <div className="pr">
        <b>{inr(book.price)}</b>
        {book.mrp > book.price && <s>{inr(book.mrp)}</s>}
        {discount(book) > 0 && <em>{discount(book)}% off</em>}
      </div>
      <CartControl book={book} />
    </article>
  );
}
