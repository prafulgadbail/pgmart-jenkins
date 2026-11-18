import { useEffect, useMemo, useState, type ReactNode, type CSSProperties } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { BookCard, CartControl } from '../components/BookCard';
import { Cover } from '../components/Cover';
import { useWishlistToggle } from '../components/useWishlistToggle';
import { contentFor } from '../content';
import { discount, inr } from '../format';
import { useStore } from '../state/store';
import type { Book, BookContent } from '../types';

/** Up to 8 pages to flip through: title page, contents, about, chapters, key concepts, sample. */
function lookInside(b: Book, c: BookContent): ReactNode[] {
  const flow = c.flow ?? [];
  const pages: ReactNode[] = [
    <div className="tp" key="t">
      <small>PGMART PRESS</small>
      <h5 style={{ fontSize: 26, marginTop: 40 }}>{b.title}</h5>
      <p>{b.subtitle}</p>
    </div>,
    <div key="c">
      <h5>Contents</h5>
      <ol>{flow.length ? flow.map((f, i) => <li key={i}>{f.split('|')[0]}</li>) : <li>Introduction</li>}</ol>
    </div>,
    <div key="a">
      <h5>About this book</h5>
      <p>{c.intro ?? `${b.subtitle}.`}</p>
      <ul>{(c.why ?? b.highlights).map((w, i) => <li key={i}>{w}</li>)}</ul>
    </div>,
  ];
  for (let i = 0; i < flow.length; i += 3) {
    pages.push(
      <div key={`ch${i}`}>
        {flow.slice(i, i + 3).map((f, j) => {
          const [title, text] = f.split('|');
          return (
            <div key={j}>
              <h5 style={{ fontSize: 17 }}>Chapter {i + j + 1}: {title}</h5>
              <p>{text}</p>
            </div>
          );
        })}
      </div>,
    );
  }
  if (c.cc) {
    pages.push(
      <div key="cc">
        <h5>Key concepts</h5>
        {c.cc.map((x, i) => <p key={i}><b>{x.split('|')[0]}</b>: {x.split('|')[1]}</p>)}
      </div>,
    );
  }
  if (c.file) {
    pages.push(
      <div key="f">
        <h5 style={{ fontSize: 17 }}>{c.file.n}</h5>
        <pre>{c.file.c.split('\n').slice(0, 18).join('\n')}</pre>
      </div>,
    );
  } else if (c.code) {
    pages.push(
      <div key="s">
        <h5>Sample</h5>
        <pre dangerouslySetInnerHTML={{ __html: c.code }} />
      </div>,
    );
  }
  return pages.slice(0, 8);
}

