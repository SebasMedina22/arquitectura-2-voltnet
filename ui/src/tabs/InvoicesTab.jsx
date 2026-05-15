import { useState } from 'react'
import { listInvoices } from '../api.js'

export default function InvoicesTab() {
  const [userId, setUserId] = useState('USR-001')
  const [rows, setRows] = useState([])
  const [error, setError] = useState(null)

  const load = async () => {
    setError(null)
    try { setRows(await listInvoices(userId)) }
    catch (e) { setError(e.message) }
  }

  return (
    <div className="card">
      <h2>Facturas (MS-Billing)</h2>
      <p className="help">GET /invoices?userId=... (lectura interna; en VoltNet Billing es 100% async hacia el resto del sistema)</p>
      <div className="row">
        <div>
          <label>User ID</label>
          <input value={userId} onChange={e => setUserId(e.target.value.toUpperCase())} />
        </div>
        <button className="primary" onClick={load}>Buscar facturas</button>
      </div>
      {error && <div className="alert err">{error}</div>}
      {rows.length > 0 && (
        <table style={{ marginTop: 16 }}>
          <thead>
            <tr><th>Invoice ID</th><th>Sesion</th><th>Monto</th><th>Estado</th><th>Creada</th><th>Vence</th></tr>
          </thead>
          <tbody>
            {rows.map(r => (
              <tr key={r.id}>
                <td><code>{r.id.slice(0,8)}...</code></td>
                <td><code>{r.sessionId.slice(0,8)}...</code></td>
                <td>{r.amount} {r.currency}</td>
                <td><span className={`status ${r.status}`}>{r.status}</span></td>
                <td>{new Date(r.createdAt).toLocaleString()}</td>
                <td>{new Date(r.dueAt).toLocaleString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {rows.length === 0 && !error && <p className="legend">Sin facturas todavia.</p>}
    </div>
  )
}
