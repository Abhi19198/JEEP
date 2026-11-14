// Minimal STOMP 1.2 client over a native WebSocket, plus a small hub that
// owns one connection for the logged-in session.
//
// No extra dependency: the backend speaks plain STOMP on /ws, so a frame
// parser is enough, and it avoids pulling a STOMP library into the bundle.
// The backend authenticates the socket from the Authorization header of
// the STOMP CONNECT frame (browsers cannot set headers on the WebSocket
// handshake itself) and authorizes every subscription server-side.
//
// Behaviour: subscriptions made before the socket exists are queued and
// replayed on (re)connect; a dropped socket reconnects with backoff.
// When the socket is down, nothing breaks — pages keep working from
// their REST loads, which remain the fallback.

export function websocketUrl() {
  const apiBase = (import.meta.env.VITE_API_BASE_URL || '/api/v1').replace(/\/$/, '')
  if (/^https?:\/\//.test(apiBase)) {
    const url = new URL(apiBase)
    return `${url.protocol === 'https:' ? 'wss:' : 'ws:'}//${url.host}/ws`
  }
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/ws`
}

function createStompConnection(url, token, { onConnect, onDisconnect } = {}) {
  let socket = null
  let connected = false
  let closed = false
  let reconnectDelay = 1000
  let reconnectTimer = null
  let nextSubId = 0
  let buffer = ''
  const subscriptions = new Map() // id -> { destination, callback }

  const sendFrame = (command, headers = {}, body = '') => {
    if (!socket || socket.readyState !== WebSocket.OPEN) return
    const headerText = Object.entries(headers).map(([key, value]) => `${key}:${value}`).join('\n')
    socket.send(`${command}\n${headerText}\n\n${body}\0`)
  }

  const subscribeAll = () => {
    for (const [id, sub] of subscriptions) {
      sendFrame('SUBSCRIBE', { id, destination: sub.destination, ack: 'auto' })
    }
  }

  const handleFrame = (raw) => {
    const splitAt = raw.indexOf('\n\n')
    if (splitAt === -1) return
    const [command, ...headerLines] = raw.slice(0, splitAt).split('\n')
    const body = raw.slice(splitAt + 2)
    const headers = {}
    for (const line of headerLines) {
      const colon = line.indexOf(':')
      if (colon > 0) headers[line.slice(0, colon)] = line.slice(colon + 1)
    }
    if (command === 'CONNECTED') {
      connected = true
      reconnectDelay = 1000
      subscribeAll()
      onConnect?.()
    } else if (command === 'MESSAGE') {
      const sub = subscriptions.get(headers.subscription)
      if (!sub) return
      try {
        sub.callback(JSON.parse(body || 'null'))
      } catch {
        // Malformed frame payload: ignore it; the next REST load repairs state.
      }
    } else if (command === 'ERROR') {
      onDisconnect?.(body)
    }
  }

  const connect = () => {
    if (closed) return
    socket = new WebSocket(url)
    socket.onopen = () => sendFrame('CONNECT', {
      'accept-version': '1.2',
      host: window.location.host,
      Authorization: `Bearer ${token}`,
      'heart-beat': '0,0',
    })
    socket.onmessage = (event) => {
      buffer += event.data
      let end
      while ((end = buffer.indexOf('\0')) !== -1) {
        const raw = buffer.slice(0, end).replace(/^\n+/, '')
        buffer = buffer.slice(end + 1)
        if (raw.trim()) handleFrame(raw)
      }
    }
    socket.onclose = () => {
      if (connected) {
        connected = false
        onDisconnect?.()
      }
      if (!closed) {
        reconnectTimer = setTimeout(connect, reconnectDelay)
        reconnectDelay = Math.min(reconnectDelay * 2, 30000)
      }
    }
    // onerror needs no handler: onclose follows and drives the reconnect.
  }

  const subscribe = (destination, callback) => {
    const id = `sub-${nextSubId++}`
    subscriptions.set(id, { destination, callback })
    if (connected) sendFrame('SUBSCRIBE', { id, destination, ack: 'auto' })
    return () => {
      subscriptions.delete(id)
      if (connected) sendFrame('UNSUBSCRIBE', { id })
    }
  }

  const disconnect = () => {
    closed = true
    clearTimeout(reconnectTimer)
    if (socket) socket.close()
  }

  connect()
  return { subscribe, disconnect, isConnected: () => connected }
}

// One hub for the app lifetime. Pages subscribe any time — even before a
// socket exists — and connect(token) with a fresh token replaces the old
// connection while keeping every subscription.
export function createLiveHub({ onConnect, onDisconnect } = {}) {
  let connection = null
  let connectionToken = null
  let nextId = 0
  const subscriptions = new Map() // hub id -> { destination, callback, connectionUnsubscribe }

  const subscribeThroughConnection = (entry) => {
    if (connection) entry.connectionUnsubscribe = connection.subscribe(entry.destination, entry.callback)
  }

  return {
    connect(url, token) {
      if (connection && connectionToken === token) return
      if (connection) connection.disconnect()
      connectionToken = token
      connection = createStompConnection(url, token, { onConnect, onDisconnect })
      for (const entry of subscriptions.values()) subscribeThroughConnection(entry)
    },
    disconnect() {
      if (connection) {
        connection.disconnect()
        connection = null
        connectionToken = null
      }
      for (const entry of subscriptions.values()) entry.connectionUnsubscribe = null
    },
    subscribe(destination, callback) {
      const id = nextId++
      const entry = { destination, callback, connectionUnsubscribe: null }
      subscriptions.set(id, entry)
      subscribeThroughConnection(entry)
      return () => {
        subscriptions.delete(id)
        entry.connectionUnsubscribe?.()
      }
    },
    isConnected() {
      return connection?.isConnected() ?? false
    },
  }
}
