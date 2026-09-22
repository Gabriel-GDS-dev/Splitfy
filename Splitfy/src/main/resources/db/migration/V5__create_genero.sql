CREATE TABLE genero(
                       id BIGSERIAL PRIMARY KEY,
                       nome VARCHAR(150) NOT NULL UNIQUE,
                       descricao TEXT
);
