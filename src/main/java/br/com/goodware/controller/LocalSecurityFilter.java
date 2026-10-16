package br.com.goodware.controller;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.net.URI;
import java.util.Set;
@Component
@org.springframework.core.annotation.Order(10)
public class LocalSecurityFilter extends OncePerRequestFilter {
 private final Set<String> allowedOrigins;
 public LocalSecurityFilter(@org.springframework.beans.factory.annotation.Value("${goodware.allowed-origins:http://localhost:8080,http://127.0.0.1:8080,http://localhost}") String origins){
  allowedOrigins=Set.copyOf(java.util.Arrays.asList(origins.split(",")));
  for(String origin:allowedOrigins){URI u=URI.create(origin);if(u.getHost()==null || u.getUserInfo()!=null || u.getQuery()!=null || u.getFragment()!=null || !u.getPath().isEmpty() || !("https".equals(u.getScheme()) || ("http".equals(u.getScheme()) && Set.of("localhost","127.0.0.1","[::1]").contains(u.getHost()))))throw new IllegalArgumentException("Origens devem usar HTTPS, exceto localhost.");}
 }
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
  if(req.isSecure())res.setHeader("Strict-Transport-Security","max-age=31536000");
  res.setHeader("X-Content-Type-Options","nosniff");res.setHeader("X-Frame-Options","DENY");
  res.setHeader("Referrer-Policy","no-referrer");res.setHeader("Cache-Control","no-store");
  res.setHeader("Content-Security-Policy","default-src 'self'; script-src 'self'; style-src 'self' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; img-src 'self' data:; connect-src 'self'; object-src 'none'; base-uri 'none'; form-action 'self'; frame-ancestors 'none'");
  if(!Set.of("GET","HEAD","OPTIONS").contains(req.getMethod()) && !"1".equals(req.getHeader("X-GoodWare-Request"))) {reject(res);return;}
  String host=req.getServerName();
  String requestOrigin=req.getScheme()+"://"+host+((req.getServerPort()==80 && "http".equals(req.getScheme()) || req.getServerPort()==443 && "https".equals(req.getScheme()))?"":":"+req.getServerPort());
  if(!allowedOrigins.contains(requestOrigin) || "cross-site".equals(req.getHeader("Sec-Fetch-Site"))) {reject(res);return;}
  String origin=req.getHeader("Origin");
  if(origin!=null) {
   try {URI u=URI.create(origin);int port=u.getPort()<0?("https".equals(u.getScheme())?443:80):u.getPort();
    if(!req.getScheme().equals(u.getScheme()) || !host.equals(u.getHost()) || port!=req.getServerPort()) {reject(res);return;}
   } catch(IllegalArgumentException e) {reject(res);return;}
  }
  chain.doFilter(req,res);
 }
 private void reject(HttpServletResponse res)throws IOException {res.setStatus(403);res.setContentType("application/problem+json");res.getWriter().write("{\"status\":403,\"detail\":\"Acesso permitido somente pela origem configurada do GoodWare.\"}");}
}
