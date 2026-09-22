CREATE TABLE album(
                      id BIGSERIAL PRIMARY KEY,
                      titulo VARCHAR(150) NOT NULL,
                      ano_lancamento INTEGER,
                      capa_url VARCHAR(500),
                      artista_id BIGINT NOT NULL REFERENCES artista(id),
                      criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
