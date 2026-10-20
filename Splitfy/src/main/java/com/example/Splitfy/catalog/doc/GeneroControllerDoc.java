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
import com.example.Splitfy.catalog.dto.*;
import com.example.Splitfy.catalog.exception.ErroResponse;
import org.springframework.http.ResponseEntity;
import java.util.List;

@Tag(name = "Gêneros", description = "Consulta e manutenção de gêneros")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado", content = @Content()),
        @ApiResponse(responseCode = "403", description = "Usuário sem permissão para a operação", content = @Content())
})
public interface GeneroControllerDoc {
    @Operation(summary = "Listar gêneros", description = "Disponível para USER e ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada, podendo estar vazia", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = GeneroResponse.class))))
    })
    List<GeneroResponse> listar();

    @Operation(summary = "Buscar gênero", description = "Disponível para USER e ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GeneroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CatalogErrorResponse.class)))
    })
    GeneroResponse buscar(@Parameter(description = "Identificador do registro", example = "1", required = true) Long id);

    @Operation(summary = "Cadastrar gênero", description = "Exige ADMIN. Nome obrigatório e único, sem diferenciar maiúsculas e minúsculas.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registro criado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GeneroResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou duplicados", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CatalogErrorResponse.class)))
    })
    ResponseEntity<GeneroResponse> criar(@io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = GeneroRequest.class),
                    examples = @ExampleObject(value = "{\"nome\":\"Rock\",\"descricao\":\"Gênero musical\"}")))
                                         GeneroRequest request);

    @Operation(summary = "Atualizar gênero", description = "Exige ADMIN. Substitui os dados do registro.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro atualizado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GeneroResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou duplicados", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CatalogErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Registro ou referência não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CatalogErrorResponse.class)))
    })
    GeneroResponse atualizar(@Parameter(description = "Identificador do registro", example = "1", required = true) Long id, @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = GeneroRequest.class),
                    examples = @ExampleObject(value = "{\"nome\":\"Rock\",\"descricao\":\"Gênero musical\"}")))
    GeneroRequest request);

    @Operation(summary = "Excluir gênero", description = "Exige ADMIN. Gêneros com músicas vinculadas não podem ser excluídos.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registro excluído", content = @Content()),
            @ApiResponse(responseCode = "400", description = "Existem vínculos que impedem a exclusão", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CatalogErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CatalogErrorResponse.class)))
    })
    ResponseEntity<Void> excluir(@Parameter(description = "Identificador do registro", example = "1", required = true) Long id);

    @Operation(summary = "Listar músicas do gênero", description = "Disponível para USER e ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Músicas do gênero", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = MusicaResponse.class)))),
            @ApiResponse(responseCode = "404", description = "Gênero não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CatalogErrorResponse.class)))
    })
    List<MusicaResponse> listarMusicasPorGenero(@Parameter(description = "Identificador do registro", example = "1", required = true) Long id);
}
