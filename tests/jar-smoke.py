"""Starts isolated LOCAL PGlite and JAR. Requires Java 21, Node, npm ci, Python 3.
TLS disabled only for this loopback-only, disposable test server.
"""
import os,subprocess,tempfile,time,socket,http.client
from pathlib import Path
java=str(Path(os.environ['JAVA_HOME'])/'bin/java') if os.environ.get('JAVA_HOME') else 'java'
with tempfile.TemporaryDirectory(prefix='goodware-smoke-') as temp:
 env=dict(os.environ,GOODWARE_DB_URL='jdbc:postgresql://127.0.0.1:55432/postgres?prepareThreshold=0',GOODWARE_DB_USER='postgres',GOODWARE_DB_PASSWORD='synthetic-local-only',GOODWARE_DB_SSL_ROOT_CERT='/unused',GOODWARE_MIGRATE='false',GOODWARE_MIGRATION_URL='jdbc:postgresql://127.0.0.1:55432/postgres?sslmode=disable')
 command=[java,'-jar','dist/goodware-backend-1.0.0.jar','--spring.datasource.hikari.data-source-properties.sslmode=disable','--spring.datasource.hikari.maximum-pool-size=1','--server.port=18080']
 def wait(port,process):
  for _ in range(100):
   if process.poll() is not None:raise RuntimeError('Test process exited; inspect tests/evidence logs')
   try:
    with socket.create_connection(('127.0.0.1',port),timeout=.2):return
   except OSError:time.sleep(.2)
  raise TimeoutError(port)
 def stop(p):
  if p and p.poll() is None:
   p.terminate()
   try:p.wait(timeout=8)
   except subprocess.TimeoutExpired:p.kill();p.wait()
 Path('tests/evidence').mkdir(exist_ok=True)
 db=app=None
 with open('tests/evidence/pg-server.log','w') as pglog,open('tests/evidence/jar-start.log','w') as log:
  try:
   db=subprocess.Popen(['node','tests/local-postgres-server.mjs'],stdout=pglog,stderr=subprocess.STDOUT);wait(55432,db)
   app=subprocess.Popen(command,env=env,stdout=log,stderr=subprocess.STDOUT);wait(18080,app)
   subprocess.run(['python3','tests/http-persistence.py','18080',temp+'/ids.json','create'],check=True)
   stop(app);env['GOODWARE_MIGRATE']='false'
   app=subprocess.Popen(command,env=env,stdout=log,stderr=subprocess.STDOUT);wait(18080,app)
   subprocess.run(['python3','tests/http-persistence.py','18080',temp+'/ids.json','restart'],check=True)
   stop(db)
   subprocess.run(['python3','tests/http-persistence.py','18080',temp+'/ids.json','outage'],check=True)
  finally:stop(app);stop(db)
