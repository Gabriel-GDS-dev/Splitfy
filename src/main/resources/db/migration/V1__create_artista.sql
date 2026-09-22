CREATE TABLE artista (
    id              BIGSERIAL    PRIMARY KEY,
    nome            VARCHAR(150) NOT NULL,
    biografia       TEXT,
    pais            VARCHAR(100),
    inicio_carreira DATE,
    criado_em       TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_artista_nome ON artista (nome);
