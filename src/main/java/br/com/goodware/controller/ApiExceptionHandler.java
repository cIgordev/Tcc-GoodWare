package br.com.goodware.controller;
import br.com.goodware.engine.ScanException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import java.util.Map;
@RestControllerAdvice
public class ApiExceptionHandler {
 @ExceptionHandler(ScanException.class) public ResponseEntity<?> scan(ScanException e){return error(400,e.getMessage());}
 @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<?> status(ResponseStatusException e){return error(e.getStatusCode().value(),e.getReason());}
 @ExceptionHandler(MaxUploadSizeExceededException.class) public ResponseEntity<?> size(Exception e){return error(413,"Upload acima do limite de 100 MiB.");}
 @ExceptionHandler({MultipartException.class,MissingServletRequestPartException.class,MissingServletRequestParameterException.class})
 public ResponseEntity<?> input(Exception e){return error(400,"Envie um ZIP no campo file do formulário multipart.");}
 @ExceptionHandler(HttpRequestMethodNotSupportedException.class) public ResponseEntity<?> method(Exception e){return error(405,"Método não permitido.");}
 @ExceptionHandler(HttpMediaTypeNotSupportedException.class) public ResponseEntity<?> media(Exception e){return error(415,"Use multipart/form-data para enviar o ZIP.");}
 @ExceptionHandler({org.springframework.dao.DataAccessException.class,org.springframework.transaction.TransactionException.class})
 public ResponseEntity<?> database(Exception e){return error(503,"Banco de dados indisponível. A operação não foi confirmada; consulte o histórico antes de tentar novamente.");}
 @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
 public ResponseEntity<?> invalidParameter(Exception e){return error(400,"Parâmetro inválido.");}
 @ExceptionHandler(Exception.class) public ResponseEntity<?> unexpected(Exception e){return error(500,"Não foi possível concluir a operação. Tente novamente com um pacote menor.");}
 private ResponseEntity<?> error(int status,String detail){return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(Map.of("status",status,"detail",detail==null?"Falha na solicitação.":detail));}
}
