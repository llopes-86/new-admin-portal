(async () => {
  const auth = requireAuth();
  if (!auth) return;

  const mustChange = await checkMustChangePassword();
  if (mustChange) { window.location.replace('index.html'); return; }

  let currentTab = null;
  let switching = false;
  let swipeAnimating = false;

  function switchTab(tab, direction) {
    if (switching || swipeAnimating) return;
    if (tab === currentTab && !direction) return;
    switching = true;
    const prefersReduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const isMobile = window.innerWidth <= 1024.98;
    const useAnim = !!direction && isMobile && !prefersReduced;

    const prevTab = currentTab;
    currentTab = tab;

    const prevEl = document.getElementById(`tab-${prevTab}`);
    const nextEl = document.getElementById(`tab-${tab}`);

    function finalize() {
      document.querySelectorAll('.tab-content').forEach(el => {
        el.classList.remove('active', 'slide-in-left', 'slide-in-right', 'slide-out-left', 'slide-out-right');
        el.style.transform = '';
        el.style.opacity = '';
      });
      if (nextEl) nextEl.classList.add('active');
      document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));
      const navItem = document.querySelector(`.nav-item[data-tab="${tab}"]`);
      if (navItem) navItem.classList.add('active');
      document.querySelectorAll('.bottom-nav-item[data-tab]').forEach(el => el.classList.remove('active'));
      const bnItem = document.querySelector(`.bottom-nav-item[data-tab="${tab}"]`);
      if (bnItem) bnItem.classList.add('active');

      const titles = { guests: 'Lista de convidados', messages: 'Mensagens do mural' };
      const titleEl = document.getElementById('tab-title');
      if (titleEl) titleEl.textContent = titles[tab] || tab;

      const statsG = document.getElementById('stats-guests');
      if (statsG) statsG.classList.toggle('hidden', tab !== 'guests');
      const statsM = document.getElementById('stats-messages');
      if (statsM) statsM.classList.toggle('hidden', tab !== 'messages');

      if (tab === 'guests') Guests.load();
      else if (tab === 'messages') Messages.load();
      switching = false;
      swipeAnimating = false;
    }

    if (useAnim && prevEl && nextEl && prevEl !== nextEl) {
      swipeAnimating = true;
      const outClass = direction === 'left' ? 'slide-out-left' : 'slide-out-right';
      const inClass = direction === 'left' ? 'slide-in-right' : 'slide-in-left';
      prevEl.classList.add(outClass);
      nextEl.classList.add('active', inClass);
      document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));
      const navItem = document.querySelector(`.nav-item[data-tab="${tab}"]`);
      if (navItem) navItem.classList.add('active');
      document.querySelectorAll('.bottom-nav-item[data-tab]').forEach(el => el.classList.remove('active'));
      const bnItem = document.querySelector(`.bottom-nav-item[data-tab="${tab}"]`);
      if (bnItem) bnItem.classList.add('active');
      const titles = { guests: 'Lista de convidados', messages: 'Mensagens do mural' };
      const titleEl = document.getElementById('tab-title');
      if (titleEl) titleEl.textContent = titles[tab] || tab;
      const statsG = document.getElementById('stats-guests');
      if (statsG) statsG.classList.toggle('hidden', tab !== 'guests');
      const statsM = document.getElementById('stats-messages');
      if (statsM) statsM.classList.toggle('hidden', tab !== 'messages');
      setTimeout(() => {
        document.querySelectorAll('.tab-content').forEach(el => {
          el.classList.remove('slide-out-left', 'slide-out-right', 'slide-in-left', 'slide-in-right');
        });
        prevEl.classList.remove('active');
        if (tab === 'guests') Guests.load();
        else if (tab === 'messages') Messages.load();
        switching = false;
        swipeAnimating = false;
      }, 320);
      return;
    }

    document.querySelectorAll('.tab-content').forEach(el => el.classList.remove('active'));
    if (nextEl) nextEl.classList.add('active');
    document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));
    const navItem = document.querySelector(`.nav-item[data-tab="${tab}"]`);
    if (navItem) navItem.classList.add('active');
    document.querySelectorAll('.bottom-nav-item[data-tab]').forEach(el => el.classList.remove('active'));
    const bnItem = document.querySelector(`.bottom-nav-item[data-tab="${tab}"]`);
    if (bnItem) bnItem.classList.add('active');

    const titles = { guests: 'Lista de convidados', messages: 'Mensagens do mural' };
    const titleEl = document.getElementById('tab-title');
    if (titleEl) titleEl.textContent = titles[tab] || tab;

    const statsG = document.getElementById('stats-guests');
    if (statsG) statsG.classList.toggle('hidden', tab !== 'guests');
    const statsM = document.getElementById('stats-messages');
    if (statsM) statsM.classList.toggle('hidden', tab !== 'messages');

    if (tab === 'guests') Guests.load();
    else if (tab === 'messages') Messages.load();
    switching = false;
  }

  document.querySelectorAll('.nav-item[data-tab]').forEach(btn => {
    btn.addEventListener('click', () => switchTab(btn.dataset.tab));
  });

  document.querySelectorAll('.bottom-nav-item[data-tab]').forEach(btn => {
    btn.addEventListener('click', () => switchTab(btn.dataset.tab));
  });

  function handleLogoutRequest(e) {
    if (e) e.preventDefault();
    if (document.body.classList.contains('is-logging-out')) return;
    const useConfirm = typeof CONFIG !== 'undefined' && CONFIG.CONFIRM_LOGOUT;
    if (useConfirm) {
      showModal(
        'Sair do sistema',
        'Tem certeza que deseja encerrar sua sessão? Você precisará fazer login novamente.',
        () => logout()
      );
    } else {
      logout();
    }
  }

  const logoutBtn = document.getElementById('logout-btn');
  if (logoutBtn) logoutBtn.addEventListener('click', handleLogoutRequest);

  const logoutMobile = document.getElementById('logout-btn-mobile');
  if (logoutMobile) logoutMobile.addEventListener('click', handleLogoutRequest);

  const exportBtn = document.getElementById('export-btn');
  if (exportBtn) exportBtn.addEventListener('click', showExportModal);
  const exportMobile = document.getElementById('export-btn-mobile');
  if (exportMobile) exportMobile.addEventListener('click', showExportModal);

  const search = document.getElementById('guests-search');
  if (search) search.addEventListener('input', () => Guests.applyFilters());
  const guestFilter = document.getElementById('guests-filter');
  if (guestFilter) guestFilter.addEventListener('change', () => Guests.applyFilters());
  const guestCatFilter = document.getElementById('guests-category-filter');
  if (guestCatFilter) guestCatFilter.addEventListener('change', () => Guests.applyFilters());
  const msgFilter = document.getElementById('messages-filter');
  if (msgFilter) msgFilter.addEventListener('change', () => Messages.applyFilters());

  renderUserInfo();
  renderVersion();

  switchTab('guests');
  Messages.load().catch(() => {});

  initSwipe();
  window.switchTab = switchTab;

  function showPageIndicator(current, total) {
    if (total <= 1) return;
    let el = document.getElementById('page-indicator');
    if (!el) {
      el = document.createElement('div');
      el.id = 'page-indicator';
      el.className = 'page-indicator';
      el.setAttribute('aria-live', 'polite');
      document.body.appendChild(el);
    }
    el.textContent = `${current}/${total}`;
    el.classList.remove('page-indicator');
    void el.offsetWidth;
    el.classList.add('page-indicator');
    clearTimeout(el._hideTimer);
    el._hideTimer = setTimeout(() => { if (el.parentNode) el.remove(); }, 700);
  }

  function getPaginatedEl() {
    return document.getElementById(currentTab === 'guests' ? 'guests-paginated' : 'messages-paginated') || document.getElementById(`tab-${currentTab}`);
  }

  function triggerBounce() {
    const el = getPaginatedEl();
    if (!el) return;
    const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (reduced) return;
    el.classList.remove('page-bounce');
    void el.offsetWidth;
    el.classList.add('page-bounce');
    el.addEventListener('animationend', () => el.classList.remove('page-bounce'), { once: true });
    if (navigator.vibrate) { try { navigator.vibrate(20); } catch (_) {} }
  }

  function animatePagination(direction, target) {
    const el = getPaginatedEl();
    const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const total = Math.ceil(target.filtered.length / target.pageSize);
    if (!el || reduced) { target.render(); if (total > 1) showPageIndicator(target.currentPage, total); return; }
    const outClass = direction === 'left' ? 'slide-out-left' : 'slide-out-right';
    const inClass = direction === 'left' ? 'slide-in-right' : 'slide-in-left';
    el.classList.add(outClass);
    setTimeout(() => {
      target.render();
      el.classList.remove(outClass);
      el.classList.add(inClass);
      el.addEventListener('animationend', () => el.classList.remove(inClass), { once: true });
      if (total > 1) showPageIndicator(target.currentPage, total);
    }, 180);
  }

  function paginateSwipe(direction) {
    if (currentTab === 'guests') {
      const total = Math.ceil(Guests.filtered.length / Guests.pageSize);
      if (total <= 1) { triggerBounce(); return; }
      if (direction === 'left' && Guests.currentPage < total) {
        Guests.currentPage++;
        animatePagination('left', Guests);
      } else if (direction === 'right' && Guests.currentPage > 1) {
        Guests.currentPage--;
        animatePagination('right', Guests);
      } else {
        triggerBounce();
      }
    } else if (currentTab === 'messages') {
      const total = Math.ceil(Messages.filtered.length / Messages.pageSize);
      if (total <= 1) { triggerBounce(); return; }
      if (direction === 'left' && Messages.currentPage < total) {
        Messages.currentPage++;
        animatePagination('left', Messages);
      } else if (direction === 'right' && Messages.currentPage > 1) {
        Messages.currentPage--;
        animatePagination('right', Messages);
      } else {
        triggerBounce();
      }
    }
  }

  function initSwipe() {
    const swipeArea = document.querySelector('[data-swipeable]') || document.querySelector('.main-content');
    if (!swipeArea) return;
    const MIN_DISTANCE = 60;
    const MAX_VERTICAL = 50;
    const MAX_DURATION = 500;
    const MIN_VELOCITY = 0.3;
    let startX = 0, startY = 0, startTime = 0, tracking = false;

    function isTypingTarget(el) {
      if (!el) return false;
      const tag = el.tagName;
      return tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT' || el.isContentEditable;
    }

    swipeArea.addEventListener('touchstart', e => {
      if (window.innerWidth > 1024.98) return;
      if (e.touches.length !== 1) return;
      if (isTypingTarget(e.target)) return;
      const t = e.touches[0];
      startX = t.clientX;
      startY = t.clientY;
      startTime = Date.now();
      tracking = true;
    }, { passive: true });

    swipeArea.addEventListener('touchend', e => {
      if (!tracking) return;
      tracking = false;
      if (window.innerWidth > 1024.98) return;
      if (e.changedTouches.length !== 1) return;
      const t = e.changedTouches[0];
      const dx = t.clientX - startX;
      const dy = t.clientY - startY;
      const elapsed = Date.now() - startTime;
      if (elapsed > MAX_DURATION) return;
      if (Math.abs(dy) > MAX_VERTICAL) return;
      if (Math.abs(dx) < MIN_DISTANCE) return;
      if (Math.abs(dx) < Math.abs(dy)) return;
      const velocity = Math.abs(dx) / elapsed;
      if (velocity < MIN_VELOCITY) return;
      const wrapper = e.target.closest('.table-wrapper');
      if (wrapper && wrapper.scrollWidth > wrapper.clientWidth) {
        const canScrollLeft = wrapper.scrollLeft > 0;
        const canScrollRight = wrapper.scrollLeft + wrapper.clientWidth < wrapper.scrollWidth - 1;
        if ((dx < 0 && canScrollRight) || (dx > 0 && canScrollLeft)) return;
      }
      if (dx < 0) paginateSwipe('left');
      else paginateSwipe('right');
    }, { passive: true });

    swipeArea.addEventListener('touchcancel', () => { tracking = false; }, { passive: true });
  }
})();

