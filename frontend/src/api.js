const API_BASE = (import.meta.env.VITE_API_BASE_URL || '/api/v1').replace(/\/$/, '')
const SESSION_KEY = 'jeep-session'

export { SESSION_KEY }

export async function requestJson(path, options = {}) {
  const { token, _retried, headers: extraHeaders, ...fetchOptions } = options
  const response = await fetch(`${API_BASE}${path}`, {
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}), ...(extraHeaders || {}) },
    ...fetchOptions,
  })
  const contentType = response.headers.get('content-type') || ''
  const payload = contentType.includes('application/json') ? await response.json() : null
  if (!response.ok) {
    const error = new Error(payload?.message || payload?.error || 'Request failed')
    error.status = response.status
    throw error
  }
  return payload
}

// Authenticated requests go through here: on a 401 the access token is
// refreshed once with the stored refresh token and the request is retried.
// If the refresh also fails, the session is expired and the user is logged out.
export function createApiClient({ getSession, onSessionRefreshed, onSessionExpired }) {
  const apiRequest = async (path, options = {}) => {
    const session = getSession()
    try {
      return await requestJson(path, { ...options, token: options.token || session?.token })
    } catch (reason) {
      if (reason.status !== 401 || !session?.refreshToken || options._retried) throw reason
      try {
        const refreshed = await requestJson('/auth/refresh', {
          method: 'POST',
          body: JSON.stringify({ refreshToken: session.refreshToken }),
        })
        const nextSession = { ...session, token: refreshed.accessToken, refreshToken: refreshed.refreshToken || session.refreshToken }
        onSessionRefreshed(nextSession)
        return await requestJson(path, { ...options, token: nextSession.token, _retried: true })
      } catch (refreshReason) {
        onSessionExpired()
        throw refreshReason
      }
    }
  }
  return apiRequest
}

// Backend list endpoints for tasks/projects return a Spring Page:
// { content: [...], totalPages, totalElements, number, size, ... }.
// Older list endpoints return a plain array. Normalize both shapes.
export function pageContent(result) {
  if (Array.isArray(result)) return { items: result, totalPages: 1, totalElements: result.length, number: 0 }
  return {
    items: result?.content || [],
    totalPages: result?.totalPages ?? 1,
    totalElements: result?.totalElements ?? (result?.content?.length || 0),
    number: result?.number ?? 0,
  }
}
