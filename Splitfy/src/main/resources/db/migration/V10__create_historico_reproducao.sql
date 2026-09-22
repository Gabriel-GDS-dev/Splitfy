CREATE TABLE historico_reproducao(
                                     id BIGSERIAL PRIMARY KEY,
                                     usuario_id BIGINT NOT NULL REFERENCES usuario(id),
                                     musica_id BIGINT NOT NULL REFERENCES musica(id),
                                     reproduzido_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_historico_usuario_reproduzido
    ON historico_reproducao (usuario_id, reproduzido_em DESC);
