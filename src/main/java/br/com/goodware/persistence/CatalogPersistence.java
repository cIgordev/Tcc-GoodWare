package br.com.goodware.persistence;
import br.com.goodware.service.SignatureService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
@Component
public class CatalogPersistence implements ApplicationRunner {
 private final AnalysisRepository repository;
 private final SignatureService signatures;
 public CatalogPersistence(AnalysisRepository repository,SignatureService signatures){this.repository=repository;this.signatures=signatures;}
 public void run(ApplicationArguments args){repository.snapshot(signatures.catalog());}
}
