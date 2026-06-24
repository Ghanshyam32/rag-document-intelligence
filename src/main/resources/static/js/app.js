'use strict';

// ─── STATE ────────────────────────────────────────────────────────────────────
const state = {
  token: localStorage.getItem('es_token') || null,
  user:  JSON.parse(localStorage.getItem('es_user') || 'null'),
  documents: [],
  conversationId: null,
};

// ─── API ──────────────────────────────────────────────────────────────────────
const api = {
  async req(path, opts = {}) {
    const headers = { 'Content-Type': 'application/json', ...opts.headers };
    if (state.token) headers['Authorization'] = `Bearer ${state.token}`;
    if (opts.body instanceof FormData) delete headers['Content-Type'];

    const res = await fetch(path, { ...opts, headers });
    if (res.status === 401) { app.logout(); throw new Error('Session expired'); }
    if (!res.ok) {
      let msg = `HTTP ${res.status}`;
      try { const j = await res.json(); msg = j.message || j.error || msg; } catch {}
      throw new Error(msg);
    }
    if (res.status === 204) return null;
    return res.json();
  },

  login:    (email, password)     => api.req('/api/auth/login',    { method: 'POST', body: JSON.stringify({ email, password }) }),
  register: (name, email, password) => api.req('/api/auth/register', { method: 'POST', body: JSON.stringify({ name, email, password }) }),

  getDocuments:   ()   => api.req('/api/documents'),
  deleteDocument: (id) => api.req(`/api/documents/${id}`, { method: 'DELETE' }),
  uploadDocument: (file) => {
    const fd = new FormData();
    fd.append('file', file);
    return api.req('/api/documents/upload', { method: 'POST', body: fd });
  },

  ask: (question, conversationId) => api.req('/api/chat/ask', {
    method: 'POST',
    body: JSON.stringify({ question, conversationId, topK: 5 }),
  }),
  clearHistory: (id) => api.req(`/api/chat/history/${id}`, { method: 'DELETE' }),
};

// ─── TOAST ────────────────────────────────────────────────────────────────────
let toastTimer;
function toast(msg, type = 'info') {
  const el = document.getElementById('toast');
  el.textContent = msg;
  el.className = `toast show ${type}`;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { el.className = 'toast'; }, 3200);
}

// ─── HELPERS ──────────────────────────────────────────────────────────────────
function formatTime(d = new Date()) {
  return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

function escapeHtml(str) {
  return String(str)
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;').replace(/'/g, '&#x27;');
}

