package com.example.Splitfy.catalog.dto;

import com.example.Splitfy.catalog.entity.Album;
import com.example.Splitfy.catalog.entity.Artista;

public final class AlbumMapper {

    private AlbumMapper() {
    }

    public static Album toEntity(AlbumRequest request, Artista artista) {
        Album album = new Album();
        updateEntity(album, request, artista);
        return album;
    }

    public static void updateEntity(Album album, AlbumRequest request, Artista artista) {
        album.setTitulo(request.titulo());
        album.setAnoLancamento(request.anoLancamento());
        album.setCapaUrl(request.capaUrl());
        album.setArtista(artista);
    }

    public static AlbumResponse toResponse(Album album) {
        Artista artista = album.getArtista();
        return new AlbumResponse(
                album.getId(),
                album.getTitulo(),
                album.getAnoLancamento(),
                album.getCapaUrl(),
                artista.getId(),
                artista.getNome(),
                album.getCriadoEm()
        );
    }
}
