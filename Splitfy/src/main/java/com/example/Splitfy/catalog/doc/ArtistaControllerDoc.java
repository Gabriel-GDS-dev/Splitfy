package com.example.Splitfy.catalog.doc;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.Splitfy.catalog.dto.AlbumResponse;
import com.example.Splitfy.catalog.dto.ArtistaRequest;
import com.example.Splitfy.catalog.dto.ArtistaResponse;
import com.example.Splitfy.catalog.exception.ErroResponse;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;

@Tag(name = "Artistas", description = "Consulta e manutenção de artistas")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado", content = @Content()),
        @ApiResponse(responseCode = "403", description = "Usuário sem permissão para a operação", content = @Content())
})
public interface ArtistaControllerDoc {
    @Operation(summary = "Cadastrar artista", description = "Exige ADMIN. Nome obrigatório; início de carreira não pode ser data futura.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registro criado; a URL dele vem no header Location", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ArtistaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class)))
    })
    ResponseEntity<ArtistaResponse> criar(@io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ArtistaRequest.class),
                    examples = @ExampleObject(value = "{\"nome\":\"Taylor Swift\",\"biografia\":\"Cantora e compositora norte-americana.\",\"pais\":\"Estados Unidos\",\"inicioCarreira\":\"2006-06-19\"}")))
                                          ArtistaRequest request);

    @Operation(summary = "Listar artistas", description = "Disponível para USER e ADMIN. Paginado; filtra opcionalmente por trecho do nome.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de artistas, podendo estar vazia")
    })
    PagedModel<ArtistaResponse> listar(@Parameter(description = "Trecho do nome do artista", example = "Taylor") String nome,
                                       @ParameterObject Pageable pageable);

    @Operation(summary = "Buscar artista", description = "Disponível para USER e ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ArtistaResponse.class))),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class)))
    })
    ArtistaResponse buscarPorId(@Parameter(description = "Identificador do registro", example = "1", required = true) Long id);

    @Operation(summary = "Atualizar artista", description = "Exige ADMIN. Substitui os dados do registro.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro atualizado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ArtistaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class)))
    })
    ArtistaResponse atualizar(@Parameter(description = "Identificador do registro", example = "1", required = true) Long id, @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ArtistaRequest.class),
                    examples = @ExampleObject(value = "{\"nome\":\"Taylor Swift\",\"biografia\":\"Cantora e compositora norte-americana.\",\"pais\":\"Estados Unidos\",\"inicioCarreira\":\"2006-06-19\"}")))
    ArtistaRequest request);

    @Operation(summary = "Excluir artista", description = "Exige ADMIN. Artistas com álbuns cadastrados não podem ser excluídos.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registro excluído", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "409", description = "Artista possui álbuns cadastrados", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class)))
    })
    ResponseEntity<Void> excluir(@Parameter(description = "Identificador do registro", example = "1", required = true) Long id);

    @Operation(summary = "Listar álbuns do artista", description = "Disponível para USER e ADMIN. Paginado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de álbuns do artista"),
            @ApiResponse(responseCode = "404", description = "Artista não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class)))
    })
    PagedModel<AlbumResponse> listarAlbuns(@Parameter(description = "Identificador do artista", example = "1", required = true) Long id,
                                           @ParameterObject Pageable pageable);
}
