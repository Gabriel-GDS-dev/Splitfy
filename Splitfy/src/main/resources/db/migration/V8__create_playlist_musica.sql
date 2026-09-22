CREATE TABLE playlist_musica(
                                id BIGSERIAL PRIMARY KEY,
                                playlist_id BIGINT NOT NULL REFERENCES playlist(id),
                                musica_id BIGINT NOT NULL REFERENCES musica(id),
                                adicionado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                UNIQUE (playlist_id, musica_id)
);
