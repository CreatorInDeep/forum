import { useEffect, useMemo, useState } from 'react';
import {
  ArrowLeft,
  BookOpen,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Clock3,
  DatabaseBackup,
  Edit3,
  Eye,
  FileJson2,
  Hash,
  KeyRound,
  Lock,
  LogIn,
  LogOut,
  MessageCircle,
  MessageSquarePlus,
  Plus,
  RefreshCw,
  RotateCcw,
  Save,
  Search,
  Send,
  ServerCog,
  Shield,
  Trash2,
  UserCog,
  UserPlus,
  Users,
  X,
  XCircle,
} from 'lucide-react';

const AUTH_KEY = 'forum-ui-auth';
const REPLY_PAGE_SIZE = 10;

const endpoints = [
  { method: 'POST', path: '/auth/login', access: 'public', label: 'Login' },
  { method: 'POST', path: '/auth/register', access: 'public', label: 'Register' },
  { method: 'GET', path: '/post', access: 'public', label: 'Topic index' },
  { method: 'POST', path: '/post', access: 'user', label: 'Create topic' },
  { method: 'GET', path: '/post/{id}', access: 'public', label: 'Open topic' },
  { method: 'PUT', path: '/post/{id}', access: 'owner/mod', label: 'Edit topic' },
  { method: 'GET', path: '/post/{id}/replies', access: 'public', label: 'Paged replies' },
  { method: 'POST', path: '/post/{id}/replies', access: 'user', label: 'Reply in topic' },
  { method: 'GET', path: '/reply', access: 'public', label: 'Reply list' },
  { method: 'GET', path: '/reply/{id}', access: 'public', label: 'Reply detail' },
  { method: 'POST', path: '/reply', access: 'user', label: 'Create reply' },
  { method: 'PUT', path: '/reply/{id}', access: 'owner/mod', label: 'Edit reply' },
  { method: 'GET', path: '/user', access: 'admin', label: 'Users' },
  { method: 'POST', path: '/user', access: 'admin', label: 'Create user' },
  { method: 'PUT', path: '/user/{id}', access: 'admin', label: 'Update user' },
  { method: 'DELETE', path: '/user/{id}', access: 'admin', label: 'Delete user' },
  { method: 'GET', path: '/maintenance/status', access: 'admin', label: 'Maintenance status' },
  { method: 'POST', path: '/maintenance/backup/start', access: 'admin', label: 'Backup mode' },
  { method: 'POST', path: '/maintenance/restore/start', access: 'admin', label: 'Restore mode' },
  { method: 'POST', path: '/maintenance/finish', access: 'admin', label: 'Finish maintenance' },
  { method: 'GET', path: '/healthz', access: 'public', label: 'Healthcheck' },
];

const emptyReplyPage = {
  page: 0,
  size: REPLY_PAGE_SIZE,
  totalItems: 0,
  totalPages: 0,
  items: [],
};

function getStoredAuth() {
  try {
    return JSON.parse(localStorage.getItem(AUTH_KEY));
  } catch {
    return null;
  }
}

async function apiRequest(path, options = {}) {
  const { method = 'GET', token, body } = options;
  const response = await fetch(path, {
    method,
    headers: {
      Accept: 'application/json',
      ...(body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });

  const text = await response.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = text;
    }
  }

  if (!response.ok) {
    const error = new Error(apiErrorMessage(response, data));
    error.status = response.status;
    error.data = data;
    throw error;
  }

  return data;
}

function apiErrorMessage(response, data) {
  if (data && typeof data === 'object') {
    return data.detail || data.message || data.error || `Request failed with ${response.status}`;
  }
  if (typeof data === 'string' && data.trim()) {
    return data;
  }
  if (response.status === 401) {
    return 'Login required or token expired.';
  }
  if (response.status === 403) {
    return 'This account cannot perform that action.';
  }
  if (response.status === 409) {
    return 'This value already exists.';
  }
  if (response.status >= 500) {
    return 'Forum API is unavailable or returned a server error.';
  }
  return `Request failed with ${response.status}`;
}

function normalizeList(data) {
  return Array.isArray(data) ? data : [];
}

function sortTopics(topics) {
  return [...topics].sort((left, right) => {
    const byUpdate = (Date.parse(right.updatedAt || right.createdAt || '') || 0)
      - (Date.parse(left.updatedAt || left.createdAt || '') || 0);
    if (byUpdate !== 0) {
      return byUpdate;
    }
    return (right.id || 0) - (left.id || 0);
  });
}

function formatDate(value) {
  if (!value) {
    return 'No date';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date);
}

function roleName(role) {
  return String(role || '').toLowerCase();
}

function isAdmin(auth) {
  return roleName(auth?.role) === 'admin';
}

function canEditContent(auth, item) {
  if (!auth || !item?.createdBy) {
    return false;
  }
  const role = roleName(auth.role);
  return role === 'admin' || role === 'moderator' || item.createdBy.id === auth.userId;
}

function compactNumber(value) {
  return new Intl.NumberFormat(undefined, { notation: 'compact' }).format(Number(value || 0));
}

function userInitial(username) {
  return String(username || '?').trim().slice(0, 1).toUpperCase();
}

