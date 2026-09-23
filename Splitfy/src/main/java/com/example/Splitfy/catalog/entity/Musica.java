package com.example.Splitfy.catalog.entity;

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
@Table(name = "musica")
public class Musica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(name = "duracao_segundos", nullable = false)
    private Integer duracaoSegundos;

    @Column(name = "numero_faixa")
    private Integer numeroFaixa;

    @Column(nullable = false)
    private Long reproducoes = 0L;

    @Column(name = "album_id", nullable = false)
    private Long albumId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "genero_id", nullable = false)
    private Genero genero;

    @Column(name = "criado_em", insertable = false, updatable = false)
    private LocalDateTime criadoEm;

    protected Musica() {
    }

    public Musica(String titulo, Integer duracaoSegundos, Integer numeroFaixa, Long albumId, Genero genero) {
        this.titulo = titulo;
        this.duracaoSegundos = duracaoSegundos;
        this.numeroFaixa = numeroFaixa;
        this.albumId = albumId;
        this.genero = genero;
        this.reproducoes = 0L;
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public Integer getDuracaoSegundos() {
        return duracaoSegundos;
    }

    public void setDuracaoSegundos(Integer duracaoSegundos) {
        this.duracaoSegundos = duracaoSegundos;
    }

    public Integer getNumeroFaixa() {
        return numeroFaixa;
    }

    public void setNumeroFaixa(Integer numeroFaixa) {
        this.numeroFaixa = numeroFaixa;
    }

    public Long getReproducoes() {
        return reproducoes;
    }

    public void setReproducoes(Long reproducoes) {
        this.reproducoes = reproducoes;
    }

    public Long getAlbumId() {
        return albumId;
    }

    public void setAlbumId(Long albumId) {
        this.albumId = albumId;
    }

    public Genero getGenero() {
        return genero;
    }

    public void setGenero(Genero genero) {
        this.genero = genero;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }
}
