package br.com.goodware.engine;
import br.com.goodware.model.Finding;
import java.util.List;
public interface ScanModule {
 List<Finding> scan(String path, String language, String content);
}
