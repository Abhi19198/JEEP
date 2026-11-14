import { useEffect, useState } from 'react'
import { PanelTitle } from '../components/Brand.jsx'

export function TeamsPage({ ws }) {
  const { api, organizations, selectedOrganization, setSelectedOrganization, setLoading, setError } = ws
  const [teamForm, setTeamForm] = useState({ name: '' })
  const [teams, setTeams] = useState([])
  const [selectedTeam, setSelectedTeam] = useState(null)
  const [members, setMembers] = useState([])

  useEffect(() => {
    if (!selectedOrganization) { setTeams([]); setSelectedTeam(null); return }
    let cancelled = false
    api(`/teams/organization/${selectedOrganization}`)
      .then((result) => { if (!cancelled) setTeams(result || []) })
      .catch((reason) => { if (!cancelled) setError(reason.message) })
    return () => { cancelled = true }
  }, [api, selectedOrganization, setError])

  const createTeam = async (event) => {
    event.preventDefault()
    setLoading(true)
    setError('')
    try {
      await api('/teams', { method: 'POST', body: JSON.stringify({ ...teamForm, organizationId: selectedOrganization }) })
      setTeamForm({ name: '' })
      setTeams(await api(`/teams/organization/${selectedOrganization}`))
    } catch (reason) {
      setError(reason.message)
    } finally {
      setLoading(false)
    }
  }

  const viewMembers = async (team) => {
    setSelectedTeam(team)
    setLoading(true)
    setError('')
    try {
      setMembers(await api(`/teams/${team.id}/members`))
    } catch (reason) {
      setError(reason.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="panel">
      <PanelTitle title="Teams" />
      <select value={selectedOrganization} onChange={(event) => setSelectedOrganization(event.target.value)}>
        <option value="">Choose organization</option>
        {organizations.map((org) => <option key={org.id} value={org.id}>{org.name}</option>)}
      </select>
      <form className="inline-form" onSubmit={createTeam}>
        <input name="name" placeholder="Team name" value={teamForm.name} onChange={(event) => setTeamForm({ name: event.target.value })} required />
        <button className="primary-btn" disabled={ws.loading || !selectedOrganization}>Create team</button>
      </form>
      <div className="org-list">
        {teams.map((team) => (
          <div className={`org-item ${selectedTeam?.id === team.id ? 'selected' : ''}`} key={team.id}>
            <button type="button" className="org-select" onClick={() => viewMembers(team)}>
              <strong>{team.name}</strong>
              <span>Created {team.createdAt ? new Date(team.createdAt).toLocaleDateString() : ''}</span>
            </button>
            <div className="row-actions">
              <button type="button" className="small-btn" onClick={() => viewMembers(team)}>View members</button>
            </div>
          </div>
        ))}
        {teams.length === 0 && <p>No teams yet. Create one above.</p>}
      </div>
      {selectedTeam && (
        <section className="panel org-panel">
          <PanelTitle title={`Members — ${selectedTeam.name}`} />
          <div className="org-list">
            {members.map((member) => (
              <div className="org-item" key={member.userId}>
                <strong>{member.firstName} {member.lastName}</strong>
                <span>{member.email} · {member.role}</span>
              </div>
            ))}
            {members.length === 0 && <p>No members in this team.</p>}
          </div>
        </section>
      )}
    </section>
  )
}
