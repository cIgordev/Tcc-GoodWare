// TEST ONLY: local PostgreSQL/WASM, no TLS/authentication. Never expose beyond loopback.
import {readFile} from 'node:fs/promises';
import {PGlite} from '@electric-sql/pglite';
import {PGLiteSocketServer} from '@electric-sql/pglite-socket';
const db=await PGlite.create(process.env.GOODWARE_TEST_PG_DIR||'memory://');
for(const path of ['src/main/resources/db/migration/V1__persistent_analyses.sql','src/main/resources/db/postgres/V2__private_backend_access.sql'])await db.exec(await readFile(path,'utf8'));
const server=new PGLiteSocketServer({db,host:'127.0.0.1',port:55432,maxConnections:5});
await server.start();console.log('Synthetic test database listening on 127.0.0.1:55432');
for(const signal of ['SIGINT','SIGTERM'])process.on(signal,async()=>{await server.stop();await db.close();process.exit(0);});
