CREATE TABLE perfil(
                       id BIGSERIAL PRIMARY KEY,
                       usuario_id BIGINT NOT NULL UNIQUE REFERENCES usuario(id),
                       nome_exibicao VARCHAR(150) NOT NULL,
                       bio TEXT,
                       foto_url VARCHAR(500),
                       criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
