import { useState } from 'react'
import { startSession, stopSession } from '../api.js'

export default function StartSessionTab() {
  const [userId, setUserId] = useState('USR-001')
  const [stationId, setStationId] = useState('STN-001')
  const [result, setResult] = useState(null)
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)
  const [kwh, setKwh] = useState('18.5')

  const onStart = async () => {
    setBusy(true); setError(null); setResult(null)
    try { setResult(await startSession(userId, stationId)) }
    catch (e) { setError({ status: e.status, body: e.body }) }
    finally { setBusy(false) }
  }

  const onStop = async () => {
    if (!result?.sessionId) return
    setBusy(true); setError(null)
    try { setResult(await stopSession(result.sessionId, parseFloat(kwh))) }
    catch (e) { setError({ status: e.status, body: e.body }) }
    finally { setBusy(false) }
  }

  return (
    <div className="card">
      <h2>Iniciar / cerrar sesion de carga</h2>
      <p className="help">
        Aplica R1 (capacidad de red, sincrono via Feign+CircuitBreaker) y R2 (solvencia, proyeccion local).
      </p>

      <div className="row">
        <div>
          <label>User ID</label>
          <input value={userId} onChange={e => setUserId(e.target.value.toUpperCase())} />
        </div>
        <div>
          <label>Station ID</label>
          <input value={stationId} onChange={e => setStationId(e.target.value.toUpperCase())} />
        </div>
        <button className="primary" onClick={onStart} disabled={busy}>
          Iniciar carga
        </button>
      </div>

      {result?.status === 'STARTED' && (
        <div className="row" style={{ marginTop: 20 }}>
          <div>
            <label>kWh consumidos</label>
            <input value={kwh} onChange={e => setKwh(e.target.value)} type="number" step="0.1" min="0" />
          </div>
          <button className="primary" onClick={onStop} disabled={busy}>
            Cerrar sesion ({result.sessionId.slice(0,8)}...)
          </button>
        </div>
      )}

      {error && (
        <div className="alert err">
          HTTP {error.status} - <strong>{error.body?.code}</strong>: {error.body?.message}
        </div>
      )}
      {result && (
        <>
          <div className="alert ok">
            Sesion <code>{result.sessionId.slice(0,8)}...</code> en estado <span className={`status ${result.status}`}>{result.status}</span>
            {result.kwhConsumed > 0 && <> - {result.kwhConsumed} kWh registrados</>}
          </div>
          <pre>{JSON.stringify(result, null, 2)}</pre>
        </>
      )}

      <p className="legend">
        Seed disponible: <code>USR-001</code> (solvente), <code>USR-002</code> (15d, no bloquea), <code>USR-003</code> (45d -&gt; R2 bloquea). Estaciones: <code>STN-001</code> (45 kW), <code>STN-002</code> (92 kW), <code>STN-003</code> (105 kW -&gt; R1 bloquea).
      </p>
    </div>
  )
}