function App() {
  const [auth, setAuth] = useState(getStoredAuth);
  const [activeView, setActiveView] = useState('forum');
  const [topics, setTopics] = useState([]);
  const [selectedTopicId, setSelectedTopicId] = useState(null);
  const [replyPage, setReplyPage] = useState(emptyReplyPage);
  const [users, setUsers] = useState([]);
  const [maintenance, setMaintenance] = useState(null);
  const [health, setHealth] = useState({ status: 'checking', label: 'Checking API' });
  const [notice, setNotice] = useState(null);
  const [busy, setBusy] = useState(false);
  const [topicSearch, setTopicSearch] = useState('');
  const [authOpen, setAuthOpen] = useState(false);
  const [authMode, setAuthMode] = useState('login');
  const [composerOpen, setComposerOpen] = useState(false);
  const [loginForm, setLoginForm] = useState({ username: 'admin', password: 'admin123' });
  const [registerForm, setRegisterForm] = useState({ username: '', email: '', password: '' });
  const [topicForm, setTopicForm] = useState({ title: '', content: '' });
  const [editingTopicId, setEditingTopicId] = useState(null);
  const [topicEditForm, setTopicEditForm] = useState({ title: '', content: '' });
  const [replyForm, setReplyForm] = useState({ content: '' });
  const [editingReplyId, setEditingReplyId] = useState(null);
  const [replyEditForm, setReplyEditForm] = useState({ content: '' });
  const [userForm, setUserForm] = useState({ username: '', email: '', role: 'user', password: '' });
  const [editingUserId, setEditingUserId] = useState(null);
  const [userEditForm, setUserEditForm] = useState({ username: '', email: '', role: 'user', password: '' });
  const [maintenanceForm, setMaintenanceForm] = useState({ retryAfterSeconds: 120, message: '' });

  const selectedTopic = useMemo(
    () => topics.find((topic) => topic.id === selectedTopicId) || null,
    [topics, selectedTopicId],
  );

  const filteredTopics = useMemo(() => {
    const query = topicSearch.trim().toLowerCase();
    if (!query) {
      return topics;
    }
    return topics.filter((topic) => {
      const author = topic.createdBy?.username || '';
      return `${topic.title || ''} ${topic.content || ''} ${author}`.toLowerCase().includes(query);
    });
  }, [topics, topicSearch]);

  const forumStats = useMemo(() => {
    const views = topics.reduce((total, topic) => total + Number(topic.viewCount || 0), 0);
    return {
      topics: topics.length,
      views,
      replies: replyPage.totalItems || 0,
    };
  }, [topics, replyPage.totalItems]);

  useEffect(() => {
    loadTopics();
    checkHealth();
  }, []);

  useEffect(() => {
    if (auth) {
      localStorage.setItem(AUTH_KEY, JSON.stringify(auth));
      setAuthOpen(false);
    } else {
      localStorage.removeItem(AUTH_KEY);
      setUsers([]);
      setMaintenance(null);
      if (activeView === 'users' || activeView === 'maintenance') {
        setActiveView('forum');
      }
    }
  }, [auth, activeView]);

  useEffect(() => {
    if (isAdmin(auth)) {
      loadUsers();
      loadMaintenance();
    }
  }, [auth]);

  function showNotice(type, message) {
    setNotice({ type, message });
  }

  async function runAction(action, successMessage) {
    setBusy(true);
    setNotice(null);
    try {
      await action();
      if (successMessage) {
        showNotice('success', successMessage);
      }
    } catch (error) {
      showNotice('error', error.message);
    } finally {
      setBusy(false);
    }
  }

  function upsertTopic(topic) {
    setTopics((current) => {
      const exists = current.some((item) => item.id === topic.id);
      const next = exists
        ? current.map((item) => (item.id === topic.id ? topic : item))
        : [topic, ...current];
      return sortTopics(next);
    });
  }

  async function loadTopics() {
    try {
      const data = await apiRequest('/post');
      setTopics(sortTopics(normalizeList(data)));
    } catch (error) {
      showNotice('error', error.message);
    }
  }

  async function loadReplies(topicId, page = 0) {
    const data = await apiRequest(`/post/${topicId}/replies?page=${page}&size=${REPLY_PAGE_SIZE}`);
    setReplyPage({
      page: data?.page ?? 0,
      size: data?.size ?? REPLY_PAGE_SIZE,
      totalItems: data?.totalItems ?? 0,
      totalPages: data?.totalPages ?? 0,
      items: normalizeList(data?.items),
    });
  }

  async function openTopic(topicId, page = 0) {
    await runAction(async () => {
      const opened = await apiRequest(`/post/${topicId}`);
      upsertTopic(opened);
      setSelectedTopicId(opened.id);
      setEditingTopicId(null);
      setEditingReplyId(null);
      await loadReplies(opened.id, page);
    });
  }

  function closeTopic() {
    setSelectedTopicId(null);
    setEditingTopicId(null);
    setEditingReplyId(null);
  }

  async function refreshCurrentBoard() {
    await runAction(async () => {
      await loadTopics();
      if (selectedTopicId) {
        await loadReplies(selectedTopicId, replyPage.page);
      }
      await checkHealth();
    });
  }

  async function checkHealth() {
    try {
      const data = await apiRequest('/healthz');
      setHealth({
        status: 'ok',
        label: data?.status ? `API ${data.status}` : 'API online',
      });
    } catch (error) {
      setHealth({
        status: error.status === 503 ? 'maintenance' : 'down',
        label: error.status === 503 ? 'Maintenance mode' : 'API offline',
      });
    }
  }

  async function loadUsers() {
    if (!auth?.accessToken) {
      return;
    }
    try {
      const data = await apiRequest('/user', { token: auth.accessToken });
      setUsers(normalizeList(data));
    } catch (error) {
      showNotice('error', error.message);
    }
  }

  async function loadMaintenance() {
    if (!auth?.accessToken) {
      return;
    }
    try {
      const data = await apiRequest('/maintenance/status', { token: auth.accessToken });
      setMaintenance(data);
    } catch (error) {
      showNotice('error', error.message);
    }
  }

  async function handleLogin(event) {
    event.preventDefault();
    await runAction(async () => {
      const data = await apiRequest('/auth/login', {
        method: 'POST',
        body: {
          username: loginForm.username.trim(),
          password: loginForm.password,
        },
      });
      setAuth(data);
      setAuthMode('login');
    }, 'Signed in.');
  }

  async function handleRegister(event) {
    event.preventDefault();
    await runAction(async () => {
      await apiRequest('/auth/register', {
        method: 'POST',
        body: {
          username: registerForm.username.trim(),
          email: registerForm.email.trim() || undefined,
          password: registerForm.password,
        },
      });
      setLoginForm({ username: registerForm.username.trim(), password: registerForm.password });
      setRegisterForm({ username: '', email: '', password: '' });
      setAuthMode('login');
    }, 'Account created. Sign in with the new user.');
  }

  function handleLogout() {
    setAuth(null);
    showNotice('success', 'Signed out.');
  }

  async function handleCreateTopic(event) {
    event.preventDefault();
    if (!auth?.accessToken) {
      showNotice('error', 'Login required to create a topic.');
      return;
    }
    await runAction(async () => {
      const created = await apiRequest('/post', {
        method: 'POST',
        token: auth.accessToken,
        body: {
          title: topicForm.title.trim(),
          content: topicForm.content.trim(),
        },
      });
      setTopicForm({ title: '', content: '' });
      setComposerOpen(false);
      await loadTopics();
      await openTopic(created.id);
    }, 'Topic created.');
  }

  function startEditTopic(topic) {
    setEditingTopicId(topic.id);
    setTopicEditForm({ title: topic.title || '', content: topic.content || '' });
  }

  async function handleUpdateTopic(event) {
    event.preventDefault();
    if (!auth?.accessToken || !selectedTopic) {
      return;
    }
    await runAction(async () => {
      const updated = await apiRequest(`/post/${selectedTopic.id}`, {
        method: 'PUT',
        token: auth.accessToken,
        body: {
          title: topicEditForm.title.trim(),
          content: topicEditForm.content.trim(),
        },
      });
      upsertTopic(updated);
      setEditingTopicId(null);
    }, 'Topic updated.');
  }

  async function handleCreateReply(event) {
    event.preventDefault();
    if (!auth?.accessToken) {
      showNotice('error', 'Login required to reply.');
      return;
    }
    if (!selectedTopic) {
      showNotice('error', 'Open a topic before replying.');
      return;
    }
    await runAction(async () => {
      await apiRequest(`/post/${selectedTopic.id}/replies`, {
        method: 'POST',
        token: auth.accessToken,
        body: {
          content: replyForm.content.trim(),
        },
      });
      setReplyForm({ content: '' });
      await loadReplies(selectedTopic.id, replyPage.page);
    }, 'Reply posted.');
  }

  function startEditReply(reply) {
    setEditingReplyId(reply.id);
    setReplyEditForm({ content: reply.content || '' });
  }

  async function handleUpdateReply(event) {
    event.preventDefault();
    if (!auth?.accessToken || !selectedTopic || !editingReplyId) {
      return;
    }
    await runAction(async () => {
      await apiRequest(`/reply/${editingReplyId}`, {
        method: 'PUT',
        token: auth.accessToken,
        body: {
          content: replyEditForm.content.trim(),
        },
      });
      setEditingReplyId(null);
      setReplyEditForm({ content: '' });
      await loadReplies(selectedTopic.id, replyPage.page);
    }, 'Reply updated.');
  }

  async function handleCreateUser(event) {
    event.preventDefault();
    if (!auth?.accessToken) {
      return;
    }
    await runAction(async () => {
      await apiRequest('/user', {
        method: 'POST',
        token: auth.accessToken,
        body: {
          username: userForm.username.trim(),
          email: userForm.email.trim() || undefined,
          role: userForm.role,
          password: userForm.password,
        },
      });
      setUserForm({ username: '', email: '', role: 'user', password: '' });
      await loadUsers();
    }, 'User created.');
  }

  function startEditUser(user) {
    setEditingUserId(user.id);
    setUserEditForm({
      username: user.username || '',
      email: user.email || '',
      role: roleName(user.role) || 'user',
      password: '',
    });
  }

  async function handleUpdateUser(event) {
    event.preventDefault();
    if (!auth?.accessToken || !editingUserId) {
      return;
    }
    await runAction(async () => {
      const body = {
        username: userEditForm.username.trim(),
        email: userEditForm.email.trim() || undefined,
        role: userEditForm.role,
      };
      if (userEditForm.password) {
        body.password = userEditForm.password;
      }
      await apiRequest(`/user/${editingUserId}`, {
        method: 'PUT',
        token: auth.accessToken,
        body,
      });
      setEditingUserId(null);
      setUserEditForm({ username: '', email: '', role: 'user', password: '' });
      await loadUsers();
    }, 'User updated.');
  }

  async function handleDeleteUser(user) {
    if (!window.confirm(`Delete ${user.username}?`)) {
      return;
    }
    await runAction(async () => {
      await apiRequest(`/user/${user.id}`, {
        method: 'DELETE',
        token: auth.accessToken,
      });
      await loadUsers();
    }, 'User deleted.');
  }

  async function handleMaintenance(mode) {
    if (!auth?.accessToken) {
      return;
    }
    await runAction(async () => {
      const payload = {
        retryAfterSeconds: Number(maintenanceForm.retryAfterSeconds) || undefined,
        message: maintenanceForm.message.trim() || undefined,
      };
      const path = mode === 'backup'
        ? '/maintenance/backup/start'
        : '/maintenance/restore/start';
      const data = await apiRequest(path, {
        method: 'POST',
        token: auth.accessToken,
        body: payload,
      });
      setMaintenance(data);
      await checkHealth();
    }, `${mode === 'backup' ? 'Backup' : 'Restore'} mode started.`);
  }

  async function handleFinishMaintenance() {
    if (!auth?.accessToken) {
      return;
    }
    await runAction(async () => {
      const data = await apiRequest('/maintenance/finish', {
        method: 'POST',
        token: auth.accessToken,
      });
      setMaintenance(data);
      await checkHealth();
    }, 'Maintenance finished.');
  }

  return (
    <div className="app">
      <Sidebar
        auth={auth}
        health={health}
        activeView={activeView}
        onNavigate={setActiveView}
        onSignIn={() => setAuthOpen(true)}
        onLogout={handleLogout}
        onHealthRefresh={checkHealth}
      />

      <div className="content">
        <Topbar
          title={viewTitle(activeView)}
          auth={auth}
          busy={busy}
          onRefresh={refreshCurrentBoard}
        />

        <div className="content__body">
          {notice && (
            <div className={`notice ${notice.type}`} role="status">
              {notice.type === 'success' ? <CheckCircle2 size={18} /> : <XCircle size={18} />}
              <span>{notice.message}</span>
            </div>
          )}

          {activeView === 'forum' && (
            <ForumView
              auth={auth}
              busy={busy}
              stats={forumStats}
              topics={filteredTopics}
              allTopics={topics}
              selectedTopic={selectedTopic}
              replyPage={replyPage}
              topicSearch={topicSearch}
              topicForm={topicForm}
              topicEditForm={topicEditForm}
              editingTopicId={editingTopicId}
              composerOpen={composerOpen}
              replyForm={replyForm}
              editingReplyId={editingReplyId}
              replyEditForm={replyEditForm}
              onTopicSearchChange={setTopicSearch}
              onToggleComposer={() => setComposerOpen((open) => !open)}
              onTopicFormChange={setTopicForm}
              onTopicEditFormChange={setTopicEditForm}
              onReplyFormChange={setReplyForm}
              onReplyEditFormChange={setReplyEditForm}
              onOpenTopic={openTopic}
              onBack={closeTopic}
              onSignIn={() => setAuthOpen(true)}
              onCreateTopic={handleCreateTopic}
              onStartEditTopic={startEditTopic}
              onCancelEditTopic={() => setEditingTopicId(null)}
              onUpdateTopic={handleUpdateTopic}
              onCreateReply={handleCreateReply}
              onStartEditReply={startEditReply}
              onCancelEditReply={() => setEditingReplyId(null)}
              onUpdateReply={handleUpdateReply}
              onPageChange={(page) => selectedTopic && loadReplies(selectedTopic.id, page)}
            />
          )}

          {activeView === 'users' && isAdmin(auth) && (
            <UsersView
              busy={busy}
              users={users}
              userForm={userForm}
              editingUserId={editingUserId}
              userEditForm={userEditForm}
              onUserFormChange={setUserForm}
              onUserEditFormChange={setUserEditForm}
              onCreateUser={handleCreateUser}
              onStartEditUser={startEditUser}
              onCancelEditUser={() => setEditingUserId(null)}
              onUpdateUser={handleUpdateUser}
              onDeleteUser={handleDeleteUser}
              onRefresh={loadUsers}
            />
          )}

          {activeView === 'maintenance' && isAdmin(auth) && (
            <MaintenanceView
              busy={busy}
              maintenance={maintenance}
              form={maintenanceForm}
              onFormChange={setMaintenanceForm}
              onBackup={() => handleMaintenance('backup')}
              onRestore={() => handleMaintenance('restore')}
              onFinish={handleFinishMaintenance}
              onRefresh={loadMaintenance}
            />
          )}

          {activeView === 'api' && <ApiView />}
        </div>
      </div>

      {authOpen && !auth && (
        <AuthModal
          authMode={authMode}
          busy={busy}
          loginForm={loginForm}
          registerForm={registerForm}
          onClose={() => setAuthOpen(false)}
          onAuthModeChange={setAuthMode}
          onLoginFormChange={setLoginForm}
          onRegisterFormChange={setRegisterForm}
          onLogin={handleLogin}
          onRegister={handleRegister}
        />
      )}
    </div>
  );
}

