CREATE TABLE usuario(
                        id BIGSERIAL PRIMARY KEY,
                        nome VARCHAR(150) NOT NULL,
                        email VARCHAR(150) NOT NULL UNIQUE,
                        senha_hash VARCHAR(255) NOT NULL,
                        data_nascimento DATE NOT NULL,
                        role VARCHAR(10) NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
                        ativo BOOLEAN NOT NULL DEFAULT TRUE,
                        criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