function renderUserInfo() {
  const auth = getAuth();
  if (!auth?.user) return;
  const email = auth.user.email || '';
  const initial = email.charAt(0).toUpperCase();

  const elD = document.getElementById('user-email');
  if (elD) elD.textContent = email;
  const elI = document.getElementById('user-initial');
  if (elI) elI.textContent = initial;
  const elM = document.getElementById('user-email-mobile');
  if (elM) elM.textContent = email;
  const elIM = document.getElementById('user-initial-mobile');
  if (elIM) elIM.textContent = initial;
}

function renderVersion() {
  const v = (typeof CONFIG !== 'undefined' && CONFIG.APP_VERSION) ? CONFIG.APP_VERSION : '';
  const el = document.getElementById('app-version-dashboard');
  if (el && v) el.textContent = v;
}

function showModal(title, bodyHTML, onConfirm) {
  const existing = document.getElementById('modal-overlay');
  if (existing) existing.remove();

  const overlay = document.createElement('div');
  overlay.id = 'modal-overlay';
  overlay.className = 'modal-overlay';
  overlay.innerHTML = `
    <div class="modal-dialog">
      <div class="modal-title">${esc(title)}</div>
      <div class="modal-body">${sanitizeHTML(bodyHTML)}</div>
      <div class="modal-actions">
        <button class="btn btn-outline" data-action="cancel">Cancelar</button>
        <button class="btn btn-primary" data-action="confirm">Confirmar</button>
      </div>
    </div>`;

  overlay.addEventListener('click', e => {
    if (e.target === overlay) overlay.remove();
  });
  overlay.querySelector('[data-action="cancel"]').addEventListener('click', () => overlay.remove());
  overlay.querySelector('[data-action="confirm"]').addEventListener('click', () => {
    overlay.remove();
    if (onConfirm) onConfirm();
  });

  document.body.appendChild(overlay);
}

