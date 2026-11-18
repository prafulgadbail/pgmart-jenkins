import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { api, ApiError, getToken, setToken, whenLoggedOut } from '../api';
import type { Book, Notification, Session, Topic, User } from '../types';

type Drawer = 'cart' | 'notes' | null;
export type Cart = Record<number, number>;

interface Store {
  user: User | null;
  ready: boolean;
  signIn: (s: Session) => void;
  signOut: () => void;

  books: Book[];
  topics: Topic[];
  catalogError: string | null;
  bookById: (id: number) => Book | undefined;
  reloadBooks: () => Promise<void>;

  cart: Cart;
  cartCount: number;
  addToCart: (id: number, delta: number) => void;
  removeFromCart: (id: number) => void;
  clearCart: () => void;

  wishlist: Set<number>;
  toggleWishlist: (id: number) => Promise<boolean>;

  notifications: Notification[];
  unread: number;
  refreshNotifications: () => void;
  openNotifications: () => void;
  clearNotifications: () => void;

  drawer: Drawer;
  setDrawer: (d: Drawer) => void;
  paletteOpen: boolean;
  setPaletteOpen: (o: boolean) => void;

  toast: (message: string) => void;
  toastMessage: string | null;
}

const StoreContext = createContext<Store | null>(null);

const CART_KEY = 'pgmart.cart';

function readCart(): Cart {
  try {
    const raw = localStorage.getItem(CART_KEY);
    return raw ? (JSON.parse(raw) as Cart) : {};
  } catch {
    return {};
  }
}

/** Reads the user id from the JWT without checking it (the services check it). */
function userFromToken(token: string | null): User | null {
  if (!token) return null;
  try {
    const p = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
    if (p.exp * 1000 < Date.now()) return null;
    return { id: Number(p.sub), email: p.email, name: p.name, role: p.role, phone: null };
  } catch {
    return null;
  }
}