function Sidebar({ auth, health, activeView, onNavigate, onSignIn, onLogout, onHealthRefresh }) {
  return (
    <aside className="sidebar">
      <div className="sidebar__brand">
        <div className="sidebar__mark" aria-hidden="true">
          <MessageCircle size={22} />
        </div>
        <div className="sidebar__brandText">
          <strong>MasterSE</strong>
          <span>Community</span>
        </div>
      </div>

      <button
        type="button"
        className={`healthPill ${health.status}`}
        onClick={onHealthRefresh}
        title="Refresh API health"
      >
        <span className="statusDot" aria-hidden="true" />
        <span className="healthPill__label">{health.label}</span>
        <RefreshCw size={13} />
      </button>

      <nav className="sidebar__nav" aria-label="Primary navigation">
        <p className="sidebar__navLabel">Browse</p>
        <NavItem active={activeView === 'forum'} icon={BookOpen} label="Forum" onClick={() => onNavigate('forum')} />
        <NavItem active={activeView === 'api'} icon={FileJson2} label="API Reference" onClick={() => onNavigate('api')} />
        {isAdmin(auth) && (
          <>
            <p className="sidebar__navLabel">Administration</p>
            <NavItem active={activeView === 'users'} icon={Users} label="Members" onClick={() => onNavigate('users')} />
            <NavItem active={activeView === 'maintenance'} icon={ServerCog} label="Maintenance" onClick={() => onNavigate('maintenance')} />
          </>
        )}
      </nav>

      <div className="sidebar__footer">
        {auth ? (
          <div className="userChip">
            <div className="userChip__avatar">{userInitial(auth.username)}</div>
            <div className="userChip__id">
              <strong>{auth.username}</strong>
              <span className={`roleText ${roleName(auth.role)}`}>{roleName(auth.role)}</span>
            </div>
            <button type="button" className="iconButton" title="Sign out" aria-label="Sign out" onClick={onLogout}>
              <LogOut size={17} />
            </button>
          </div>
        ) : (
          <button type="button" className="signInBtn" onClick={onSignIn}>
            <LogIn size={17} />
            <span>Sign in / Register</span>
          </button>
        )}
      </div>
    </aside>
  );
}