function showExportModal() {
  const existing = document.getElementById('modal-overlay');
  if (existing) existing.remove();

  const guestCount = Guests.filtered ? Guests.filtered.length : 0;
  const msgCount = Messages.filtered ? Messages.filtered.length : 0;

  if (guestCount === 0 && msgCount === 0) {
    showToast('Nenhum registro para exportar.', 'error');
    return;
  }

  const overlay = document.createElement('div');
  overlay.id = 'modal-overlay';
  overlay.className = 'modal-overlay';
  overlay.innerHTML = `
    <div class="modal-dialog" style="max-width:460px">
      <div class="modal-title">Exportar Dados</div>
      <div class="modal-body">
        <label class="checkbox-label">
          <input type="checkbox" id="export-guests"${guestCount > 0 ? ' checked' : ''}>
          <span>Convidados (${guestCount} registros)</span>
        </label>
        <label class="checkbox-label">
          <input type="checkbox" id="export-messages"${msgCount > 0 ? ' checked' : ''}>
          <span>Mural (${msgCount} mensagens)</span>
        </label>
      </div>
      <div class="modal-actions">
        <button class="btn btn-outline" data-action="cancel">Cancelar</button>
        <button class="btn btn-primary" data-action="pdf">Exportar PDF</button>
        <button class="btn btn-primary" data-action="xlsx">Exportar Excel</button>
      </div>
    </div>`;

  overlay.addEventListener('click', e => {
    if (e.target === overlay) overlay.remove();
  });
  overlay.querySelector('[data-action="cancel"]').addEventListener('click', () => overlay.remove());
  overlay.querySelector('[data-action="pdf"]').addEventListener('click', () => {
    const guests = document.getElementById('export-guests').checked;
    const messages = document.getElementById('export-messages').checked;
    if (!guests && !messages) { showToast('Selecione pelo menos um conjunto de dados.', 'error'); return; }
    overlay.remove();
    Export.gerarPDF({ guests, messages });
  });
  overlay.querySelector('[data-action="xlsx"]').addEventListener('click', () => {
    const guests = document.getElementById('export-guests').checked;
    const messages = document.getElementById('export-messages').checked;
    if (!guests && !messages) { showToast('Selecione pelo menos um conjunto de dados.', 'error'); return; }
    overlay.remove();
    Export.gerarXLSX({ guests, messages });
  });

  document.body.appendChild(overlay);
}
