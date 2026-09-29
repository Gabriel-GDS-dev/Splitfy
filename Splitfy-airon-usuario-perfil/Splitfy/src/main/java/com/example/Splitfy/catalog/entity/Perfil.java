package com.example.Splitfy.catalog.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "perfil")
public class Perfil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "nome_exibicao", nullable = false, length = 150)
    private String nomeExibicao;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
    private LocalDateTime criadoEm;

    protected Perfil() {}

    public Perfil(Usuario usuario, String nomeExibicao, String bio, String fotoUrl) {
        this.usuario = usuario;
        this.nomeExibicao = nomeExibicao;
        this.bio = bio;
        this.fotoUrl = fotoUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getNomeExibicao() {
        return nomeExibicao;
    }

    public void setNomeExibicao(String nomeExibicao) {
        this.nomeExibicao = nomeExibicao;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }
//    public Long getId() { return id; }
//    public Usuario getUsuario() { return usuario; }
//    public String getNomeExibicao() { return nomeExibicao; }
//    public void setNomeExibicao(String nomeExibicao) { this.nomeExibicao = nomeExibicao; }
//    public String getBio() { return bio; }
//    public void setBio(String bio) { this.bio = bio; }
//    public String getFotoUrl() { return fotoUrl; }
//    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
//    public LocalDateTime getCriadoEm() { return criadoEm; }
}
