// Requires a running GoodWare and `npx playwright install chromium`.
// Uses no real credentials. Authenticated behavior is covered by Java tests.
const {chromium}=require('playwright');
const assert=require('node:assert/strict');
(async()=>{const browser=await chromium.launch({headless:true});try{
 const page=await browser.newPage();const errors=[];page.on('pageerror',e=>errors.push(e.message));
 const base=process.env.GOODWARE_TEST_URL||'http://localhost:8080';
 for(const width of [390,1440]){
  await page.setViewportSize({width,height:900});await page.goto(base+'/login.html');
  await page.locator('#email').waitFor();assert.equal(await page.locator('#password').getAttribute('type'),'password');
  assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1));
 }
 await page.goto(base+'/painel.html');await page.waitForURL('**/login.html');assert.deepEqual(errors,[]);
 console.log('PASS login desktop/mobile and anonymous redirect. No remote login tested.');
}finally{await browser.close();}})().catch(e=>{console.error(e);process.exitCode=1;});
