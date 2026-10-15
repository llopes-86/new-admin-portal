function showToast(msg, type = 'info') {
  let container = document.getElementById('toast-container');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toast-container';
    container.className = 'toast-container';
    document.body.appendChild(container);
  }
  const icons = {
    success: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>',
    error: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>',
    info: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>'
  };
  const el = document.createElement('div');
  el.className = `toast toast-${type}`;
  const textEl = document.createElement('span');
  textEl.textContent = msg;
  el.innerHTML = icons[type] || icons.info;
  el.appendChild(textEl);
  container.appendChild(el);
  setTimeout(() => {
    el.classList.add('toast-out');
    let removed = false;
    const remove = () => {
      if (removed) return;
      removed = true;
      if (el.parentNode) el.remove();
    };
    el.addEventListener('animationend', remove, { once: true });
    // Fallback: reduced-motion or interrupted animations may never fire animationend
    setTimeout(remove, 400);
  }, 4000);
}
