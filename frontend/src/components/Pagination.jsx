export function Pagination({ page, totalPages, totalElements, onPageChange }) {
  if (!totalPages || totalPages <= 1) {
    return <p className="pagination-meta">{totalElements} item{totalElements === 1 ? '' : 's'}</p>
  }
  const start = Math.max(0, Math.min(page - 2, totalPages - 5))
  const pages = Array.from({ length: Math.min(5, totalPages) }, (_, i) => start + i)
  return (
    <div className="pagination">
      <button type="button" className="page-btn" disabled={page <= 0} onClick={() => onPageChange(page - 1)}>← Prev</button>
      {pages.map((p) => (
        <button key={p} type="button" className={`page-btn ${p === page ? 'active' : ''}`} onClick={() => onPageChange(p)}>
          {p + 1}
        </button>
      ))}
      <button type="button" className="page-btn" disabled={page >= totalPages - 1} onClick={() => onPageChange(page + 1)}>Next →</button>
      <span className="pagination-meta">Page {page + 1} of {totalPages} · {totalElements} total</span>
    </div>
  )
}
