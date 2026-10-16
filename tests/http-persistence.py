"""Explicitly local synthetic HTTP checks. Never connects to Supabase."""
import http.client,json,sys,uuid,io,zipfile
from pathlib import Path
port=int(sys.argv[1]) if len(sys.argv)>1 else 18080
state=Path(sys.argv[2]) if len(sys.argv)>2 else Path('/tmp/goodware-http-state.json')
mode=sys.argv[3] if len(sys.argv)>3 else 'create'
def request(method,path,body=None,headers={}):
 c=http.client.HTTPConnection('127.0.0.1',port,timeout=45)
 c.request(method,path,body,headers);r=c.getresponse();data=r.read();c.close()
 return r.status,json.loads(data) if data else None
if mode=='create':
 def upload(data):
  boundary=uuid.uuid4().hex
  body=(f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="synthetic-test.zip"\r\nContent-Type: application/zip\r\n\r\n').encode()+data+f'\r\n--{boundary}--\r\n'.encode()
  return request('POST','/api/analyses',body,{'Content-Type':f'multipart/form-data; boundary={boundary}'})
 ids=[]
 for value,expected in [('class Synthetic {}',0),('Runtime.getRuntime().exec(input);',1)]:
  buf=io.BytesIO()
  with zipfile.ZipFile(buf,'w') as z:z.writestr('src/A.java',value)
  status,data=upload(buf.getvalue());assert status==201,(status,data);assert len(data['findings'])==expected
  ids.append(data['id']);assert request('GET','/api/analyses/'+data['id']+'/files')[1][0]['path']=='src/A.java'
 status,data=upload(b'not a ZIP');assert status==400,(status,data)
 status,data=request('GET','/api/analyses/history?page=0&size=2');assert status==200 and len(data['items'])==2 and data['total']>=3
 assert request('GET','/api/analyses/history?page=-1')[0]==400
 state.write_text(json.dumps(ids));print('PASS HTTP uploads safe/vulnerable; malformed rejected; files and paginated history')
elif mode=='restart':
 for id in json.loads(state.read_text()):
  status,data=request('GET','/api/analyses/'+id);assert status==200 and data['status']=='COMPLETED',(status,data)
  assert request('DELETE','/api/analyses/'+id)[0]==204
  assert request('GET','/api/analyses/'+id)[0]==404
 print('PASS same IDs recovered after backend restart; cascade deletion through HTTP')
elif mode=='outage':
 status,data=request('GET','/api/analyses/history');assert status==503,(status,data)
 assert not any(x in str(data).lower() for x in ['jdbc:','postgresql','password','stacktrace'])
 print('PASS actual database outage returns sanitized HTTP 503')
