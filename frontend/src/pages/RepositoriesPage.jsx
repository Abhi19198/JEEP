import { useEffect, useState } from 'react'
import { PanelTitle } from '../components/Brand.jsx'

const emptyRepository = { provider: 'github', remoteUrl: '', defaultBranch: 'main' }
const PROVIDERS = [
  { value: 'github', label: 'GitHub' },
  { value: 'gitlab', label: 'GitLab' },
  { value: 'bitbucket', label: 'Bitbucket' },
  { value: 'azure-devops', label: 'Azure DevOps' },
]

const idleSection = { status: 'idle', message: '', items: [] }

function formatDate(value) {
  if (!value) return ''
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString()
}

function ActivitySection({ title, section, children }) {
  return (
    <div>
      <h4>{title}</h4>
      {section.status === 'loading' && <p>Loading…</p>}
      {section.status === 'unsupported' && <p>{section.message}</p>}
      {section.status === 'error' && <p>Could not load {title.toLowerCase()}: {section.message}</p>}
      {section.status === 'ok' && section.items.length === 0 && <p>No {title.toLowerCase()} found.</p>}
      {section.status === 'ok' && section.items.length > 0 && children(section.items)}
    </div>
  )
}

export function RepositoriesPage({ ws }) {
  const { api, projects, selectedProject, setSelectedProject, setLoading, setError } = ws
  const [repoForm, setRepoForm] = useState(emptyRepository)
  const [repositories, setRepositories] = useState([])
  const [selectedRepoId, setSelectedRepoId] = useState('')
  const [commits, setCommits] = useState(idleSection)
  const [pullRequests, setPullRequests] = useState(idleSection)
  const [contributors, setContributors] = useState(idleSection)

  useEffect(() => {
    if (!selectedProject) { setRepositories([]); setSelectedRepoId(''); return }
    let cancelled = false
    api(`/repositories/project/${selectedProject}`)
      .then((result) => { if (!cancelled) setRepositories(result || []) })
      .catch((reason) => { if (!cancelled) setError(reason.message) })
    return () => { cancelled = true }
  }, [api, selectedProject, setError])

  const selectedRepo = repositories.find((repo) => repo.id === selectedRepoId) || null

  useEffect(() => {
    if (!selectedRepoId) {
      setCommits(idleSection)
      setPullRequests(idleSection)
      setContributors(idleSection)
      return
    }
    let cancelled = false
    const loadSection = async (path, key, setSection) => {
      setSection({ status: 'loading', message: '', items: [] })
      try {
        const result = await api(`/repositories/${selectedRepoId}/github/${path}`)
        if (cancelled) return
        if (result?.supported === false) {
          setSection({ status: 'unsupported', message: result.message || 'GitHub activity is not supported for this repository.', items: [] })
        } else {
          setSection({ status: 'ok', message: '', items: result?.[key] || [] })
        }
      } catch (reason) {
        if (!cancelled) setSection({ status: 'error', message: reason.message, items: [] })
      }
    }
    loadSection('commits', 'commits', setCommits)
    loadSection('pull-requests', 'pullRequests', setPullRequests)
    loadSection('contributors', 'contributors', setContributors)
    return () => { cancelled = true }
  }, [api, selectedRepoId])

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
            <button
              className="primary-btn"
              type="button"
              onClick={() => setSelectedRepoId(selectedRepoId === repo.id ? '' : repo.id)}
            >
              {selectedRepoId === repo.id ? 'Hide GitHub activity' : 'View GitHub activity'}
            </button>
          </div>
        ))}
        {repositories.length === 0 && <p>No repositories linked to this project yet.</p>}
      </div>
      {selectedRepo && (
        <div>
          <h3>GitHub activity — {selectedRepo.remoteUrl}</h3>
          <p>Live data from the public GitHub API (unauthenticated, 60 requests/hour). Other providers show a “not supported” note instead.</p>
          <ActivitySection title="Recent commits" section={commits}>
            {(items) => (
              <div className="org-list">
                {items.map((commit) => (
                  <div className="org-item" key={commit.sha}>
                    <strong>{commit.message}</strong>
                    <span>{commit.authorName || commit.authorLogin || 'Unknown author'}{commit.authorLogin ? ` (@${commit.authorLogin})` : ''} · {formatDate(commit.date)}</span>
                    <span>{commit.sha?.slice(0, 7)}{commit.url ? <> · <a href={commit.url} target="_blank" rel="noreferrer">View on GitHub</a></> : null}</span>
                  </div>
                ))}
              </div>
            )}
          </ActivitySection>
          <ActivitySection title="Open pull requests" section={pullRequests}>
            {(items) => (
              <div className="org-list">
                {items.map((pr) => (
                  <div className="org-item" key={pr.number}>
                    <strong>#{pr.number} {pr.title}</strong>
                    <span>{pr.authorLogin ? `@${pr.authorLogin}` : 'Unknown author'} · {pr.state} · {formatDate(pr.createdAt)}</span>
                    {pr.url && <span><a href={pr.url} target="_blank" rel="noreferrer">View on GitHub</a></span>}
                  </div>
                ))}
              </div>
            )}
          </ActivitySection>
          <ActivitySection title="Contributors" section={contributors}>
            {(items) => (
              <div className="org-list">
                {items.map((contributor) => (
                  <div className="org-item" key={contributor.login}>
                    {contributor.avatarUrl && <img src={contributor.avatarUrl} alt={`${contributor.login} avatar`} width="32" height="32" />}
                    <strong>@{contributor.login}</strong>
                    <span>{contributor.contributions} contributions</span>
                  </div>
                ))}
              </div>
            )}
          </ActivitySection>
        </div>
      )}
    </section>
  )
}
