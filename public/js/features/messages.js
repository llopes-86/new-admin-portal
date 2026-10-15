const Messages = {
  data: [],
  filtered: [],
  currentPage: 1,
  pageSize: 12,

  get pendentes() {
    return this.data.filter(r => (r.status || 'Pendente') === 'Pendente');
  },

  async load() {
    requireAuth();
    try {
      this.data = await PB.fetchMessages();
      this.filtered = this.data.slice();
      this.currentPage = 1;
      this.applyFilters();
      this.updateStats();
    } catch (err) {
      showToast('Erro ao carregar mensagens: ' + err.message, 'error');
    }
  },


  applyFilters(resetPage = true) {
    const filter = $('#messages-filter').value;
    this.filtered = filter === 'all' ? this.data.slice() : this.data.filter(m => m.status === filter);
    this.updateFilterCounts();
    if (resetPage) {
      this.currentPage = 1;
    } else if (this.currentPage > Math.ceil(this.filtered.length / this.pageSize)) {
      this.currentPage = Math.max(1, Math.ceil(this.filtered.length / this.pageSize));
    }
    this.render();
  },

  render() {
    this.renderSummary();
    this.renderCards();
    this.renderApproveAllBtn();
    this.renderPagination();
  },

  updateFilterCounts() {
    const sel = $('#messages-filter');
    if (!sel) return;
    Array.from(sel.options).forEach(opt => {
      if (!opt.dataset.baselabel) opt.dataset.baselabel = opt.textContent;
      const v = opt.value;
      const n = v === 'all' ? this.data.length : this.data.filter(m => m.status === v).length;
      opt.textContent = `${opt.dataset.baselabel} (${n})`;
    });
  },

  renderSummary() {
    let text = `${this.data.length} mensagens (${this.pendentes.length} pendentes)`;
    if ($('#messages-filter').value !== 'all') {
      text += ` • ${this.filtered.length} na seleção`;
    }
    $('#stats-messages').textContent = text;
  },

  renderApproveAllBtn() {
    const container = $('#messages-approve-all');
    if (!container) return;
    const p = this.pendentes.length;
    if (p === 0) { container.classList.add('hidden'); return; }
    container.classList.remove('hidden');
    const span = container.querySelector('span');
    const btn = container.querySelector('button');
    if (span) span.textContent = `${p} pendente${p !== 1 ? 's' : ''}`;
    if (btn) btn.onclick = () => Messages.approveAll();
  },

  renderCards() {
    const grid = $('#messages-grid');
    if (this.filtered.length === 0) {
      grid.innerHTML = '<div class="empty-state"><p>Nenhuma mensagem encontrada</p></div>';
      return;
    }
    const start = (this.currentPage - 1) * this.pageSize;
    const end = Math.min(start + this.pageSize, this.filtered.length);
    const page = this.filtered.slice(start, end);
    let html = '';
    for (let i = 0; i < page.length; i++) {
      const m = page[i];
      const status = m.status || 'Pendente';
      const statusEsc = esc(status);
      const statusLcEsc = esc(status.toLowerCase());
      html += `<div class="card msg-card" data-id="${esc(m.id)}">
        <div class="msg-card-header">
          <span class="msg-card-name">${esc(m.nome || 'Anônimo')}</span>
          <span class="msg-card-date">${formatDate(m.created)}</span>
        </div>
        <div class="msg-card-text">${esc(m.mensagem || '')}</div>
        <div class="msg-card-status"><span class="badge badge-${statusLcEsc}">${statusEsc}</span></div>
        <div class="msg-card-actions">
          <button class="btn btn-sm btn-approve" data-action="approve">Aprovar</button>
          <button class="btn btn-sm btn-reject" data-action="reject">Rejeitar</button>
          <button class="btn btn-sm btn-pending" data-action="pending">Pendente</button>
        </div></div>`;
    }
    grid.innerHTML = html;
    grid.querySelectorAll('.msg-card-actions button[data-action]').forEach(btn => {
      btn.addEventListener('click', () => {
        const card = btn.closest('.msg-card');
        this.toggleStatus(card.dataset.id, btn.dataset.action);
      });
    });
  },

  async toggleStatus(id, action) {
    const next = action === 'approve' ? 'Aprovado' : action === 'reject' ? 'Rejeitado' : 'Pendente';
    requireAuth();
    const item = this.data.find(i => i.id === id);
    if (!item) return;
    try {
      await PB.update('messages', id, { status: next });
      item.status = next;
      this.applyFilters(false);
      this.updateStats();
      const cardId = (typeof CSS !== 'undefined' && CSS.escape) ? CSS.escape(id) : id;
      const badge = document.querySelector(`.msg-card[data-id="${cardId}"] .badge`);
      if (badge) {
        badge.classList.remove('badge-status-updated');
        void badge.offsetWidth;
        badge.classList.add('badge-status-updated');
        badge.addEventListener('animationend', () => badge.classList.remove('badge-status-updated'), { once: true });
      }
      const label = next === 'Aprovado' ? 'aprovada' : next === 'Rejeitado' ? 'rejeitada' : 'marcada como pendente';
      showToast(`Mensagem ${label}`, 'success');
    } catch (err) {
      showToast(`Erro ao atualizar: ${err.message}`, 'error');
    }
  },

  approveAll() {
    const p = this.pendentes;
    if (!p.length) return;
    showModal(
      'Confirmar ação',
      `Aprovar <strong>${p.length}</strong> mensagens pendentes?`,
      () => this._approveAllPending(p)
    );
  },

  async _approveAllPending(pending) {
    let success = 0;
    let failed = 0;
    showToast(`Aprovando ${pending.length} mensagens…`, 'info');
    for (let i = 0; i < pending.length; i++) {
      try {
        await PB.update('messages', pending[i].id, { status: 'Aprovado' });
        pending[i].status = 'Aprovado';
        success++;
      } catch (_) {
        failed++;
      }
    }
    this.updateStats();
    this.applyFilters(false);
    if (failed > 0) {
      showToast(`${success} aprovadas, ${failed} falha(s)`, failed === pending.length ? 'error' : 'warning');
    } else {
      showToast(`${success} mensagens aprovadas!`, 'success');
    }
  },

  updateStats() {
    const p = this.pendentes.length;
    $('#nav-messages-count').textContent = p || '';
    $('#nav-messages-count').className = `badge-count${p === 0 ? ' zero' : ''}`;
    $('#nav-mobile-messages-count').textContent = p || '';
    $('#nav-mobile-messages-count').className = `badge-count${p === 0 ? ' zero' : ''}`;
  },

  renderPagination() {
    const container = $('#messages-pagination');
    const totalPages = Math.ceil(this.filtered.length / this.pageSize);
    if (totalPages <= 1) { container.innerHTML = ''; return; }
    const isMobile = window.innerWidth <= 1024;
    const maxVisible = isMobile ? 5 : 10;
    let start = Math.max(1, this.currentPage - Math.floor(maxVisible / 2));
    let end = Math.min(totalPages, start + maxVisible - 1);
    if (end - start + 1 < maxVisible) { start = Math.max(1, end - maxVisible + 1); }
    let html = '';
    if (start > 1) { html += '<button data-page="1">1</button>'; if (start > 2) html += '<span class="pagination-ellipsis">...</span>'; }
    for (let p = start; p <= end; p++) {
      html += `<button class="${p === this.currentPage ? 'active' : ''}" data-page="${p}">${p}</button>`;
    }
    if (end < totalPages) { if (end < totalPages - 1) html += '<span class="pagination-ellipsis">...</span>'; html += `<button data-page="${totalPages}">${totalPages}</button>`; }
    container.innerHTML = html;
    container.querySelectorAll('button').forEach(btn => {
      btn.addEventListener('click', () => {
        this.currentPage = parseInt(btn.dataset.page);
        this.render();
      });
    });
  }
};
