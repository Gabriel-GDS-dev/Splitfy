ALTER TABLE album ALTER COLUMN titulo TYPE VARCHAR(200);

CREATE INDEX idx_artista_nome ON artista (nome);
CREATE INDEX idx_album_artista_id ON album (artista_id);
CREATE INDEX idx_album_ano_lancamento ON album (ano_lancamento);
