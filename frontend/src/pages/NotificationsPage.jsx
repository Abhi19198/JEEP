import { useEffect, useState } from 'react'
import { PanelTitle } from '../components/Brand.jsx'

export function NotificationsPage({ ws }) {
  const { api, session, setLoading, setError } = ws
  const [notifications, setNotifications] = useState([])

  // Live updates: new notifications for this user are prepended as they
  // arrive. REST below stays the source of truth on load and the
  // fallback when the socket is down.
  useEffect(() => {
    if (!session?.userId) return undefined
    return ws.live.subscribe(`/topic/user/${session.userId}/notifications`, (notification) => {
      if (!notification?.id) return
      setNotifications((current) => (current.some((item) => item.id === notification.id)
        ? current
        : [notification, ...current]))
    })
  }, [session?.userId, ws.live])

  const loadNotifications = async () => {
    setNotifications((await api('/notifications')) || [])
  }

  useEffect(() => {
    let cancelled = false
    api('/notifications')
      .then((result) => { if (!cancelled) setNotifications(result || []) })
      .catch((reason) => { if (!cancelled) setError(reason.message) })
    return () => { cancelled = true }
  }, [api, setError])

  const markAsRead = async (notification) => {
    setLoading(true)
    setError('')
    try {
      await api(`/notifications/${notification.id}/read`, { method: 'PUT' })
      await loadNotifications()
    } catch (reason) {
      setError(reason.message)
    } finally {
      setLoading(false)
    }
  }

  const unreadCount = notifications.filter((notification) => !notification.read).length

  return (
    <section className="panel">
      <PanelTitle title={`Notifications${unreadCount > 0 ? ` (${unreadCount} unread)` : ''}`} />
      <p className="live-status">{ws.liveConnected ? '● Live updates on' : '○ Live updates off — showing last loaded data'}</p>
      <div className="org-list">
        {notifications.map((notification) => (
          <div className={`org-item ${notification.read ? '' : 'unread'}`} key={notification.id}>
            <strong>{notification.title}</strong>
            <span>{notification.message || 'No message'}</span>
            <span>{notification.type} · {notification.channel}{notification.createdAt ? ` · ${new Date(notification.createdAt).toLocaleString()}` : ''}</span>
            {!notification.read && (
              <div className="row-actions">
                <button type="button" className="small-btn" disabled={ws.loading} onClick={() => markAsRead(notification)}>Mark as read</button>
              </div>
            )}
          </div>
        ))}
        {notifications.length === 0 && <p>No notifications yet. Assign a task or change its status and the assignee will be notified automatically.</p>}
      </div>
    </section>
  )
}
