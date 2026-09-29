package com.example.Splitfy.catalog.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 255)
    private String senhaHash;

    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    @Column(nullable = false, length = 10)
    private String role = "USER";

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
    private LocalDateTime criadoEm;

    protected Usuario() {}

    public Usuario(String nome, String email, String senhaHash, LocalDate dataNascimento, String role) {
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.dataNascimento = dataNascimento;
        this.role = role;
        this.ativo = true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(LocalDate dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }
//    public Long getId() { return id; }
//    public String getNome() { return nome; }
//    public void setNome(String nome) { this.nome = nome; }
//    public String getEmail() { return email; }
//    public void setEmail(String email) { this.email = email; }
//    public String getSenhaHash() { return senhaHash; }
//    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
//    public LocalDate getDataNascimento() { return dataNascimento; }
//    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }
//    public String getRole() { return role; }
//    public void setRole(String role) { this.role = role; }
//    public boolean isAtivo() { return ativo; }
//    public void setAtivo(boolean ativo) { this.ativo = ativo; }
//    public LocalDateTime getCriadoEm() { return criadoEm; }
}
