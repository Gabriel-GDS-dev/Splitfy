CREATE TABLE album (
    id             BIGSERIAL    PRIMARY KEY,
    titulo         VARCHAR(200) NOT NULL,
    ano_lancamento INTEGER,
    capa_url       VARCHAR(500),
    artista_id     BIGINT       NOT NULL,
    criado_em      TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT fk_album_artista FOREIGN KEY (artista_id) REFERENCES artista (id) ON DELETE RESTRICT
);

CREATE INDEX idx_album_artista_id ON album (artista_id);
CREATE INDEX idx_album_ano_lancamento ON album (ano_lancamento);
