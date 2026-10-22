import { useCallback, useEffect, useMemo, useState } from 'react'
import './features.css'

const API = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '')
function encodeBasicCredentials(value) {
  const bytes = new TextEncoder().encode(value)
  let binary = ''
  bytes.forEach(byte => { binary += String.fromCharCode(byte) })
  return window.btoa(binary)
}
function basicAuth(credentials) {
  if (!credentials?.email || !credentials?.password) return {}
  return { Authorization: `Basic ${encodeBasicCredentials(`${credentials.email.trim()}:${credentials.password}`)}` }
}
async function api(path, options = {}, credentials) {
  const response = await fetch(`${API}/api${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...basicAuth(credentials), ...(options.headers || {}) },
  })
  if (!response.ok) {
    let message = `Request failed (${response.status})`
    try { const body = await response.json(); message = body.detail || body.message || message } catch {}
    if (response.status === 401) message = 'Invalid email or password.'
    if (response.status === 403) message = 'You are not allowed to perform this action.'
    throw new Error(message)
  }
  if (response.status === 204) return null
  return response.json()
}
function Badge({ children, kind = '' }) { return <span className={`badge ${kind}`}>{children}</span> }
function App() {
  const [credentials, setCredentials] = useState({ email: '', password: '' })
  const [signedIn, setSignedIn] = useState(false)
  const [authMode, setAuthMode] = useState('login')
  const [registration, setRegistration] = useState({ name: '', email: '', password: '', confirmPassword: '' })
  const [emailOtp, setEmailOtp] = useState('')
  const [otpSent, setOtpSent] = useState(false)
  const [emailVerified, setEmailVerified] = useState(false)
  const [emailVerificationToken, setEmailVerificationToken] = useState('')
  const [profile, setProfile] = useState(null)
  const [url, setUrl] = useState('https://example.com')
  const [scope, setScope] = useState('General exploratory checks')
  const [browser, setBrowser] = useState('Chrome')
  const [authorized, setAuthorized] = useState(false)
  const [requests, setRequests] = useState([])
  const [selected, setSelected] = useState(null)
  const [results, setResults] = useState([])
  const [plan, setPlan] = useState([])
  const [findings, setFindings] = useState([])
  const [finding, setFinding] = useState({ title: '', description: '', severity: 'MEDIUM', evidenceUrl: '' })
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const callApi = useCallback((path, options = {}) => api(path, options, credentials), [credentials])

  const refresh = useCallback(async () => {
    if (!signedIn) return
    try { setRequests(await callApi('/testing-requests')) } catch (e) { setError(e.message) }
  }, [callApi, signedIn])
  useEffect(() => { refresh() }, [refresh])

  useEffect(() => {
    if (!signedIn || !selected?.request?.id) return
    let cancelled = false
    const load = async () => {
      try {
        const [detail, foundResults] = await Promise.all([
          callApi(`/testing-requests/${selected.request.id}`),
          callApi(`/testing-requests/${selected.request.id}/results`),
        ])
        const foundFindings = detail.manualReview?.id
          ? await callApi(`/manual-reviews/${detail.manualReview.id}/findings`)
          : []
        if (!cancelled) {
          setSelected(detail)
          setResults(foundResults)
          setFindings(foundFindings)
        }
      } catch (e) { if (!cancelled) setError(e.message) }
    }
    load()
    const timer = setInterval(load, 4000)
    return () => { cancelled = true; clearInterval(timer) }
  }, [selected?.request?.id, signedIn, callApi])

  const counts = useMemo(() => ({
    total: results.length,
    pass: results.filter(r => r.outcome === 'PASS').length,
    fail: results.filter(r => ['FAIL', 'ERROR'].includes(r.outcome)).length,
  }), [results])
  const hasActiveRun = Boolean(selected?.runs?.some(run => ['QUEUED', 'RUNNING'].includes(run.status)))

  async function connect(event) {
    event.preventDefault(); setBusy(true); setError(''); setNotice('')
    try {
      const currentProfile = await api('/auth/login', { method: 'POST' }, credentials)
      const list = await api('/testing-requests', {}, credentials)
      setProfile(currentProfile)
      setRequests(list)
      setSignedIn(true)
      setNotice(`Welcome back, ${currentProfile.name}.`)
    } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  async function sendEmailOtp() {
    setBusy(true); setError(''); setNotice('')
    try {
      await api('/auth/send-otp', { method: 'POST', body: JSON.stringify({ email: registration.email.trim() }) })
      setOtpSent(true); setEmailVerified(false); setEmailOtp(''); setEmailVerificationToken('')
      setNotice('Verification code sent. Check your inbox and spam folder.')
    } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  async function verifyEmailOtp() {
    setBusy(true); setError(''); setNotice('')
    try {
      const verified = await api('/auth/verify-otp', { method: 'POST', body: JSON.stringify({ email: registration.email.trim(), otp: emailOtp }) })
      setEmailVerificationToken(verified.verificationToken)
      setEmailVerified(true)
      setNotice('Email verified. You can now create your account.')
    } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  async function registerAccount(event) {
    event.preventDefault(); setBusy(true); setError(''); setNotice('')
    if (!emailVerified) { setError('Verify your email before creating an account.'); setBusy(false); return }
    if (registration.password !== registration.confirmPassword) {
      setError('The passwords do not match.')
      setBusy(false)
      return
    }
    if (new TextEncoder().encode(registration.password).length > 72) {
      setError('Password must be no more than 72 UTF-8 bytes.')
      setBusy(false)
      return
    }
    try {
      await api('/auth/register', {
        method: 'POST',
        body: JSON.stringify({
          name: registration.name.trim(),
          email: registration.email.trim(),
          password: registration.password,
          emailVerificationToken,
        }),
      })
      setCredentials({ email: registration.email.trim(), password: registration.password })
      setRegistration(v => ({ ...v, confirmPassword: '' }))
      setEmailOtp(''); setOtpSent(false); setEmailVerified(false); setEmailVerificationToken('')
      setAuthMode('login')
      setNotice('Account created successfully. Sign in with your new email and password.')
    } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  function signOut() {
    setSignedIn(false)
    setCredentials({ email: '', password: '' })
    setProfile(null)
    setRequests([]); setSelected(null); setResults([]); setPlan([]); setFindings([])
    setNotice('Signed out. Your credentials have been cleared from this page.')
  }
  async function createRequest(event) {
    event.preventDefault(); setBusy(true); setError(''); setNotice('')
    try {
      const created = await callApi('/testing-requests', { method: 'POST', body: JSON.stringify({
        targetUrl: url, scope, browser, authorizationConfirmed: authorized,
      }) })
      const detail = { request: created.request, manualReview: created.manualReview, runs: [] }
      setSelected(detail); setResults([]); setFindings([]); setPlan([])
      const [plannedChecks, initialFindings] = await Promise.all([
        callApi(`/testing-requests/${created.request.id}/plan`),
        callApi(`/manual-reviews/${created.manualReview.id}/findings`),
      ])
      setPlan(plannedChecks); setFindings(initialFindings)
      setNotice('Request created. AI-assisted checks are planned; human review remains pending until a tester completes it.')
      await refresh()
    } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  async function startAutomation() {
    if (!selected?.request?.id) return
    setBusy(true); setError(''); setNotice('')
    try {
      const queuedRun = await callApi(`/testing-requests/${selected.request.id}/automation`, { method: 'POST' })
      setSelected(previous => previous ? { ...previous, runs: [queuedRun, ...(previous.runs || [])] } : previous)
      setNotice('Selenium run queued. Results and request status refresh automatically.')
      await refresh()
    } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  async function openRequest(request) {
    setError('')
    try {
      const detail = await callApi(`/testing-requests/${request.id}`)
      const [foundResults, plannedChecks, foundFindings] = await Promise.all([
        callApi(`/testing-requests/${request.id}/results`),
        callApi(`/testing-requests/${request.id}/plan`),
        detail.manualReview?.id ? callApi(`/manual-reviews/${detail.manualReview.id}/findings`) : Promise.resolve([]),
      ])
      setSelected(detail); setResults(foundResults); setPlan(plannedChecks); setFindings(foundFindings)
    } catch (e) { setError(e.message) }
  }
  async function updateReview(status) {
    if (!selected?.manualReview?.id) return
    setBusy(true); setError('')
    try {
      const review = await callApi(`/manual-reviews/${selected.manualReview.id}`, {
        method: 'PATCH', body: JSON.stringify({
          status,
          assignedTester: selected.manualReview.assignedTester || '',
          reviewerNotes: selected.manualReview.reviewerNotes || '',
        }),
      })
      setSelected({ ...selected, manualReview: review })
      setNotice(`Manual review updated to ${status}.`)
    } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  async function addFinding(event) {
    event.preventDefault()
    if (!selected?.manualReview?.id) return
    setBusy(true); setError(''); setNotice('')
    try {
      const created = await callApi(`/manual-reviews/${selected.manualReview.id}/findings`, {
        method: 'POST', body: JSON.stringify(finding),
      })
      setFindings(previous => [created, ...previous])
      setFinding({ title: '', description: '', severity: 'MEDIUM', evidenceUrl: '' })
      setNotice('Manual finding saved.')
    } catch (e) { setError(e.message) } finally { setBusy(false) }
  }

  return <div className="app-shell">
    <header className="topbar">
      <a className="brand" href="#"><span className="brand-mark">T</span><span>TestPilot <b>AI</b></span></a>
      <div className="topbar-right"><span className="top-tag"><i /> AI-ASSISTED QUALITY ENGINE</span>{signedIn && <><span className="account-label">{profile?.name || profile?.email}</span><button className="text-button" onClick={signOut}>Sign out</button></>}</div>
    </header>
    <main>
      <section className="hero">
        <div className="eyebrow">AUTOMATION + HUMAN REVIEW</div>
        <h1>Test any authorized website.<br /><span>Understand every result.</span></h1>
        <p>Submit a testing request, run allow-listed Selenium checks planned by AI, and track human findings in one workspace.</p>
      </section>
      {error && <div className="alert error">{error}<button onClick={() => setError('')}>Dismiss</button></div>}
      {notice && <div className="alert success">{notice}<button onClick={() => setNotice('')}>Dismiss</button></div>}

      {!signedIn ? <section className="panel auth-panel">
        {authMode === 'register' ? <>
          <div className="panel-heading"><div><span className="step">ACCOUNT</span><h2>Create your TestPilot account</h2></div><Badge>FREE PREVIEW</Badge></div>
          <p className="muted auth-copy">Verify your email address before creating your account. Verification codes expire after 10 minutes.</p>
          <form onSubmit={registerAccount} className="auth-form">
            <div><label htmlFor="register-name">Full name</label><input id="register-name" autoComplete="name" required maxLength={80} value={registration.name} onChange={e => setRegistration(v => ({ ...v, name: e.target.value }))} /></div>
            <div><label htmlFor="register-email">Email address</label><input id="register-email" type="email" autoComplete="email" required maxLength={254} value={registration.email} onChange={e => { setRegistration(v => ({ ...v, email: e.target.value })); setOtpSent(false); setEmailVerified(false); setEmailOtp('') }} /></div>
            <div className="otp-row"><button type="button" className="secondary" disabled={busy || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(registration.email)} onClick={sendEmailOtp}>{otpSent ? 'Resend verification code' : 'Send email OTP'}</button><span className={emailVerified ? 'otp-status verified' : 'otp-status'}>{emailVerified ? 'Email verified' : otpSent ? 'Code sent · expires in 10 minutes' : 'Email verification required'}</span></div>
            {otpSent && !emailVerified && <div><label htmlFor="register-otp">6-digit email verification code</label><input id="register-otp" inputMode="numeric" autoComplete="one-time-code" pattern="[0-9]{6}" maxLength={6} required value={emailOtp} onChange={e => setEmailOtp(e.target.value.replace(/\D/g, '').slice(0, 6))} /></div>}
            {otpSent && !emailVerified && <div className="otp-row"><button type="button" className="secondary" disabled={busy || emailOtp.length !== 6} onClick={verifyEmailOtp}>Verify code</button></div>}
            <div><label htmlFor="register-password">Password (12+ characters; max 72 UTF-8 bytes)</label><input id="register-password" type="password" autoComplete="new-password" required minLength={12} maxLength={72} value={registration.password} onChange={e => setRegistration(v => ({ ...v, password: e.target.value }))} /></div>
            <div><label htmlFor="register-confirm-password">Confirm password</label><input id="register-confirm-password" type="password" autoComplete="new-password" required minLength={12} maxLength={72} value={registration.confirmPassword} onChange={e => setRegistration(v => ({ ...v, confirmPassword: e.target.value }))} /></div>
            <button className="primary" disabled={busy || !emailVerified}>{busy ? 'Creating account…' : 'Create verified account'} <span>→</span></button>
          </form>
          <button type="button" className="auth-switch" onClick={() => { setAuthMode('login'); setError(''); setNotice('') }}>Already have an account? Sign in</button>
          <p className="fine-print">Passwords are stored as BCrypt hashes. Use a unique password. Account registration currently has no email verification, so this is still a prototype—not a production identity service.</p>
        </> : <>
          <div className="panel-heading"><div><span className="step">SIGN IN</span><h2>Sign in to your testing workspace</h2></div><Badge>PROTECTED API</Badge></div>
          <p className="muted auth-copy">Use the email address and password you registered with. Your credentials stay in page memory and are not saved to browser storage.</p>
          <form onSubmit={connect} className="auth-form">
            <div><label htmlFor="login-email">Email address</label><input id="login-email" type="email" autoComplete="username" required maxLength={254} value={credentials.email} onChange={e => setCredentials(v => ({ ...v, email: e.target.value }))} /></div>
            <div><label htmlFor="login-password">Password</label><input id="login-password" type="password" autoComplete="current-password" required value={credentials.password} onChange={e => setCredentials(v => ({ ...v, password: e.target.value }))} /></div>
            <button className="primary" disabled={busy}>{busy ? 'Signing in…' : 'Sign in and load dashboard'} <span>→</span></button>
          </form>
          <button type="button" className="auth-switch" onClick={() => { setAuthMode('register'); setError(''); setNotice('') }}>New to TestPilot? Create an account</button>
          <p className="fine-print">To connect this deployed frontend to your backend later, set VITE_API_BASE_URL to the backend's HTTPS origin and set the backend's FRONTEND_ORIGIN to this site's exact URL.</p>
        </>}
      </section> : <div className="layout">
        <section className="panel request-panel">
          <div className="panel-heading"><div><span className="step">01</span><h2>New testing request</h2></div><span className="muted">~ 1 minute</span></div>
          <form onSubmit={createRequest}>
            <label htmlFor="url">Website URL</label>
            <input id="url" type="url" required value={url} onChange={e => setUrl(e.target.value)} placeholder="https://your-authorized-site.com" />
            <label htmlFor="scope">Testing scope</label>
            <select id="scope" value={scope} onChange={e => setScope(e.target.value)}>
              <option>General exploratory checks</option><option>Login and navigation</option><option>Signup and forms</option><option>Search and filters</option><option>Responsive and content checks</option>
            </select>
            <label htmlFor="browser">Browser</label>
            <select id="browser" value={browser} onChange={e => setBrowser(e.target.value)}><option>Chrome</option></select>
            <p className="fine-print browser-note">The bundled Docker browser worker currently provides Chrome.</p>
            <label className="check-row"><input type="checkbox" checked={authorized} onChange={e => setAuthorized(e.target.checked)} required /> <span>I own this website or have permission to test it.</span></label>
            <div className="manual-note"><span>✓</span><div><b>Human manual review included</b><small>A separate review request will be created and remain pending until a tester completes it.</small></div></div>
            <button className="primary" disabled={busy || !authorized}>{busy ? 'Working…' : 'Create testing request'} <span>→</span></button>
          </form>
          <p className="fine-print">Only test systems you are authorized to assess. URL checks are defense-in-depth and do not replace browser-worker network isolation.</p>
        </section>
        <section className="panel requests-panel">
          <div className="panel-heading"><div><span className="step">02</span><h2>Testing workspace</h2></div><button className="text-button" onClick={refresh}>↻ Refresh</button></div>
          <div className="request-list">
            {requests.length === 0 && <div className="empty"><div className="empty-icon">◎</div><b>No testing requests yet</b><p>Create your first request to see results here.</p></div>}
            {requests.map(r => <button className={`request-item ${selected?.request?.id === r.id ? 'selected' : ''}`} key={r.id} onClick={() => openRequest(r)}>
              <span className="site-icon">↗</span><span className="request-info"><b>{r.targetUrl}</b><small>{r.scope} · {r.browser}</small></span><Badge kind={r.status?.toLowerCase()}>{r.status}</Badge>
            </button>)}
          </div>
          {selected && <div className="details">
            <div className="detail-title"><div><span className="eyebrow">SELECTED REQUEST</span><h3>{selected.request.targetUrl}</h3></div><Badge kind={selected.request.status?.toLowerCase()}>{selected.request.status}</Badge></div>
            <div className="stats">
              <div><small>Checks recorded</small><b>{counts.total}</b></div><div><small>Passed</small><b className="green">{counts.pass}</b></div><div><small>Failed / error</small><b className="red">{counts.fail}</b></div>
            </div>
            <div className="section-label">AI-ASSISTED TEST PLAN</div>
            <div className="plan-list">{plan.map(check => <div className="plan-row" key={check.id}><span>✓</span><div><b>{check.name}</b><small>{check.description}</small></div></div>)}</div>
            <button className="primary run-button" onClick={startAutomation} disabled={busy || hasActiveRun}>{busy ? 'Working…' : hasActiveRun ? 'Selenium run in progress' : '▶ Run Selenium checks'} <span>{hasActiveRun ? 'RUNNING' : '→'}</span></button>
            <div className="section-label">AUTOMATION RESULTS</div>
            {results.length === 0 ? <p className="muted">No automation results yet.</p> : results.map(r => <div className="result-row" key={r.id}><span className={`result-dot ${r.outcome?.toLowerCase()}`}/><div><b>{r.testName}</b><small>{r.details}</small></div><Badge kind={r.outcome?.toLowerCase()}>{r.outcome}</Badge></div>)}
            <div className="manual-card">
              <div className="manual-card-head"><div><span className="section-label">HUMAN REVIEW</span><h4>Manual testing request</h4></div><Badge kind={selected.manualReview?.status?.toLowerCase()}>{selected.manualReview?.status || 'PENDING'}</Badge></div>
              <p>Manual review is a human task; automation does not complete it automatically.</p>
              <div className="manual-actions"><button onClick={() => updateReview('IN_PROGRESS')} disabled={busy}>Mark in progress</button><button onClick={() => updateReview('COMPLETED')} disabled={busy}>Mark completed</button></div>
              <div className="section-label">ADD MANUAL FINDING</div>
              <form className="finding-form" onSubmit={addFinding}>
                <label htmlFor="finding-title">Finding title</label>
                <input id="finding-title" required maxLength={180} value={finding.title} onChange={e => setFinding(v => ({ ...v, title: e.target.value }))} placeholder="e.g. Submit button is not keyboard accessible" />
                <label htmlFor="finding-description">Steps and evidence</label>
                <textarea id="finding-description" required maxLength={4000} rows={3} value={finding.description} onChange={e => setFinding(v => ({ ...v, description: e.target.value }))} placeholder="Steps to reproduce, expected result, actual result…" />
                <div className="finding-fields"><div><label htmlFor="severity">Severity</label><select id="severity" value={finding.severity} onChange={e => setFinding(v => ({ ...v, severity: e.target.value }))}><option>LOW</option><option>MEDIUM</option><option>HIGH</option><option>CRITICAL</option></select></div><div><label htmlFor="evidence">Evidence URL (optional)</label><input id="evidence" type="url" value={finding.evidenceUrl} onChange={e => setFinding(v => ({ ...v, evidenceUrl: e.target.value }))} placeholder="https://…" /></div></div>
                <button className="primary" disabled={busy}>{busy ? 'Saving…' : 'Save finding'} <span>＋</span></button>
              </form>
              <div className="section-label">RECORDED FINDINGS ({findings.length})</div>
              {findings.length === 0 ? <p className="muted">No manual findings recorded yet.</p> : findings.map(item => <div className="finding-row" key={item.id}><div><b>{item.title}</b><p>{item.description}</p>{item.evidenceUrl && <a href={item.evidenceUrl} target="_blank" rel="noreferrer">Open evidence ↗</a>}</div><Badge kind={item.severity.toLowerCase()}>{item.severity}</Badge></div>)}
              <small className="muted">Requests and findings are scoped to the signed-in account. Only test websites you own or are authorized to assess.</small>
            </div>
          </div>}
        </section>
      </div>}
      <footer><span>TESTPILOT AI</span><span>Built for responsible, evidence-based QA.</span><span>Prototype · Secure before public launch</span></footer>
    </main>
  </div>
}
export default App
