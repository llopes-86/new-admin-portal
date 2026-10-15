const Export = {
  guestColumns: [
    { key: 'sheet_row_id', label: '#', w: 6 },
    { key: 'nome',         label: 'Nome / Responsável', w: 22 },
    { key: 'tipo',         label: 'Tipo', w: 10 },
    { key: 'idade',        label: 'Idade', w: 8 },
    { key: 'nome_crianca', label: 'Nome Da Criança', w: 22 },
    { key: 'restricoes',   label: 'Restrições', w: 22 },
    { key: 'contato',      label: 'Contato', w: 18 },
    { key: 'data',         label: 'Data', w: 12 },
    { key: 'status',       label: 'Status', w: 10 }
  ],

  messageColumns: [
    { key: 'sheet_row_id', label: '#', w: 6 },
    { key: 'nome',         label: 'Nome', w: 20 },
    { key: 'mensagem',     label: 'Mensagem', w: 50 },
    { key: 'data',         label: 'Data', w: 14 },
    { key: 'status',       label: 'Status', w: 10 }
  ],

  _generateHash() {
    const chars = 'abcdefghijklmnopqrstuvwxyz0123456789';
    let hash = '';
    for (let i = 0; i < 8; i++) hash += chars.charAt(Math.floor(Math.random() * chars.length));
    return hash;
  },

  _filePrefix(type) {
    return type === 'messages' ? 'mensagens-mural-ml' : 'convidados-bodas-ml';
  },

  _fileName(type) {
    const d = new Date();
    const pad = n => String(n).padStart(2, '0');
    return `${this._filePrefix(type)}-${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}-${this._generateHash()}`;
  },

  _guestMetaInfo(rows) {
    const now = new Date();
    const statusFilter = document.getElementById('guests-filter');
    const catFilter = document.getElementById('guests-category-filter');
    const statusLabel = statusFilter?.selectedOptions[0]?.textContent || 'Todos';
    const catLabel = catFilter?.selectedOptions[0]?.textContent || 'Todas as categorias';
    return {
      title: 'Convidados — Bodas M&L',
      filters: `Filtros: ${statusLabel} | ${catLabel} | ${rows.length} registros`,
      exported: `Exportado em: ${now.toLocaleDateString('pt-BR')} às ${now.toLocaleTimeString('pt-BR')}`,
      dateObj: now
    };
  },

  _messageMetaInfo(rows) {
    const now = new Date();
    const filterEl = document.getElementById('messages-filter');
    const filterLabel = filterEl?.selectedOptions[0]?.textContent || 'Todos';
    return {
      title: 'Mensagens do Mural — Bodas M&L',
      filters: `Filtro: ${filterLabel} | ${rows.length} mensagens`,
      exported: `Exportado em: ${now.toLocaleDateString('pt-BR')} às ${now.toLocaleTimeString('pt-BR')}`,
      dateObj: now
    };
  },

  _buildGuestRows(filtered) {
    if (!filtered?.length) return [];
    return filtered.map(item => {
      const restricoesArr = ParseUtils.parseRestricoes(item.restricoes);
      const tipo = item.adulto === '1' ? 'Adulto' : item.criancas === '1' ? 'Criança' : '-';
      return {
        sheet_row_id: item.sheet_row_id != null ? String(item.sheet_row_id) : '-',
        nome: item.responsavel || item.nome || '-',
        tipo,
        idade: item.idade || '-',
        responsavel: item.responsavel || '-',
        nome_crianca: item.nome_crianca || '-',
        restricoes: restricoesArr.length ? restricoesArr.join(', ') : (item.restricoes || '-'),
        contato: item.contato || '-',
        data: formatDate(item.created),
        status: item.status || 'Pendente'
      };
    });
  },

  _pdfGuestColumnStyles() {
    return {
      0: { cellWidth: 12 },
      1: { cellWidth: 45 },
      2: { cellWidth: 18 },
      3: { cellWidth: 15 },
      4: { cellWidth: 40 },
      5: { cellWidth: 55 },
      6: { cellWidth: 35 },
      7: { cellWidth: 32 },
      8: { cellWidth: 20 }
    };
  },

  _pdfMessageColumnStyles() {
    return {
      0: { cellWidth: 12 },
      1: { cellWidth: 35 },
      2: { cellWidth: 155, overflow: 'linebreak', valign: 'top' },
      3: { cellWidth: 38 },
      4: { cellWidth: 25 }
    };
  },

  _buildMessageRows(filtered) {
    if (!filtered?.length) return [];
    return filtered.map(item => ({
      sheet_row_id: item.sheet_row_id != null ? String(item.sheet_row_id) : '-',
      nome: item.nome || 'Anônimo',
      mensagem: item.mensagem || '-',
      data: formatDate(item.created),
      status: item.status || 'Pendente'
    }));
  },

  gerarXLSX(opts) {
    const { guests, messages } = opts || {};
    if (!guests && !messages) { showToast('Nenhum dado selecionado.', 'error'); return; }

    showToast('Gerando Excel…', 'info');
    const wb = XLSX.utils.book_new();
    let guestCount = 0;
    let msgCount = 0;

    if (guests) {
      const rows = this._buildGuestRows(Guests.filtered);
      guestCount = rows.length;
      if (rows.length) {
        const headers = this.guestColumns.map(c => c.label);
        const data = [headers];
        rows.forEach(r => data.push(this.guestColumns.map(c => r[c.key])));
        const ws = XLSX.utils.aoa_to_sheet(data);
        ws['!cols'] = this.guestColumns.map(c => ({ wch: c.w }));
        XLSX.utils.book_append_sheet(wb, ws, 'Convidados');
      }
    }

    if (messages) {
      const rows = this._buildMessageRows(Messages.filtered);
      msgCount = rows.length;
      if (rows.length) {
        const headers = this.messageColumns.map(c => c.label);
        const data = [headers];
        rows.forEach(r => data.push(this.messageColumns.map(c => r[c.key])));
        const ws = XLSX.utils.aoa_to_sheet(data);
        ws['!cols'] = this.messageColumns.map(c => ({ wch: c.w }));
        XLSX.utils.book_append_sheet(wb, ws, 'Mural');
      }
    }

    if (wb.SheetNames.length === 0) {
      showToast('Nenhum registro para exportar.', 'error');
      return;
    }

    const sheetCount = wb.SheetNames.length;
    const name = sheetCount === 1
      ? this._fileName(guests ? 'guests' : 'messages')
      : `dados-bodas-ml-${new Date().toISOString().slice(0, 10).replace(/-/g, '')}-${this._generateHash()}`;

    XLSX.writeFile(wb, `${name}.xlsx`);
    const totalXlsx = guestCount + msgCount;
    showToast(`Excel exportado: ${totalXlsx} registro${totalXlsx !== 1 ? 's' : ''}!`, 'success');
  },

  gerarPDF(opts) {
    const { guests, messages } = opts || {};
    if (!guests && !messages) { showToast('Nenhum dado selecionado.', 'error'); return; }

    showToast('Gerando PDF…', 'info');
    let guestCount = 0;
    let msgCount = 0;

    const doc = new jspdf.jsPDF({ orientation: 'landscape', unit: 'mm', format: 'a4' });
    let startY = 14;
    let firstSection = true;

    if (guests) {
      const rows = this._buildGuestRows(Guests.filtered);
      guestCount = rows.length;
      if (rows.length) {
        const meta = this._guestMetaInfo(rows);
        const headers = this.guestColumns.map(c => c.label);
        const body = rows.map(r => this.guestColumns.map(c => String(r[c.key] || '')));

        if (!firstSection) startY += 8;
        doc.setFontSize(14);
        doc.setTextColor(30, 41, 59);
        doc.text(meta.title, 14, startY);

        doc.setFontSize(9);
        doc.setTextColor(100, 116, 139);
        doc.text(meta.filters, 14, startY + 6);
        doc.text(meta.exported, 14, startY + 12);

        doc.autoTable({
          head: [headers],
          body,
          startY: startY + 17,
          styles: { fontSize: 8, cellPadding: 2, lineColor: [200, 200, 200], lineWidth: 0.2, overflow: 'linebreak', valign: 'top' },
          headStyles: { fillColor: [5, 150, 105], textColor: [255, 255, 255], fontStyle: 'bold', halign: 'center' },
          columnStyles: this._pdfGuestColumnStyles(),
          alternateRowStyles: { fillColor: [245, 247, 250] },
          rowPageBreak: 'avoid',
          showHead: 'everyPage',
          margin: { top: 32, left: 14, right: 14 },
          didDrawPage: () => {}
        });

        startY = doc.lastAutoTable.finalY + 6;
        firstSection = false;
      }
    }

    if (messages) {
      const rows = this._buildMessageRows(Messages.filtered);
      msgCount = rows.length;
      if (rows.length) {
        const meta = this._messageMetaInfo(rows);
        const headers = this.messageColumns.map(c => c.label);
        const body = rows.map(r => this.messageColumns.map(c => String(r[c.key] || '')));

        if (!firstSection) startY += 8;
        doc.setFontSize(14);
        doc.setTextColor(30, 41, 59);
        doc.text(meta.title, 14, startY);

        doc.setFontSize(9);
        doc.setTextColor(100, 116, 139);
        doc.text(meta.filters, 14, startY + 6);
        doc.text(meta.exported, 14, startY + 12);

        doc.autoTable({
          head: [headers],
          body,
          startY: startY + 17,
          styles: { fontSize: 8, cellPadding: 2, lineColor: [200, 200, 200], lineWidth: 0.2, overflow: 'linebreak', valign: 'top' },
          headStyles: { fillColor: [99, 102, 241], textColor: [255, 255, 255], fontStyle: 'bold', halign: 'center' },
          columnStyles: this._pdfMessageColumnStyles(),
          alternateRowStyles: { fillColor: [245, 247, 250] },
          rowPageBreak: 'avoid',
          showHead: 'everyPage',
          margin: { top: 32, left: 14, right: 14 },
          didDrawPage: () => {}
        });

        startY = doc.lastAutoTable.finalY + 6;
        firstSection = false;
      }
    }

    if (firstSection) {
      showToast('Nenhum registro para exportar.', 'error');
      return;
    }

    const footerDate = `Exportado em: ${new Date().toLocaleDateString('pt-BR')} às ${new Date().toLocaleTimeString('pt-BR')}`;
    const totalPages = doc.internal.getNumberOfPages();
    for (let i = 1; i <= totalPages; i++) {
      doc.setPage(i);
      doc.setFontSize(8);
      doc.setTextColor(148, 163, 184);
      doc.text(footerDate, 14, doc.internal.pageSize.height - 8);
      doc.text(`Página ${i}`, doc.internal.pageSize.width - 14, doc.internal.pageSize.height - 8, { align: 'right' });
    }

    const guestName = guests ? this._fileName('guests') : null;
    const msgName = messages ? this._fileName('messages') : null;
    const name = guestName && msgName
      ? `dados-bodas-ml-${new Date().toISOString().slice(0, 10).replace(/-/g, '')}-${this._generateHash()}`
      : (guestName || msgName);

    doc.save(`${name}.pdf`);
    const totalPdf = guestCount + msgCount;
    showToast(`PDF exportado: ${totalPdf} registro${totalPdf !== 1 ? 's' : ''}!`, 'success');
  }
};