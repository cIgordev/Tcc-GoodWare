const {JSDOM,ResourceLoader,VirtualConsole}=require('jsdom');
const fs=require('fs'),path=require('path'),assert=require('assert/strict');
const root=path.resolve('src/main/resources/static');
class Assets extends ResourceLoader {fetch(url){const p=path.join(root,new URL(url).pathname);return fs.existsSync(p)?Promise.resolve(fs.readFileSync(p)):null;}}
(async()=>{
 let checks=0;
 for(const page of ['login.html','painel.html','analisar.html','achado.html']){
  const errors=[],calls=[];const vc=new VirtualConsole();vc.on('jsdomError',e=>{if(!e.message.includes('CSS'))errors.push(e.message);});
  const dom=new JSDOM(fs.readFileSync(path.join(root,page),'utf8'),{url:'http://localhost/'+page,resources:new Assets(),runScripts:'dangerously',virtualConsole:vc,beforeParse(w){
   w.Headers=Headers;w.matchMedia=()=>({matches:false,addEventListener(){}});w.fetch=async(url,options)=>{calls.push({url,options});
    if(url==='/api/auth/signup')return {ok:true,status:200,json:async()=>({message:'Confira seu email.'})};
    if(url==='/api/auth/login')return {ok:false,status:401,json:async()=>({detail:'Dados inválidos.'})};
    if(url==='/api/auth/me')return {ok:true,status:200,json:async()=>({id:'synthetic'})};
    return {ok:true,status:200,json:async()=>({items:[],total:0,page:0,size:20})};
   };
  }});
  await new Promise(r=>dom.window.addEventListener('load',r,{once:true}));
  await new Promise(r=>setImmediate(r));
  assert.deepEqual(errors,[],page);checks++;
  if(page==='login.html'){
   const w=dom.window,f=w.document.getElementById('authForm');f.elements.email.value='synthetic@example.invalid';f.elements.password.value='synthetic-pass';
   f.dispatchEvent(new w.SubmitEvent('submit',{bubbles:true,cancelable:true,submitter:f.querySelector('[value="signup"]')}));await new Promise(r=>setImmediate(r));
   assert.equal(w.document.getElementById('authFeedback').textContent,'Confira seu email.');assert.equal(calls[0].options.headers['X-GoodWare-Request'],'1');assert.equal(w.localStorage.length,0);checks++;
   f.elements.password.value='synthetic-pass';f.dispatchEvent(new w.SubmitEvent('submit',{bubbles:true,cancelable:true,submitter:f.querySelector('[value="login"]')}));await new Promise(r=>setImmediate(r));
   assert.equal(w.document.getElementById('authFeedback').textContent,'Dados inválidos.');checks++;
  }else{assert.equal(typeof dom.window.goodwareFetch,'function');assert.ok(calls.some(c=>c.url==='/api/auth/me'));assert.ok(dom.window.document.body.textContent.includes('Sair da conta'));checks++;}
  dom.window.close();
 }
 console.log(`PASS ${checks} DOM/auth checks (mocked network; not visual or remote E2E).`);
})().catch(e=>{console.error(e);process.exitCode=1;});
