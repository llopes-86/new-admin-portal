(function () {
  const STORAGE_KEY = 'theme';
  const THEME_LIGHT = 'light';
  const THEME_DARK = 'dark';

  function getStoredTheme() {
    try { return localStorage.getItem(STORAGE_KEY); } catch (_) { return null; }
  }

  function getPreferredTheme() {
    const stored = getStoredTheme();
    if (stored === THEME_DARK || stored === THEME_LIGHT) return stored;
    return THEME_LIGHT;
  }

  function applyTheme(theme) {
    const root = document.documentElement;
    if (theme === THEME_DARK) {
      root.setAttribute('data-theme', 'dark');
    } else {
      root.removeAttribute('data-theme');
    }
    try { localStorage.setItem(STORAGE_KEY, theme); } catch (_) {}
    const meta = document.querySelector('meta[name="theme-color"]');
    if (meta) meta.setAttribute('content', theme === THEME_DARK ? '#020617' : '#0F1A2E');
    const btn = document.getElementById('theme-toggle');
    if (btn) {
      const isDark = theme === THEME_DARK;
      btn.setAttribute('aria-pressed', isDark ? 'true' : 'false');
      btn.setAttribute('aria-label', isDark ? 'Mudar para tema claro' : 'Mudar para tema escuro');
      btn.setAttribute('title', isDark ? 'Mudar para tema claro' : 'Mudar para tema escuro');
      const sun = btn.querySelector('.theme-icon-sun');
      const moon = btn.querySelector('.theme-icon-moon');
      if (sun) sun.classList.toggle('hidden', isDark);
      if (moon) moon.classList.toggle('hidden', !isDark);
    }
  }

  function toggleTheme() {
    const current = document.documentElement.getAttribute('data-theme') === 'dark' ? THEME_DARK : THEME_LIGHT;
    const next = current === THEME_DARK ? THEME_LIGHT : THEME_DARK;
    applyTheme(next);
  }

  function initTheme() {
    applyTheme(getPreferredTheme());
    const btn = document.getElementById('theme-toggle');
    if (btn) btn.addEventListener('click', toggleTheme);
  }

  window.initTheme = initTheme;
  window.toggleTheme = toggleTheme;
  window.applyTheme = applyTheme;

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initTheme);
  } else {
    initTheme();
  }
})();
