package br.com.goodware;
import br.com.goodware.controller.ApiExceptionHandler;
import br.com.goodware.persistence.AnalysisRepository;
import br.com.goodware.service.*;
import br.com.goodware.engine.ZipFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockMultipartFile;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class DatabaseFailureTest {
 @Test void transactionConnectionFailureIsSanitized(){
  var response=new ApiExceptionHandler().database(new org.springframework.transaction.CannotCreateTransactionException("secret-connection-detail"));
  assertEquals(503,response.getStatusCode().value());assertFalse(response.getBody().toString().contains("secret-connection-detail"));
 }
 @Test void unavailableDatabaseNeverReturnsSuccessfulAnalysis()throws Exception {
  var repository=mock(AnalysisRepository.class);
  doThrow(new DataAccessResourceFailureException("secret-connection-detail")).when(repository).start(anyString(),anyString(),anyString(),anyLong(),any());
  var service=new AnalysisService(mock(StaticRuleService.class),mock(SignatureService.class),repository);
  var file=new MockMultipartFile("file","synthetic.zip","application/zip",ZipFixtures.zip(java.util.Map.of("a.java","class A {}".getBytes())));
  assertThrows(DataAccessResourceFailureException.class,()->service.analyze(file));
  verify(repository,never()).complete(any());
  var response=new ApiExceptionHandler().database(new DataAccessResourceFailureException("secret-connection-detail"));
  assertEquals(503,response.getStatusCode().value());assertFalse(response.getBody().toString().contains("secret-connection-detail"));
 }
}
