package com.example.Splitfy.historico.entity;

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
@Table(name = "historico_reproducao")
public class HistoricoReproducao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "musica_id", nullable = false)
    private Musica musica;

    @Column(name = "reproduzido_em", nullable = false, insertable = false, updatable = false)
    private LocalDateTime reproduzidoEm;

    protected HistoricoReproducao() {
    }

    public HistoricoReproducao(Long usuarioId, Musica musica) {
        this.usuarioId = usuarioId;
        this.musica = musica;
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public Musica getMusica() {
        return musica;
    }

    public LocalDateTime getReproduzidoEm() {
        return reproduzidoEm;
    }
}
