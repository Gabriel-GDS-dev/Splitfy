package com.example.Splitfy.catalog.controller;

import com.example.Splitfy.catalog.dto.AlbumRequest;
import com.example.Splitfy.catalog.dto.AlbumResponse;
import com.example.Splitfy.catalog.exception.ErroResponse;
import com.example.Splitfy.catalog.service.AlbumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/albuns")
@Tag(name = "Álbuns", description = "Cadastro e consulta de álbuns")
public class AlbumController {

    private final AlbumService albumService;

    public AlbumController(AlbumService albumService) {
        this.albumService = albumService;
    }

    @PostMapping
    @Operation(summary = "Cadastra um álbum", description = "O ano de lançamento deve estar entre 1900 e o ano atual.")
    @ApiResponse(responseCode = "201", description = "Álbum criado; a URL dele vem no header Location")
    @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    @ApiResponse(responseCode = "404", description = "Artista informado não encontrado",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    public ResponseEntity<AlbumResponse> criar(@RequestBody @Valid AlbumRequest request) {
        AlbumResponse criado = albumService.criar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @GetMapping
    @Operation(summary = "Lista álbuns paginados", description = "Filtra opcionalmente por ano de lançamento e/ou artista.")
    @ApiResponse(responseCode = "200", description = "Página de álbuns")
    @ApiResponse(responseCode = "400", description = "Parâmetro com valor inválido",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    public PagedModel<AlbumResponse> listar(
            @Parameter(description = "Ano de lançamento", example = "2014")
            @RequestParam(required = false) Integer ano,
            @Parameter(description = "Id do artista", example = "1")
            @RequestParam(required = false) Long artistaId,
            @ParameterObject Pageable pageable) {
        return new PagedModel<>(albumService.listar(ano, artistaId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um álbum pelo id")
    @ApiResponse(responseCode = "200", description = "Álbum encontrado")
    @ApiResponse(responseCode = "404", description = "Álbum não encontrado",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    public AlbumResponse buscarPorId(@Parameter(description = "Id do álbum", example = "1") @PathVariable Long id) {
        return albumService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um álbum")
    @ApiResponse(responseCode = "200", description = "Álbum atualizado")
    @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    @ApiResponse(responseCode = "404", description = "Álbum ou artista não encontrado",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    public AlbumResponse atualizar(@Parameter(description = "Id do álbum", example = "1") @PathVariable Long id,
                                   @RequestBody @Valid AlbumRequest request) {
        return albumService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui um álbum")
    @ApiResponse(responseCode = "204", description = "Álbum excluído")
    @ApiResponse(responseCode = "404", description = "Álbum não encontrado",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    public ResponseEntity<Void> excluir(@Parameter(description = "Id do álbum", example = "1") @PathVariable Long id) {
        albumService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/musicas")
    @Operation(summary = "Lista as músicas de um álbum",
            description = "Ainda não implementado: depende do módulo de Música.")
    @ApiResponse(responseCode = "501", description = "Ainda não implementado",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    public ResponseEntity<Void> listarMusicas(@Parameter(description = "Id do álbum", example = "1") @PathVariable Long id) {
        // TODO: depende do módulo de Música (Gabriel). Quando Musica/MusicaRepository existirem,
        //  validar que o álbum existe e retornar as músicas paginadas usando o DTO de resposta de Música.
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
                "Listagem de músicas do álbum ainda não disponível: depende do módulo de Música");
    }
}