function NavItem({ active, icon: Icon, label, onClick }) {
  return (
    <button type="button" className={active ? 'navItem active' : 'navItem'} onClick={onClick}>
      <Icon size={18} />
      <span>{label}</span>
    </button>
  );
}

function Topbar({ title, auth, busy, onRefresh }) {
  return (
    <header className="topbar">
      <div className="topbar__title">
        <p className="eyebrow">Board</p>
        <h2>{title}</h2>
      </div>
      <div className="topbar__actions">
        {auth && (
          <span className={`roleBadge ${roleName(auth.role)}`}>
            <Shield size={15} />
            {auth.username} · {roleName(auth.role)}
          </span>
        )}
        <button type="button" className="secondaryButton" onClick={onRefresh} disabled={busy}>
          <RefreshCw size={16} />
          <span>Refresh</span>
        </button>
      </div>
    </header>
  );
}

function AuthModal({
  authMode,
  busy,
  loginForm,
  registerForm,
  onClose,
  onAuthModeChange,
  onLoginFormChange,
  onRegisterFormChange,
  onLogin,
  onRegister,
}) {
  return (
    <div className="modalOverlay" role="dialog" aria-modal="true" onClick={onClose}>
      <div className="modal" onClick={(event) => event.stopPropagation()}>
        <div className="modal__head">
          <div>
            <p className="eyebrow">Welcome back</p>
            <h3>{authMode === 'login' ? 'Sign in to continue' : 'Create your account'}</h3>
          </div>
          <button type="button" className="iconButton" title="Close" aria-label="Close" onClick={onClose}>
            <X size={18} />
          </button>
        </div>

        <div className="segmented" role="tablist" aria-label="Authentication mode">
          <button
            type="button"
            className={authMode === 'login' ? 'segment active' : 'segment'}
            onClick={() => onAuthModeChange('login')}
          >
            <LogIn size={15} />
            <span>Login</span>
          </button>
          <button
            type="button"
            className={authMode === 'register' ? 'segment active' : 'segment'}
            onClick={() => onAuthModeChange('register')}
          >
            <UserPlus size={15} />
            <span>Register</span>
          </button>
        </div>

        {authMode === 'login' ? (
          <form className="modalForm" onSubmit={onLogin}>
            <label>
              Username
              <input
                aria-label="Username"
                value={loginForm.username}
                onChange={(event) => onLoginFormChange({ ...loginForm, username: event.target.value })}
                required
              />
            </label>
            <label>
              Password
              <input
                aria-label="Password"
                type="password"
                value={loginForm.password}
                onChange={(event) => onLoginFormChange({ ...loginForm, password: event.target.value })}
                required
              />
            </label>
            <button className="primaryButton full" type="submit" disabled={busy}>
              <KeyRound size={16} />
              <span>Sign in</span>
            </button>
          </form>
        ) : (
          <form className="modalForm" onSubmit={onRegister}>
            <label>
              Username
              <input
                aria-label="New username"
                value={registerForm.username}
                onChange={(event) => onRegisterFormChange({ ...registerForm, username: event.target.value })}
                maxLength={100}
                required
              />
            </label>
            <label>
              Email
              <input
                aria-label="Email"
                type="email"
                value={registerForm.email}
                onChange={(event) => onRegisterFormChange({ ...registerForm, email: event.target.value })}
                maxLength={255}
              />
            </label>
            <label>
              Password
              <input
                aria-label="New password"
                type="password"
                value={registerForm.password}
                onChange={(event) => onRegisterFormChange({ ...registerForm, password: event.target.value })}
                minLength={8}
                maxLength={128}
                required
              />
            </label>
            <button className="primaryButton full" type="submit" disabled={busy}>
              <UserPlus size={16} />
              <span>Create account</span>
            </button>
          </form>
        )}

        <p className="modalHint">Browsing works without an account. Sign in to post topics and replies.</p>
      </div>
    </div>
  );
}

