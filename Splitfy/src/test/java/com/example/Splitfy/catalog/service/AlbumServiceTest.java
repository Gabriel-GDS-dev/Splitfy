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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    private record Lancamento(String titulo, int ano) {
    }

    // Discografias usadas nos testes: álbuns de estúdio em ordem de lançamento.
    // Para a Taylor Swift, inclui também as regravações (Taylor's Version);
    // para o Kamaitachi, apenas os lançamentos mais recentes.
    private static final Map<String, List<Lancamento>> DISCOGRAFIAS = new LinkedHashMap<>();

    static {
        DISCOGRAFIAS.put("Taylor Swift", List.of(
                new Lancamento("Taylor Swift", 2006),
                new Lancamento("Fearless", 2008),
                new Lancamento("Speak Now", 2010),
                new Lancamento("Red", 2012),
                new Lancamento("1989", 2014),
                new Lancamento("reputation", 2017),
                new Lancamento("Lover", 2019),
                new Lancamento("folklore", 2020),
                new Lancamento("evermore", 2020),
                new Lancamento("Fearless (Taylor's Version)", 2021),
                new Lancamento("Red (Taylor's Version)", 2021),
                new Lancamento("Midnights", 2022),
                new Lancamento("Speak Now (Taylor's Version)", 2023),
                new Lancamento("1989 (Taylor's Version)", 2023),
                new Lancamento("The Tortured Poets Department", 2024),
                new Lancamento("The Life of a Showgirl", 2025)));
        DISCOGRAFIAS.put("Calvin Harris", List.of(
                new Lancamento("I Created Disco", 2007),
                new Lancamento("Ready for the Weekend", 2009),
                new Lancamento("18 Months", 2012),
                new Lancamento("Motion", 2014),
                new Lancamento("Funk Wav Bounces Vol. 1", 2017),
                new Lancamento("Funk Wav Bounces Vol. 2", 2022)));
        DISCOGRAFIAS.put("Demi Lovato", List.of(
                new Lancamento("Don't Forget", 2008),
                new Lancamento("Here We Go Again", 2009),
                new Lancamento("Unbroken", 2011),
                new Lancamento("Demi", 2013),
                new Lancamento("Confident", 2015),
                new Lancamento("Tell Me You Love Me", 2017),
                new Lancamento("Dancing with the Devil... the Art of Starting Over", 2021),
                new Lancamento("Holy Fvck", 2022),
                new Lancamento("It's Not That Deep", 2025)));
        DISCOGRAFIAS.put("Alok", List.of(
                new Lancamento("O Futuro É Ancestral", 2024)));
        DISCOGRAFIAS.put("Katy Perry", List.of(
                new Lancamento("Katy Hudson", 2001),
                new Lancamento("One of the Boys", 2008),
                new Lancamento("Teenage Dream", 2010),
                new Lancamento("Prism", 2013),
                new Lancamento("Witness", 2017),
                new Lancamento("Smile", 2020),
                new Lancamento("143", 2024)));
        DISCOGRAFIAS.put("Madonna", List.of(
                new Lancamento("Madonna", 1983),
                new Lancamento("Like a Virgin", 1984),
                new Lancamento("True Blue", 1986),
                new Lancamento("Like a Prayer", 1989),
                new Lancamento("Erotica", 1992),
                new Lancamento("Bedtime Stories", 1994),
                new Lancamento("Ray of Light", 1998),
                new Lancamento("Music", 2000),
                new Lancamento("American Life", 2003),
                new Lancamento("Confessions on a Dance Floor", 2005),
                new Lancamento("Hard Candy", 2008),
                new Lancamento("MDNA", 2012),
                new Lancamento("Rebel Heart", 2015),
                new Lancamento("Madame X", 2019),
                new Lancamento("Confessions II", 2026)));
        DISCOGRAFIAS.put("Dua Lipa", List.of(
                new Lancamento("Dua Lipa", 2017),
                new Lancamento("Future Nostalgia", 2020),
                new Lancamento("Radical Optimism", 2024)));
        DISCOGRAFIAS.put("Kamaitachi", List.of(
                new Lancamento("Festa", 2023),
                new Lancamento("Acústico Kamaitachi (Ao Vivo)", 2025)));
        DISCOGRAFIAS.put("Twenty One Pilots", List.of(
                new Lancamento("Twenty One Pilots", 2009),
                new Lancamento("Regional at Best", 2011),
                new Lancamento("Vessel", 2013),
                new Lancamento("Blurryface", 2015),
                new Lancamento("Trench", 2018),
                new Lancamento("Scaled and Icy", 2021),
                new Lancamento("Clancy", 2024),
                new Lancamento("Breach", 2025)));
        DISCOGRAFIAS.put("Depeche Mode", List.of(
                new Lancamento("Speak & Spell", 1981),
                new Lancamento("A Broken Frame", 1982),
                new Lancamento("Construction Time Again", 1983),
                new Lancamento("Some Great Reward", 1984),
                new Lancamento("Black Celebration", 1986),
                new Lancamento("Music for the Masses", 1987),
                new Lancamento("Violator", 1990),
                new Lancamento("Songs of Faith and Devotion", 1993),
                new Lancamento("Ultra", 1997),
                new Lancamento("Exciter", 2001),
                new Lancamento("Playing the Angel", 2005),
                new Lancamento("Sounds of the Universe", 2009),
                new Lancamento("Delta Machine", 2013),
                new Lancamento("Spirit", 2017),
                new Lancamento("Memento Mori", 2023)));
        DISCOGRAFIAS.put("Roxette", List.of(
                new Lancamento("Pearls of Passion", 1986),
                new Lancamento("Look Sharp!", 1988),
                new Lancamento("Joyride", 1991),
                new Lancamento("Tourism", 1992),
                new Lancamento("Crash! Boom! Bang!", 1994),
                new Lancamento("Have a Nice Day", 1999),
                new Lancamento("Room Service", 2001),
                new Lancamento("Charm School", 2011),
                new Lancamento("Travelling", 2012),
                new Lancamento("Good Karma", 2016)));
        DISCOGRAFIAS.put("Cyndi Lauper", List.of(
                new Lancamento("She's So Unusual", 1983),
                new Lancamento("True Colors", 1986),
                new Lancamento("A Night to Remember", 1989),
                new Lancamento("Hat Full of Stars", 1993),
                new Lancamento("Sisters of Avalon", 1996),
                new Lancamento("Merry Christmas... Have a Nice Life", 1998),
                new Lancamento("At Last", 2003),
                new Lancamento("Shine", 2004),
                new Lancamento("The Body Acoustic", 2005),
                new Lancamento("Bring Ya to the Brink", 2008),
                new Lancamento("Memphis Blues", 2010),
                new Lancamento("Detour", 2016)));
        DISCOGRAFIAS.put("New Order", List.of(
                new Lancamento("Movement", 1981),
                new Lancamento("Power, Corruption & Lies", 1983),
                new Lancamento("Low-Life", 1985),
                new Lancamento("Brotherhood", 1986),
                new Lancamento("Technique", 1989),
                new Lancamento("Republic", 1993),
                new Lancamento("Get Ready", 2001),
                new Lancamento("Waiting for the Sirens' Call", 2005),
                new Lancamento("Lost Sirens", 2013),
                new Lancamento("Music Complete", 2015)));
    }

    static Stream<Arguments> albunsDasDiscografias() {
        return DISCOGRAFIAS.entrySet().stream()
                .flatMap(e -> e.getValue().stream().map(l -> arguments(e.getKey(), l.titulo(), l.ano())));
    }

    static Stream<String> artistasDasDiscografias() {
        return DISCOGRAFIAS.keySet().stream();
    }

    @ParameterizedTest(name = "{0} - {1} ({2})")
    @MethodSource("albunsDasDiscografias")
    void criarDeveSalvarTodosOsAlbunsDaDiscografia(String nomeArtista, String titulo, int ano) {
        when(artistaRepository.findById(1L)).thenReturn(Optional.of(artista(1L, nomeArtista)));
        when(albumRepository.save(any(Album.class))).thenAnswer(inv -> inv.getArgument(0));

        AlbumResponse response = albumService.criar(new AlbumRequest(titulo, ano, null, 1L));

        assertThat(response.titulo()).isEqualTo(titulo);
        assertThat(response.anoLancamento()).isEqualTo(ano);
        assertThat(response.artistaNome()).isEqualTo(nomeArtista);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("artistasDasDiscografias")
    void listarPorArtistaDeveRetornarDiscografiaCompleta(String nomeArtista) {
        Artista artista = artista(1L, nomeArtista);
        List<Lancamento> lancamentos = DISCOGRAFIAS.get(nomeArtista);
        Pageable paginaCompleta = PageRequest.of(0, 20);
        List<Album> discografia = new ArrayList<>();
        for (int i = 0; i < lancamentos.size(); i++) {
            Lancamento l = lancamentos.get(i);
            discografia.add(album(i + 1L, artista, l.titulo(), l.ano()));
        }
        when(artistaRepository.existsById(1L)).thenReturn(true);
        when(albumRepository.findByArtistaId(1L, paginaCompleta))
                .thenReturn(new PageImpl<>(discografia, paginaCompleta, discografia.size()));

        Page<AlbumResponse> resultado = albumService.listarPorArtista(1L, paginaCompleta);

        assertThat(resultado.getTotalElements()).isEqualTo(lancamentos.size());
        assertThat(resultado.getContent())
                .extracting(AlbumResponse::titulo)
                .containsExactlyElementsOf(lancamentos.stream().map(Lancamento::titulo).toList());
        assertThat(resultado.getContent()).extracting(AlbumResponse::artistaNome).containsOnly(nomeArtista);
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
