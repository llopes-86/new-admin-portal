function esc(s) {
  return String(s ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

const HTML_ALLOWED_TAGS = new Set(['STRONG', 'B', 'EM', 'I', 'U', 'BR', 'P', 'SPAN', 'UL', 'OL', 'LI', 'CODE']);

function sanitizeHTML(html) {
  const doc = new DOMParser().parseFromString(String(html || ''), 'text/html');
  const walk = (node) => {
    Array.from(node.childNodes).forEach((child) => {
      if (child.nodeType === Node.ELEMENT_NODE) {
        if (!HTML_ALLOWED_TAGS.has(child.tagName)) {
          child.replaceWith(document.createTextNode(child.textContent || ''));
          return;
        }
        Array.from(child.attributes).forEach((attr) => child.removeAttribute(attr.name));
        walk(child);
      } else if (child.nodeType === Node.COMMENT_NODE) {
        child.remove();
      }
    });
  };
  walk(doc.body);
  return doc.body.innerHTML;
}

function formatDate(d) {
  if (!d) return '-';
  try {
    return new Date(d).toLocaleDateString('pt-BR', {
      day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit'
    });
  } catch (_) { return d; }
}

function formatDateShort(d) {
  if (!d) return '-';
  try { return new Date(d).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' }); }
  catch (_) { return d; }
}
