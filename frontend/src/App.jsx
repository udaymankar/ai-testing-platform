import { useCallback, useEffect, useMemo, useState } from 'react'

const API = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '')
async function api(path, options = {}) {
  const response = await fetch(`${API}/api${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
  })
  if (!response.ok) {
    let message = `Request failed (${response.status})`
    try { const body = await response.json(); message = body.detail || body.message || message } catch {}
    throw new Error(message)
  }
  if (response.status === 204) return null
  return response.json()
}
function Badge({ children, kind = '' }) { return <span className={`badge ${kind}`}>{children}</span> }
function App() {
  const [url, setUrl] = useState('https://example.com')
  const [scope, setScope] = useState('General exploratory checks')
  const [browser, setBrowser] = useState('Chrome')
  const [authorized, setAuthorized] = useState(false)
  const [requests, setRequests] = useState([])
  const [selected, setSelected] = useState(null)
  const [results, setResults] = useState([])
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const refresh = useCallback(async () => {
    try { setRequests(await api('/testing-requests')) } catch (e) { setError(e.message) }
  }, [])
  useEffect(() => { refresh() }, [refresh])
  useEffect(() => {
    if (!selected?.request?.id) return
    let cancelled = false
    const load = async () => {
      try {
        const [detail, foundResults] = await Promise.all([
          api(`/testing-requests/${selected.request.id}`),
          api(`/testing-requests/${selected.request.id}/results`)
        ])
        if (!cancelled) { setSelected(detail); setResults(foundResults) }
      } catch (e) { if (!cancelled) setError(e.message) }
    }
    load()
    const timer = setInterval(load, 4000)
    return () => { cancelled = true; clearInterval(timer) }
  }, [selected?.request?.id])
  const counts = useMemo(() => ({
    total: results.length,
    pass: results.filter(r => r.outcome === 'PASS').length,
    fail: results.filter(r => ['FAIL', 'ERROR'].includes(r.outcome)).length
  }), [results])
  async function createRequest(event) {
    event.preventDefault(); setBusy(true); setError(''); setNotice('')
    try {
      const created = await api('/testing-requests', { method: 'POST', body: JSON.stringify({
        targetUrl: url, scope, browser, authorizationConfirmed: authorized
      }) })
      const detail = { request: created.request, manualReview: created.manualReview, runs: [] }
      setSelected(detail); setResults([]); setNotice('Request created. Manual review is pending until a human tester completes it.')
      await refresh()
    } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  async function startAutomation() {
    if (!selected?.request?.id) return
    setBusy(true); setError(''); setNotice('')
    try {
      await api(`/testing-requests/${selected.request.id}/automation`, { method: 'POST' })
      setNotice('Automation queued. This page refreshes status automatically.')
      await refresh()
    } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  async function openRequest(request) {
    setError('')
    try {
      const detail = await api(`/testing-requests/${request.id}`)
      setSelected(detail)
      setResults(await api(`/testing-requests/${request.id}/results`))
    } catch (e) { setError(e.message) }
  }
  async function updateReview(status) {
    if (!selected?.manualReview?.id) return
    setBusy(true); setError('')
    try {
      const review = await api(`/manual-reviews/${selected.manualReview.id}`, {
        method: 'PATCH', body: JSON.stringify({
          status,
          assignedTester: selected.manualReview.assignedTester || '',
          reviewerNotes: selected.manualReview.reviewerNotes || ''
        })
      })
      setSelected({ ...selected, manualReview: review })
      setNotice(`Manual review updated to ${status}.`)
    } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  return <div className="app-shell">
    <header className="topbar">
      <a className="brand" href="#"><span className="brand-mark">T</span><span>TestPilot <b>AI</b></span></a>
      <span className="top-tag"><i /> AI-ASSISTED QUALITY ENGINE</span>
    </header>
    <main>
      <section className="hero">
        <div className="eyebrow">AUTOMATION + HUMAN REVIEW</div>
        <h1>Test any authorized website.<br /><span>Understand every result.</span></h1>
        <p>Submit a testing request, run browser checks with Selenium, and track manual review in one workspace.</p>
      </section>
      {error && <div className="alert error">{error}<button onClick={() => setError('')}>Dismiss</button></div>}
      {notice && <div className="alert success">{notice}<button onClick={() => setNotice('')}>Dismiss</button></div>}
      <div className="layout">
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
            <select id="browser" value={browser} onChange={e => setBrowser(e.target.value)}><option>Chrome</option><option>Firefox</option><option>Edge</option></select>
            <label className="check-row"><input type="checkbox" checked={authorized} onChange={e => setAuthorized(e.target.checked)} required /> <span>I own this website or have permission to test it.</span></label>
            <div className="manual-note"><span>✓</span><div><b>Human manual review included</b><small>A separate review request will be created and remain pending until a tester completes it.</small></div></div>
            <button className="primary" disabled={busy || !authorized}>{busy ? 'Working…' : 'Create testing request'} <span>→</span></button>
          </form>
          <p className="fine-print">Only test systems you are authorized to assess. Browser execution is subject to server-side URL and network safety checks.</p>
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
            <button className="primary" onClick={startAutomation} disabled={busy || ['QUEUED','RUNNING'].includes(selected.request.status)}>{busy ? 'Working…' : '▶ Run Selenium checks'}</button>
            <div className="section-label">AUTOMATION RESULTS</div>
            {results.length === 0 ? <p className="muted">No automation results yet.</p> : results.map(r => <div className="result-row" key={r.id}><span className={`result-dot ${r.outcome?.toLowerCase()}`}/><div><b>{r.testName}</b><small>{r.details}</small></div><Badge kind={r.outcome?.toLowerCase()}>{r.outcome}</Badge></div>)}
            <div className="manual-card">
              <div className="manual-card-head"><div><span className="section-label">HUMAN REVIEW</span><h4>Manual testing request</h4></div><Badge kind={selected.manualReview?.status?.toLowerCase()}>{selected.manualReview?.status || 'PENDING'}</Badge></div>
              <p>Manual review is a human task; automation does not complete it automatically.</p>
              <div className="manual-actions"><button onClick={() => updateReview('IN_PROGRESS')} disabled={busy}>Mark in progress</button><button onClick={() => updateReview('COMPLETED')} disabled={busy}>Mark completed</button></div>
              <small className="muted">Admin/tester access controls must be added before this dashboard is publicly exposed.</small>
            </div>
          </div>}
        </section>
      </div>
      <footer><span>TESTPILOT AI</span><span>Built for responsible, evidence-based QA.</span><span>Prototype · Secure before public launch</span></footer>
    </main>
  </div>
}
export default App
