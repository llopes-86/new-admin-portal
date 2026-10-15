const CACHE = 'bodas-admin-v2.0.12';
const SHELL = [
  '/',
  '/css/admin.css',
  '/css/fonts.css',
  '/css/base/reset.css',
  '/css/base/variables.css',
  '/css/components/badges.css',
  '/css/components/buttons.css',
  '/css/components/cards.css',
  '/css/components/forms.css',
  '/css/components/modal.css',
  '/css/components/sidebar.css',
  '/css/components/table.css',
  '/css/components/toast.css',
  '/images/Sankofa.png',
  '/images/africa-day-bg.jpg',
  '/images/icons/favicon-16x16.png',
  '/images/icons/favicon-32x32.png',
  '/images/icons/favicon.ico',
  '/images/icons/apple-touch-icon.png',
  '/images/icons/icon-192.png',
  '/images/icons/icon-512.png',
  '/images/icons/icon-maskable-512.png',
  '/lib/fonts/Inter-latin.woff2',
  '/lib/fonts/Inter-latin-ext.woff2',
  '/lib/pocketbase/pocketbase.umd.js',
  '/lib/xlsx/xlsx.full.min.js',
  '/lib/jspdf/jspdf.umd.min.js',
  '/lib/jspdf-autotable/jspdf.plugin.autotable.min.js',
  '/js/config.js',
  '/js/app.js',
  '/js/login.js',
  '/js/sw-register.js',
  '/js/utils/auth.js',
  '/js/utils/theme.js',
  '/js/utils/dom.js',
  '/js/utils/toast.js',
  '/js/utils/sanitize.js',
  '/js/utils/parse.js',
  '/js/api/pocketbase.js',
  '/js/features/guests.js',
  '/js/features/messages.js',
  '/js/features/export.js'
];

self.addEventListener('install', e => {
  e.waitUntil(
    caches.open(CACHE).then(cache => {
      return cache.addAll(SHELL);
    })
  );
  self.skipWaiting();
});

self.addEventListener('activate', e => {
  e.waitUntil(
    caches.keys().then(keys =>
      Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k)))
    )
  );
  self.clients.claim();
});

self.addEventListener('fetch', e => {
  if (e.request.mode === 'navigate') {
    e.respondWith(
      fetch(e.request).catch(() => caches.match('/offline.html'))
    );
    return;
  }
  e.respondWith(
    caches.match(e.request).then(cached => cached || fetch(e.request))
  );
});
