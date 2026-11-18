import { NavLink, Link, useNavigate } from 'react-router-dom';
import { useStore } from '../state/store';

function InfinityLogo() {
  return (
    <svg className="inf" viewBox="0 0 64 32" aria-hidden="true">
      <defs>
        <linearGradient id="ig" x1="0" x2="1">
          <stop offset="0" stopColor="#38BDF8" />
          <stop offset="1" stopColor="#F97316" />
        </linearGradient>
      </defs>
      <path id="ip" d="M32 16c-4-6-8-9-13-9C12.4 7 8 11 8 16s4.4 9 11 9c5 0 9-3 13-9s8-9 13-9c6.6 0 11 4 11 9s-4.4 9-11 9c-5 0-9-3-13-9z"
        fill="none" stroke="url(#ig)" strokeWidth="4.5" strokeLinecap="round" />
      <circle r="2.6" fill="#fff">
        <animateMotion dur="3.2s" repeatCount="indefinite">
          <mpath href="#ip" />
        </animateMotion>
      </circle>
    </svg>
  );
}

export function Header() {
  const { user, signOut, cartCount, unread, setDrawer, openNotifications, setPaletteOpen, toast } = useStore();
  const navigate = useNavigate();
  const cls = ({ isActive }: { isActive: boolean }) => (isActive ? 'on' : '');
  return (
    <header>
      <div className="wrap">
        <Link className="logo" to="/">
          <InfinityLogo />
          <span className="bn">
            <span>PG<em>Mart</em></span>
            <small className="bs">Cloud &amp; DevOps Book Store</small>
          </span>
        </Link>
        <nav className="nav">
          <NavLink to="/" end className={cls}>Home</NavLink>
          <NavLink to="/products" className={cls}>Products</NavLink>
          <NavLink to="/wishlist" className={cls}>Wishlist</NavLink>
          <NavLink to="/orders" className={cls}>Orders</NavLink>
        </nav>
        <button className="btn" id="sb" aria-label="Search" onClick={() => setPaletteOpen(true)}>
          Search <span className="kh"><kbd>Ctrl</kbd> <kbd>K</kbd></span>
        </button>
        {user && (
          <button className="btn" aria-label="Notifications" onClick={openNotifications}>
            🔔{unread > 0 && <span className="dot">{unread}</span>}
          </button>
        )}
        <button className="btn p" id="cb" onClick={() => setDrawer('cart')}>
          Cart <span>{cartCount}</span>
        </button>
        {user ? (
          <>
            <Link className="btn" id="ub" to="/orders" title={user.email}>{user.name.split(' ')[0]}</Link>
            <button
              className="btn"
              id="lo"
              onClick={() => {
                signOut();
                toast('Logged out');
                navigate('/');
              }}
            >
              Logout
            </button>
          </>
        ) : (
          <Link className="btn" id="ub" to="/login">Login</Link>
        )}
      </div>
    </header>
  );
}

/** Header for the admin account: brand, who is signed in, logout. */
export function AdminHeader() {
  const { user, signOut, toast } = useStore();
  const navigate = useNavigate();
  return (
    <header>
      <div className="wrap">
        <Link className="logo" to="/admin">
          <InfinityLogo />
          <span className="bn">
            <span>PG<em>Mart</em></span>
            <small className="bs">Admin console</small>
          </span>
        </Link>
        <span className="mut" style={{ marginLeft: 'auto', fontSize: 14 }}>{user?.email}</span>
        <button
          className="btn"
          id="lo"
          onClick={() => {
            navigate('/login');
            signOut();
            toast('Logged out');
          }}
        >
          Logout
        </button>
      </div>
    </header>
  );
}

export function Footer() {
  return (
    <footer>
      <div className="wrap" style={{ marginTop: 6 }}>
        PG<b>Mart</b> · Cloud &amp; DevOps Book Store · built and operated by <b>Praful Gadbail</b> ·{' '}
        <a href="/" style={{ textDecoration: 'underline' }}>How PGMart is built</a>
      </div>
    </footer>
  );
}
