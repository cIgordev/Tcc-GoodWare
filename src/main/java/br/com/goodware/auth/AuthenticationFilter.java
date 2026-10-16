package br.com.goodware.auth;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.time.Instant;
@Component @Order(20)
public class AuthenticationFilter extends OncePerRequestFilter {
 private final AuthGateway auth;
 public AuthenticationFilter(AuthGateway auth){this.auth=auth;}
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException {
  String path=req.getServletPath(); if(path.isEmpty())path=req.getRequestURI();
  if(!path.startsWith("/api/") || path.equals("/api/health") || path.equals("/api/auth/login") || path.equals("/api/auth/signup") || path.equals("/api/auth/logout")){chain.doFilter(req,res);return;}
  try{
   var s=req.getSession(false);
   if(s==null || !(s.getAttribute("expires") instanceof Long exp) || exp<=Instant.now().getEpochSecond()){
    if(s!=null)s.invalidate();error(res,401,"Entre na sua conta.");return;
   }
   try {OwnerContext.set(auth.verify((String)s.getAttribute("token")));}
   catch(ResponseStatusException e){error(res,e.getStatusCode().value(),e.getReason());return;}
   chain.doFilter(req,res);
  }finally{OwnerContext.clear();}
 }
 private void error(HttpServletResponse res,int code,String message)throws IOException{res.setStatus(code);res.setContentType("application/problem+json");res.setCharacterEncoding("UTF-8");res.getWriter().write("{\"status\":"+code+",\"detail\":\""+message+"\"}");}
}
