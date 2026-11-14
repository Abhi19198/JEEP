import { useEffect, useState } from 'react'
import { PanelTitle } from '../components/Brand.jsx'
import { Pagination } from '../components/Pagination.jsx'
import { pageContent } from '../api.js'

const emptyTask = { title: '', description: '', priority: 'MEDIUM', status: 'TODO' }
const KANBAN_STATUSES = ['TODO', 'IN_PROGRESS', 'REVIEW', 'DONE']
const COLUMN_LABELS = { TODO: 'To Do', IN_PROGRESS: 'In Progress', REVIEW: 'Review', DONE: 'Done' }
const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']
const PAGE_SIZE = 12

const matchesSearch = (task, search) => {
  if (!search.trim()) return true
  const query = search.trim().toLowerCase()
  return task.title?.toLowerCase().includes(query) || (task.description || '').toLowerCase().includes(query)
}

// Merges one live task event into the currently loaded page. Events for
// tasks on other pages are ignored — the REST load fetches those when the
// user navigates there. Events never trigger a refetch: the payload
// carries the full updated task.
const applyTaskEvent = (current, event, search, page) => {
  const task = event.task
  if (!task?.id) return current
  const present = current.items.some((item) => item.id === task.id)
  if (event.type === 'TASK_DELETED') {
    if (!present) return current
    return { ...current, items: current.items.filter((item) => item.id !== task.id), totalElements: Math.max(0, current.totalElements - 1) }
  }
  if (event.type === 'TASK_UPDATED') {
    if (!present) return current
    if (!matchesSearch(task, search)) {
      return { ...current, items: current.items.filter((item) => item.id !== task.id), totalElements: Math.max(0, current.totalElements - 1) }
    }
    return { ...current, items: current.items.map((item) => (item.id === task.id ? task : item)) }
  }
  if (event.type === 'TASK_CREATED') {
    if (page !== 0 || present || !matchesSearch(task, search)) return current
    return { ...current, items: [task, ...current.items].slice(0, PAGE_SIZE), totalElements: current.totalElements + 1 }
  }
  return current
}

