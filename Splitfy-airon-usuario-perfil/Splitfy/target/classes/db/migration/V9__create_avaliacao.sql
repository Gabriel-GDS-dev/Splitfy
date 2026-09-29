CREATE TABLE avaliacao(
                          id BIGSERIAL PRIMARY KEY,
                          usuario_id BIGINT NOT NULL REFERENCES usuario(id),
                          musica_id BIGINT NOT NULL REFERENCES musica(id),
                          curtido BOOLEAN NOT NULL DEFAULT FALSE,
                          nota INTEGER CHECK (nota BETWEEN 1 AND 5),
                          criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          UNIQUE (usuario_id, musica_id)
);
