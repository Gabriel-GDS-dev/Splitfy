package com.example.Splitfy.catalog.service;

import com.example.Splitfy.catalog.dto.AlbumRequest;
import com.example.Splitfy.catalog.dto.AlbumResponse;
import com.example.Splitfy.catalog.entity.Album;
import com.example.Splitfy.catalog.entity.Artista;
import com.example.Splitfy.catalog.exception.DadosInvalidosException;
import com.example.Splitfy.catalog.exception.RecursoNaoEncontradoException;
import com.example.Splitfy.catalog.exception.RegraDeNegocioException;
import com.example.Splitfy.catalog.repository.AlbumRepository;
import com.example.Splitfy.catalog.repository.ArtistaRepository;
import com.example.Splitfy.catalog.repository.MusicaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlbumServiceTest {

    @Mock
    private AlbumRepository albumRepository;

    @Mock
    private ArtistaRepository artistaRepository;

    @Mock
    private MusicaRepository musicaRepository;

    @InjectMocks
    private AlbumService albumService;

    private final Pageable pageable = PageRequest.of(0, 10);

    // Discografia da Taylor Swift: álbuns de estúdio e regravações (Taylor's Version), em ordem de lançamento.
    static Stream<Arguments> albunsTaylorSwift() {
        return Stream.of(
                arguments("Taylor Swift", 2006),
                arguments("Fearless", 2008),
                arguments("Speak Now", 2010),
                arguments("Red", 2012),
                arguments("1989", 2014),
                arguments("reputation", 2017),
                arguments("Lover", 2019),
                arguments("folklore", 2020),
                arguments("evermore", 2020),
                arguments("Fearless (Taylor's Version)", 2021),
                arguments("Red (Taylor's Version)", 2021),
                arguments("Midnights", 2022),
                arguments("Speak Now (Taylor's Version)", 2023),
                arguments("1989 (Taylor's Version)", 2023),
                arguments("The Tortured Poets Department", 2024),
                arguments("The Life of a Showgirl", 2025)
        );
    }

    @ParameterizedTest(name = "{0} ({1})")
    @MethodSource("albunsTaylorSwift")
    void criarDeveSalvarTodosOsAlbunsDaTaylorSwift(String titulo, int ano) {
        when(artistaRepository.findById(1L)).thenReturn(Optional.of(artista(1L, "Taylor Swift")));
        when(albumRepository.save(any(Album.class))).thenAnswer(inv -> inv.getArgument(0));

        AlbumResponse response = albumService.criar(new AlbumRequest(titulo, ano, null, 1L));

        assertThat(response.titulo()).isEqualTo(titulo);
        assertThat(response.anoLancamento()).isEqualTo(ano);
        assertThat(response.artistaNome()).isEqualTo("Taylor Swift");
    }

    @Test
    void listarPorArtistaDeveRetornarDiscografiaCompletaDaTaylorSwift() {
        Artista taylor = artista(1L, "Taylor Swift");
        Pageable paginaCompleta = PageRequest.of(0, 20);
        long[] id = {1};
        List<Album> discografia = albunsTaylorSwift()
                .map(Arguments::get)
                .map(args -> album(id[0]++, taylor, (String) args[0], (Integer) args[1]))
                .toList();
        when(artistaRepository.existsById(1L)).thenReturn(true);
        when(albumRepository.findByArtistaId(1L, paginaCompleta))
                .thenReturn(new PageImpl<>(discografia, paginaCompleta, discografia.size()));

        Page<AlbumResponse> resultado = albumService.listarPorArtista(1L, paginaCompleta);

        assertThat(resultado.getTotalElements()).isEqualTo(16);
        assertThat(resultado.getContent())
                .extracting(AlbumResponse::titulo)
                .startsWith("Taylor Swift", "Fearless")
                .endsWith("The Tortured Poets Department", "The Life of a Showgirl");
        assertThat(resultado.getContent()).extracting(AlbumResponse::artistaNome).containsOnly("Taylor Swift");
    }

    @Test
    void criarDeveSalvarComArtista() {
        when(artistaRepository.findById(1L)).thenReturn(Optional.of(artista(1L, "Taylor Swift")));
        when(albumRepository.save(any(Album.class))).thenAnswer(inv -> {
            Album a = inv.getArgument(0);
            a.setId(10L);
            a.setCriadoEm(LocalDateTime.now());
            return a;
        });

        AlbumResponse response = albumService.criar(new AlbumRequest("Fearless", 2008, null, 1L));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.artistaId()).isEqualTo(1L);
        assertThat(response.artistaNome()).isEqualTo("Taylor Swift");
    }

    @Test
    void criarComArtistaInexistenteDeveLancar404() {
        when(artistaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> albumService.criar(new AlbumRequest("X", 2000, null, 99L)))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Artista");
        verify(albumRepository, never()).save(any());
    }

    @Test
    void criarComAnoFuturoDeveLancarDadosInvalidos() {
        int anoFuturo = Year.now().getValue() + 1;

        assertThatThrownBy(() -> albumService.criar(new AlbumRequest("X", anoFuturo, null, 1L)))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("anoLancamento");
        verifyNoInteractions(albumRepository, artistaRepository);
    }

    @Test
    void criarComAnoAtualDevePermitir() {
        when(artistaRepository.findById(1L)).thenReturn(Optional.of(artista(1L, "Taylor Swift")));
        when(albumRepository.save(any(Album.class))).thenAnswer(inv -> inv.getArgument(0));

        AlbumResponse response = albumService.criar(new AlbumRequest("Novo", Year.now().getValue(), null, 1L));

        assertThat(response.anoLancamento()).isEqualTo(Year.now().getValue());
    }

    @Test
    void atualizarComAnoFuturoDeveLancarDadosInvalidos() {
        int anoFuturo = Year.now().getValue() + 1;

        assertThatThrownBy(() -> albumService.atualizar(10L, new AlbumRequest("X", anoFuturo, null, 1L)))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    void buscarPorIdInexistenteDeveLancar404() {
        when(albumRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> albumService.buscarPorId(7L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("7");
    }

    @Test
    void atualizarTrocandoParaArtistaInexistenteDeveLancar404() {
        Album album = album(10L, artista(1L, "Taylor Swift"), 2014);
        when(albumRepository.findById(10L)).thenReturn(Optional.of(album));
        when(artistaRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> albumService.atualizar(10L, new AlbumRequest("X", 2014, null, 2L)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void atualizarTrocandoArtistaDeveAtualizarRelacionamento() {
        Album album = album(10L, artista(1L, "Taylor Swift"), 2014);
        when(albumRepository.findById(10L)).thenReturn(Optional.of(album));
        when(artistaRepository.findById(2L)).thenReturn(Optional.of(artista(2L, "Titãs")));

        AlbumResponse response = albumService.atualizar(10L, new AlbumRequest("Novo", 1986, null, 2L));

        assertThat(response.artistaId()).isEqualTo(2L);
        assertThat(response.artistaNome()).isEqualTo("Titãs");
        assertThat(response.titulo()).isEqualTo("Novo");
    }

    @Test
    void atualizarComMesmoArtistaNaoConsultaArtista() {
        Album album = album(10L, artista(1L, "Taylor Swift"), 2014);
        when(albumRepository.findById(10L)).thenReturn(Optional.of(album));

        albumService.atualizar(10L, new AlbumRequest("Novo", 2005, null, 1L));

        verify(artistaRepository, never()).findById(anyLong());
    }

    @Test
    void listarComAnoEArtistaDeveCombinarFiltros() {
        Artista taylor = artista(1L, "Taylor Swift");
        Page<Album> pagina = new PageImpl<>(
                List.of(album(8L, taylor, "folklore", 2020), album(9L, taylor, "evermore", 2020)), pageable, 2);
        when(albumRepository.findByArtistaIdAndAnoLancamento(1L, 2020, pageable)).thenReturn(pagina);

        Page<AlbumResponse> resultado = albumService.listar(2020, 1L, pageable);

        assertThat(resultado.getContent()).extracting(AlbumResponse::titulo).containsExactly("folklore", "evermore");
        verify(albumRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void listarSomenteComAnoDeveFiltrarPorAno() {
        when(albumRepository.findByAnoLancamento(2014, pageable)).thenReturn(Page.empty(pageable));

        albumService.listar(2014, null, pageable);

        verify(albumRepository).findByAnoLancamento(2014, pageable);
    }

    @Test
    void listarSomenteComArtistaDeveFiltrarPorArtista() {
        when(albumRepository.findByArtistaId(1L, pageable)).thenReturn(Page.empty(pageable));

        albumService.listar(null, 1L, pageable);

        verify(albumRepository).findByArtistaId(1L, pageable);
    }

    @Test
    void listarSemFiltrosDeveRetornarTodos() {
        when(albumRepository.findAll(pageable)).thenReturn(Page.empty(pageable));

        albumService.listar(null, null, pageable);

        verify(albumRepository).findAll(pageable);
    }

    @Test
    void listarPorArtistaInexistenteDeveLancar404() {
        when(artistaRepository.existsById(3L)).thenReturn(false);

        assertThatThrownBy(() -> albumService.listarPorArtista(3L, pageable))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void excluirDeveRemoverAlbum() {
        Album album = album(10L, artista(1L, "Taylor Swift"), 2014);
        when(albumRepository.findById(10L)).thenReturn(Optional.of(album));
        when(musicaRepository.existsByAlbumId(10L)).thenReturn(false);

        albumService.excluir(10L);

        verify(albumRepository).delete(album);
    }

    @Test
    void excluirAlbumComMusicasDeveLancar409() {
        Album album = album(10L, artista(1L, "Taylor Swift"), 2014);
        when(albumRepository.findById(10L)).thenReturn(Optional.of(album));
        when(musicaRepository.existsByAlbumId(10L)).thenReturn(true);

        assertThatThrownBy(() -> albumService.excluir(10L))
                .isInstanceOf(RegraDeNegocioException.class);
        verify(albumRepository, never()).delete(any(Album.class));
    }

    private static Artista artista(Long id, String nome) {
        Artista a = new Artista();
        a.setId(id);
        a.setNome(nome);
        return a;
    }

    private static Album album(Long id, Artista artista, Integer ano) {
        return album(id, artista, "Álbum " + id, ano);
    }

    private static Album album(Long id, Artista artista, String titulo, Integer ano) {
        Album a = new Album();
        a.setId(id);
        a.setTitulo(titulo);
        a.setAnoLancamento(ano);
        a.setArtista(artista);
        a.setCriadoEm(LocalDateTime.now());
        return a;
    }
}
