import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { api, ApiError } from '../api';
import { useStore } from '../state/store';
import type { Session } from '../types';

type Mode = 'login' | 'register' | 'verify' | 'forgot' | 'reset';

/** Login, sign-up with an emailed 6-digit code, and password reset. */
export function Login() {
  const { user, signIn, toast } = useStore();
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const next = params.get('next');
  const [mode, setMode] = useState<Mode>('login');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [code, setCode] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);

  useEffect(() => {
    if (user) navigate(next ?? (user.role === 'ADMIN' ? '/admin' : '/'), { replace: true });
  }, [user, next, navigate]);

  const go = (m: Mode) => {
    setMode(m);
    setError(null);
    setInfo(null);
    setCode('');
  };

  async function run(action: () => Promise<unknown>) {
    setBusy(true);
    setError(null);
    try {
      await action();
    } catch (e) {
      if (e instanceof ApiError && e.code === 'EMAIL_NOT_VERIFIED') {
        go('verify');
        setInfo(e.message);
      } else {
        setError(e instanceof ApiError ? e.message : 'Something went wrong');
      }
    } finally {
      setBusy(false);
    }
  }

  const done = (s: Session) => {
    signIn(s);
    toast(`Welcome, ${s.user.name}`);
  };

  function submit(e: FormEvent) {
    e.preventDefault();
    if (mode === 'login') void run(async () => done(await api.login(email, password)));
    if (mode === 'register') {
      if (password.length < 8) return setError('Use at least 8 characters for the password');
      void run(async () => {
        const r = await api.register(name, email, password);
        go('verify');
        setInfo(r.message);
      });
    }
    if (mode === 'verify') void run(async () => done(await api.verify(email, code)));
    if (mode === 'forgot') {
      void run(async () => {
        const r = await api.forgotPassword(email);
        go('reset');
        setInfo(r.message);
      });
    }
    if (mode === 'reset') {
      if (password.length < 8) return setError('Use at least 8 characters for the new password');
      void run(async () => done(await api.resetPassword(email, code, password)));
    }
  }

  const title = { login: 'Login', register: 'Create account', verify: 'Verify your email', forgot: 'Forgot password', reset: 'Set a new password' }[mode];
  const button = { login: 'Login', register: 'Create account', verify: 'Verify and continue', forgot: 'Send reset code', reset: 'Save password and login' }[mode];

  return (
    <>
      <div className="crumb"><button className="btn" onClick={() => navigate(-1)}>← Back</button></div>
      <form className="auth" onSubmit={submit}>
        <h2>{title}</h2>
        {(mode === 'login' || mode === 'register') && (
          <div className="tabs">
            <button type="button" className={`tab${mode === 'login' ? ' on' : ''}`} onClick={() => go('login')}>Login</button>
            <button type="button" className={`tab${mode === 'register' ? ' on' : ''}`} onClick={() => go('register')}>Register</button>
          </div>
        )}
        {info && <p className="info">{info}</p>}

        {mode === 'register' && (
          <>
            <label htmlFor="a-n">Name</label>
            <input className="in2" id="a-n" autoComplete="name" required value={name} onChange={(e) => setName(e.target.value)} />
          </>
        )}
        {mode !== 'verify' && mode !== 'reset' && (
          <>
            <label htmlFor="a-e">Email</label>
            <input className="in2" id="a-e" type="email" autoComplete="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
          </>
        )}
        {(mode === 'verify' || mode === 'reset') && (
          <>
            <label htmlFor="a-c">6-digit code sent to {email}</label>
            <input className="in2 otp" id="a-c" inputMode="numeric" autoComplete="one-time-code" maxLength={6} required
              value={code} onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))} />
          </>
        )}
        {(mode === 'login' || mode === 'register' || mode === 'reset') && (
          <>
            <label htmlFor="a-p">{mode === 'reset' ? 'New password' : 'Password'}</label>
            <input className="in2" id="a-p" type="password" required minLength={mode === 'login' ? 1 : 8}
              autoComplete={mode === 'login' ? 'current-password' : 'new-password'} value={password} onChange={(e) => setPassword(e.target.value)} />
          </>
        )}

        {error && <p className="err">{error}</p>}
        <button className="btn p" style={{ width: '100%', marginTop: 14 }} disabled={busy}>{busy ? 'Please wait…' : button}</button>

        <p className="mut" style={{ fontSize: 13, display: 'flex', justifyContent: 'space-between', gap: 10, flexWrap: 'wrap' }}>
          {mode === 'login' && <button type="button" className="lnk" onClick={() => go('forgot')}>Forgot password?</button>}
          {mode === 'verify' && (
            <button type="button" className="lnk" disabled={busy} onClick={() => run(async () => {
              const r = await api.resendCode(email);
              setInfo(r.message);
            })}>Send a new code</button>
          )}
          {(mode === 'verify' || mode === 'forgot' || mode === 'reset') && (
            <button type="button" className="lnk" onClick={() => go('login')}>Back to login</button>
          )}
        </p>
      </form>
    </>
  );
}