function ForumView({
  auth,
  busy,
  stats,
  topics,
  allTopics,
  selectedTopic,
  replyPage,
  topicSearch,
  topicForm,
  topicEditForm,
  editingTopicId,
  composerOpen,
  replyForm,
  editingReplyId,
  replyEditForm,
  onTopicSearchChange,
  onToggleComposer,
  onTopicFormChange,
  onTopicEditFormChange,
  onReplyFormChange,
  onReplyEditFormChange,
  onOpenTopic,
  onBack,
  onSignIn,
  onCreateTopic,
  onStartEditTopic,
  onCancelEditTopic,
  onUpdateTopic,
  onCreateReply,
  onStartEditReply,
  onCancelEditReply,
  onUpdateReply,
  onPageChange,
}) {
  if (selectedTopic) {
    return (
      <ThreadView
        auth={auth}
        busy={busy}
        selectedTopic={selectedTopic}
        replyPage={replyPage}
        editingTopicId={editingTopicId}
        topicEditForm={topicEditForm}
        replyForm={replyForm}
        editingReplyId={editingReplyId}
        replyEditForm={replyEditForm}
        onBack={onBack}
        onSignIn={onSignIn}
        onTopicEditFormChange={onTopicEditFormChange}
        onReplyFormChange={onReplyFormChange}
        onReplyEditFormChange={onReplyEditFormChange}
        onStartEditTopic={onStartEditTopic}
        onCancelEditTopic={onCancelEditTopic}
        onUpdateTopic={onUpdateTopic}
        onCreateReply={onCreateReply}
        onStartEditReply={onStartEditReply}
        onCancelEditReply={onCancelEditReply}
        onUpdateReply={onUpdateReply}
        onPageChange={onPageChange}
      />
    );
  }

  return (
    <div className="forum">
      <div className="statStrip">
        <Stat icon={BookOpen} label="Topics" value={stats.topics} />
        <Stat icon={Eye} label="Total views" value={compactNumber(stats.views)} />
        <Stat icon={MessageCircle} label="Replies in view" value={compactNumber(stats.replies)} />
      </div>

      <section className="panel">
        <header className="panel__head">
          <div className="panel__heading">
            <p className="eyebrow">General Discussion</p>
            <h3>Topics</h3>
          </div>
          <div className="panel__tools">
            <label className="searchBox">
              <Search size={16} />
              <input
                aria-label="Search topics"
                placeholder="Search topics"
                value={topicSearch}
                onChange={(event) => onTopicSearchChange(event.target.value)}
              />
            </label>
            <button type="button" className="primaryButton" onClick={auth ? onToggleComposer : onSignIn}>
              <MessageSquarePlus size={17} />
              <span>{auth ? 'New topic' : 'Sign in to post'}</span>
            </button>
          </div>
        </header>

        {composerOpen && auth && (
          <form className="composer composer--bordered" onSubmit={onCreateTopic}>
            <label>
              Title
              <input
                value={topicForm.title}
                onChange={(event) => onTopicFormChange({ ...topicForm, title: event.target.value })}
                maxLength={500}
                required
              />
            </label>
            <label>
              Message
              <textarea
                value={topicForm.content}
                onChange={(event) => onTopicFormChange({ ...topicForm, content: event.target.value })}
                required
              />
            </label>
            <div className="buttonLine">
              <button type="button" className="secondaryButton" onClick={onToggleComposer}>
                <X size={16} />
                <span>Cancel</span>
              </button>
              <button className="primaryButton" type="submit" disabled={busy}>
                <Plus size={17} />
                <span>Create topic</span>
              </button>
            </div>
          </form>
        )}

        <div className="topicList" role="table" aria-label="Forum topics">
          <div className="topicList__head" role="row">
            <span>Topic</span>
            <span>Author</span>
            <span>Views</span>
            <span>Updated</span>
          </div>
          {topics.length === 0 ? (
            <EmptyState
              icon={BookOpen}
              title={allTopics.length === 0 ? 'No topics yet' : 'No matching topics'}
              text={allTopics.length === 0 ? 'Start the first discussion once you are signed in.' : 'Try a different search.'}
            />
          ) : (
            topics.map((topic) => (
              <button
                type="button"
                className="topicRow"
                key={topic.id}
                onClick={() => onOpenTopic(topic.id)}
                role="row"
              >
                <span className="topicRow__main">
                  <span className="topicRow__icon" aria-hidden="true">
                    <MessageCircle size={18} />
                  </span>
                  <span className="topicRow__text">
                    <strong>{topic.title}</strong>
                    <small>{topic.content}</small>
                  </span>
                </span>
                <span className="topicRow__author">
                  <span className="tinyAvatar">{userInitial(topic.createdBy?.username)}</span>
                  <span>{topic.createdBy?.username || 'unknown'}</span>
                </span>
                <span className="topicRow__metric">{compactNumber(topic.viewCount)}</span>
                <span className="topicRow__date">{formatDate(topic.updatedAt || topic.createdAt)}</span>
              </button>
            ))
          )}
        </div>
      </section>
    </div>
  );
}

