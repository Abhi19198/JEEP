import { useEffect, useState } from 'react'
import { PanelTitle } from '../components/Brand.jsx'

const emptyRepository = { provider: 'github', remoteUrl: '', defaultBranch: 'main' }
const PROVIDERS = [
  { value: 'github', label: 'GitHub' },
  { value: 'gitlab', label: 'GitLab' },
  { value: 'bitbucket', label: 'Bitbucket' },
  { value: 'azure-devops', label: 'Azure DevOps' },
]

export function RepositoriesPage({ ws }) {
  const { api, projects, selectedProject, setSelectedProject, setLoading, setError } = ws
  const [repoForm, setRepoForm] = useState(emptyRepository)
  const [repositories, setRepositories] = useState([])

  useEffect(() => {
    if (!selectedProject) { setRepositories([]); return }
    let cancelled = false
    api(`/repositories/project/${selectedProject}`)
      .then((result) => { if (!cancelled) setRepositories(result || []) })
      .catch((reason) => { if (!cancelled) setError(reason.message) })
    return () => { cancelled = true }
  }, [api, selectedProject, setError])

  const updateForm = (setter) => (event) => setter((current) => ({ ...current, [event.target.name]: event.target.value }))

  const linkRepository = async (event) => {
    event.preventDefault()
    setLoading(true)
    setError('')
    try {
      await api('/repositories', { method: 'POST', body: JSON.stringify({ ...repoForm, projectId: selectedProject }) })
      setRepoForm(emptyRepository)
      setRepositories(await api(`/repositories/project/${selectedProject}`))
    } catch (reason) {
      setError(reason.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="panel">
      <PanelTitle title="Repositories" />
      <select value={selectedProject} onChange={(event) => setSelectedProject(event.target.value)}>
        <option value="">Choose project</option>
        {projects.map((project) => <option key={project.id} value={project.id}>{project.name}</option>)}
      </select>
      <form className="inline-form" onSubmit={linkRepository}>
        <select name="provider" value={repoForm.provider} onChange={updateForm(setRepoForm)}>
          {PROVIDERS.map((provider) => <option key={provider.value} value={provider.value}>{provider.label}</option>)}
        </select>
        <input name="remoteUrl" placeholder="Remote URL (https://…)" value={repoForm.remoteUrl} onChange={updateForm(setRepoForm)} required />
        <input name="defaultBranch" placeholder="Default branch" value={repoForm.defaultBranch} onChange={updateForm(setRepoForm)} required />
        <button className="primary-btn" disabled={ws.loading || !selectedProject}>Link repository</button>
      </form>
      <div className="org-list">
        {repositories.map((repo) => (
          <div className="org-item" key={repo.id}>
            <strong>{repo.provider} · {repo.defaultBranch}</strong>
            <span>{repo.remoteUrl}</span>
          </div>
        ))}
        {repositories.length === 0 && <p>No repositories linked to this project yet.</p>}
      </div>
    </section>
  )
}
