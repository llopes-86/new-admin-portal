class SessionAuthStore {
  constructor(key = 'pb_auth') {
    this._key = key;
    this._listeners = [];
  }
  _read() {
    try { return JSON.parse(sessionStorage.getItem(this._key) || 'null') || {}; }
    catch (_) { return {}; }
  }
  _write(data) {
    try { sessionStorage.setItem(this._key, JSON.stringify(data)); } catch (_) {}
  }
  get token() { return this._read().token || ''; }
  get record() { return this._read().record || null; }
  get model() { return this.record; }
  get isValid() {
    const t = this.token;
    if (!t) return false;
    try {
      const payload = JSON.parse(atob(t.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
      return !payload.exp || (payload.exp * 1000) >= Date.now();
    } catch (_) { return false; }
  }
  get isSuperuser() { return false; }
  save(token, record) {
    this._write({ token, record });
    this._listeners.forEach(fn => fn(token, record));
  }
  clear() {
    sessionStorage.removeItem(this._key);
    this._listeners.forEach(fn => fn(null, null));
  }
  onChange(fn) {
    this._listeners.push(fn);
    return () => { this._listeners = this._listeners.filter(f => f !== fn); };
  }
}

const pb = new PocketBase(CONFIG.PB_URL, new SessionAuthStore());
pb.autoCancellation(false);

pb.afterSend = (response, data) => {
  if (response && response.status === 401) logout();
  return data;
};

async function login(email, password) {
  try {
    await pb.collection('users').authWithPassword(email, password);
    const rec = pb.authStore.record;
    const mustChange = !!(rec && rec.must_change_password);
    return { ok: true, mustChangePassword: mustChange };
  } catch (err) {
    if (err && (err.status === 400 || err.isAbort)) {
      return { ok: false, error: 'E-mail ou senha inválidos. Verifique e tente novamente.' };
    }
    return { ok: false, error: 'Sem conexão com o servidor. Verifique sua internet e tente novamente.' };
  }
}

async function changePassword(oldPassword, newPassword) {
  try {
    const auth = getAuth();
    if (!auth) return { ok: false, error: 'Sessão expirada. Faça login novamente.' };
    try {
      await pb.collection('users').update(auth.user.id, {
        oldPassword,
        password: newPassword,
        passwordConfirm: newPassword,
        must_change_password: false
      });
    } catch (err) {
      const resp = (err && err.response) || {};
      const data = resp.data || {};
      let msg = 'Não foi possível salvar a nova senha. Tente novamente.';
      if ((resp.message || '').includes('authenticate') || data.oldPassword) msg = 'Senha atual incorreta. Digite novamente.';
      return { ok: false, error: msg };
    }
    pb.authStore.clear();
    try {
      await pb.collection('users').authWithPassword(auth.user.email, newPassword);
    } catch (_) {}
    return { ok: true };
  } catch (err) {
    return { ok: false, error: 'Sem conexão com o servidor. Verifique sua internet e tente novamente.' };
  }
}

function _createLogoutOverlay() {
  if (document.getElementById('logout-overlay')) return document.getElementById('logout-overlay');
  const overlay = document.createElement('div');
  overlay.id = 'logout-overlay';
  overlay.className = 'logout-overlay';
  overlay.setAttribute('role', 'status');
  overlay.setAttribute('aria-live', 'assertive');
  overlay.setAttribute('aria-label', 'Saindo');
  overlay.innerHTML = `
    <div class="logout-overlay-card">
      <div class="logout-overlay-spinner">
        <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10" stroke-dasharray="31.4 31.4" stroke-linecap="round"/>
        </svg>
      </div>
      <p class="logout-overlay-text">Saindo...</p>
      <p class="logout-overlay-sub">Encerrando sua sessão</p>
    </div>`;
  document.body.appendChild(overlay);
  void overlay.offsetWidth;
  requestAnimationFrame(() => overlay.classList.add('is-visible'));
  return overlay;
}

function _setLogoutButtonsLoading(loading) {
  ['logout-btn', 'logout-btn-mobile'].forEach(id => {
    const btn = document.getElementById(id);
    if (!btn) return;
    btn.disabled = loading;
    btn.setAttribute('aria-busy', loading ? 'true' : 'false');
    const spinner = btn.querySelector('.logout-spinner');
    const label = btn.querySelector('.logout-label');
    const icon = btn.querySelector('.logout-icon');
    if (spinner) spinner.classList.toggle('hidden', !loading);
    if (label) label.style.opacity = loading ? '0.6' : '';
    if (icon) icon.style.display = loading ? 'none' : '';
    if (btn.id === 'logout-btn-mobile') {
      const span = btn.querySelector('span:not(.logout-spinner)');
      if (span && !label) span.style.opacity = loading ? '0.6' : '';
    }
  });
}

function logout() {
  if (document.body.classList.contains('is-logging-out')) return;
  pb.authStore.clear();
  const reduced = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const doNav = () => window.location.replace('index.html');
  if (reduced || typeof document === 'undefined') { doNav(); return; }
  if (typeof showToast === 'function') showToast('Saindo…', 'info');
  _setLogoutButtonsLoading(true);
  _createLogoutOverlay();
  document.body.classList.add('is-logging-out');
  setTimeout(doNav, 750);
}

function getAuth() {
  if (!pb.authStore.isValid) return null;
  const rec = pb.authStore.record;
  if (!rec) return null;
  return {
    token: pb.authStore.token,
    user: { id: rec.id, email: rec.email, name: rec.name }
  };
}

function requireAuth() {
  const auth = getAuth();
  if (!auth) { window.location.replace('index.html'); return null; }
  return auth;
}

async function checkMustChangePassword() {
  const auth = requireAuth();
  if (!auth) return false;
  try {
    const rec = await pb.collection('users').getOne(auth.user.id);
    return !!rec.must_change_password;
  } catch (err) {
    logout();
    return false;
  }
}
