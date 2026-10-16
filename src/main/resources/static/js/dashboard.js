/* Histórico paginado persistido pelo backend; renderização sem HTML de terceiros. */
(async function () {
 'use strict';
 const list=document.getElementById('analysisList'), empty=document.getElementById('emptyState');
 const el=(tag,cls,text)=>{const n=document.createElement(tag);n.className=cls||'';if(text!==undefined)n.textContent=text;return n;};
 let page=0;
 const navigation=el('div','report-actions');list.after(navigation);
 async function load() {
  list.setAttribute('aria-busy','true');list.textContent='Carregando análises…';navigation.replaceChildren();
  try {
   const response=await goodwareFetch('/api/analyses/history?page='+page+'&size=20');
   if(!response.ok)throw new Error('Não foi possível acessar o histórico (HTTP '+response.status+').');
   const data=await response.json(),analyses=data.items;list.replaceChildren();
   empty.classList.toggle('is-visible',data.total===0);
   for(const item of analyses) {
    const row=el('div','analysis-row');
    const complete=item.status==='COMPLETED';
    row.append(el('span','analysis-name',item.name),el('span','badge '+(complete?'badge-ok':'badge-neutral'),complete?'Concluída':item.status==='RUNNING'?'Em análise':'Falhou'),
     el('span','badge '+(item.critical?'badge-critical':'badge-neutral'),complete?item.findings.length+' achados':'Sem resultado final'),
     el('span','analysis-meta',new Date(item.createdAt).toLocaleString('pt-BR')));
    if(complete){const link=el('a','analysis-link','Ver relatório');link.href='achado.html?id='+encodeURIComponent(item.id);row.append(link);}
    if(item.status==='FAILED'){
     const remove=el('button','btn btn-secondary','Excluir falha');remove.type='button';
     remove.addEventListener('click',async()=>{remove.disabled=true;try{const r=await goodwareFetch('/api/analyses/'+encodeURIComponent(item.id),{method:'DELETE'});if(!r.ok)throw new Error();await load();}catch(e){remove.disabled=false;remove.textContent='Falha ao excluir; tentar novamente';}});row.append(remove);
    }
    list.append(row);
   }
   const cards=[[data.total,'Análises no histórico','accent'],[analyses.reduce((s,a)=>s+a.critical,0),'Críticos nesta página','critical'],
    [analyses.reduce((s,a)=>s+a.high,0),'Altos nesta página','high'],[analyses.reduce((s,a)=>s+a.findings.length,0),'Achados nesta página','']];
   const grid=document.getElementById('statsGrid');grid.replaceChildren();
   cards.forEach(([value,label,tone])=>{const card=el('div','stat-card');card.dataset.tone=tone;card.append(el('div','stat-value',value),el('div','stat-label',label));grid.append(card);});
   const previous=el('button','btn btn-secondary','Anterior'),next=el('button','btn btn-secondary','Próxima');
   previous.type=next.type='button';previous.disabled=page===0;next.disabled=(page+1)*data.size>=data.total;
   previous.addEventListener('click',()=>{page--;load();});next.addEventListener('click',()=>{page++;load();});
   navigation.append(previous,el('span','analysis-meta','Página '+(page+1)),next);
  } catch(error) {
   list.replaceChildren(el('p','',error.message+' Verifique se o GoodWare e o banco estão disponíveis.'));list.setAttribute('role','alert');
   const retry=el('button','btn btn-secondary retry-button','Tentar novamente');retry.type='button';retry.addEventListener('click',load);list.append(retry);
  } finally { list.setAttribute('aria-busy','false'); }
 }
 await load();
})();
