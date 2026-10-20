package com.example.Splitfy.catalog.doc;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.Splitfy.catalog.dto.AlbumRequest;
import com.example.Splitfy.catalog.dto.AlbumResponse;
import com.example.Splitfy.catalog.dto.CatalogErrorResponse;
import com.example.Splitfy.catalog.dto.MusicaResponse;
import com.example.Splitfy.catalog.exception.ErroResponse;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import java.util.List;

@Tag(name = "Álbuns", description = "Consulta e manutenção de álbuns")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado", content = @Content()),
        @ApiResponse(responseCode = "403", description = "Usuário sem permissão para a operação", content = @Content())
})
public interface AlbumControllerDoc {
    @Operation(summary = "Cadastrar álbum", description = "Exige ADMIN. Título e artista obrigatórios; ano de lançamento entre 1900 e o ano atual.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registro criado; a URL dele vem no header Location", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AlbumResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Artista informado não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class)))
    })
    ResponseEntity<AlbumResponse> criar(@io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AlbumRequest.class),
                    examples = @ExampleObject(value = "{\"titulo\":\"1989\",\"anoLancamento\":2014,\"capaUrl\":\"https://exemplo.com/capas/1989.jpg\",\"artistaId\":1}")))
                                        AlbumRequest request);

    @Operation(summary = "Listar álbuns", description = "Disponível para USER e ADMIN. Paginado; filtra opcionalmente por ano de lançamento e/ou artista.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de álbuns, podendo estar vazia"),
            @ApiResponse(responseCode = "400", description = "Parâmetro com valor inválido", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class)))
    })
    PagedModel<AlbumResponse> listar(@Parameter(description = "Ano de lançamento", example = "2014") Integer ano,
                                     @Parameter(description = "Identificador do artista", example = "1") Long artistaId,
                                     @ParameterObject Pageable pageable);

    @Operation(summary = "Buscar álbum", description = "Disponível para USER e ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AlbumResponse.class))),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class)))
    })
    AlbumResponse buscarPorId(@Parameter(description = "Identificador do registro", example = "1", required = true) Long id);

    @Operation(summary = "Atualizar álbum", description = "Exige ADMIN. Substitui os dados do registro.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro atualizado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AlbumResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Álbum ou artista não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class)))
    })
    AlbumResponse atualizar(@Parameter(description = "Identificador do registro", example = "1", required = true) Long id, @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AlbumRequest.class),
                    examples = @ExampleObject(value = "{\"titulo\":\"1989\",\"anoLancamento\":2014,\"capaUrl\":\"https://exemplo.com/capas/1989.jpg\",\"artistaId\":1}")))
    AlbumRequest request);

    @Operation(summary = "Excluir álbum", description = "Exige ADMIN. Álbuns com músicas cadastradas não podem ser excluídos.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registro excluído", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "409", description = "Álbum possui músicas cadastradas", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class)))
    })
    ResponseEntity<Void> excluir(@Parameter(description = "Identificador do registro", example = "1", required = true) Long id);

    @Operation(summary = "Listar músicas do álbum", description = "Disponível para USER e ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Músicas do álbum", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = MusicaResponse.class)))),
            @ApiResponse(responseCode = "404", description = "Álbum não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CatalogErrorResponse.class)))
    })
    List<MusicaResponse> listarMusicas(@Parameter(description = "Identificador do álbum", example = "1", required = true) Long id);
}
