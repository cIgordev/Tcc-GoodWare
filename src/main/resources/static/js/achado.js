/**
 * GOODWARE — Tela de Achado
 * Campos seguem o "formato padrão de um achado" do guia do projeto:
 * id, categoria, severidade, arquivo, linha, evidência, impacto,
 * recomendação e confiança.
 *
 * SEGURANÇA: trechos de código vêm de arquivos analisados (conteúdo
 * potencialmente hostil). Por isso tudo é inserido com textContent /
 * createElement — nunca com innerHTML — para evitar XSS no próprio analisador.
 */

(function () {
  let findings = [];
  let activeId = null;
  const severityNames = {critical:'Crítica',high:'Alta',medium:'Média',low:'Baixa'};
  const search = document.getElementById('findingSearch');
  const filter = document.getElementById('severityFilter');
  function visibleFindings() {
    const query = search.value.trim().toLocaleLowerCase('pt-BR');
    return findings.filter(item => (filter.value === 'all' || item.severidade === filter.value) &&
      [item.titulo,item.arquivo,item.ruleId,item.categoria].join(' ').toLocaleLowerCase('pt-BR').includes(query));
  }
  function applyFilters() {
    const visible = visibleFindings();
    if (!visible.some(item => item.id === activeId)) activeId = visible[0]?.id;
    if (activeId) select(activeId);
    else { renderSidebar(null); detail.textContent = 'Nenhum achado corresponde aos filtros. Tente outro termo ou selecione todas as severidades.'; }
  }
  search.addEventListener('input', applyFilters);
  filter.addEventListener('change', applyFilters);
  const analysisId = new URLSearchParams(window.location.search).get('id');
  function normalizeFinding(f) {
    const sev = String(f.severity || 'LOW').toLowerCase();
    return {
      id: f.id || f.ruleId || 'N/A', ruleId: f.ruleId, categoria: categoryName(f.category),
      severidade: sev, tag: 'Severidade ' + (severityNames[sev] || sev), titulo: f.title || 'Achado',
      arquivo: f.file || 'N/A', linha: f.line ?? '-',
      codigoInicio: f.line || 1, codigo: [f.evidence || 'Sem trecho disponível'],
      linhaEvidencia: f.line || 1, evidencia: f.evidence || '',
      impacto: f.impact || '', recomendacao: f.recommendation || '',
      confianca: ({HIGH:'Alta',MEDIUM:'Média',LOW:'Baixa'})[f.confidence] || f.confidence || 'Não definida', quando: 'Análise atual',
    };
  }

  function categoryName(value) { return ({POTENTIAL_VULNERABILITY:'Vulnerabilidade potencial', SUSPICIOUS_INDICATOR:'Indicador suspeito', KNOWN_SIGNATURE:'Assinatura catalogada', TEST_SIGNATURE:'Assinatura de teste', DEPENDENCY_ADVISORY:'Aviso de dependência', COVERAGE_NOTICE:'Limitação de cobertura'})[value] || value; }

  const BADGE_BY_SEVERITY = {
    critical: 'badge-critical',
    high: 'badge-high',
    medium: 'badge-medium',
    low: 'badge-low',
  };

  const sidebar = document.getElementById('findingSidebar');
  const detail = document.getElementById('findingDetail');

  // ---------- helpers de DOM seguros ----------
  function el(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined) node.textContent = text;
    return node;
  }

  // ---------- Lista lateral ----------
  function renderSidebar(activeId) {
    sidebar.replaceChildren();

    const visible = visibleFindings();
    document.getElementById('findingCount').textContent = visible.length + ' de ' + findings.length + ' achados';
    visible.forEach((item) => {
      const btn = el('button', 'finding-item' + (item.id === activeId ? ' is-active' : ''));
      btn.type = 'button';
      btn.setAttribute('aria-pressed', String(item.id === activeId));

      const dot = el('span', 'finding-dot');
      dot.dataset.sev = item.severidade;

      const text = el('span');
      text.append(
        el('span', 'finding-item-title', item.titulo),
        el('span', 'finding-item-when', (severityNames[item.severidade] || item.severidade) + ' · ' + item.arquivo)
      );

      btn.append(dot, text);
      btn.addEventListener('click', () => { select(item.id); detail.focus({preventScroll:true}); if (window.matchMedia('(max-width:850px)').matches) detail.scrollIntoView({block:'start'}); });
      sidebar.append(btn);
    });
  }

  // ---------- Detalhe ----------
  function renderDetail(item) {
    detail.replaceChildren();

    // cabeçalho: tag + título + botão
    const header = el('div', 'detail-header');
    const headerText = el('div');
    headerText.append(
      el('span', 'badge ' + BADGE_BY_SEVERITY[item.severidade], item.tag),
      el('h2', 'detail-title', item.titulo)
    );
    const classification = el('div', 'detail-classification');
    classification.append(el('span','badge badge-neutral',item.categoria), el('span','badge badge-neutral','Confiança: ' + item.confianca));
    headerText.append(classification);
    const resolveBtn = el('button', 'btn btn-secondary', 'Marcar como revisado');
    resolveBtn.type = 'button';
    const reviewKey = 'goodware-reviewed-' + analysisId + '-' + item.id;
    try { if (sessionStorage.getItem(reviewKey)) resolveBtn.textContent = 'Revisado nesta aba'; } catch (_) {}
    resolveBtn.addEventListener('click', () => { resolveBtn.textContent = 'Revisado nesta aba'; try { sessionStorage.setItem(reviewKey, '1'); } catch (_) {} });
    header.append(headerText, resolveBtn);

    // metadados
    const meta = el('div', 'detail-meta');
    [['Regra', item.ruleId], ['ID', item.id], ['Arquivo', item.arquivo], ['Linha', String(item.linha)]].forEach(([k, v]) => {
      const span = el('span');
      span.append(el('b', '', k + ' '), document.createTextNode(v));
      meta.append(span);
    });

    // bloco de código com linha de evidência destacada
    const code = el('div', 'code-block');
    item.codigo.forEach((text, i) => {
      const num = item.codigoInicio + i;
      const line = el('div', 'code-line' + (num === item.linhaEvidencia ? ' is-evidence' : ''));
      line.append(el('span', 'code-num', String(num)), el('span', 'code-text', text));
      code.append(line);
    });

    // evidência / impacto / recomendação / confiança
    const cause = el('div', 'root-cause');
    const title = el('h3');
    const icon = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    icon.setAttribute('width', '14');
    icon.setAttribute('height', '14');
    icon.setAttribute('viewBox', '0 0 14 14');
    const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
    path.setAttribute('d', 'M7 0 L8.6 5.4 L14 7 L8.6 8.6 L7 14 L5.4 8.6 L0 7 L5.4 5.4 Z');
    path.setAttribute('fill', 'currentColor');
    icon.append(path);
    title.append(icon, document.createTextNode('Evidência e recomendação'));

    const dl = el('dl');
    [
      ['Evidência', item.evidencia],
      ['Impacto', item.impacto],
      ['Recomendação', item.recomendacao],
      ['Confiança', item.confianca],
    ].forEach(([k, v]) => {
      dl.append(el('dt', '', k), el('dd', '', v));
    });

    cause.append(title, dl);

    const disclaimer = el(
      'p',
      'disclaimer',
      'Indicador não é confirmação: este achado sugere risco e deve ser validado por uma pessoa antes de qualquer conclusão.'
    );

    detail.append(header, meta, code, cause, disclaimer);
  }

  function select(id) {
    const item = findings.find((f) => f.id === id);
    if (!item) return;
    activeId = id;
    renderSidebar(id);
    renderDetail(item);
  }

  function renderSummary(data) {
    const summary = document.getElementById('reportSummary');
    summary.replaceChildren(el('h1', '', data.name), el('p', 'mono report-hash', 'SHA-256: ' + data.sha256),
      el('p', 'text-muted', data.filesAnalyzed + ' arquivos · ' + data.findings.length + ' achados · ' + data.durationMs + ' ms · ' + new Date(data.createdAt).toLocaleString('pt-BR')));
    const metrics = el('div', 'report-metrics');
    [['critical','Críticos'],['high','Altos'],['medium','Médios'],['low','Baixos']].forEach(([key,label]) => metrics.append(el('span','badge badge-'+key,data[key]+' '+label)));
    summary.append(metrics, el('p','notice','Análise estática limitada. A ausência de achados não garante segurança; indicadores exigem revisão humana. O histórico é preservado no banco após reiniciar o servidor.'));
    const warnings = el('ul', 'report-warnings');
    data.warnings.forEach(w => warnings.append(el('li', '', w)));
    const coverage = el('details', 'coverage-details');
    coverage.append(el('summary', '', 'Cobertura e limitações (' + data.warnings.length + ')'), warnings);
    summary.append(coverage);
    const actions = el('div', 'report-actions');
    const download = el('a', 'btn btn-primary', 'Baixar relatório JSON');
    download.href = '/api/analyses/' + encodeURIComponent(data.id) + '/report'; download.download = 'goodware-report.json';
    const remove = el('button', 'btn btn-secondary', 'Excluir relatório'); remove.type = 'button';
    remove.addEventListener('click', async () => {
      if (!window.confirm('Excluir este relatório do histórico local?')) return;
      remove.disabled = true;
      try { const response = await goodwareFetch('/api/analyses/' + encodeURIComponent(data.id), {method:'DELETE'});
        if (!response.ok) throw new Error('Não foi possível excluir.'); window.location.href = 'painel.html';
      } catch (error) { remove.disabled = false; const message = el('p', 'notice', error.message); message.setAttribute('role','alert'); summary.append(message); }
    });
    actions.append(download, remove); summary.append(actions);
    const inventory = document.getElementById('fileInventory'); inventory.replaceChildren();
    for (const file of data.files) {
      const row = el('tr');
      const path = el('td', 'mono', file.path); path.append(el('small', 'file-sha', file.sha256));
      row.append(path, el('td', '', file.size), el('td', '', file.language), el('td', '', file.note), el('td', '', file.findings)); inventory.append(row);
    }
    document.getElementById('inventorySection').hidden = false;
  }

  async function loadAnalysis() {
    if (!analysisId) {
      document.getElementById('reportStatus').textContent = 'Sem análise';
      document.querySelector('.mock-breadcrumb').textContent = 'Relatório';
      detail.textContent = 'Abra este relatório pelo botão Ver relatório após realizar uma análise.';
      return;
    }
    try {
      const response = await goodwareFetch('/api/analyses/' + encodeURIComponent(analysisId));
      if (!response.ok) throw new Error('Análise não encontrada (HTTP ' + response.status + ')');
      const data = await response.json();
      if (data.status !== 'COMPLETED') {
        document.getElementById('reportStatus').textContent = data.status === 'RUNNING' ? 'Em análise' : 'Falhou';
        detail.textContent = data.status === 'RUNNING' ? 'Análise em andamento. Atualize esta página para consultar o estado. Se o processo foi interrompido, envie o ZIP novamente.' : 'A análise não foi concluída. Verifique o ZIP e tente novamente.';
        return;
      }
      document.getElementById('reportStatus').textContent = 'Concluída';
      document.getElementById('reportStatus').className = 'badge badge-ok';
      renderSummary(data);
      const breadcrumb = document.querySelector('.mock-breadcrumb');
      if (breadcrumb) breadcrumb.textContent = (data.name || 'Projeto') + ' / achados';
      findings = (data.findings || []).map(normalizeFinding);
      document.getElementById('findingToolbar').hidden = !findings.length;
      if (!findings.length) {
        sidebar.textContent = 'Nenhum achado registrado';
        detail.textContent = 'Nenhuma regra disparou nesta análise. Isso não garante que o projeto seja seguro.';
        return;
      }
      select(findings[0].id);
    } catch (error) {
      document.getElementById('reportStatus').textContent = 'Indisponível';
      document.querySelector('.mock-breadcrumb').textContent = 'Relatório indisponível';
      detail.setAttribute('role','alert');
      detail.textContent = 'Não foi possível carregar o relatório: ' + error.message + '. Verifique o banco de dados e se o relatório foi excluído.';
    }
  }
  loadAnalysis();
})();
