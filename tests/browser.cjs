/* Executar com servidor em localhost:8080; requer playwright e Chromium. */
const { chromium } = require('playwright');
const path = require('node:path');
const assert = require('node:assert/strict');
(async () => {
 const browser = await chromium.launch({headless:true,executablePath:process.env.GOODWARE_CHROME_PATH||undefined,args:['--no-sandbox']});
 const page = await browser.newPage({viewport:{width:1366,height:900},reducedMotion:'reduce'});
 const errors=[]; page.on('pageerror', e=>errors.push(e.message));
 const results=[];
 const base=process.env.GOODWARE_TEST_URL||'http://127.0.0.1:8080';
 const fixture=name=>path.join(__dirname,'..','examples',name);
 async function upload(name) {
  await page.goto(base+'/analisar.html');
  await page.locator('#fileInput').setInputFiles(fixture(name));
  await page.locator('#consent').check();
  await page.locator('#startBtn').click();
 }
 try {
  await page.goto(base+'/'); await page.locator('.hero h1').waitFor(); results.push('Landing original carregada');
  await page.goto(base+'/login.html');assert.equal(await page.locator('input[type=password]').count(),0);results.push('Acesso local explícito sem login fictício');
  await upload('projeto-vulneravel.zip');
  await page.locator('#pipelineDone').waitFor({state:'visible'});
  await page.locator('#pipelineDone a').click();
  await page.locator('#reportSummary h1').waitFor();
  assert.equal(await page.locator('#findingSidebar button').count(),5);
  await page.locator('#inventorySection summary').click();
  assert.equal(await page.locator('#fileInventory tr').count(),4);
  const reportUrl=page.url();
  const reportId=new URL(reportUrl).searchParams.get('id');
  const report=await (await page.request.get(base+'/api/analyses/'+reportId+'/report')).json();
  assert.equal(report.findings.length,5);assert.equal(report.sha256.length,64);
  assert(!JSON.stringify(report).includes('fixture-secret-not-real'));
  await page.getByRole('button',{name:'Marcar como revisado'}).click();
  await page.getByRole('button',{name:'Revisado nesta aba'}).waitFor();
  results.push('Upload real → 5 achados → 4 arquivos → relatório JSON → revisão local; segredo mascarado');
  if(process.env.GOODWARE_SCREENSHOT_DIR)await page.screenshot({path:path.join(process.env.GOODWARE_SCREENSHOT_DIR,'report-desktop.png'),fullPage:true});
  await page.goto(base+'/analisar.html');await page.locator('#tabHash').click();await page.locator('#hashInput').fill(report.sha256);await page.locator('#hashSearchBtn').click();
  await page.locator('#hashResult a').waitFor();results.push('Consulta por SHA-256 real');
  await page.goto(base+'/painel.html');await page.locator('#analysisList .analysis-row').first().waitFor();assert((await page.locator('#analysisList').textContent()).includes('projeto-vulneravel.zip'));results.push('Painel sem dados fictícios');
  await upload('projeto-seguro.zip');await page.locator('#pipelineDone a').waitFor({state:'visible'});await page.locator('#pipelineDone a').click();await page.getByText('Nenhum achado registrado',{exact:true}).waitFor();results.push('Arquivo seguro mostra ressalva e zero achados');
  await upload('invalido.zip');await page.getByText('Falha na análise',{exact:true}).waitFor();assert.equal(await page.locator('#pipelineDone').isVisible(),false);results.push('Erro real de formato exibido sem resultado simulado');
  await upload('zip-slip-rejeitado.zip');await page.getByText('Falha na análise',{exact:true}).waitFor();results.push('Zip Slip rejeitado na interface');
  let dialogs=0;page.on('dialog',async d=>{dialogs++;await d.dismiss();});
  await upload('xss-nome-e-evidencia.zip');await page.locator('#pipelineDone a').waitFor({state:'visible'});await page.locator('#pipelineDone a').click();await page.locator('#reportSummary h1').waitFor();
  assert.equal(await page.locator('#findingDetail img').count(),0);assert.equal(dialogs,0);assert((await page.locator('#findingDetail').textContent()).includes('<img'));results.push('Nome/evidência hostis tratados como texto (sem XSS)');
  await page.setViewportSize({width:390,height:844});
  for(const route of ['/','/painel.html','/analisar.html',reportUrl.replace(base,'')]) {
   await page.goto(base+route);await page.locator('body').waitFor();
   const overflow=await page.evaluate(()=>document.documentElement.scrollWidth>window.innerWidth+1);assert(!overflow,'overflow mobile: '+route);
  }
  if(process.env.GOODWARE_SCREENSHOT_DIR)await page.screenshot({path:path.join(process.env.GOODWARE_SCREENSHOT_DIR,'report-mobile.png'),fullPage:true});
  results.push('Quatro páginas sem overflow horizontal em 390px');
  await page.request.delete(base+'/api/analyses/'+reportId);await page.goto(reportUrl);await page.getByText('Indisponível',{exact:true}).waitFor();results.push('Relatório excluído tratado como indisponível');
  assert.deepEqual(errors,[]);results.push('Nenhum erro JavaScript de página');
  console.log(JSON.stringify({passed:results.length,results},null,2));
 } finally {await browser.close();}
})().catch(error=>{console.error(error);process.exitCode=1;});
