package com.example.Splitfy.playlist.entity;

import com.example.Splitfy.catalog.entity.Musica;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "playlist_musica")
public class PlaylistMusica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "playlist_id", nullable = false)
    private Playlist playlist;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "musica_id", nullable = false)
    private Musica musica;

    @Column(name = "adicionado_em", insertable = false, updatable = false)
    private LocalDateTime adicionadoEm;

    protected PlaylistMusica() {
    }

    public PlaylistMusica(Playlist playlist, Musica musica) {
        this.playlist = playlist;
        this.musica = musica;
    }

    public Long getId() {
        return id;
    }

    public Playlist getPlaylist() {
        return playlist;
    }

    public Musica getMusica() {
        return musica;
    }

    public LocalDateTime getAdicionadoEm() {
        return adicionadoEm;
    }
}
