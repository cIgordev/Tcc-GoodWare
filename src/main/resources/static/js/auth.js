'use strict';
window.goodwareFetch = async (url, options = {}) => {
  const headers = new Headers(options.headers || {});
  headers.set('X-GoodWare-Request', '1');
  const response = await fetch(url, {...options, headers, credentials: 'same-origin'});
  if (response.status === 401) { window.location.replace('/login.html'); throw new Error('Sessão expirada. Entre novamente.'); }
  return response;
};
(async () => {
  try {
    const response = await goodwareFetch('/api/auth/me');
    if (!response.ok) throw new Error('Não foi possível verificar a sessão. Tente novamente.');
    const button = document.createElement('button');
    button.type = 'button'; button.className = 'btn'; button.textContent = 'Sair da conta';
    button.addEventListener('click', async () => {
      button.disabled = true;
      try { await goodwareFetch('/api/auth/logout', {method: 'POST'}); }
      finally { sessionStorage.clear(); window.location.replace('/login.html'); }
    });
    (document.querySelector('header') || document.body).append(button);
  } catch (error) {
    const alert = document.createElement('p'); alert.setAttribute('role','alert'); alert.textContent = error.message; document.body.prepend(alert);
  }
})();
