import { useState } from 'react'
import { getStationLoad, setStationLoad } from '../api.js'

export default function AdminTab() {
  const [stationId, setStationId] = useState('STN-001')
  const [kw, setKw] = useState('50')
  const [current, setCurrent] = useState(null)
  const [error, setError] = useState(null)

  const refresh = async () => {
    setError(null)
    try { setCurrent(await getStationLoad(stationId)) }
    catch (e) { setError(e.message) }
  }
  const apply = async () => {
    setError(null)
    try {
      setCurrent(await setStationLoad(stationId, parseFloat(kw)))
    }
    catch (e) { setError(e.message) }
  }

  return (
    <div className="card">
      <h2>Simulacion de carga de red (MS-GridLoad)</h2>
      <p className="help">
        En produccion los sensores de los cargadores actualizarian este valor. Para la demo lo seteamos manualmente para forzar R1.
      </p>
      <div className="row">
        <div>
          <label>Station ID</label>
          <input value={stationId} onChange={e => setStationId(e.target.value.toUpperCase())} />
        </div>
        <div>
          <label>currentLoadKw</label>
          <input value={kw} onChange={e => setKw(e.target.value)} type="number" step="0.1" min="0" />
        </div>
        <button className="primary" onClick={apply}>Set</button>
        <button className="primary" onClick={refresh} style={{ background: '#1f6feb', borderColor: '#388bfd' }}>
          Refrescar
        </button>
      </div>
      {error && <div className="alert err">{error}</div>}
      {current && (
        <div className="alert ok" style={{ marginTop: 12 }}>
          {current.stationId} - {current.currentLoadKw} kW {current.overloaded && <strong>(SOBRECARGADA, R1 bloqueara)</strong>}
        </div>
      )}
      <p className="legend">Umbral del caso: <code>100 kW</code>. Por encima, R1 bloquea cualquier <code>POST /sessions/start</code> sobre esa estacion.</p>
    </div>
  )
}
