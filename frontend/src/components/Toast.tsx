import { useStore } from '../state/store';

export function Toast() {
  const { toastMessage } = useStore();
  return (
    <div className={`toast${toastMessage ? ' show' : ''}`} role="status">
      {toastMessage}
    </div>
  );
}
