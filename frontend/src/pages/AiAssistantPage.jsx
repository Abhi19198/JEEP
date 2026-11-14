import { useState } from 'react'
import { PanelTitle } from '../components/Brand.jsx'

export function AiAssistantPage({ ws }) {
  const { api, projects, selectedProject, setSelectedProject, setLoading, setError } = ws
  const [aiForm, setAiForm] = useState({ prompt: '', type: 'risk_review' })
  const [aiResponse, setAiResponse] = useState(null)

  const updateForm = (setter) => (event) => setter((current) => ({ ...current, [event.target.name]: event.target.value }))

  const getSuggestion = async (event) => {
    event.preventDefault()
    setLoading(true)
    try {
      setAiResponse(await api('/ai/suggest', { method: 'POST', body: JSON.stringify({ ...aiForm, projectId: selectedProject }) }))
    } catch (reason) {
      setError(reason.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="panel">
      <PanelTitle title="AI Assistant" />
      <select value={selectedProject} onChange={(event) => setSelectedProject(event.target.value)}>
        <option value="">Choose project</option>
        {projects.map((project) => <option key={project.id} value={project.id}>{project.name}</option>)}
      </select>
      <form className="auth-form" onSubmit={getSuggestion}>
        <label>Suggestion type<input name="type" value={aiForm.type} onChange={updateForm(setAiForm)} required /></label>
        <label>Prompt<textarea name="prompt" value={aiForm.prompt} onChange={updateForm(setAiForm)} required placeholder="Describe the project risk or decision" /></label>
        <button className="primary-btn" disabled={ws.loading || !selectedProject}>Generate suggestion</button>
      </form>
      {aiResponse && (
        <div className="ai-card">
          <p>{aiResponse.suggestion}</p>
          <div className="ai-stats">
            <div><strong>{Math.round(aiResponse.confidence * 100)}%</strong><span>confidence</span></div>
            <div><strong>{aiResponse.type}</strong><span>type</span></div>
          </div>
        </div>
      )}
    </section>
  )
}
