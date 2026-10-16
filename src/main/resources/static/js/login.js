'use strict';
const form = document.getElementById('authForm');
const feedback = document.getElementById('authFeedback');
form.addEventListener('submit', async event => {
  event.preventDefault();
  const action = event.submitter?.value || 'login';
  const buttons = form.querySelectorAll('button'); buttons.forEach(b => b.disabled = true);
  feedback.textContent = 'Aguarde…';
  try {
    const response = await fetch('/api/auth/' + action, {
      method: 'POST', credentials: 'same-origin', headers: {'Content-Type':'application/json', 'X-GoodWare-Request':'1'},
      body: JSON.stringify({email: form.elements.email.value.trim(), password: form.elements.password.value})
    });
    const data = await response.json();
    if (!response.ok) throw new Error(data.detail || 'Não foi possível concluir. Tente novamente.');
    form.elements.password.value = '';
    if (action === 'login') window.location.replace('/painel.html');
    else feedback.textContent = data.message;
  } catch (error) { feedback.textContent = error.message; }
  finally { buttons.forEach(b => b.disabled = false); }
});
