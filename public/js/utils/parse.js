const ParseUtils = {
  parseRestricoes(str) {
    let arr = [];
    try {
      const parsed = JSON.parse(str || '');
      arr = Array.isArray(parsed) ? parsed : (parsed ? [parsed] : []);
    } catch (_) {
      arr = str ? str.split(',').map(s => s.trim()).filter(Boolean) : [];
    }
    return arr;
  },

  hasNecessidades(arr) {
    const joined = arr.join(',').toLowerCase();
    return joined.includes('necessidade') || joined.includes('especial');
  },

  outrasRestricoes(arr) {
    return arr.filter(r => r.toLowerCase().indexOf('necessidade') === -1 && r.toLowerCase().indexOf('especial') === -1);
  }
};
