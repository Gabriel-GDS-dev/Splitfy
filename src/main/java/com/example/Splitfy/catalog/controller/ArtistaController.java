package com.example.Splitfy.catalog.controller;

import com.example.Splitfy.catalog.config.CatalogOpenApiConfig;
import com.example.Splitfy.catalog.dto.AlbumResponse;
import com.example.Splitfy.catalog.dto.ArtistaRequest;
import com.example.Splitfy.catalog.dto.ArtistaResponse;
import com.example.Splitfy.catalog.exception.ErroResponse;
import com.example.Splitfy.catalog.service.AlbumService;
import com.example.Splitfy.catalog.service.ArtistaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/artistas")
@Tag(name = "Artistas", description = "Cadastro e consulta de artistas")
@SecurityRequirement(name = CatalogOpenApiConfig.BASIC_AUTH)
@ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content)
public class ArtistaController {

    private final ArtistaService artistaService;
    private final AlbumService albumService;

    public ArtistaController(ArtistaService artistaService, AlbumService albumService) {
        this.artistaService = artistaService;
        this.albumService = albumService;
    }

    @PostMapping
    @Operation(summary = "Cadastra um artista")
    @ApiResponse(responseCode = "201", description = "Artista criado; a URL dele vem no header Location")
    @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    public ResponseEntity<ArtistaResponse> criar(@RequestBody @Valid ArtistaRequest request) {
        ArtistaResponse criado = artistaService.criar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @GetMapping
    @Operation(summary = "Lista artistas paginados", description = "Filtra opcionalmente por parte do nome.")
    @ApiResponse(responseCode = "200", description = "Página de artistas")
    public PagedModel<ArtistaResponse> listar(
            @Parameter(description = "Trecho do nome do artista", example = "Taylor")
            @RequestParam(required = false) String nome,
            @ParameterObject Pageable pageable) {
        return new PagedModel<>(artistaService.listar(nome, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um artista pelo id")
    @ApiResponse(responseCode = "200", description = "Artista encontrado")
    @ApiResponse(responseCode = "404", description = "Artista não encontrado",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    public ArtistaResponse buscarPorId(@Parameter(description = "Id do artista", example = "1") @PathVariable Long id) {
        return artistaService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um artista")
    @ApiResponse(responseCode = "200", description = "Artista atualizado")
    @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    @ApiResponse(responseCode = "404", description = "Artista não encontrado",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    public ArtistaResponse atualizar(@Parameter(description = "Id do artista", example = "1") @PathVariable Long id,
                                     @RequestBody @Valid ArtistaRequest request) {
        return artistaService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui um artista", description = "Só é permitido se o artista não tiver álbuns cadastrados.")
    @ApiResponse(responseCode = "204", description = "Artista excluído")
    @ApiResponse(responseCode = "404", description = "Artista não encontrado",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    @ApiResponse(responseCode = "409", description = "Artista possui álbuns cadastrados",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    public ResponseEntity<Void> excluir(@Parameter(description = "Id do artista", example = "1") @PathVariable Long id) {
        artistaService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/albuns")
    @Operation(summary = "Lista os álbuns de um artista")
    @ApiResponse(responseCode = "200", description = "Página de álbuns do artista")
    @ApiResponse(responseCode = "404", description = "Artista não encontrado",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    public PagedModel<AlbumResponse> listarAlbuns(@Parameter(description = "Id do artista", example = "1") @PathVariable Long id,
                                                  @ParameterObject Pageable pageable) {
        return new PagedModel<>(albumService.listarPorArtista(id, pageable));
    }
}
