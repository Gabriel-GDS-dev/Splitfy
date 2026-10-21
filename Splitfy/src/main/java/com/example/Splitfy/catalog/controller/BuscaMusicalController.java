package com.example.Splitfy.catalog.controller;

import com.example.Splitfy.catalog.service.BuscaMusicalService;
import com.example.Splitfy.catalog.exception.CatalogValidationException;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/integracoes/musicas")
@PreAuthorize("hasRole('ADMIN')")
public class BuscaMusicalController {
    private final BuscaMusicalService busca;
    public BuscaMusicalController(BuscaMusicalService busca) { this.busca = busca; }
    @GetMapping("/status")
    public BuscaMusicalService.Resposta status() { return busca.status(); }
    @GetMapping("/buscar")
    public BuscaMusicalService.Resposta buscar(@RequestParam String q) {
        if (q.trim().length() < 2 || q.length() > 180) throw new CatalogValidationException("Informe musica e cantor, entre 2 e 180 caracteres.", Map.of());
        return busca.buscar(q.trim());
    }
}
