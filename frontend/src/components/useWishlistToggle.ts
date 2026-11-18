import { useCallback } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useStore } from '../state/store';

/** Saving to the wishlist needs an account: guests are sent to login first and come back after. */
export function useWishlistToggle() {
  const { user, toggleWishlist, toast } = useStore();
  const navigate = useNavigate();
  const location = useLocation();
  return useCallback(
    async (id: number) => {
      if (!user) {
        toast('Log in to save books to your wishlist');
        navigate(`/login?next=${encodeURIComponent(location.pathname)}`);
        return;
      }
      await toggleWishlist(id);
    },
    [user, toggleWishlist, toast, navigate, location.pathname],
  );
}
