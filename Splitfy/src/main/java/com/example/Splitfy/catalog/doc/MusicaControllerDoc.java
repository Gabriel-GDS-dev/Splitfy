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
import com.example.Splitfy.historico.dto.RegistrarReproducaoRequisicao;

@Tag(name = "Músicas", description = "Consulta e manutenção de músicas")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado", content = @Content()),
        @ApiResponse(responseCode = "403", description = "Usuário sem permissão para a operação", content = @Content())
})
public interface MusicaControllerDoc {
    @Operation(summary = "Listar músicas", description = "Disponível para USER e ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada, podendo estar vazia", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = MusicaResponse.class))))
    })
    List<MusicaResponse> listar(
            @Parameter(description = "Parte do título; filtro opcional", example = "Minha") String titulo,
            @Parameter(description = "ID do gênero; filtro opcional", example = "1") Long generoId,
            @Parameter(description = "ID do álbum; filtro opcional", example = "1") Long albumId);

    @Operation(summary = "Buscar música", description = "Disponível para USER e ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MusicaResponse.class))),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CatalogErrorResponse.class)))
    })
    MusicaResponse buscar(@Parameter(description = "Identificador do registro", example = "1", required = true) Long id);

    @Operation(summary = "Cadastrar música", description = "Exige ADMIN. Álbum e gênero precisam existir; duração deve ser positiva; número da faixa é opcional.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registro criado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MusicaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou duplicados", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CatalogErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Álbum ou gênero não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CatalogErrorResponse.class)))
    })
    ResponseEntity<MusicaResponse> criar(@io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MusicaRequest.class),
                    examples = @ExampleObject(value = "{\"titulo\":\"Minha música\",\"duracaoSegundos\":210,\"numeroFaixa\":1,\"albumId\":1,\"generoId\":1}")))
                                         MusicaRequest request);

    @Operation(summary = "Atualizar música", description = "Exige ADMIN. Substitui os dados do registro.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro atualizado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MusicaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou duplicados", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CatalogErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Registro ou referência não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CatalogErrorResponse.class)))
    })
    MusicaResponse atualizar(@Parameter(description = "Identificador do registro", example = "1", required = true) Long id, @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MusicaRequest.class),
                    examples = @ExampleObject(value = "{\"titulo\":\"Minha música\",\"duracaoSegundos\":210,\"numeroFaixa\":1,\"albumId\":1,\"generoId\":1}")))
    MusicaRequest request);

    @Operation(summary = "Excluir música", description = "Exige ADMIN. Vínculos existentes podem impedir a exclusão.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registro excluído", content = @Content()),
            @ApiResponse(responseCode = "400", description = "Existem vínculos que impedem a exclusão", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CatalogErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CatalogErrorResponse.class)))
    })
    ResponseEntity<Void> excluir(@Parameter(description = "Identificador do registro", example = "1", required = true) Long id);

    @Operation(summary = "Registrar reprodução", description = "USER pode registrar para seu próprio usuarioId; ADMIN pode registrar para outro usuário. Incrementa o contador e integra com o histórico de reprodução.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reprodução registrada", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MusicaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Corpo ou identificador inválido", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Música ou usuário não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class)))
    })
    MusicaResponse registrarPlay(@Parameter(description = "Identificador do registro", example = "1", required = true) Long id,
                                 @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = RegistrarReproducaoRequisicao.class),
                                                 examples = @ExampleObject(value = "{\"usuarioId\":1}")))
                                 RegistrarReproducaoRequisicao requisicao);
}