export function BookPage() {
  const { id } = useParams();
  const { bookById, books, topics, ready, wishlist, addToCart, cart } = useStore();
  const toggleWish = useWishlistToggle();
  const navigate = useNavigate();
  const book = bookById(Number(id));
  const content = useMemo(() => contentFor(Number(id)), [id]);
  const pages = useMemo(() => (book ? lookInside(book, content) : []), [book, content]);
  const [page, setPage] = useState(0);
  const [zoom, setZoom] = useState({ x: '50%', y: '50%' });

  useEffect(() => {
    setPage(0);
  }, [id]);
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && navigate(-1);
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [navigate]);

  if (!book) {
    return ready ? (
      <div className="empty">
        This book is not available.<br />
        <Link className="btn p" to="/products" style={{ display: 'inline-block', marginTop: 12 }}>Back to products</Link>
      </div>
    ) : <div className="empty">Loading…</div>;
  }

  const topicName = topics.find((t) => t.key === book.topic)?.name ?? book.topic;
  const flow = content.flow ?? [];
  const related = books.filter((x) => x.topic === book.topic && x.id !== book.id).slice(0, 4);
  const saved = wishlist.has(book.id);

  return (
    <div id="p-book">
      <div className="crumb">
        <button className="btn" onClick={() => navigate(-1)}>← Back</button>
        <nav aria-label="Breadcrumb">
          <Link to="/">Home</Link> › <Link to="/products">Products</Link> ›{' '}
          <Link to={`/products?topic=${encodeURIComponent(book.topic)}`}>{topicName}</Link> › <b>{book.title}</b>
        </nav>
        <button className="xbtn" aria-label="Close" onClick={() => navigate(-1)}>✕</button>
      </div>

      <div className="pd">
        <div>
          <div className="zm" onMouseMove={(e) => {
            const r = e.currentTarget.getBoundingClientRect();
            setZoom({ x: `${((e.clientX - r.left) / r.width) * 100}%`, y: `${((e.clientY - r.top) / r.height) * 100}%` });
          }} style={{ '--zx': zoom.x, '--zy': zoom.y } as CSSProperties}>
            <div className="book"><Cover book={book} /></div>
          </div>
          <p className="mut" style={{ textAlign: 'center', fontSize: 13 }}>Move the mouse over the cover to zoom</p>
        </div>
        <div>
          <h1 style={{ fontSize: 34, margin: 0 }}>{book.title}</h1>
          <p className="mut" style={{ margin: '4px 0 10px' }}>{book.subtitle} · {topicName}{book.pages ? ` · ${book.pages} pages` : ''}</p>
          <div className="pr big">
            <b>{inr(book.price)}</b>
            {book.mrp > book.price && <s>{inr(book.mrp)}</s>}
            {discount(book) > 0 && <em>{discount(book)}% off</em>}
          </div>
          <ul className="of">
            {book.mrp > book.price && <li>Save {inr(book.mrp - book.price)} on this book today</li>}
            <li>Cash on Delivery available</li>
            <li>Free delivery on orders of ₹499 or more</li>
          </ul>
          <div className={book.stock <= 5 ? 'lw2' : 'ok2'}>
            {book.stock < 1 ? 'Out of stock' : book.stock <= 5 ? `Only ${book.stock} left` : 'In stock'}
          </div>
          <div className="bcr">
            <div style={{ minWidth: 170 }}><CartControl book={book} /></div>
            {book.stock > 0 && (
              <button className="btn" onClick={() => {
                if (!cart[book.id]) addToCart(book.id, 1);
                navigate('/checkout');
              }}>Buy now</button>
            )}
            <button className="btn" onClick={() => void toggleWish(book.id)}>{saved ? '♥ In wishlist' : '♡ Wishlist'}</button>
          </div>
        </div>
      </div>

      <h2>Look inside</h2>
      <div className="vw">
        <div className="paper" key={page}>
          {pages[page]}
          <span className="pn">{page + 1}</span>
        </div>
        <div className="pgc">
          <button className="btn" aria-label="Previous page" onClick={() => setPage((p) => Math.max(0, p - 1))}>‹</button>
          <span>Page {page + 1} of {pages.length}</span>
          <button className="btn" aria-label="Next page" onClick={() => setPage((p) => Math.min(pages.length - 1, p + 1))}>›</button>
        </div>
        <div className="vt">
          {pages.map((_, i) => (
            <button key={i} className={i === page ? 'on' : ''} onClick={() => setPage(i)}>{i + 1}</button>
          ))}
        </div>
      </div>

      <h2>About this book</h2>
      <p>{content.intro ?? `${book.subtitle}.`}</p>
      <ul className="why" style={{ '--c1': book.color1 } as CSSProperties}>
        {(content.why ?? book.highlights).map((w, i) => <li key={i}>{w}</li>)}
      </ul>

      {flow.length > 0 && (
        <>
          <h2>Table of contents</h2>
          {flow.map((f, i) => (
            <details key={i}>
              <summary>Chapter {i + 1}: {f.split('|')[0]}</summary>
              <p className="mut" style={{ margin: '8px 0 0' }}>{f.split('|')[1]}</p>
            </details>
          ))}
        </>
      )}

      {content.cc && (
        <>
          <h2>Key concepts</h2>
          <dl className="cc">
            {content.cc.map((c, i) => (
              <div key={i}><dt>{c.split('|')[0]}</dt><dd>{c.split('|')[1]}</dd></div>
            ))}
          </dl>
        </>
      )}

      {content.file ? (
        <>
          <h2>Sample file</h2>
          <div className="fh">{content.file.n}</div>
          <pre>{content.file.c}</pre>
          <dl className="cc">
            {content.file.k.map(([k, v], i) => <div key={i}><dt>{k}</dt><dd>{v}</dd></div>)}
          </dl>
        </>
      ) : content.code ? (
        <>
          <h2>Sample</h2>
          <pre dangerouslySetInnerHTML={{ __html: content.code }} />
        </>
      ) : null}

      <h2>Book details</h2>
      <table>
        <tbody>
          <tr><td>Topic</td><td>{topicName}</td></tr>
          {flow.length > 0 && <tr><td>Chapters</td><td>{flow.map((f) => f.split('|')[0]).join(', ')}</td></tr>}
          {book.pages > 0 && <tr><td>Pages</td><td>{book.pages}</td></tr>}
          <tr><td>Publisher</td><td>PGMart Press</td></tr>
          <tr><td>Payment</td><td>Cash on Delivery or online (card / UPI)</td></tr>
        </tbody>
      </table>

      {related.length > 0 && (
        <>
          <h2>More in {topicName}</h2>
          <div className="grid">{related.map((b) => <BookCard key={b.id} book={b} />)}</div>
        </>
      )}
    </div>
  );
}
