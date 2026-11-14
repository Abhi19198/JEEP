import { useEffect, useState } from 'react'
import { PanelTitle } from '../components/Brand.jsx'
import { Pagination } from '../components/Pagination.jsx'
import { pageContent } from '../api.js'

const emptyProject = { name: '', description: '' }
const PROJECT_STATUSES = ['ACTIVE', 'ON_HOLD', 'COMPLETED', 'ARCHIVED']
const PAGE_SIZE = 10

export function ProjectsPage({ ws }) {
  const { api, organizations, selectedOrganization, setSelectedOrganization, setSelectedProject, selectedProject, setLoading, setError, refreshProjects } = ws
  const [projectForm, setProjectForm] = useState(emptyProject)
  const [editingProject, setEditingProject] = useState(null)
  const [editForm, setEditForm] = useState({ name: '', description: '', status: 'ACTIVE' })
  const [search, setSearch] = useState('')
  const [appliedSearch, setAppliedSearch] = useState('')
  const [page, setPage] = useState(0)
  const [pageData, setPageData] = useState({ items: [], totalPages: 1, totalElements: 0, number: 0 })

  // Debounce the search box so we don't fire a request per keystroke.
  useEffect(() => {
    const timer = setTimeout(() => { setAppliedSearch(search); setPage(0) }, 400)
    return () => clearTimeout(timer)
  }, [search])

  useEffect(() => {
    if (!selectedOrganization) { setPageData({ items: [], totalPages: 1, totalElements: 0, number: 0 }); return }
    let cancelled = false
    const params = new URLSearchParams({ page: String(page), size: String(PAGE_SIZE) })
    if (appliedSearch.trim()) params.set('search', appliedSearch.trim())
    api(`/projects/organization/${selectedOrganization}?${params}`)
      .then((result) => { if (!cancelled) setPageData(pageContent(result)) })
      .catch((reason) => { if (!cancelled) setError(reason.message) })
    return () => { cancelled = true }
  }, [api, selectedOrganization, appliedSearch, page, setError])

  const updateForm = (setter) => (event) => setter((current) => ({ ...current, [event.target.name]: event.target.value }))

  const reload = () => {
    if (!selectedOrganization) return Promise.resolve()
    const params = new URLSearchParams({ page: String(page), size: String(PAGE_SIZE) })
    if (appliedSearch.trim()) params.set('search', appliedSearch.trim())
    return api(`/projects/organization/${selectedOrganization}?${params}`)
      .then((result) => setPageData(pageContent(result)))
  }

  const createProject = async (event) => {
    event.preventDefault()
    setLoading(true)
    setError('')
    try {
      await api('/projects', { method: 'POST', body: JSON.stringify({ ...projectForm, organizationId: selectedOrganization }) })
      setProjectForm(emptyProject)
      setPage(0)
      await reload()
      await refreshProjects()
    } catch (reason) {
      setError(reason.message)
    } finally {
      setLoading(false)
    }
  }

  const startEdit = (project) => {
    setEditingProject(project)
    setEditForm({ name: project.name, description: project.description || '', status: project.status })
  }

  const saveEdit = async (event) => {
    event.preventDefault()
    setLoading(true)
    setError('')
    try {
      await api(`/projects/${editingProject.id}`, { method: 'PUT', body: JSON.stringify(editForm) })
      setEditingProject(null)
      await reload()
      await refreshProjects()
    } catch (reason) {
      setError(reason.message)
    } finally {
      setLoading(false)
    }
  }

  const deleteProject = async (project) => {
    if (!window.confirm(`Delete project "${project.name}"? This archives it.`)) return
    setLoading(true)
    setError('')
    try {
      await api(`/projects/${project.id}`, { method: 'DELETE' })
      if (selectedProject === project.id) setSelectedProject('')
      setEditingProject(null)
      await reload()
      await refreshProjects()
    } catch (reason) {
      setError(reason.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="panel">
      <PanelTitle title="Projects" />
      <select value={selectedOrganization} onChange={(event) => { setSelectedOrganization(event.target.value); setPage(0) }}>
        <option value="">Choose organization</option>
        {organizations.map((org) => <option key={org.id} value={org.id}>{org.name}</option>)}
      </select>
      <form className="inline-form" onSubmit={createProject}>
        <input name="name" placeholder="Project name" value={projectForm.name} onChange={updateForm(setProjectForm)} required />
        <input name="description" placeholder="Description" value={projectForm.description} onChange={updateForm(setProjectForm)} />
        <button className="primary-btn" disabled={ws.loading || !selectedOrganization}>Create</button>
      </form>
      <div className="toolbar">
        <input className="search-box" placeholder="Search projects by name or description…" value={search} onChange={(event) => setSearch(event.target.value)} />
      </div>
      {editingProject && (
        <form className="edit-form" onSubmit={saveEdit}>
          <strong>Edit {editingProject.name}</strong>
          <input name="name" value={editForm.name} onChange={updateForm(setEditForm)} required />
          <input name="description" placeholder="Description" value={editForm.description} onChange={updateForm(setEditForm)} />
          <select name="status" value={editForm.status} onChange={updateForm(setEditForm)}>
            {PROJECT_STATUSES.map((status) => <option key={status}>{status}</option>)}
          </select>
          <div className="form-actions">
            <button className="primary-btn" type="submit" disabled={ws.loading}>Save</button>
            <button type="button" className="secondary-btn" onClick={() => setEditingProject(null)}>Cancel</button>
          </div>
        </form>
      )}
      <div className="org-list">
        {pageData.items.map((project) => (
          <div className={`org-item ${selectedProject === project.id ? 'selected' : ''}`} key={project.id}>
            <button type="button" className="org-select" onClick={() => setSelectedProject(project.id)}>
              <strong>{project.name}</strong>
              <span>{project.status} · {project.description || 'No description provided'}</span>
            </button>
            <div className="row-actions">
              <button type="button" className="small-btn" onClick={() => startEdit(project)}>Edit</button>
              <button type="button" className="small-btn danger" onClick={() => deleteProject(project)}>Delete</button>
            </div>
          </div>
        ))}
        {pageData.items.length === 0 && <p>No projects found.</p>}
      </div>
      <Pagination page={pageData.number} totalPages={pageData.totalPages} totalElements={pageData.totalElements} onPageChange={setPage} />
    </section>
  )
}
