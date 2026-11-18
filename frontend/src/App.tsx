import { useEffect, type ReactNode } from 'react';
import { BrowserRouter, Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { Drawers } from './components/Drawers';
import { AdminHeader, Footer, Header } from './components/Header';
import { SearchPalette } from './components/SearchPalette';
import { Toast } from './components/Toast';
import { Admin } from './pages/Admin';
import { BookPage } from './pages/BookPage';
import { Checkout } from './pages/Checkout';
import { Home } from './pages/Home';
import { Login } from './pages/Login';
import { Orders } from './pages/Orders';
import { Products } from './pages/Products';
import { Wishlist } from './pages/Wishlist';
import { StoreProvider, useStore } from './state/store';

/** Pages that need an account send the guest to login and back again. */
function NeedsLogin({ children }: { children: ReactNode }) {
  const { user } = useStore();
  const location = useLocation();
  if (!user) return <Navigate to={`/login?next=${encodeURIComponent(location.pathname)}`} replace />;
  return children;
}

function ScrollToTop() {
  const { pathname } = useLocation();
  useEffect(() => {
    window.scrollTo(0, 0);
  }, [pathname]);
  return null;
}

/** The admin account only runs the store: one dashboard, no shopping pages. */
function AdminShell() {
  return (
    <>
      <ScrollToTop />
      <AdminHeader />
      <main className="wrap">
        <Routes>
          <Route path="/admin" element={<Admin />} />
          <Route path="*" element={<Navigate to="/admin" replace />} />
        </Routes>
      </main>
      <Toast />
    </>
  );
}

function Shell() {
  const { user, catalogError, reloadBooks } = useStore();
  if (user?.role === 'ADMIN') return <AdminShell />;
  return (
    <>
      <ScrollToTop />
      <Header />
      <main className="wrap">
        {catalogError && (
          <div className="ord" role="alert">
            <b>The store could not load the books.</b> <span className="mut">{catalogError}</span>{' '}
            <button className="lnk" onClick={() => void reloadBooks()}>Try again</button>
          </div>
        )}
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/products" element={<Products />} />
          <Route path="/book/:id" element={<BookPage />} />
          <Route path="/wishlist" element={<Wishlist />} />
          <Route path="/login" element={<Login />} />
          <Route path="/checkout" element={<NeedsLogin><Checkout /></NeedsLogin>} />
          <Route path="/orders" element={<NeedsLogin><Orders /></NeedsLogin>} />
          <Route path="/admin" element={<Navigate to={user ? '/' : '/login'} replace />} />
          <Route path="*" element={<div className="empty">Page not found.</div>} />
        </Routes>
      </main>
      <Footer />
      <Drawers />
      <SearchPalette />
      <Toast />
    </>
  );
}

export function App() {
  return (
    <BrowserRouter basename="/shop">
      <StoreProvider>
        <Shell />
      </StoreProvider>
    </BrowserRouter>
  );
}
