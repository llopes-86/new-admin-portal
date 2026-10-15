const Guests = {
  data: [],
  filtered: [],
  currentPage: 1,
  pageSize: 25,

  get pendentes() {
    return this.data.filter(r => (r.status || 'Pendente') === 'Pendente');
  },

  async load() {
    requireAuth();
    try {
      this.data = (await PB.fetchRSVP()).map(item => {
        if (item.status === 'Validado') item.status = 'Confirmado';
        return item;
      });
      this.filtered = this.data.slice();
      this.currentPage = 1;
      this.applyFilters();
      this.updateStats();
    } catch (err) {
      showToast('Erro ao carregar convidados: ' + err.message, 'error');
    }
  },


  applyFilters(resetPage = true) {
    const rawSearch = ($('#guests-search').value || '').trim();
    const search = rawSearch.toLowerCase();
    const searchDigits = search.replace(/\D/g, '');
    const statusFilter = $('#guests-filter').value;
    const categoryFilter = $('#guests-category-filter').value;
    this.filtered = this.data.filter(item => {
      if (!this._matchesSearch(item, search, searchDigits)) return false;
      if (statusFilter !== 'all' && item.status !== statusFilter) return false;
      if (categoryFilter !== 'all' && !this._matchesCategory(item, categoryFilter)) return false;
      return true;
    });
    this.updateFilterCounts(search, searchDigits, statusFilter, categoryFilter);
    if (resetPage) {
      this.currentPage = 1;
    } else if (this.currentPage > Math.ceil(this.filtered.length / this.pageSize)) {
      this.currentPage = Math.max(1, Math.ceil(this.filtered.length / this.pageSize));
    }
    this.render();
  },

  _matchesSearch(item, search, searchDigits) {
    if (!search) return true;
    const hay = `${item.nome || ''} ${item.responsavel || ''} ${item.nome_crianca || ''}`.toLowerCase();
    if (hay.includes(search)) return true;
    if ((searchDigits || '').length >= 2) {
      const contatoDigits = String(item.contato || '').replace(/\D/g, '');
      if (contatoDigits !== '' && contatoDigits.includes(searchDigits)) return true;
    }
    return false;
  },

  updateFilterCounts(search, searchDigits, statusFilter, categoryFilter) {
    const paint = (selectId, countFor) => {
      const sel = $(selectId);
      if (!sel) return;
      Array.from(sel.options).forEach(opt => {
        if (!opt.dataset.baselabel) opt.dataset.baselabel = opt.textContent;
        const n = this.data.filter(item => {
          if (!this._matchesSearch(item, search, searchDigits)) return false;
          return countFor(item, opt.value);
        }).length;
        opt.textContent = `${opt.dataset.baselabel} (${n})`;
      });
    };
    paint('#guests-filter', (item, v) => {
      if (categoryFilter !== 'all' && !this._matchesCategory(item, categoryFilter)) return false;
      if (v !== 'all' && item.status !== v) return false;
      return true;
    });
    paint('#guests-category-filter', (item, v) => {
      if (statusFilter !== 'all' && item.status !== statusFilter) return false;
      if (v !== 'all' && !this._matchesCategory(item, v)) return false;
      return true;
    });
  },

  _matchesCategory(item, category) {
    if (category === 'adultos') return item.adulto === '1';
    if (category === 'criancas') return item.criancas === '1';
    const restricoesArr = ParseUtils.parseRestricoes(item.restricoes);
    const lower = restricoesArr.join(',').toLowerCase();
    if (category === 'necessidades') return lower.indexOf('necessidade') !== -1 || lower.indexOf('especial') !== -1;
    if (category === 'veganos') return lower.indexOf('vegano') !== -1 || lower.indexOf('vegan') !== -1;
    if (category === 'lactose') return lower.indexOf('lactose') !== -1;
    return true;
  },

  render() {
    this.renderSummary();
    this.renderTable();
    this.renderCards();
    this.renderApproveAllBtn();
    this.renderPagination();
  },

  renderSummary() {
    const total = this.data.length;
    const p = this.pendentes.length;
    let text = `${total} registros (${p} pendentes)`;
    const searchActive = (($('#guests-search').value || '').trim() !== '');
    const statusActive = $('#guests-filter').value !== 'all';
    const catActive = $('#guests-category-filter').value !== 'all';
    if (searchActive || statusActive || catActive) {
      text += ` • ${this.filtered.length} na seleção`;
    }
    $('#stats-guests').textContent = text;
  },

  renderApproveAllBtn() {
    const container = $('#guests-approve-all');
    if (!container) return;
    const p = this.pendentes.length;
    if (p === 0) { container.classList.add('hidden'); return; }
    container.classList.remove('hidden');
    const span = container.querySelector('span');
    const btn = container.querySelector('button');
    if (span) span.textContent = `${p} pendente${p !== 1 ? 's' : ''}`;
    if (btn) btn.onclick = () => Guests.approveAll();
  },

  renderTable() {
    const tbody = $('#guests-body');
    if (this.filtered.length === 0) {
      tbody.innerHTML = '<tr><td colspan="9" class="empty-state"><p>Nenhum convidado encontrado</p></td></tr>';
      return;
    }
    const start = (this.currentPage - 1) * this.pageSize;
    const end = Math.min(start + this.pageSize, this.filtered.length);
    const page = this.filtered.slice(start, end);
    let html = '';
    for (let i = 0; i < page.length; i++) {
      const g = page[i];

      const restricoesArr = ParseUtils.parseRestricoes(g.restricoes);
      const hasNecessidades = ParseUtils.hasNecessidades(restricoesArr);
      const outrasRestricoes = ParseUtils.outrasRestricoes(restricoesArr);

      let restricoesCell = '';
      if (outrasRestricoes.length) {
        restricoesCell += esc(outrasRestricoes.join(', '));
      }
      if (hasNecessidades) {
        if (restricoesCell) restricoesCell += '<br>';
        restricoesCell += '<span class="cell-necessidades">Necessidades Especiais</span>';
      }
      if (!restricoesCell) restricoesCell = '-';

      let criancaCell = '-';
      if (g.criancas === '1' && g.nome_crianca) {
        criancaCell = esc(g.nome_crianca);
      }

      const nomeCell = g.responsavel ? esc(g.responsavel) : esc(g.nome || '-');
      const status = g.status || 'Pendente';
      const statusEsc = esc(status);
      const statusLcEsc = esc(status.toLowerCase());

      html += `<tr${hasNecessidades ? ' class="row-necessidades"' : ''}>
        <td class="col-fixed">${esc(g.sheet_row_id || '-')}</td>
        <td><strong>${nomeCell}</strong></td>
        <td>${g.adulto === '1' ? 'Adulto' : g.criancas === '1' ? 'Criança' : '-'}</td>
        <td>${esc(g.idade || '-')}</td>
        <td>${criancaCell}</td>
        <td>${restricoesCell}</td>
        <td>${esc(g.contato || '-')}</td>
        <td>${formatDate(g.created)}</td>
        <td><span class="badge badge-${statusLcEsc}" data-id="${esc(g.id)}" data-current="${statusEsc}">${statusEsc}</span></td>
        </tr>`;
    }
    tbody.innerHTML = html;
    tbody.querySelectorAll('.badge').forEach(el => {
      el.addEventListener('click', function (e) {
        e.stopPropagation();
        Guests.toggleStatus(this.dataset.id, this.dataset.current, this);
      });
    });
  },

  renderCards() {
    const container = $('#guests-cards');
    if (this.filtered.length === 0) {
      container.innerHTML = '<div class="empty-state"><p>Nenhum convidado encontrado</p></div>';
      return;
    }
    const start = (this.currentPage - 1) * this.pageSize;
    const end = Math.min(start + this.pageSize, this.filtered.length);
    const page = this.filtered.slice(start, end);
    let html = '';
    for (let i = 0; i < page.length; i++) {
      const g = page[i];
      const isAdulto  = g.adulto === '1';
      const isCrianca = g.criancas === '1';

      const restricoesArr = ParseUtils.parseRestricoes(g.restricoes);
      const hasNecessidades = ParseUtils.hasNecessidades(restricoesArr);
      const outrasRestricoes = ParseUtils.outrasRestricoes(restricoesArr);

      let cardTitle;
      if (isCrianca && g.responsavel) {
        cardTitle = `<strong>Nome do responsável:</strong> <span style="font-weight:400">${esc(g.responsavel)}</span>`;
      } else if (isAdulto) {
        cardTitle = `<strong>Nome do convidado(a):</strong> <span style="font-weight:400">${esc(g.nome || '-')}</span>`;
      } else {
        cardTitle = esc(g.nome || '-');
      }

      let rows = '';

      if (isCrianca && g.nome_crianca) {
        rows += `<div class="guest-card-row"><strong>Nome da criança / adolescente:</strong> ${esc(g.nome_crianca)}</div>`;
      }
      if (g.idade) {
        rows += `<div class="guest-card-row"><strong>Idade:</strong> ${esc(g.idade)}</div>`;
      }

      if (isAdulto) {
        rows += '<div class="guest-card-row"><strong>Adulto</strong></div>';
      }

      if (g.contato) {
        rows += `<div class="guest-card-row"><strong>Contato:</strong> ${esc(g.contato)}</div>`;
      }

      if (outrasRestricoes.length) {
        rows += `<div class="guest-card-row"><strong>Restrições:</strong> ${esc(outrasRestricoes.join(', '))}</div>`;
      }

      if (hasNecessidades) {
        rows += '<div class="guest-card-row guest-card-necessidades"><strong>Necessidades Especiais:</strong> Sim</div>';
      }

      const status = g.status || 'Pendente';
      const statusEsc = esc(status);
      const statusLcEsc = esc(status.toLowerCase());

      html += `<div class="card guest-card">
        <div class="guest-card-header">
          <span class="guest-card-name">${cardTitle}</span>
          <span class="guest-card-row-id">#${esc(g.sheet_row_id || '-')}</span>
        </div>
        <div class="guest-card-body">${rows}</div>
        <div class="guest-card-footer">
          <span><strong>Data:</strong> ${formatDate(g.created)}</span>
          <span class="badge badge-${statusLcEsc}" data-id="${esc(g.id)}" data-current="${statusEsc}">${statusEsc}</span>
        </div></div>`;
    }
    container.innerHTML = html;
    container.querySelectorAll('.badge').forEach(el => {
      el.addEventListener('click', function (e) {
        e.stopPropagation();
        Guests.toggleStatus(this.dataset.id, this.dataset.current, this);
      });
    });
  },

  async toggleStatus(id, current, el) {
    const next = current === 'Pendente' ? 'Confirmado' : 'Pendente';
    requireAuth();
    el.textContent = '…';
    try {
      await PB.update('rsvp', id, { status: next });
      const item = this.data.find(i => i.id === id);
      if (item) item.status = next;
      el.className = `badge badge-${next.toLowerCase()}`;
      el.textContent = next;
      el.dataset.current = next;
      el.classList.remove('badge-status-updated');
      void el.offsetWidth;
      el.classList.add('badge-status-updated');
      el.addEventListener('animationend', () => el.classList.remove('badge-status-updated'), { once: true });
      this.updateStats();
      this.renderApproveAllBtn();
      showToast(`Status alterado para ${next}`, 'success');
    } catch (err) {
      el.textContent = current;
      showToast(`Erro ao atualizar: ${err.message}`, 'error');
    }
  },

  approveAll() {
    const p = this.pendentes;
    if (!p.length) return;
    showModal(
      'Confirmar ação',
      `Confirmar <strong>${p.length}</strong> convidados pendentes?`,
      () => this._approveAllPending(p)
    );
  },

  async _approveAllPending(pending) {
    let success = 0;
    let failed = 0;
    showToast(`Confirmando ${pending.length} convidados…`, 'info');
    for (let i = 0; i < pending.length; i++) {
      try {
        await PB.update('rsvp', pending[i].id, { status: 'Confirmado' });
        pending[i].status = 'Confirmado';
        success++;
      } catch (_) {
        failed++;
      }
    }
    this.updateStats();
    this.applyFilters(false);
    if (failed > 0) {
      showToast(`${success} confirmados, ${failed} falha(s)`, failed === pending.length ? 'error' : 'warning');
    } else {
      showToast(`${success} convidados confirmados!`, 'success');
    }
  },

  updateStats() {
    const p = this.pendentes.length;
    $('#nav-guests-count').textContent = p || '';
    $('#nav-guests-count').className = `badge-count${p === 0 ? ' zero' : ''}`;
    $('#nav-mobile-guests-count').textContent = p || '';
    $('#nav-mobile-guests-count').className = `badge-count${p === 0 ? ' zero' : ''}`;
  },

  renderPagination() {
    const container = $('#guests-pagination');
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
