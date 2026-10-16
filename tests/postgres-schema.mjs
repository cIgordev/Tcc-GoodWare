// Synthetic, isolated PostgreSQL/WASM tests. Never connects to Supabase.
import {PGlite} from '@electric-sql/pglite';
import {readFile,mkdtemp,rm} from 'node:fs/promises';
import {tmpdir} from 'node:os';
import {join} from 'node:path';
import assert from 'node:assert/strict';
const dir=await mkdtemp(join(tmpdir(),'goodware-pg-'));
let db=new PGlite(dir);let passed=0;
const ok=label=>{passed++;console.log('PASS '+label);};
const id='10000000-0000-4000-8000-000000000001',other='10000000-0000-4000-8000-000000000002';
try {
 await db.exec('CREATE ROLE anon; CREATE ROLE authenticated; CREATE ROLE service_role;');
 for(const path of ['src/main/resources/db/migration/V1__persistent_analyses.sql','src/main/resources/db/postgres/V2__private_backend_access.sql'])await db.exec(await readFile(path,'utf8'));
 ok('V1 and V2 executed on isolated PostgreSQL/WASM');
 assert.equal((await db.query("SELECT count(*)::int AS n FROM pg_tables WHERE schemaname='goodware' AND rowsecurity")).rows[0].n,4);ok('RLS enabled on all four tables');
 for(const role of ['anon','authenticated','service_role']){
  await db.exec('SET ROLE '+role);
  await assert.rejects(db.query('SELECT * FROM goodware.analyses'),/permission denied/);
  await db.exec('RESET ROLE');
 }ok('Data API roles denied');
 await db.exec('SET ROLE goodware_backend');
 const insert="INSERT INTO goodware.analyses(id,name,sha256,upload_size,status,created_at,started_at) VALUES ($1,'SYNTHETIC TEST',repeat('a',64),100,'RUNNING',now(),now())";
 await db.query(insert,[id]);await db.query(insert,[other]);ok('Restricted backend can insert/read');
 await assert.rejects(db.exec('DROP TABLE goodware.analyses'),/must be owner/);ok('Runtime cannot drop schema tables');
 await db.query("INSERT INTO goodware.analyzed_files VALUES ($1,'src/a.java',20,repeat('b',64),'java','TEXT_SCANNED','Synthetic fixture')",[id]);
 await db.query("INSERT INTO goodware.findings(analysis_id,id,file_path,category,severity,confidence,title,line_start,rule_id,evidence,impact,recommendation) VALUES ($1,'test','src/a.java','POTENTIAL_VULNERABILITY','HIGH','MEDIUM','Synthetic finding',1,'TEST','[REDACTED]','Example','Review')",[id]);
 await assert.rejects(db.query("INSERT INTO goodware.findings(analysis_id,id,file_path,category,severity,confidence,title,rule_id,evidence,impact,recommendation) VALUES ($1,'cross','src/a.java','POTENTIAL_VULNERABILITY','HIGH','MEDIUM','Synthetic','TEST','','','')",[other]),/foreign key/);ok('Cross-analysis file reference rejected');
 await assert.rejects(db.query("INSERT INTO goodware.analyzed_files VALUES ($1,'../escape',1,repeat('b',64),'java','TEXT_SCANNED','test')",[id]),/check constraint/);ok('Unsafe file path rejected');
 await db.exec('BEGIN');await db.query(insert,['10000000-0000-4000-8000-000000000003']);await db.exec('ROLLBACK');
 assert.equal((await db.query('SELECT count(*)::int AS n FROM goodware.analyses')).rows[0].n,2);ok('Rollback preserves consistency');
 await db.exec('RESET ROLE');await db.close();db=new PGlite(dir);
 assert.equal((await db.query('SELECT count(*)::int AS n FROM goodware.findings')).rows[0].n,1);ok('Data survives database close/reopen');
 await db.exec('SET ROLE goodware_backend');await db.query('DELETE FROM goodware.analyses WHERE id=$1',[id]);
 for(const table of ['findings','analyzed_files'])assert.equal((await db.query('SELECT count(*)::int AS n FROM goodware.'+table)).rows[0].n,0);
 ok('Parent delete cascades to files and findings');
 console.log(`${passed} checks passed; no remote database used.`);
}finally{await db.close();await rm(dir,{recursive:true,force:true});}