function renderMarkdown(text) {
  // Basic markdown: code blocks, inline code, bold, bullet lines
  let out = escapeHtml(text);
  out = out.replace(/```([\s\S]*?)```/g, '<pre><code>$1</code></pre>');
  out = out.replace(/`([^`]+)`/g, '<code>$1</code>');
  out = out.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
  out = out.replace(/^- (.+)$/gm, '• $1');
  out = out.replace(/\n/g, '<br>');
  return out;
}

function autoResize(el) {
  el.style.height = 'auto';
  el.style.height = Math.min(el.scrollHeight, 140) + 'px';
}

// ─── DOM HELPERS ──────────────────────────────────────────────────────────────
const $ = id => document.getElementById(id);

function showView(name) {
  const auth  = $('auth-screen');
  const dash  = $('dashboard');
  if (name === 'auth') {
    auth.classList.remove('hidden');
    dash.classList.add('hidden');
  } else {
    auth.classList.add('hidden');
    dash.classList.remove('hidden');
  }
}

// ─── AUTH UI ──────────────────────────────────────────────────────────────────
function initAuthTabs() {
  document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      const tab = btn.dataset.tab;
      $('login-form').classList.toggle('hidden', tab !== 'login');
      $('register-form').classList.toggle('hidden', tab !== 'register');
      $('login-error').textContent = '';
      $('reg-error').textContent = '';
    });
  });

  $('login-form').addEventListener('submit', async e => {
    e.preventDefault();
    const btn = $('login-btn');
    const errEl = $('login-error');
    errEl.textContent = '';
    btn.classList.add('loading');
    try {
      const data = await api.login($('login-email').value.trim(), $('login-password').value);
      saveAuth(data);
      await app.init();
    } catch (err) {
      errEl.textContent = err.message;
    } finally {
      btn.classList.remove('loading');
    }
  });

  $('register-form').addEventListener('submit', async e => {
    e.preventDefault();
    const btn = $('reg-btn');
    const errEl = $('reg-error');
    errEl.textContent = '';
    btn.classList.add('loading');
    try {
      const data = await api.register(
        $('reg-name').value.trim(),
        $('reg-email').value.trim(),
        $('reg-password').value,
      );
      saveAuth(data);
      await app.init();
    } catch (err) {
      errEl.textContent = err.message;
    } finally {
      btn.classList.remove('loading');
    }
  });
}

function saveAuth(data) {
  state.token = data.token;
  state.user  = { name: data.name, email: data.email };
  localStorage.setItem('es_token', data.token);
  localStorage.setItem('es_user', JSON.stringify(state.user));
}

// ─── USER BAR ─────────────────────────────────────────────────────────────────
function renderUserBar() {
  if (!state.user) return;
  $('user-name').textContent  = state.user.name || 'User';
  $('user-email').textContent = state.user.email || '';
  $('user-avatar').textContent = (state.user.name || 'U')[0].toUpperCase();
}

// ─── DOCUMENT LIST ────────────────────────────────────────────────────────────
async function loadDocuments() {
  try {
    state.documents = await api.getDocuments();
    renderDocuments();
  } catch {}
}

function renderDocuments() {
  const list = $('document-list');
  if (!state.documents.length) {
    list.innerHTML = '<div class="docs-empty">No documents yet</div>';
    return;
  }
  list.innerHTML = state.documents.map(doc => `
    <div class="doc-item" data-id="${doc.id}">
      <div class="doc-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
          <polyline points="14 2 14 8 20 8"/>
          <line x1="16" y1="13" x2="8" y2="13"/>
          <line x1="16" y1="17" x2="8" y2="17"/>
        </svg>
      </div>
      <div class="doc-info">
        <div class="doc-name" title="${escapeHtml(doc.originalName)}">${escapeHtml(doc.originalName)}</div>
        <div class="doc-meta">${doc.totalChunks || 0} chunks</div>
      </div>
      <span class="status-dot ${(doc.status || '').toLowerCase()}" title="${doc.status || ''}"></span>
      <button class="btn-doc-delete" title="Delete document" onclick="app.deleteDocument('${doc.id}')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="3 6 5 6 21 6"/>
          <path d="M19 6l-1 14H6L5 6"/>
          <path d="M10 11v6M14 11v6"/>
          <path d="M9 6V4h6v2"/>
        </svg>
      </button>
    </div>
  `).join('');
}

// ─── UPLOAD ───────────────────────────────────────────────────────────────────
function initUpload() {
  const zone  = $('drop-zone');
  const input = $('file-input');

  zone.addEventListener('click', () => input.click());
  input.addEventListener('change', () => {
    if (input.files[0]) handleUpload(input.files[0]);
    input.value = '';
  });

  zone.addEventListener('dragover', e => { e.preventDefault(); zone.classList.add('over'); });
  zone.addEventListener('dragleave', () => zone.classList.remove('over'));
  zone.addEventListener('drop', e => {
    e.preventDefault();
    zone.classList.remove('over');
    const f = e.dataTransfer.files[0];
    if (f) handleUpload(f);
  });
}

async function handleUpload(file) {
  const allowed = ['application/pdf', 'text/plain'];
  if (!allowed.includes(file.type) && !file.name.match(/\.(pdf|txt)$/i)) {
    toast('Only PDF and TXT files are supported', 'error');
    return;
  }

  const prog  = $('upload-progress');
  const fill  = $('progress-fill');
  const label = $('upload-status-text');
  const zone  = $('drop-zone');

  zone.classList.add('hidden');
  prog.classList.remove('hidden');
  label.textContent = `Uploading ${file.name}…`;

  // Fake progress animation
  let pct = 0;
  const tick = setInterval(() => {
    pct = Math.min(pct + (Math.random() * 8), 88);
    fill.style.width = pct + '%';
  }, 250);

  try {
    label.textContent = 'Processing & embedding…';
    const doc = await api.uploadDocument(file);
    clearInterval(tick);
    fill.style.width = '100%';
    label.textContent = 'Done!';
    state.documents.unshift(doc);
    renderDocuments();
    toast(`"${file.name}" indexed successfully`, 'success');
    setTimeout(() => {
      prog.classList.add('hidden');
      zone.classList.remove('hidden');
      fill.style.width = '0%';
    }, 1200);
  } catch (err) {
    clearInterval(tick);
    toast(`Upload failed: ${err.message}`, 'error');
    prog.classList.add('hidden');
    zone.classList.remove('hidden');
    fill.style.width = '0%';
  }
}

// ─── CHAT ─────────────────────────────────────────────────────────────────────
function addMessage(role, text, sources = []) {
  const msgs     = $('messages');
  const emptyEl  = $('empty-state');
  if (emptyEl) emptyEl.remove();

  const isUser = role === 'user';
  const div    = document.createElement('div');
  div.className = `message message-${isUser ? 'user' : 'ai'}`;

  const avatarHtml = isUser
    ? `<div class="msg-avatar">${(state.user?.name || 'U')[0].toUpperCase()}</div>`
    : `<div class="msg-avatar">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>
        </svg>
       </div>`;

  const sourceHtml = (!isUser && sources.length) ? `
    <button class="sources-toggle" onclick="toggleSources(this)">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <polyline points="6 9 12 15 18 9"/>
      </svg>
      ${sources.length} source${sources.length > 1 ? 's' : ''}
    </button>
    <div class="sources-list">
      ${sources.map(s => `
        <div class="source-card">
          <div class="source-card-header">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
              <polyline points="14 2 14 8 20 8"/>
            </svg>
            ${escapeHtml(s.documentName)} · chunk ${s.chunkIndex}
          </div>
          <div class="source-excerpt">"${escapeHtml((s.excerpt || '').slice(0, 160))}…"</div>
        </div>
      `).join('')}
    </div>
  ` : '';

  div.innerHTML = `
    ${avatarHtml}
    <div class="msg-body">
      <div class="msg-bubble">${isUser ? escapeHtml(text) : renderMarkdown(text)}</div>
      ${sourceHtml}
      <span class="msg-time">${formatTime()}</span>
    </div>
  `;

  msgs.appendChild(div);
  msgs.scrollTop = msgs.scrollHeight;
}

function toggleSources(btn) {
  btn.classList.toggle('open');
  btn.nextElementSibling.classList.toggle('open');
}

function addTypingIndicator() {
  const msgs = $('messages');
  const div  = document.createElement('div');
  div.id = 'typing';
  div.className = 'typing-indicator message message-ai';
  div.innerHTML = `
    <div class="msg-avatar">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>
      </svg>
    </div>
    <div class="typing-dots">
      <span></span><span></span><span></span>
    </div>
  `;
  msgs.appendChild(div);
  msgs.scrollTop = msgs.scrollHeight;
}

function removeTypingIndicator() {
  const el = $('typing');
  if (el) el.remove();
}

// ─── INPUT ────────────────────────────────────────────────────────────────────
function initInput() {
  const input = $('question-input');
  input.addEventListener('input', () => autoResize(input));
  input.addEventListener('keydown', e => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      app.ask();
    }
  });
}

// ─── APP CONTROLLER ───────────────────────────────────────────────────────────
const app = {
  async init() {
    if (!state.token) { showView('auth'); return; }
    showView('dashboard');
    renderUserBar();
    await loadDocuments();
  },

  logout() {
    state.token = null;
    state.user  = null;
    state.conversationId = null;
    localStorage.removeItem('es_token');
    localStorage.removeItem('es_user');
    showView('auth');
    $('messages').innerHTML = `
      <div id="empty-state" class="empty-state">
        <div class="empty-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
        </div>
        <h3>Ask about your documents</h3>
        <p>Upload a PDF or TXT file using the sidebar, then ask questions in natural language. I'll find relevant passages and cite my sources.</p>
        <div class="feature-chips">
          <span class="chip">📄 PDF & TXT</span>
          <span class="chip">🔍 Semantic search</span>
          <span class="chip">📌 Source citations</span>
          <span class="chip">💬 Conversation memory</span>
        </div>
      </div>`;
  },

  newChat() {
    state.conversationId = null;
    const msgs = $('messages');
    msgs.innerHTML = `
      <div id="empty-state" class="empty-state">
        <div class="empty-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
        </div>
        <h3>Ask about your documents</h3>
        <p>Upload a PDF or TXT file using the sidebar, then ask questions in natural language. I'll find relevant passages and cite my sources.</p>
        <div class="feature-chips">
          <span class="chip">📄 PDF & TXT</span>
          <span class="chip">🔍 Semantic search</span>
          <span class="chip">📌 Source citations</span>
          <span class="chip">💬 Conversation memory</span>
        </div>
      </div>`;
    $('chat-title').textContent = 'Ask your documents';
    toast('New conversation started', 'info');
  },

  async clearChat() {
    if (state.conversationId) {
      try { await api.clearHistory(state.conversationId); } catch {}
    }
    app.newChat();
  },

  async ask() {
    const input = $('question-input');
    const question = input.value.trim();
    if (!question) return;

    const sendBtn = $('send-btn');
    sendBtn.disabled = true;
    input.value = '';
    autoResize(input);

    addMessage('user', question);
    addTypingIndicator();

    if (state.conversationId === null) {
      $('chat-title').textContent = question.slice(0, 48) + (question.length > 48 ? '…' : '');
    }

    try {
      const data = await api.ask(question, state.conversationId);
      state.conversationId = data.conversationId;
      removeTypingIndicator();
      addMessage('ai', data.answer, data.sources || []);
    } catch (err) {
      removeTypingIndicator();
      addMessage('ai', `Sorry, something went wrong: ${err.message}`);
      toast(err.message, 'error');
    } finally {
      sendBtn.disabled = false;
      input.focus();
    }
  },

  async deleteDocument(id) {
    if (!confirm('Delete this document and all its embeddings?')) return;
    try {
      await api.deleteDocument(id);
      state.documents = state.documents.filter(d => d.id !== id);
      renderDocuments();
      toast('Document deleted', 'info');
    } catch (err) {
      toast(`Delete failed: ${err.message}`, 'error');
    }
  },
};

// ─── BOOT ─────────────────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  initAuthTabs();
  initUpload();
  initInput();
  app.init();
});
