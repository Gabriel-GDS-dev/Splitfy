CREATE TABLE musica(
                       id BIGSERIAL PRIMARY KEY,
                       titulo VARCHAR(150) NOT NULL,
                       duracao_segundos INTEGER NOT NULL CHECK (duracao_segundos > 0),
                       numero_faixa INTEGER,
                       reproducoes BIGINT NOT NULL DEFAULT 0 CHECK (reproducoes >= 0),
                       album_id BIGINT NOT NULL REFERENCES album(id),
                       genero_id BIGINT NOT NULL REFERENCES genero(id),
                       criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
