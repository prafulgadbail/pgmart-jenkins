import type { CSSProperties } from 'react';
import type { Book } from '../types';
import { Logo } from './Logo';

type CoverBook = Pick<Book, 'title' | 'subtitle' | 'logo' | 'color1' | 'color2' | 'imageUrl'>;

const colors = (b: Pick<Book, 'color1' | 'color2'>) => ({ '--c1': b.color1, '--c2': b.color2 }) as CSSProperties;

/** The book cover is drawn from data: two colours, the tool logo and the title. */
export function Cover({ book }: { book: CoverBook }) {
  return (
    <div className="cv" style={colors(book)}>
      <small>PGMART · CLOUD &amp; DEVOPS</small>
      <div className="lg">
        <Logo name={book.logo} />
      </div>
      <b>{book.title}</b>
      <i>{book.subtitle}</i>
      <u>PGMart Press</u>
      {book.imageUrl && (
        <img className="cimg" src={book.imageUrl} alt="" onError={(e) => e.currentTarget.remove()} />
      )}
    </div>
  );
}

/** Small cover for lists (cart, search, admin table). */
export function Thumb({ book }: { book: Pick<Book, 'logo' | 'color1' | 'color2'> }) {
  return (
    <div className="th" style={colors(book)}>
      <Logo name={book.logo} />
    </div>
  );
}

export { colors as coverColors };