function ThreadView({
  auth,
  busy,
  selectedTopic,
  replyPage,
  editingTopicId,
  topicEditForm,
  replyForm,
  editingReplyId,
  replyEditForm,
  onBack,
  onSignIn,
  onTopicEditFormChange,
  onReplyFormChange,
  onReplyEditFormChange,
  onStartEditTopic,
  onCancelEditTopic,
  onUpdateTopic,
  onCreateReply,
  onStartEditReply,
  onCancelEditReply,
  onUpdateReply,
  onPageChange,
}) {
  const canEditTopic = canEditContent(auth, selectedTopic);
  const totalPages = Math.max(replyPage.totalPages || 0, 1);

  return (
    <div className="thread">
      <nav className="breadcrumb" aria-label="Breadcrumb">
        <button type="button" className="ghostButton" onClick={onBack}>
          <ArrowLeft size={16} />
          <span>Board</span>
        </button>
        <ChevronRight size={15} />
        <span className="breadcrumb__current">
          <Hash size={14} />
          {selectedTopic.id}
        </span>
      </nav>

      <section className="panel">
        <header className="thread__head">
          <div className="thread__heading">
            <p className="eyebrow">Thread</p>
            <h3>{selectedTopic.title}</h3>
          </div>
          <div className="thread__headActions">
            <span className="metricPill">
              <Eye size={15} />
              {compactNumber(selectedTopic.viewCount)}
            </span>
            {canEditTopic && (
              <button
                type="button"
                className="iconButton"
                title="Edit topic"
                aria-label="Edit topic"
                onClick={() => onStartEditTopic(selectedTopic)}
              >
                <Edit3 size={17} />
              </button>
            )}
          </div>
        </header>

        {editingTopicId === selectedTopic.id ? (
          <form className="composer composer--bordered" onSubmit={onUpdateTopic}>
            <label>
              Title
              <input
                value={topicEditForm.title}
                onChange={(event) => onTopicEditFormChange({ ...topicEditForm, title: event.target.value })}
                maxLength={500}
                required
              />
            </label>
            <label>
              Message
              <textarea
                value={topicEditForm.content}
                onChange={(event) => onTopicEditFormChange({ ...topicEditForm, content: event.target.value })}
                required
              />
            </label>
            <div className="buttonLine">
              <button type="button" className="secondaryButton" onClick={onCancelEditTopic}>
                <X size={16} />
                <span>Cancel</span>
              </button>
              <button className="primaryButton" type="submit" disabled={busy}>
                <Save size={16} />
                <span>Save</span>
              </button>
            </div>
          </form>
        ) : (
          <PostArticle item={selectedTopic} isTopic />
        )}

        <div className="thread__replyBar">
          <strong>{replyPage.totalItems || 0} replies</strong>
          <Pagination page={replyPage.page} totalPages={totalPages} busy={busy} onPageChange={onPageChange} />
        </div>

        <div className="replyList">
          {replyPage.items.length === 0 ? (
            <EmptyState icon={MessageCircle} title="No replies yet" text="This topic is waiting for its first answer." />
          ) : (
            replyPage.items.map((reply) => (
              <ReplyArticle
                key={reply.id}
                auth={auth}
                reply={reply}
                busy={busy}
                editing={editingReplyId === reply.id}
                form={replyEditForm}
                onFormChange={onReplyEditFormChange}
                onStartEdit={() => onStartEditReply(reply)}
                onCancel={onCancelEditReply}
                onSubmit={onUpdateReply}
              />
            ))
          )}
        </div>

        {auth ? (
          <form className="composer composer--reply" onSubmit={onCreateReply}>
            <div className="composer__title">
              <Send size={17} />
              <strong>Post a reply</strong>
            </div>
            <label>
              Message
              <textarea
                value={replyForm.content}
                onChange={(event) => onReplyFormChange({ content: event.target.value })}
                required
              />
            </label>
            <div className="buttonLine">
              <button className="primaryButton" type="submit" disabled={busy}>
                <Send size={16} />
                <span>Submit reply</span>
              </button>
            </div>
          </form>
        ) : (
          <div className="signInPrompt">
            <MessageCircle size={20} />
            <span>Sign in to join the conversation.</span>
            <button type="button" className="primaryButton" onClick={onSignIn}>
              <LogIn size={16} />
              <span>Sign in</span>
            </button>
          </div>
        )}
      </section>
    </div>
  );
}

