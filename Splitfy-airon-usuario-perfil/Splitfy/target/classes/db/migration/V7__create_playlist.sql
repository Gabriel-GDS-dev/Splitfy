CREATE TABLE playlist(
                         id BIGSERIAL PRIMARY KEY,
                         nome VARCHAR(150) NOT NULL,
                         descricao TEXT,
                         publica BOOLEAN NOT NULL DEFAULT FALSE,
                         usuario_id BIGINT NOT NULL REFERENCES usuario(id),
                         criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
