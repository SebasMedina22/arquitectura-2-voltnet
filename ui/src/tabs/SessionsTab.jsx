import { useState } from 'react'
import { listSessions } from '../api.js'

export default function SessionsTab() {
  const [userId, setUserId] = useState('USR-001')
  const [rows, setRows] = useState([])
  const [error, setError] = useState(null)

  const load = async () => {
    setError(null)
    try { setRows(await listSessions(userId)) }
    catch (e) { setError(e.message) }
  }

  return (
    <div className="card">
      <h2>Sesiones del usuario</h2>
      <p className="help">GET /sessions?userId=... (Orchestrator)</p>
      <div className="row">
        <div>
          <label>User ID</label>
          <input value={userId} onChange={e => setUserId(e.target.value.toUpperCase())} />
        </div>
        <button className="primary" onClick={load}>Buscar</button>
      </div>
      {error && <div className="alert err">{error}</div>}
      {rows.length > 0 && (
        <table style={{ marginTop: 16 }}>
          <thead>
            <tr><th>Session ID</th><th>Estacion</th><th>Estado</th><th>kWh</th><th>Inicio</th><th>Cierre</th></tr>
          </thead>
          <tbody>
            {rows.map(r => (
              <tr key={r.sessionId}>
                <td><code>{r.sessionId.slice(0,8)}...</code></td>
                <td>{r.stationId}</td>
                <td><span className={`status ${r.status}`}>{r.status}</span></td>
                <td>{r.kwhConsumed}</td>
                <td>{new Date(r.startedAt).toLocaleString()}</td>
                <td>{r.completedAt ? new Date(r.completedAt).toLocaleString() : '-'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {rows.length === 0 && !error && <p className="legend">Sin sesiones todavia.</p>}
    </div>
  )
}