function PostArticle({ item, isTopic = false }) {
  return (
    <article className={isTopic ? 'post post--op' : 'post'}>
      <aside className="post__author">
        <div className="avatar">{userInitial(item.createdBy?.username)}</div>
        <strong>{item.createdBy?.username || 'unknown'}</strong>
        <span className={`roleText ${roleName(item.createdBy?.role)}`}>
          {roleName(item.createdBy?.role) || 'member'}
        </span>
      </aside>
      <div className="post__body">
        <div className="post__meta">
          <span>
            <Clock3 size={14} />
            {formatDate(item.createdAt)}
          </span>
          <span>Updated {formatDate(item.updatedAt)}</span>
        </div>
        <p>{item.content}</p>
      </div>
    </article>
  );
}

function ReplyArticle({
  auth,
  reply,
  busy,
  editing,
  form,
  onFormChange,
  onStartEdit,
  onCancel,
  onSubmit,
}) {
  return (
    <article className="post">
      <aside className="post__author">
        <div className="avatar">{userInitial(reply.createdBy?.username)}</div>
        <strong>{reply.createdBy?.username || 'unknown'}</strong>
        <span className={`roleText ${roleName(reply.createdBy?.role)}`}>
          {roleName(reply.createdBy?.role) || 'member'}
        </span>
      </aside>
      <div className="post__body">
        <div className="post__meta">
          <span>
            <Clock3 size={14} />
            {formatDate(reply.createdAt)}
          </span>
          <span>Updated {formatDate(reply.updatedAt)}</span>
          {canEditContent(auth, reply) && !editing && (
            <button type="button" className="iconButton small" title="Edit reply" aria-label="Edit reply" onClick={onStartEdit}>
              <Edit3 size={15} />
            </button>
          )}
        </div>

        {editing ? (
          <form className="composer composer--inline" onSubmit={onSubmit}>
            <label>
              Message
              <textarea
                value={form.content}
                onChange={(event) => onFormChange({ content: event.target.value })}
                required
              />
            </label>
            <div className="buttonLine">
              <button type="button" className="secondaryButton" onClick={onCancel}>
                <X size={16} />
                <span>Cancel</span>
              </button>
              <button className="primaryButton" type="submit" disabled={busy}>
                <Save size={16} />
                <span>Save</span>
              </button>
            </div>
          </form>
        ) : (
          <p>{reply.content}</p>
        )}
      </div>
    </article>
  );
}

function UsersView({
  busy,
  users,
  userForm,
  editingUserId,
  userEditForm,
  onUserFormChange,
  onUserEditFormChange,
  onCreateUser,
  onStartEditUser,
  onCancelEditUser,
  onUpdateUser,
  onDeleteUser,
  onRefresh,
}) {
  return (
    <div className="admin">
      <form className="panel adminForm" onSubmit={onCreateUser}>
        <div className="composer__title">
          <UserPlus size={18} />
          <strong>Create member</strong>
        </div>
        <UserFields form={userForm} onChange={onUserFormChange} requirePassword />
        <button className="primaryButton full" type="submit" disabled={busy}>
          <Plus size={17} />
          <span>Create user</span>
        </button>
      </form>

      <section className="panel">
        <header className="panel__head">
          <div className="panel__heading">
            <p className="eyebrow">Admin</p>
            <h3>{users.length} members</h3>
          </div>
          <button type="button" className="iconButton" title="Refresh users" aria-label="Refresh users" onClick={onRefresh}>
            <RefreshCw size={17} />
          </button>
        </header>

        <div className="memberList">
          {users.length === 0 ? (
            <EmptyState icon={Users} title="No members returned" text="The API did not return any users." />
          ) : (
            users.map((user) => (
              <article className="memberRow" key={user.id}>
                {editingUserId === user.id ? (
                  <form className="memberEdit" onSubmit={onUpdateUser}>
                    <UserFields form={userEditForm} onChange={onUserEditFormChange} />
                    <div className="buttonLine">
                      <button type="button" className="secondaryButton" onClick={onCancelEditUser}>
                        <X size={16} />
                        <span>Cancel</span>
                      </button>
                      <button className="primaryButton" type="submit" disabled={busy}>
                        <Save size={16} />
                        <span>Save</span>
                      </button>
                    </div>
                  </form>
                ) : (
                  <>
                    <div className="memberRow__id">
                      <span className="tinyAvatar">{userInitial(user.username)}</span>
                      <div>
                        <strong>{user.username}</strong>
                        <span>{user.email || 'No email'}</span>
                      </div>
                    </div>
                    <span className={`roleBadge ${roleName(user.role)}`}>
                      <UserCog size={15} />
                      {roleName(user.role)}
                    </span>
                    <span className="memberRow__date">{formatDate(user.createdAt)}</span>
                    <div className="memberRow__actions">
                      <button type="button" className="iconButton" title="Edit user" aria-label="Edit user" onClick={() => onStartEditUser(user)}>
                        <Edit3 size={17} />
                      </button>
                      <button type="button" className="iconButton danger" title="Delete user" aria-label="Delete user" onClick={() => onDeleteUser(user)} disabled={busy}>
                        <Trash2 size={17} />
                      </button>
                    </div>
                  </>
                )}
              </article>
            ))
          )}
        </div>
      </section>
    </div>
  );
}

