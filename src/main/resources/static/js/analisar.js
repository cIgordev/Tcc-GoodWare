/**
 * GOODWARE — Nova análise
 *
 * Fluxo: escolher pacote -> validar (UX) -> calcular SHA-256 no navegador ->
 * consultar se já existe análise -> confirmar autorização -> iniciar -> andamento.
 *
 * SEGURANÇA
 * - Toda validação aqui é só para UX. O backend DEVE repetir e ser a fonte da verdade:
 *   tipo real do arquivo (magic bytes), tamanho, nº de arquivos, profundidade,
 *   tamanho após extração e proteção contra path traversal / zip bomb.
 * - Nome do arquivo, hash digitado e qualquer dado vindo da API são conteúdo não
 *   confiável: só entram na página via textContent / createElement (nunca innerHTML).
 * - O arquivo nunca é lido como código nem executado: só é lido como bytes para o hash.
 */

(function () {
  'use strict';

  // ---------- Configuração ----------
  const LIMITS = {
    maxBytes: 100 * 1024 * 1024, // 100 MB (deve coincidir com o limite do servidor)
    extensions: ['.zip'],
  };
  const SHA256_REGEX = /^[a-f0-9]{64}$/;

  // Etapas do pipeline de análise (seção 5 do guia do projeto)
  const STAGES = [
    { title: 'Envio e análise no servidor', desc: 'Aguarde a resposta real. Esta API não informa porcentagem intermediária.' }
  ];

  // Consulta real ao backend Spring Boot (mesma origem).
  async function lookupHash(hash) {
    const response = await goodwareFetch('/api/analyses?sha256=' + encodeURIComponent(hash));
    if (response.status === 404) return null;
    if (!response.ok) throw new Error('Erro HTTP ' + response.status);
    return normalizeAnalysis(await response.json());
  }

  function normalizeAnalysis(data) {
    return {
      id: data.id, name: data.name, sha256: data.sha256,
      concluidaEm: data.createdAt ? 'em ' + new Date(data.createdAt).toLocaleString('pt-BR') : 'recentemente',
      criticos: data.critical || 0, altos: data.high || 0,
      medios: data.medium || 0, baixos: data.low || 0,
    };
  }

  // ---------- Elementos ----------
  const $ = (id) => document.getElementById(id);

  const formSection = $('formSection');
  const tabFile = $('tabFile');
  const tabHash = $('tabHash');
  const dropzone = $('dropzone');
  const dropzoneError = $('dropzoneError');
  const fileInput = $('fileInput');
  const fileCard = $('fileCard');
  const fileName = $('fileName');
  const fileSize = $('fileSize');
  const fileHash = $('fileHash');
  const removeFileBtn = $('removeFileBtn');
  const lookup = $('lookup');
  const consent = $('consent');
  const startBtn = $('startBtn');
  const submitHint = $('submitHint');

  const hashForm = $('hashForm');
  const hashInput = $('hashInput');
  const hashSearchBtn = $('hashSearchBtn');
  const hashError = $('hashError');
  const hashResult = $('hashResult');

  const pipelineSection = $('pipelineSection');
  const pipelineFile = $('pipelineFile');
  const pipelineBadge = $('pipelineBadge');
  const pipelineActions = $('pipelineActions');
  const pipelineDone = $('pipelineDone');
  const stageList = $('stageList');
  const cancelBtn = $('cancelBtn');
  const newAnalysisBtn = $('newAnalysisBtn');

  // ---------- Estado ----------
  let currentFile = null;
  let busy = false;          // calculando hash / consultando
  let alreadyAnalyzed = false;
  let fileToken = 0;         // invalida respostas de seleções antigas


  // ---------- Helpers ----------
  function el(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined) node.textContent = text;
    return node;
  }

  function formatBytes(bytes) {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  }

  // Resumo de uma análise existente (usado na aba Arquivo e na aba Hash)
  function buildFoundSummary(found, title) {
    const frag = document.createDocumentFragment();
    frag.append(el('div', 'found-title', title));

    const meta = el('div', 'found-meta');
    meta.append(el('span', 'found-name', found.name), document.createTextNode(' · concluída ' + found.concluidaEm));
    frag.append(meta);

    const badges = el('div', 'found-badges');
    [
      ['crítico(s)', 'badge-critical', found.criticos],
      ['alto(s)', 'badge-high', found.altos],
      ['médio(s)', 'badge-medium', found.medios],
      ['baixo(s)', 'badge-low', found.baixos],
    ].forEach(([label, cls, count]) => {
      badges.append(el('span', 'badge ' + (count > 0 ? cls : 'badge-neutral'), count + ' ' + label));
    });
    frag.append(badges);

    const actions = el('div', 'lookup-actions');
    const link = el('a', 'btn btn-secondary', 'Abrir relatório');
    link.href = 'achado.html?id=' + encodeURIComponent(found.id);
    actions.append(link);
    frag.append(actions);

    return frag;
  }

  // ---------- Abas ----------
  function selectTab(tab, focus) {
    [tabFile, tabHash].forEach((t) => {
      const selected = t === tab;
      t.setAttribute('aria-selected', String(selected));
      t.tabIndex = selected ? 0 : -1;
      $(t.getAttribute('aria-controls')).hidden = !selected;
    });
    if (focus) tab.focus();
  }

  [tabFile, tabHash].forEach((tab) => {
    tab.addEventListener('click', () => selectTab(tab, false));
    tab.addEventListener('keydown', (event) => {
      const order = [tabFile, tabHash];
      const index = order.indexOf(tab);
      let next = null;
      if (event.key === 'ArrowRight') next = order[(index + 1) % order.length];
      if (event.key === 'ArrowLeft') next = order[(index - 1 + order.length) % order.length];
      if (event.key === 'Home') next = order[0];
      if (event.key === 'End') next = order[order.length - 1];
      if (next) {
        event.preventDefault();
        selectTab(next, true);
      }
    });
  });

  // ---------- Dropzone ----------
  function showDropError(message) {
    dropzoneError.textContent = message;
    dropzoneError.classList.add('is-visible');
  }

  function clearDropError() {
    dropzoneError.textContent = '';
    dropzoneError.classList.remove('is-visible');
  }

  dropzone.addEventListener('click', () => fileInput.click());
  dropzone.addEventListener('keydown', (event) => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      fileInput.click();
    }
  });

  ['dragenter', 'dragover'].forEach((name) => {
    dropzone.addEventListener(name, (event) => {
      event.preventDefault();
      dropzone.classList.add('is-dragover');
    });
  });

  ['dragleave', 'drop'].forEach((name) => {
    dropzone.addEventListener(name, (event) => {
      event.preventDefault();
      dropzone.classList.remove('is-dragover');
    });
  });

  // Evita que soltar um arquivo fora da área faça o navegador abri-lo/executá-lo
  ['dragover', 'drop'].forEach((name) => {
    window.addEventListener(name, (event) => event.preventDefault());
  });

  dropzone.addEventListener('drop', (event) => {
    const dt = event.dataTransfer;
    if (!dt) return;

    if (dt.files.length > 1) {
      showDropError('Envie um único arquivo compactado (.zip).');
      return;
    }

    const item = dt.items && dt.items[0];
    const entry = item && typeof item.webkitGetAsEntry === 'function' ? item.webkitGetAsEntry() : null;
    if (entry && entry.isDirectory) {
      showDropError('Pastas não são aceitas. Compacte o projeto em .zip.');
      return;
    }

    if (dt.files[0]) handleFile(dt.files[0]);
  });

  fileInput.addEventListener('change', (event) => {
    const file = event.target.files[0];
    if (file) handleFile(file);
  });

  function validateFile(file) {
    const name = file.name.toLowerCase();
    if (!LIMITS.extensions.some((ext) => name.endsWith(ext))) {
      return 'Formato não suportado. Envie um arquivo .zip.';
    }
    if (file.size === 0) return 'O arquivo está vazio.';
    if (file.size > LIMITS.maxBytes) return 'Arquivo acima do limite de 100 MB.';
    return null;
  }

  function handleFile(file) {
    if (busy && !formSection.hidden) { fileToken++; }
    if (formSection.hidden) return;
    const error = validateFile(file);
    if (error) {
      clearFile();
      showDropError(error);
      return;
    }
    clearDropError();
    selectFile(file);
  }

  // ---------- Hash + consulta ----------
  async function sha256Of(file) {
    if (!window.crypto || !window.crypto.subtle) throw new Error('crypto-unavailable');
    const buffer = await file.arrayBuffer(); // lido só como bytes
    const digest = await window.crypto.subtle.digest('SHA-256', buffer);
    return Array.from(new Uint8Array(digest), (b) => b.toString(16).padStart(2, '0')).join('');
  }

  function showLookup(state, content) {
    lookup.replaceChildren();
    lookup.dataset.state = state;
    lookup.classList.add('is-visible');
    if (typeof content === 'string') lookup.textContent = content;
    else if (content) lookup.append(content);
  }

  function hideLookup() {
    lookup.replaceChildren();
    lookup.classList.remove('is-visible');
    delete lookup.dataset.state;
  }

  async function selectFile(file) {
    const token = ++fileToken;
    currentFile = file;
    alreadyAnalyzed = false;
    busy = true;

    fileName.textContent = file.name;
    fileSize.textContent = formatBytes(file.size);
    fileHash.textContent = 'calculando…';
    hideLookup();
    fileCard.classList.add('is-visible');
    refreshSubmit();

    let hash = null;
    try {
      hash = await sha256Of(file);
    } catch (error) {
      if (token !== fileToken) return;
      fileHash.textContent = 'indisponível neste navegador (requer HTTPS ou localhost)';
      busy = false;
      refreshSubmit();
      return;
    }
    if (token !== fileToken) return;
    fileHash.textContent = hash;

    showLookup('loading', 'Verificando se este pacote já foi analisado…');
    try {
      const found = await lookupHash(hash);
      if (token !== fileToken) return;
      if (found) {
        alreadyAnalyzed = true;
        showLookup('found', buildFoundSummary(found, 'Este pacote já foi analisado.'));
      } else {
        showLookup('new', 'Nenhuma análise anterior para este pacote. Pronto para analisar.');
      }
    } catch (error) {
      if (token !== fileToken) return;
      showLookup('error', 'Não foi possível consultar análises anteriores. Você ainda pode iniciar uma nova análise.');
    }

    busy = false;
    refreshSubmit();
  }

  function clearFile() {
    fileToken++; // descarta respostas pendentes
    currentFile = null;
    alreadyAnalyzed = false;
    busy = false;
    fileInput.value = '';
    fileCard.classList.remove('is-visible');
    hideLookup();
    refreshSubmit();
  }

  removeFileBtn.addEventListener('click', clearFile);

  // ---------- Botão de envio ----------
  function refreshSubmit() {
    startBtn.disabled = !(currentFile && !busy && consent.checked);
    startBtn.textContent = alreadyAnalyzed ? 'Reanalisar mesmo assim' : 'Iniciar análise';

    if (!currentFile) submitHint.textContent = 'Selecione um arquivo e confirme a autorização.';
    else if (busy) submitHint.textContent = 'Verificando o pacote…';
    else if (!consent.checked) submitHint.textContent = 'Confirme a autorização para continuar.';
    else submitHint.textContent = '';
  }

  consent.addEventListener('change', refreshSubmit);

  startBtn.addEventListener('click', async () => {
    if (startBtn.disabled || !currentFile) return;
    const selectedFile = currentFile;
    busy = true;
    refreshSubmit();
    showPipeline(selectedFile.name);
    // A API atual é síncrona: mostramos estado real de envio/análise,
    // não um progresso por etapas inventado.

    Array.from(stageList.children).forEach((item) => { item.dataset.state = 'running'; });
    pipelineBadge.textContent = 'Enviando / analisando';
    cancelBtn.disabled = true;
    try {
      const formData = new FormData();
      formData.append('file', selectedFile);
      const response = await goodwareFetch('/api/analyses', { method: 'POST', body: formData });
      if (!response.ok) {
        let message = 'Erro HTTP ' + response.status;
        try { const body = await response.json(); message = body.detail || body.message || message; } catch (_) {}
        throw new Error(message);
      }
      const result = await response.json();
      Array.from(stageList.children).forEach((item) => { item.dataset.state = 'done'; });
      const reportLink = pipelineDone.querySelector('a');
      if (reportLink) reportLink.href = 'achado.html?id=' + encodeURIComponent(result.id);
      finishPipeline();
    } catch (error) {
      Array.from(stageList.children).forEach(item => { item.dataset.state = 'error'; });
      pipelineBadge.textContent = 'Falha na análise';
      pipelineBadge.className = 'badge badge-high';
      pipelineActions.hidden = false;
      pipelineDone.hidden = true;
      pipelineFile.textContent = 'Erro: ' + (error.message || 'Não foi possível enviar o arquivo');
    } finally {
      busy = false;
      cancelBtn.disabled = false;
      refreshSubmit();
    }
  });

  // ---------- Buscar por hash ----------
  function showHashError(message) {
    hashError.textContent = message;
    hashError.classList.add('is-visible');
    hashInput.classList.add('has-error');
    hashInput.setAttribute('aria-invalid','true');
  }

  function clearHashFeedback() {
    hashError.textContent = '';
    hashError.classList.remove('is-visible');
    hashInput.classList.remove('has-error');
    hashInput.removeAttribute('aria-invalid');
    hashResult.replaceChildren();
    hashResult.className = 'result-card';
    delete hashResult.dataset.state;
  }

  hashInput.addEventListener('input', () => {
    if (hashInput.classList.contains('has-error')) clearHashFeedback();
  });

  hashForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    clearHashFeedback();

    const value = hashInput.value.trim().toLowerCase();
    if (!SHA256_REGEX.test(value)) {
      showHashError('Informe um SHA-256 válido: 64 caracteres hexadecimais (0–9 e a–f).');
      return;
    }

    hashSearchBtn.disabled = true;
    hashSearchBtn.textContent = 'Buscando…';
    try {
      const found = await lookupHash(value);
      hashResult.className = 'result-card lookup is-visible';

      if (found) {
        hashResult.dataset.state = 'found';
        hashResult.append(buildFoundSummary(found, 'Análise encontrada.'));
      } else {
        hashResult.dataset.state = 'none';
        hashResult.append(el('div', 'found-title', 'Nenhuma análise encontrada para este hash.'));
        hashResult.append(el('div', 'found-meta', 'Envie o arquivo na aba “Arquivo” para iniciar uma análise.'));

        const actions = el('div', 'lookup-actions');
        const goToFile = el('button', 'btn btn-secondary', 'Enviar arquivo');
        goToFile.type = 'button';
        goToFile.addEventListener('click', () => selectTab(tabFile, true));
        actions.append(goToFile);
        hashResult.append(actions);
      }
    } catch (error) {
      showHashError('Não foi possível consultar agora. Tente novamente.');
    } finally {
      hashSearchBtn.disabled = false;
      hashSearchBtn.textContent = 'Buscar';
    }
  });

  // ---------- Estado da requisição real ----------
  function renderStages() {
    stageList.replaceChildren();
    STAGES.forEach((stage) => {
      const item = el('li', 'stage');
      item.dataset.state = 'pending';

      const icon = el('span', 'stage-icon');
      icon.setAttribute('aria-hidden', 'true');

      const body = el('div');
      body.append(el('div', 'stage-title', stage.title), el('div', 'stage-desc', stage.desc));

      item.append(icon, body);
      stageList.append(item);
    });
  }

  function showPipeline(name) {
    formSection.hidden = true;
    pipelineSection.hidden = false;
    pipelineFile.textContent = name;
    pipelineBadge.textContent = 'Em análise';
    pipelineBadge.className = 'badge badge-neutral';
    pipelineActions.hidden = false;
    pipelineDone.hidden = true;
    renderStages();
    // Aguardando resposta real do servidor.
  }

  function finishPipeline() {
    pipelineBadge.textContent = 'Concluída';
    pipelineBadge.className = 'badge badge-ok';
    pipelineActions.hidden = true;
    pipelineDone.hidden = false;
    const reportLink = pipelineDone.querySelector('a');
    if (reportLink) reportLink.focus();
  }

  function backToForm() {

    pipelineSection.hidden = true;
    formSection.hidden = false;
  }

  cancelBtn.addEventListener('click', () => { if (!busy) backToForm(); });

  newAnalysisBtn.addEventListener('click', () => {
    backToForm();
    clearFile();
    consent.checked = false;
    hashInput.value = '';
    clearHashFeedback();
    clearDropError();
    selectTab(tabFile, false);
    refreshSubmit();
  });

  refreshSubmit();
})();