export function StoreProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(() => userFromToken(getToken()));
  const [ready, setReady] = useState(false);
  const [books, setBooks] = useState<Book[]>([]);
  const [topics, setTopics] = useState<Topic[]>([]);
  const [catalogError, setCatalogError] = useState<string | null>(null);
  const [cart, setCart] = useState<Cart>(readCart);
  const [wishlist, setWishlist] = useState<Set<number>>(new Set());
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [drawer, setDrawer] = useState<Drawer>(null);
  const [paletteOpen, setPaletteOpen] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const toastTimer = useRef<number | undefined>(undefined);

  const toast = useCallback((message: string) => {
    setToastMessage(message);
    window.clearTimeout(toastTimer.current);
    toastTimer.current = window.setTimeout(() => setToastMessage(null), 2800);
  }, []);

  const signOut = useCallback(() => {
    setToken(null);
    setUser(null);
    setWishlist(new Set());
    setNotifications([]);
  }, []);

  const signIn = useCallback((s: Session) => {
    setToken(s.token);
    setUser(s.user);
  }, []);

  useEffect(() => {
    whenLoggedOut(() => {
      signOut();
      toast('Your session ended. Please log in again.');
    });
  }, [signOut, toast]);

  const reloadBooks = useCallback(async () => {
    try {
      const [b, t] = await Promise.all([api.books(), api.topics()]);
      setBooks(b);
      setTopics(t);
      setCatalogError(null);
    } catch (e) {
      setCatalogError(e instanceof ApiError ? e.message : 'Could not load books');
    } finally {
      setReady(true);
    }
  }, []);

  useEffect(() => {
    void reloadBooks();
  }, [reloadBooks]);

  const byId = useMemo(() => new Map(books.map((b) => [b.id, b])), [books]);
  const bookById = useCallback((id: number) => byId.get(id), [byId]);

  // ---------- cart (kept in the browser until checkout, like most stores do for guests) ----------
  useEffect(() => {
    try {
      localStorage.setItem(CART_KEY, JSON.stringify(cart));
    } catch {
      // ignore
    }
  }, [cart]);

  // Books that were hidden or ran out of stock disappear from the cart
  useEffect(() => {
    if (!books.length) return;
    setCart((c) => {
      const next: Cart = {};
      for (const [id, q] of Object.entries(c)) {
        const b = byId.get(Number(id));
        if (b && b.stock > 0) next[Number(id)] = Math.min(q, b.stock, 10);
      }
      return next;
    });
  }, [books, byId]);

  const addToCart = useCallback(
    (id: number, delta: number) => {
      const b = byId.get(id);
      if (!b) return;
      setCart((c) => {
        const q = (c[id] ?? 0) + delta;
        const next = { ...c };
        if (q <= 0) {
          delete next[id];
        } else if (q > Math.min(b.stock, 10)) {
          toast(b.stock <= 10 ? `Only ${b.stock} in stock` : 'You can order up to 10 copies');
          return c;
        } else {
          next[id] = q;
        }
        return next;
      });
      if (delta > 0 && (cart[id] ?? 0) + delta <= Math.min(b.stock, 10)) toast(`Added ${b.title} to cart`);
    },
    [byId, cart, toast],
  );

  const removeFromCart = useCallback((id: number) => {
    setCart((c) => {
      const next = { ...c };
      delete next[id];
      return next;
    });
  }, []);

  const clearCart = useCallback(() => setCart({}), []);
  const cartCount = useMemo(() => Object.values(cart).reduce((a, b) => a + b, 0), [cart]);

  // ---------- wishlist (saved on the server, needs login) ----------
  useEffect(() => {
    if (!user || user.role === "ADMIN") return;
    api.wishlist().then((ids) => setWishlist(new Set(ids))).catch(() => {});
  }, [user]);

  const toggleWishlist = useCallback(
    async (id: number) => {
      if (!user) return false;
      const had = wishlist.has(id);
      setWishlist((w) => {
        const n = new Set(w);
        if (had) n.delete(id);
        else n.add(id);
        return n;
      });
      try {
        if (had) await api.removeFromWishlist(id);
        else await api.addToWishlist(id);
        toast(had ? 'Removed from wishlist' : 'Saved to wishlist');
      } catch (e) {
        setWishlist((w) => {
          const n = new Set(w);
          if (had) n.add(id);
          else n.delete(id);
          return n;
        });
        toast(e instanceof ApiError ? e.message : 'Could not update the wishlist');
      }
      return true;
    },
    [user, wishlist, toast],
  );

  // ---------- notifications (order updates from order-service, checked every 20 s) ----------
  const seen = useRef<number>(0);
  const refreshNotifications = useCallback(() => {
    if (!getToken()) return;
    api
      .notifications()
      .then((n) => {
        const newest = n[0]?.id ?? 0;
        if (seen.current && newest > seen.current && !n[0].read) toast(n[0].title);
        seen.current = newest;
        setNotifications(n);
      })
      .catch(() => {});
  }, [toast]);

  useEffect(() => {
    if (!user || user.role === "ADMIN") return;
    seen.current = 0;
    refreshNotifications();
    const t = window.setInterval(refreshNotifications, 20_000);
    return () => window.clearInterval(t);
  }, [user, refreshNotifications]);

  const unread = notifications.filter((n) => !n.read).length;

  const openNotifications = useCallback(() => {
    setDrawer('notes');
    if (unread) {
      api.markNotificationsRead().catch(() => {});
      window.setTimeout(() => setNotifications((n) => n.map((x) => ({ ...x, read: true }))), 1500);
    }
  }, [unread]);

  const clearNotifications = useCallback(() => {
    setNotifications([]);
    api.clearNotifications().catch(() => {});
  }, []);

  const value: Store = {
    user, ready, signIn, signOut,
    books, topics, catalogError, bookById, reloadBooks,
    cart, cartCount, addToCart, removeFromCart, clearCart,
    wishlist, toggleWishlist,
    notifications, unread, refreshNotifications, openNotifications, clearNotifications,
    drawer, setDrawer, paletteOpen, setPaletteOpen,
    toast, toastMessage,
  };
  return <StoreContext.Provider value={value}>{children}</StoreContext.Provider>;
}

export function useStore(): Store {
  const s = useContext(StoreContext);
  if (!s) throw new Error('useStore outside StoreProvider');
  return s;
}
