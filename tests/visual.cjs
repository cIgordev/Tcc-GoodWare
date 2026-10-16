/* Real API + responsive UI regression. Start the application before running. */
const {chromium}=require('playwright');
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const base=process.env.GOODWARE_TEST_URL||'http://127.0.0.1:8080';
(async()=>{
 const browser=await chromium.launch({headless:true,executablePath:process.env.GOODWARE_CHROME_PATH||undefined,args:['--no-sandbox']});
 const page=await browser.newPage({reducedMotion:'reduce'}); const errors=[];page.on('pageerror',e=>errors.push(e.message));
 const out=process.env.GOODWARE_SCREENSHOT_DIR;
 const results=[];
 try {
  await page.goto(base+'/painel.html');await page.locator('#analysisList[aria-busy=false]').waitFor({state:'attached'});
  if(await page.locator('.analysis-row').count()===0)assert(await page.locator('#emptyState').isVisible());
  const response=await page.request.post(base+'/api/analyses',{multipart:{file:{name:'projeto-vulneravel.zip',mimeType:'application/zip',buffer:fs.readFileSync(path.join(__dirname,'../examples/projeto-vulneravel.zip'))}}});
  assert.equal(response.status(),201);const report=await response.json();
  const route='/achado.html?id='+encodeURIComponent(report.id);
  for(const width of [320,390,768,1440]) {
   await page.setViewportSize({width,height:900});
   for(const [name,url] of [['inicio','/'],['entrada','/login.html'],['painel','/painel.html'],['analise','/analisar.html'],['relatorio',route]]) {
    await page.goto(base+url); if(name==='relatorio')await page.locator('#reportSummary h1').waitFor();
    if(name==='painel')await page.locator('#analysisList[aria-busy=false]').waitFor({state:'attached'});
    assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),'overflow '+name+' '+width);
    assert(await page.locator('main').count()===1,'main landmark '+name);
    if(name==='relatorio')assert(await page.evaluate(()=>document.querySelector('.mock-window').clientHeight>=document.querySelector('.mock-window').scrollHeight-2),'report clipping');
    if(out)await page.screenshot({path:path.join(out,name+'-'+width+'.png'),fullPage:true});
   }
   results.push('Cinco páginas sem overflow/corte em '+width+'px');
  }
  await page.goto(base+route);await page.locator('#findingToolbar').waitFor();
  await page.locator('#severityFilter').selectOption('high');assert.equal(await page.locator('#findingSidebar button').count(),3);
  await page.locator('#findingSearch').fill('config.json');assert.equal(await page.locator('#findingSidebar button').count(),1);
  await page.locator('#findingSearch').fill('inexistente');assert.equal(await page.locator('#findingSidebar button').count(),0);
  await page.locator('#findingSearch').fill('');await page.locator('#severityFilter').selectOption('all');assert.equal(await page.locator('#findingSidebar button').count(),5);
  await page.locator('#findingSidebar button').last().click();assert.equal(await page.locator('#findingDetail').evaluate(el=>el===document.activeElement),true);
  results.push('Filtros combinados, estado sem resultados e foco no detalhe');
  await page.goto(base+'/analisar.html');await page.locator('#tabFile').focus();await page.keyboard.press('ArrowRight');assert.equal(await page.locator('#tabHash').getAttribute('aria-selected'),'true');
  await page.locator('#hashInput').fill('abc');await page.locator('#hashSearchBtn').click();assert.equal(await page.locator('#hashInput').getAttribute('aria-invalid'),'true');
  await page.locator('#userMenuTrigger').click();await page.keyboard.press('Escape');assert.equal(await page.locator('#userMenuTrigger').getAttribute('aria-expanded'),'false');
  results.push('Abas por teclado, hash inválido acessível e Escape no menu');
  await page.route('**/api/analyses',r=>r.fulfill({status:503,contentType:'application/json',body:'{}'}));
  await page.goto(base+'/painel.html');await page.locator('#analysisList[role=alert]').waitFor();assert(await page.getByRole('button',{name:'Tentar novamente'}).isVisible());await page.unroute('**/api/analyses');
  results.push('Falha HTTP 503 injetada no teste: alerta e nova tentativa');
  await page.goto(base+route);await page.locator('#reportSummary h1').waitFor();page.once('dialog',d=>d.accept());await page.getByRole('button',{name:'Excluir relatório'}).click();await page.waitForURL('**/painel.html');assert.equal((await page.request.get(base+'/api/analyses/'+report.id)).status(),404);
  results.push('Exclusão pela interface e confirmação de 404 na API');
  assert.deepEqual(errors,[]);results.push('Nenhum erro JavaScript de página');
  console.log(JSON.stringify({passed:results.length,results},null,2));
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
