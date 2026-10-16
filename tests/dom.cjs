/* Teste de DOM + HTTP real. Não substitui inspeção visual nem um navegador real. */
const {JSDOM,ResourceLoader,VirtualConsole}=require('jsdom');
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {webcrypto}=require('node:crypto');
const base=process.env.GOODWARE_TEST_URL||'http://127.0.0.1:8080';
const results=[],errors=[];let dom;
const watchdog=setTimeout(()=>{console.error('DOM: execução não concluiu em 45 segundos');process.exit(1);},45000);
class LocalResources extends ResourceLoader {
 fetch(url,options){if(!url.startsWith(base))return null;const controller=new AbortController();const result=fetch(url,{headers:{Connection:'close'},signal:controller.signal}).then(r=>r.arrayBuffer()).then(b=>Buffer.from(b));result.abort=()=>controller.abort();return result;}
}
async function load(route) {
 console.error('DOM:',route);
 if(dom)dom.window.close();
 const vc=new VirtualConsole();vc.on('jsdomError',e=>{if(!e.message.includes('Could not parse CSS'))errors.push(e.message);});
 const html=await (await fetch(base+route,{headers:{Connection:'close'}})).text();
 dom=new JSDOM(html,{url:base+route,resources:new LocalResources(),runScripts:'dangerously',virtualConsole:vc,
  beforeParse(w){
   w.matchMedia=()=>({matches:true,addEventListener(){},removeEventListener(){}});
   w.confirm=()=>true;
   Object.defineProperty(w.crypto,'subtle',{value:webcrypto.subtle});
   w.File.prototype.arrayBuffer=function(){return new Promise((resolve,reject)=>{const r=new w.FileReader();r.onload=()=>resolve(r.result);r.onerror=reject;r.readAsArrayBuffer(this);});};
   w.fetch=async(url,opts={})=>{
    const options={...opts,headers:{...opts.headers,Connection:'close'}};
    if(opts.body instanceof w.FormData){const form=new FormData();for(const [key,value] of opts.body.entries())form.append(key,value instanceof w.File?new Blob([await value.arrayBuffer()]):value,value instanceof w.File?value.name:undefined);options.body=form;}
    return fetch(new URL(url,base),options);
   };
  }});
 await new Promise((resolve,reject)=>{const timer=setTimeout(()=>reject(new Error('Timeout ao carregar '+route)),10000); const done=()=>{clearTimeout(timer);resolve();};if(dom.window.document.readyState==='complete')done();else dom.window.addEventListener('load',done,{once:true});});
 return dom.window.document;
}
async function until(fn,label){const end=Date.now()+6000;while(!fn()){if(Date.now()>end)throw new Error('Timeout: '+label);await new Promise(r=>setTimeout(r,20));}}
async function upload(name){const doc=await load('/analisar.html');const w=dom.window;const file=new w.File([fs.readFileSync(path.join(__dirname,'..','examples',name))],name,{type:'application/zip'});
 Object.defineProperty(doc.getElementById('fileInput'),'files',{value:[file],configurable:true});doc.getElementById('fileInput').dispatchEvent(new w.Event('change'));
 const consent=doc.getElementById('consent');consent.checked=true;consent.dispatchEvent(new w.Event('change'));
 await until(()=>!doc.getElementById('startBtn').disabled,'botão iniciar');doc.getElementById('startBtn').click();
 await until(()=>['Concluída','Falha na análise'].includes(doc.getElementById('pipelineBadge').textContent),'resposta real');return doc;
}
(async()=>{
 let doc=await load('/');assert(doc.querySelector('.hero h1'));results.push('Landing original: scripts e navegação carregados');
 doc=await load('/login.html');assert.equal(doc.querySelectorAll('input[type=password]').length,0);assert(doc.body.textContent.includes('sem conta ou senha'));results.push('Acesso local sem autenticação simulada');
 doc=await upload('projeto-vulneravel.zip');assert.equal(doc.getElementById('pipelineBadge').textContent,'Concluída');
 const reportRoute=new URL(doc.querySelector('#pipelineDone a').href).pathname+new URL(doc.querySelector('#pipelineDone a').href).search;
 doc=await load(reportRoute);await until(()=>doc.querySelectorAll('#findingSidebar button').length===5,'5 achados');
 assert.equal(doc.querySelectorAll('#fileInventory tr').length,4);
 const report=await (await fetch(doc.querySelector('#reportSummary a').href)).json();
 assert.equal(report.findings.length,5);assert(!JSON.stringify(report).includes('fixture-secret-not-real'));
 const button=doc.querySelector('#findingDetail button');button.click();assert.equal(button.textContent,'Revisado nesta aba');
 results.push('Upload real, 5 achados, 4 arquivos, exportação JSON e revisão por aba; segredo mascarado');
 fs.writeFileSync(path.join(__dirname,'..','examples','relatorio-exemplo.json'),JSON.stringify(report,null,2)+'\n');
 doc=await load('/analisar.html');doc.getElementById('tabHash').click();doc.getElementById('hashInput').value=report.sha256;doc.getElementById('hashForm').dispatchEvent(new dom.window.Event('submit',{cancelable:true}));await until(()=>doc.querySelector('#hashResult a'),'consulta SHA');results.push('Consulta por hash real');
 doc=await load('/painel.html');await until(()=>doc.querySelector('#analysisList .analysis-row'),'painel');assert(doc.getElementById('analysisList').textContent.includes('projeto-vulneravel.zip'));results.push('Painel alimentado pela API');
 doc=await upload('projeto-seguro.zip');let href=new URL(doc.querySelector('#pipelineDone a').href);doc=await load(href.pathname+href.search);await until(()=>doc.getElementById('findingSidebar').textContent.includes('Nenhum achado'),'zero achados');assert(doc.getElementById('findingDetail').textContent.includes('não garante'));results.push('Resultado sem achados mantém ressalva de segurança');
 for(const name of ['invalido.zip','zip-slip-rejeitado.zip']) {doc=await upload(name);assert.equal(doc.getElementById('pipelineBadge').textContent,'Falha na análise');assert(doc.getElementById('pipelineDone').hidden);assert(!doc.getElementById('cancelBtn').disabled);results.push(name+': erro real sem resultado fictício');}
 doc=await upload('xss-nome-e-evidencia.zip');href=new URL(doc.querySelector('#pipelineDone a').href);doc=await load(href.pathname+href.search);await until(()=>doc.querySelector('#findingDetail .code-text'),'evidência XSS');assert.equal(doc.querySelectorAll('#findingDetail img').length,0);assert(doc.getElementById('findingDetail').textContent.includes('<img'));results.push('Conteúdo hostil preservado como texto no DOM');
 await fetch(base+'/api/analyses/'+report.id,{method:'DELETE'});doc=await load(reportRoute);await until(()=>doc.getElementById('reportStatus').textContent==='Indisponível','relatório excluído');results.push('Relatório excluído tratado na interface');
 assert.deepEqual(errors,[]);results.push('Nenhum erro JavaScript de execução no DOM');
 console.log(JSON.stringify({passed:results.length,results},null,2));
})().catch(e=>{console.error(e);process.exitCode=1;}).finally(()=>{clearTimeout(watchdog);if(dom)dom.window.close();});
