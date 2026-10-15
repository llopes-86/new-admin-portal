(async () => {
  const auth = getAuth();
  let needPasswordChange = false;
  if (auth) {
    const mustChange = await checkMustChangePassword();
    if (!mustChange) { window.location.replace('dashboard.html'); return; }
    needPasswordChange = true;
  }

  const $ = id => document.getElementById(id);
  const form = $('login-form');
  const changeForm = $('change-password-form');
  const emailEl = $('email');
  const passwordEl = $('password');
  const errorEl = $('login-error');
  const btn = $('login-btn');
  const btnText = $('login-btn-text');
  const toggleBtn = $('password-toggle');
  const changeError = $('change-error');
  const changeBtn = $('change-btn');
  const changeBtnText = $('change-btn-text');
  const currentPw = $('current-password');
  const newPw = $('new-password');
  const confirmPw = $('confirm-password');
  const headerTitle = document.querySelector('.login-header h1');
  const headerSubtitle = document.querySelector('.login-subtitle');
  const loginOverlay = $('login-overlay');

  const versionEl = $('app-version');
  if (versionEl && typeof CONFIG !== 'undefined' && CONFIG.APP_VERSION) versionEl.textContent = CONFIG.APP_VERSION;

  const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  let isSubmittingLogin = false;
  let isSubmittingChange = false;

  function showFullPageLoading(title, subtitle) {
    if (!loginOverlay) return;
    const txt = loginOverlay.querySelector('.login-overlay-text');
    const sub = loginOverlay.querySelector('.login-overlay-sub');
    if (txt) txt.textContent = title;
    if (sub) sub.textContent = subtitle;
    loginOverlay.classList.remove('hidden');
  }

  function showLoginSuccess() {
    btn.disabled = true;
    btn.setAttribute('aria-busy', 'true');
    btnText.textContent = 'Verificando…';
    const wait = prefersReducedMotion ? 150 : 600;
    showFullPageLoading('Acessando painel', 'Verificando suas credenciais…');
    setTimeout(() => window.location.replace('dashboard.html'), wait);
  }

  function showChangeSuccess() {
    changeBtn.disabled = true;
    changeBtn.setAttribute('aria-busy', 'true');
    changeBtnText.textContent = 'Salvando…';
    const wait = prefersReducedMotion ? 150 : 600;
    showFullPageLoading('Acessando painel', 'Senha atualizada. Abrindo o painel…');
    setTimeout(() => window.location.replace('dashboard.html'), wait);
  }

  function resetButton() {
    btn.disabled = false;
    isSubmittingLogin = false;
    btn.setAttribute('aria-busy', 'false');
    btnText.textContent = 'Entrar';
  }

  function resetChangeButton() {
    changeBtn.disabled = false;
    isSubmittingChange = false;
    changeBtn.setAttribute('aria-busy', 'false');
    changeBtnText.textContent = 'Salvar nova senha';
  }

  if (needPasswordChange) {
    form.style.display = 'none';
    changeForm.style.display = 'flex';
    headerTitle.textContent = 'Alterar Senha';
    headerSubtitle.textContent = 'Defina sua nova senha';
  }

  function showChangeMode() {
    form.style.display = 'none';
    changeForm.style.display = 'flex';
    headerTitle.textContent = 'Alterar Senha';
    headerSubtitle.textContent = 'Defina sua nova senha';
  }
  const backBtn = $('back-to-login');
  if (backBtn) backBtn.addEventListener('click', (e) => { e.preventDefault(); logout(); });

  if (toggleBtn) toggleBtn.addEventListener('click', () => {
    const isPassword = passwordEl.type === 'password';
    passwordEl.type = isPassword ? 'text' : 'password';
    const vis = toggleBtn.querySelector('.eye-visible');
    const hid = toggleBtn.querySelector('.eye-hidden');
    if (vis) vis.classList.toggle('eye-hidden');
    if (hid) hid.classList.toggle('eye-hidden');
    toggleBtn.setAttribute('aria-label', isPassword ? 'Ocultar senha' : 'Mostrar senha');
  });

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (isSubmittingLogin) return;
    isSubmittingLogin = true;
    emailEl.classList.remove('shake');
    passwordEl.classList.remove('shake');
    errorEl.classList.add('hidden');
    btn.disabled = true;
    btn.setAttribute('aria-busy', 'true');
    btnText.textContent = 'Verificando…';

    const result = await login(emailEl.value, passwordEl.value);

    if (result.ok) {
      if (result.mustChangePassword) {
        needPasswordChange = true;
        currentPw.value = passwordEl.value;
        resetButton();
        showChangeMode();
      } else {
        showLoginSuccess();
      }
    } else {
      errorEl.textContent = result.error || 'E-mail ou senha inválidos. Verifique e tente novamente.';
      errorEl.classList.remove('hidden');
      emailEl.classList.add('shake');
      passwordEl.classList.add('shake');
      setTimeout(() => {
        emailEl.classList.remove('shake');
        passwordEl.classList.remove('shake');
      }, 400);
      setTimeout(resetButton, prefersReducedMotion ? 0 : 120);
      passwordEl.value = '';
      passwordEl.focus();
    }
  });

  changeForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (isSubmittingChange) return;
    changeError.classList.add('hidden');

    if (newPw.value.length < 8) {
      changeError.textContent = 'A nova senha precisa ter no mínimo 8 caracteres.';
      changeError.classList.remove('hidden');
      newPw.focus();
      return;
    }
    if (newPw.value !== confirmPw.value) {
      changeError.textContent = 'As novas senhas não conferem. Repita a mesma senha nos dois campos.';
      changeError.classList.remove('hidden');
      confirmPw.focus();
      return;
    }
    if (currentPw.value === newPw.value) {
      changeError.textContent = 'Crie uma senha diferente da atual.';
      changeError.classList.remove('hidden');
      newPw.focus();
      return;
    }

    isSubmittingChange = true;
    changeBtn.disabled = true;
    changeBtn.setAttribute('aria-busy', 'true');
    changeBtnText.textContent = 'Salvando…';

    const result = await changePassword(currentPw.value, newPw.value);

    if (result.ok) {
      needPasswordChange = false;
      showChangeSuccess();
    } else {
      changeError.textContent = result.error || 'Não foi possível salvar a nova senha. Tente novamente.';
      changeError.classList.remove('hidden');
      setTimeout(resetChangeButton, prefersReducedMotion ? 0 : 100);
    }
  });
})();
