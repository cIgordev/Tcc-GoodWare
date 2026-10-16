package br.com.goodware.controller;

import br.com.goodware.model.AnalysisResult;
import br.com.goodware.service.AnalysisService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analyses")
public class AnalysisController {
    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AnalysisResult analyze(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Envie um arquivo ZIP não vazio.");
        }
        return analysisService.analyze(file);
    }

    @GetMapping
    public Object listOrFind(@RequestParam(required = false) String sha256) {
        if (sha256 != null) {
            if (!sha256.matches("(?i)^[a-f0-9]{64}$")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SHA-256 inválido.");
            }
            return analysisService.findBySha256(sha256)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Análise não encontrada."));
        }
        return analysisService.list();
    }

    @GetMapping("/history")
    public Object history(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return analysisService.history(page,size);}
    @GetMapping("/{id}/status")
    public Object state(@PathVariable String id){return analysisService.state(id);}
    @GetMapping("/{id}/summary")
    public Object summary(@PathVariable String id){return analysisService.summary(id);}
    @GetMapping("/{id}")
    public AnalysisResult get(@PathVariable String id) {
        return analysisService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Análise não encontrada."));
    }

    @GetMapping("/{id}/findings")
    public List<?> findings(@PathVariable String id) {
        return analysisService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Análise não encontrada."))
                .findings();
    }
    @GetMapping("/{id}/report")
    public org.springframework.http.ResponseEntity<AnalysisResult> report(@PathVariable String id) {
        return org.springframework.http.ResponseEntity.ok()
          .header("Content-Disposition", "attachment; filename=goodware-report.json")
          .body(get(id));
    }
    @GetMapping("/{id}/files")
    public List<?> files(@PathVariable String id) { return get(id).files(); }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        if (!analysisService.delete(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Análise não encontrada.");
    }
}
