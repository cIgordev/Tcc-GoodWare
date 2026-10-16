@echo off
setlocal
cd /d "%~dp0"
where java >nul 2>nul
if errorlevel 1 (
 echo Instale o JDK 21 e configure JAVA_HOME e PATH. Consulte LEIA-ME-PRIMEIRO.md.
 pause
 exit /b 1
)
echo GoodWare local: http://localhost:8080
echo Aguarde a mensagem Started GoodwareBackendApplication e abra o navegador.
echo Para encerrar, pressione Ctrl+C nesta janela.
java -Xmx512m -jar dist\goodware-backend-1.0.0.jar
pause
