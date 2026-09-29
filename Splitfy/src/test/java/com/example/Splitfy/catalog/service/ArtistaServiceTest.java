package com.example.Splitfy.catalog.service;

import com.example.Splitfy.catalog.dto.ArtistaRequest;
import com.example.Splitfy.catalog.dto.ArtistaResponse;
import com.example.Splitfy.catalog.entity.Artista;
import com.example.Splitfy.catalog.exception.RecursoNaoEncontradoException;
import com.example.Splitfy.catalog.exception.RegraDeNegocioException;
import com.example.Splitfy.catalog.repository.AlbumRepository;
import com.example.Splitfy.catalog.repository.ArtistaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArtistaServiceTest {

    @Mock
    private ArtistaRepository artistaRepository;

    @Mock
    private AlbumRepository albumRepository;

    @InjectMocks
    private ArtistaService artistaService;

    private final Pageable pageable = PageRequest.of(0, 10);

    @Test
    void criarDeveSalvarERetornarResponse() {
        ArtistaRequest request = new ArtistaRequest("Taylor Swift", "Cantora e compositora norte-americana.", "Estados Unidos", LocalDate.of(2006, 10, 24));
        when(artistaRepository.save(any(Artista.class))).thenAnswer(inv -> {
            Artista a = inv.getArgument(0);
            a.setId(1L);
            a.setCriadoEm(LocalDateTime.now());
            return a;
        });

        ArtistaResponse response = artistaService.criar(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.nome()).isEqualTo("Taylor Swift");
        assertThat(response.pais()).isEqualTo("Estados Unidos");
        assertThat(response.criadoEm()).isNotNull();
    }

    @Test
    void buscarPorIdInexistenteDeveLancar404() {
        when(artistaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> artistaService.buscarPorId(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("99");
    }

    @Test
    void atualizarDeveAlterarCampos() {
        Artista existente = artista(1L, "Antigo");
        when(artistaRepository.findById(1L)).thenReturn(Optional.of(existente));

        ArtistaResponse response = artistaService.atualizar(1L, new ArtistaRequest("Novo", null, "Portugal", null));

        assertThat(response.nome()).isEqualTo("Novo");
        assertThat(existente.getPais()).isEqualTo("Portugal");
    }

    @Test
    void excluirArtistaComAlbunsDeveLancarConflito() {
        Artista artista = artista(1L, "Taylor Swift");
        when(artistaRepository.findById(1L)).thenReturn(Optional.of(artista));
        when(albumRepository.existsByArtistaId(1L)).thenReturn(true);

        assertThatThrownBy(() -> artistaService.excluir(1L))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("álbuns");
        verify(artistaRepository, never()).delete(any());
    }

    @Test
    void excluirArtistaSemAlbunsDeveRemover() {
        Artista artista = artista(1L, "Taylor Swift");
        when(artistaRepository.findById(1L)).thenReturn(Optional.of(artista));
        when(albumRepository.existsByArtistaId(1L)).thenReturn(false);

        artistaService.excluir(1L);

        verify(artistaRepository).delete(artista);
    }

    @Test
    void excluirArtistaInexistenteDeveLancar404() {
        when(artistaRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> artistaService.excluir(5L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void listarComNomeDeveBuscarPorNomeParcial() {
        Page<Artista> pagina = new PageImpl<>(List.of(artista(1L, "Taylor Swift")), pageable, 1);
        when(artistaRepository.findByNomeContainingIgnoreCase("swift", pageable)).thenReturn(pagina);

        Page<ArtistaResponse> resultado = artistaService.listar("  swift ", pageable);

        assertThat(resultado.getContent()).extracting(ArtistaResponse::nome).containsExactly("Taylor Swift");
        verify(artistaRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void listarSemNomeDeveRetornarTodos() {
        when(artistaRepository.findAll(pageable)).thenReturn(Page.empty(pageable));

        artistaService.listar(" ", pageable);

        verify(artistaRepository).findAll(pageable);
        verify(artistaRepository, never()).findByNomeContainingIgnoreCase(any(), any());
    }

    private static Artista artista(Long id, String nome) {
        Artista a = new Artista();
        a.setId(id);
        a.setNome(nome);
        a.setCriadoEm(LocalDateTime.now());
        return a;
    }
}
