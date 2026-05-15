import { useState } from 'react'
import StartSessionTab from './tabs/StartSessionTab.jsx'
import SessionsTab from './tabs/SessionsTab.jsx'
import InvoicesTab from './tabs/InvoicesTab.jsx'
import AdminTab from './tabs/AdminTab.jsx'

const TABS = [
  { id: 'start',    label: 'Iniciar carga',         component: StartSessionTab },
  { id: 'sessions', label: 'Sesiones del usuario',  component: SessionsTab },
  { id: 'invoices', label: 'Facturas',              component: InvoicesTab },
  { id: 'admin',    label: 'Admin (simulacion red)',component: AdminTab },
]

export default function App() {
  const [active, setActive] = useState('start')
  const ActiveComp = TABS.find(t => t.id === active).component
  return (
    <div className="app">
      <header className="brand">
        <h1>VoltNet</h1>
        <small>Gestion de Carga Electrica Urbana</small>
      </header>
      <p className="legend">
        Frontend de demo que consume los 3 microservicios via Nginx Gateway (<code>/api/charge</code>, <code>/api/grid</code>, <code>/api/billing</code>).
      </p>
      <nav className="tabs">
        {TABS.map(t => (
          <button
            key={t.id}
            className={t.id === active ? 'active' : ''}
            onClick={() => setActive(t.id)}
          >
            {t.label}
          </button>
        ))}
      </nav>
      <ActiveComp />
    </div>
  )
}
