export function Brand() {
  return (
    <div className="brand-block">
      <div className="brand-mark">J</div>
      <div>
        <p className="eyebrow">Enterprise</p>
        <h2>JEEP</h2>
      </div>
    </div>
  )
}

export function PanelTitle({ title }) {
  return (
    <div className="panel-header">
      <h3>{title}</h3>
      <span className="chip neutral">Live data</span>
    </div>
  )
}