export function TasksPage({ ws }) {
  const { api, projects, selectedProject, setSelectedProject, setLoading, setError } = ws
  const [taskForm, setTaskForm] = useState(emptyTask)
  const [editingTask, setEditingTask] = useState(null)
  const [editForm, setEditForm] = useState({ title: '', description: '', priority: 'MEDIUM', status: 'TODO', assigneeId: '' })
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
    if (!selectedProject) { setPageData({ items: [], totalPages: 1, totalElements: 0, number: 0 }); return }
    let cancelled = false
    const params = new URLSearchParams({ page: String(page), size: String(PAGE_SIZE) })
    if (appliedSearch.trim()) params.set('search', appliedSearch.trim())
    api(`/tasks/project/${selectedProject}?${params}`)
      .then((result) => { if (!cancelled) setPageData(pageContent(result)) })
      .catch((reason) => { if (!cancelled) setError(reason.message) })
    return () => { cancelled = true }
  }, [api, selectedProject, appliedSearch, page, setError])

  // Live updates: merge task events for this project into the loaded page.
  // REST above stays the source of truth on load and the fallback when
  // the socket is down.
  useEffect(() => {
    if (!selectedProject) return undefined
    return ws.live.subscribe(`/topic/project/${selectedProject}`, (event) => {
      setPageData((current) => applyTaskEvent(current, event, appliedSearch, page))
    })
  }, [selectedProject, appliedSearch, page, ws.live])

  const updateForm = (setter) => (event) => setter((current) => ({ ...current, [event.target.name]: event.target.value }))

  const reload = (pageToLoad = page) => {
    if (!selectedProject) return Promise.resolve()
    const params = new URLSearchParams({ page: String(pageToLoad), size: String(PAGE_SIZE) })
    if (appliedSearch.trim()) params.set('search', appliedSearch.trim())
    return api(`/tasks/project/${selectedProject}?${params}`).then((result) => setPageData(pageContent(result)))
  }

  const createTask = async (event) => {
    event.preventDefault()
    setLoading(true)
    setError('')
    try {
      await api('/tasks', { method: 'POST', body: JSON.stringify({ ...taskForm, projectId: selectedProject }) })
      setTaskForm(emptyTask)
      setPage(0)
      await reload(0)
    } catch (reason) {
      setError(reason.message)
    } finally {
      setLoading(false)
    }
  }

  const moveTask = async (task, status) => {
    if (task.status === status) return
    setLoading(true)
    setError('')
    try {
      await api(`/tasks/${task.id}`, {
        method: 'PUT',
        body: JSON.stringify({ title: task.title, description: task.description, priority: task.priority, status, assigneeId: task.assigneeId }),
      })
      await reload()
    } catch (reason) {
      setError(reason.message)
    } finally {
      setLoading(false)
    }
  }

  const startEdit = (task) => {
    setEditingTask(task)
    setEditForm({ title: task.title, description: task.description || '', priority: task.priority, status: task.status, assigneeId: task.assigneeId || '' })
  }

  const saveEdit = async (event) => {
    event.preventDefault()
    setLoading(true)
    setError('')
    try {
      await api(`/tasks/${editingTask.id}`, {
        method: 'PUT',
        body: JSON.stringify({ ...editForm, assigneeId: editForm.assigneeId || null }),
      })
      setEditingTask(null)
      await reload()
    } catch (reason) {
      setError(reason.message)
    } finally {
      setLoading(false)
    }
  }

  const deleteTask = async (task) => {
    if (!window.confirm(`Delete task "${task.title}"?`)) return
    setLoading(true)
    setError('')
    try {
      await api(`/tasks/${task.id}`, { method: 'DELETE' })
      setEditingTask(null)
      await reload()
    } catch (reason) {
      setError(reason.message)
    } finally {
      setLoading(false)
    }
  }

  const tasksByStatus = (status) => pageData.items.filter((task) => task.status === status)
  const otherTasks = pageData.items.filter((task) => !KANBAN_STATUSES.includes(task.status))

  const renderCard = (task) => (
    <article className="kanban-card" key={task.id}>
      <strong>{task.title}</strong>
      {task.description && <span className="card-desc">{task.description}</span>}
      <div className="card-meta">
        <span className={`chip priority-${task.priority.toLowerCase()}`}>{task.priority}</span>
        <span className="assignee">Assignee: {task.assigneeId ? task.assigneeId.slice(0, 8) : 'Unassigned'}</span>
      </div>
      <div className="card-moves">
        {KANBAN_STATUSES.filter((status) => status !== task.status).map((status) => (
          <button key={status} type="button" className="move-btn" title={`Move to ${COLUMN_LABELS[status]}`} onClick={() => moveTask(task, status)}>
            → {COLUMN_LABELS[status]}
          </button>
        ))}
      </div>
      <div className="row-actions">
        <button type="button" className="small-btn" onClick={() => startEdit(task)}>Edit</button>
        <button type="button" className="small-btn danger" onClick={() => deleteTask(task)}>Delete</button>
      </div>
    </article>
  )

  return (
    <section className="panel">
      <PanelTitle title="Tasks" />
      <p className="live-status">{ws.liveConnected ? '● Live updates on' : '○ Live updates off — showing last loaded data'}</p>
      <select value={selectedProject} onChange={(event) => { setSelectedProject(event.target.value); setPage(0) }}>
        <option value="">Choose project</option>
        {projects.map((project) => <option key={project.id} value={project.id}>{project.name}</option>)}
      </select>
      <form className="inline-form" onSubmit={createTask}>
        <input name="title" placeholder="Task title" value={taskForm.title} onChange={updateForm(setTaskForm)} required />
        <select name="priority" value={taskForm.priority} onChange={updateForm(setTaskForm)}>
          {PRIORITIES.map((priority) => <option key={priority}>{priority}</option>)}
        </select>
        <select name="status" value={taskForm.status} onChange={updateForm(setTaskForm)}>
          {KANBAN_STATUSES.map((status) => <option key={status} value={status}>{COLUMN_LABELS[status]}</option>)}
        </select>
        <input name="description" placeholder="Description (optional)" value={taskForm.description} onChange={updateForm(setTaskForm)} />
        <button className="primary-btn" disabled={ws.loading || !selectedProject}>Create</button>
      </form>
      <div className="toolbar">
        <input className="search-box" placeholder="Search tasks by title or description…" value={search} onChange={(event) => setSearch(event.target.value)} />
      </div>
      {editingTask && (
        <form className="edit-form" onSubmit={saveEdit}>
          <strong>Edit {editingTask.title}</strong>
          <input name="title" value={editForm.title} onChange={updateForm(setEditForm)} required />
          <input name="description" placeholder="Description" value={editForm.description} onChange={updateForm(setEditForm)} />
          <select name="priority" value={editForm.priority} onChange={updateForm(setEditForm)}>
            {PRIORITIES.map((priority) => <option key={priority}>{priority}</option>)}
          </select>
          <select name="status" value={editForm.status} onChange={updateForm(setEditForm)}>
            {[...KANBAN_STATUSES, 'BLOCKED', 'CANCELLED'].map((status) => <option key={status} value={status}>{COLUMN_LABELS[status] || status}</option>)}
          </select>
          <input name="assigneeId" placeholder="Assignee user id (optional)" value={editForm.assigneeId} onChange={updateForm(setEditForm)} />
          <div className="form-actions">
            <button className="primary-btn" type="submit" disabled={ws.loading}>Save</button>
            <button type="button" className="secondary-btn" onClick={() => setEditingTask(null)}>Cancel</button>
          </div>
        </form>
      )}
      {!selectedProject && <p>Choose a project to see its tasks.</p>}
      {selectedProject && (
        <>
          <div className="kanban-board">
            {KANBAN_STATUSES.map((status) => (
              <div className="kanban-column" key={status}>
                <h4>{COLUMN_LABELS[status]} <span className="column-count">{tasksByStatus(status).length}</span></h4>
                {tasksByStatus(status).map(renderCard)}
                {tasksByStatus(status).length === 0 && <p className="column-empty">No tasks</p>}
              </div>
            ))}
            {otherTasks.length > 0 && (
              <div className="kanban-column" key="OTHER">
                <h4>Other <span className="column-count">{otherTasks.length}</span></h4>
                {otherTasks.map(renderCard)}
              </div>
            )}
          </div>
          <Pagination page={pageData.number} totalPages={pageData.totalPages} totalElements={pageData.totalElements} onPageChange={setPage} />
        </>
      )}
    </section>
  )
}
