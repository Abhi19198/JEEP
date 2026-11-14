import { useState } from 'react'
import { PanelTitle } from '../components/Brand.jsx'

const emptyOrganization = { name: '', description: '' }

export function OrganizationsPage({ ws }) {
  const { api, organizations, selectedOrganization, setSelectedOrganization, setLoading, setError, refreshOrganizations } = ws
  const [organizationForm, setOrganizationForm] = useState(emptyOrganization)
  const [editingOrg, setEditingOrg] = useState(null)
  const [editForm, setEditForm] = useState(emptyOrganization)

  const updateForm = (setter) => (event) => setter((current) => ({ ...current, [event.target.name]: event.target.value }))

  const createOrganization = async (event) => {
    event.preventDefault()
    setLoading(true)
    setError('')
    try {
      await api('/organizations', { method: 'POST', body: JSON.stringify(organizationForm) })
      setOrganizationForm(emptyOrganization)
      await refreshOrganizations()
    } catch (reason) {
      setError(reason.message)
    } finally {
      setLoading(false)
    }
  }

  const startEdit = (org) => {
    setEditingOrg(org)
    setEditForm({ name: org.name, description: org.description || '' })
  }

  const saveEdit = async (event) => {
    event.preventDefault()
    setLoading(true)
    setError('')
    try {
      await api(`/organizations/${editingOrg.id}`, { method: 'PUT', body: JSON.stringify(editForm) })
      setEditingOrg(null)
      await refreshOrganizations()
    } catch (reason) {
      setError(reason.message)
    } finally {
      setLoading(false)
    }
  }

  const deleteOrganization = async (org) => {
    if (!window.confirm(`Delete organization "${org.name}"? This deactivates it.`)) return
    setLoading(true)
    setError('')
    try {
      await api(`/organizations/${org.id}`, { method: 'DELETE' })
      if (selectedOrganization === org.id) setSelectedOrganization('')
      setEditingOrg(null)
      await refreshOrganizations()
    } catch (reason) {
      setError(reason.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="panel">
      <PanelTitle title="Organizations" />
      <form className="inline-form" onSubmit={createOrganization}>
        <input name="name" placeholder="Organization name" value={organizationForm.name} onChange={updateForm(setOrganizationForm)} required />
        <input name="description" placeholder="Description" value={organizationForm.description} onChange={updateForm(setOrganizationForm)} />
        <button className="primary-btn" disabled={ws.loading}>Create</button>
      </form>
      {editingOrg && (
        <form className="edit-form" onSubmit={saveEdit}>
          <strong>Edit {editingOrg.name}</strong>
          <input name="name" value={editForm.name} onChange={updateForm(setEditForm)} required />
          <input name="description" placeholder="Description" value={editForm.description} onChange={updateForm(setEditForm)} />
          <div className="form-actions">
            <button className="primary-btn" type="submit" disabled={ws.loading}>Save</button>
            <button type="button" className="secondary-btn" onClick={() => setEditingOrg(null)}>Cancel</button>
          </div>
        </form>
      )}
      <div className="org-list">
        {organizations.map((org) => (
          <div className={`org-item ${selectedOrganization === org.id ? 'selected' : ''}`} key={org.id}>
            <button type="button" className="org-select" onClick={() => setSelectedOrganization(org.id)}>
              <strong>{org.name}</strong>
              <span>{org.description || 'No description provided'}</span>
            </button>
            <div className="row-actions">
              <button type="button" className="small-btn" onClick={() => startEdit(org)}>Edit</button>
              <button type="button" className="small-btn danger" onClick={() => deleteOrganization(org)}>Delete</button>
            </div>
          </div>
        ))}
        {organizations.length === 0 && <p>No organizations yet. Create one above.</p>}
      </div>
    </section>
  )
}
