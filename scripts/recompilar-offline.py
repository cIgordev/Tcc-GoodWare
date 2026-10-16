#!/usr/bin/env python3
"""Recompila TODOS os fontes Java 21 usando as dependências já presentes em dist.
Alternativa ao Maven para uma revisão sem mudanças de dependências.
Requer Python 3 e JDK 21 (JAVA_HOME ou javac no PATH).
Não atualiza dependências; para isso use Maven.
"""
from pathlib import Path
import os, shutil, subprocess, tempfile, zipfile
root=Path(__file__).resolve().parents[1]
jar=root/'dist/goodware-backend-1.0.0.jar'
javac=Path(os.environ['JAVA_HOME'])/'bin/javac' if os.environ.get('JAVA_HOME') else shutil.which('javac')
if not javac:raise SystemExit('Instale JDK 21 e configure JAVA_HOME.')
with tempfile.TemporaryDirectory() as tmp:
 work=Path(tmp);libs=work/'lib';classes=work/'classes';libs.mkdir();classes.mkdir()
 with zipfile.ZipFile(jar) as z:
  for n in z.namelist():
   if n.startswith('BOOT-INF/lib/') and n.endswith('.jar'):(libs/Path(n).name).write_bytes(z.read(n))
 sources=sorted((root/'src/main/java').rglob('*.java'))
 subprocess.run([str(javac),'--release','21','-parameters','-encoding','UTF-8','-cp',str(libs/'*'),'-d',str(classes),*[str(p) for p in sources]],check=True)
 output=jar.with_suffix('.new.jar')
 with zipfile.ZipFile(jar) as src,zipfile.ZipFile(output,'w') as dst:
  for info in src.infolist():
   if not info.filename.startswith('BOOT-INF/classes/'):
    dst.writestr(info,src.read(info.filename))
  dst.writestr('BOOT-INF/classes/', b'')
  for folder in [classes,root/'src/main/resources']:
   for p in sorted(folder.rglob('*')):
    if p.is_dir():dst.writestr('BOOT-INF/classes/'+p.relative_to(folder).as_posix()+'/', b'')
    if p.is_file():dst.writestr('BOOT-INF/classes/'+p.relative_to(folder).as_posix(),p.read_bytes(),compress_type=zipfile.ZIP_DEFLATED)
 with zipfile.ZipFile(output) as z:
  for p in (root/'src/main/resources').rglob('*'):
   if p.is_file():assert z.read('BOOT-INF/classes/'+p.relative_to(root/'src/main/resources').as_posix())==p.read_bytes()
 output.replace(jar)
print('Java recompilado. Todos os recursos do JAR correspondem aos fontes locais.')
