// Wrappers minimos. En dev Vite proxea /api/*; en prod Nginx lo hace.

async function request(path, opts = {}) {
  const resp = await fetch(path, {
    headers: { 'Content-Type': 'application/json', ...(opts.headers || {}) },
    ...opts,
  })
  const text = await resp.text()
  let body
  try { body = text ? JSON.parse(text) : null } catch { body = text }
  if (!resp.ok) {
    const err = new Error(typeof body === 'object' ? (body.message || JSON.stringify(body)) : body)
    err.status = resp.status
    err.body = body
    throw err
  }
  return body
}

// ===== Charge Orchestrator =====
export const startSession = (userId, stationId) =>
  request('/api/charge/sessions/start', { method: 'POST', body: JSON.stringify({ userId, stationId }) })

export const stopSession = (sessionId, kwhConsumed) =>
  request(`/api/charge/sessions/${sessionId}/stop`, { method: 'POST', body: JSON.stringify({ kwhConsumed }) })

export const listSessions = (userId) =>
  request(`/api/charge/sessions?userId=${encodeURIComponent(userId)}`)

// ===== GridLoad =====
export const getStationLoad = (stationId) =>
  request(`/api/grid/grid/load?stationId=${encodeURIComponent(stationId)}`)

export const setStationLoad = (stationId, currentLoadKw) =>
  request(`/api/grid/grid/load`, { method: 'POST', body: JSON.stringify({ stationId, currentLoadKw }) })

// ===== Billing =====
export const listInvoices = (userId) =>
  request(`/api/billing/invoices?userId=${encodeURIComponent(userId)}`)