function UserFields({ form, onChange, requirePassword = false }) {
  return (
    <div className="formGrid">
      <label>
        Username
        <input
          value={form.username}
          onChange={(event) => onChange({ ...form, username: event.target.value })}
          maxLength={100}
          required
        />
      </label>
      <label>
        Email
        <input
          type="email"
          value={form.email}
          onChange={(event) => onChange({ ...form, email: event.target.value })}
          maxLength={255}
        />
      </label>
      <label>
        Role
        <select value={form.role} onChange={(event) => onChange({ ...form, role: event.target.value })}>
          <option value="user">user</option>
          <option value="moderator">moderator</option>
          <option value="admin">admin</option>
        </select>
      </label>
      <label>
        Password
        <input
          type="password"
          value={form.password}
          onChange={(event) => onChange({ ...form, password: event.target.value })}
          minLength={requirePassword ? 8 : undefined}
          maxLength={128}
          required={requirePassword}
          placeholder={requirePassword ? '' : 'Leave blank to keep'}
        />
      </label>
    </div>
  );
}

function MaintenanceView({
  busy,
  maintenance,
  form,
  onFormChange,
  onBackup,
  onRestore,
  onFinish,
  onRefresh,
}) {
  return (
    <div className="admin admin--maintenance">
      <section className="panel adminForm">
        <header className="panel__head">
          <div className="panel__heading">
            <p className="eyebrow">Maintenance</p>
            <h3>{maintenance?.mode || 'NONE'}</h3>
          </div>
          <button type="button" className="iconButton" title="Refresh maintenance" aria-label="Refresh maintenance" onClick={onRefresh}>
            <RefreshCw size={17} />
          </button>
        </header>

        <div className={maintenance?.active ? 'maintBox active' : 'maintBox'}>
          <span>{maintenance?.active ? 'Active' : 'Inactive'}</span>
          <strong>{maintenance?.message || 'No maintenance message'}</strong>
          <small>Retry after {maintenance?.retryAfterSeconds || 0}s</small>
        </div>

        <div className="formGrid">
          <label>
            Retry seconds
            <input
              type="number"
              min="1"
              max="86400"
              value={form.retryAfterSeconds}
              onChange={(event) => onFormChange({ ...form, retryAfterSeconds: event.target.value })}
            />
          </label>
          <label>
            Message
            <input
              value={form.message}
              onChange={(event) => onFormChange({ ...form, message: event.target.value })}
            />
          </label>
        </div>

        <div className="buttonGrid">
          <button type="button" className="secondaryButton" onClick={onBackup} disabled={busy}>
            <DatabaseBackup size={17} />
            <span>Backup</span>
          </button>
          <button type="button" className="secondaryButton warning" onClick={onRestore} disabled={busy}>
            <RotateCcw size={17} />
            <span>Restore</span>
          </button>
          <button type="button" className="primaryButton" onClick={onFinish} disabled={busy}>
            <CheckCircle2 size={17} />
            <span>Finish</span>
          </button>
        </div>
      </section>

      <section className="panel modeBoard">
        <ModeTile icon={DatabaseBackup} title="BACKUP" text="Read traffic stays available while writes are blocked." />
        <ModeTile icon={Lock} title="RESTORE" text="Normal forum traffic pauses while maintenance stays reachable." />
      </section>
    </div>
  );
}

function ApiView() {
  return (
    <section className="panel apiPanel">
      <header className="panel__head">
        <div className="panel__heading">
          <p className="eyebrow">OpenAPI</p>
          <h3>{endpoints.length} endpoints</h3>
        </div>
      </header>
      <div className="endpointList">
        {endpoints.map((endpoint) => (
          <article className="endpointRow" key={`${endpoint.method}-${endpoint.path}`}>
            <span className={`methodBadge ${endpoint.method.toLowerCase()}`}>{endpoint.method}</span>
            <code>{endpoint.path}</code>
            <span className={`accessBadge ${endpoint.access.replace('/', '-')}`}>{endpoint.access}</span>
            <span className="endpointRow__label">{endpoint.label}</span>
          </article>
        ))}
      </div>
    </section>
  );
}

function Stat({ icon: Icon, label, value }) {
  return (
    <div className="statCard">
      <span className="statCard__icon">
        <Icon size={18} />
      </span>
      <span className="statCard__label">{label}</span>
      <strong className="statCard__value">{value}</strong>
    </div>
  );
}

function Pagination({ page = 0, totalPages = 1, busy, onPageChange }) {
  return (
    <div className="pagination">
      <button
        type="button"
        className="iconButton small"
        title="Previous page"
        aria-label="Previous page"
        disabled={busy || page <= 0}
        onClick={() => onPageChange(page - 1)}
      >
        <ChevronLeft size={16} />
      </button>
      <span>{page + 1} / {totalPages}</span>
      <button
        type="button"
        className="iconButton small"
        title="Next page"
        aria-label="Next page"
        disabled={busy || page >= totalPages - 1}
        onClick={() => onPageChange(page + 1)}
      >
        <ChevronRight size={16} />
      </button>
    </div>
  );
}

function ModeTile({ icon: Icon, title, text }) {
  return (
    <article className="modeTile">
      <span className="modeTile__icon">
        <Icon size={22} />
      </span>
      <div>
        <strong>{title}</strong>
        <span>{text}</span>
      </div>
    </article>
  );
}

function EmptyState({ icon: Icon, title, text }) {
  return (
    <div className="emptyState">
      <Icon size={28} />
      <strong>{title}</strong>
      <span>{text}</span>
    </div>
  );
}

function viewTitle(view) {
  switch (view) {
    case 'users':
      return 'Member Administration';
    case 'maintenance':
      return 'Maintenance Console';
    case 'api':
      return 'API Surface';
    default:
      return 'Forum';
  }
}

export default App;
