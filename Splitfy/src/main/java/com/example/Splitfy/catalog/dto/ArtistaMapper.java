package com.example.Splitfy.catalog.dto;

import com.example.Splitfy.catalog.entity.Artista;

public final class ArtistaMapper {

    private ArtistaMapper() {
    }

    public static Artista toEntity(ArtistaRequest request) {
        Artista artista = new Artista();
        updateEntity(artista, request);
        return artista;
    }

    public static void updateEntity(Artista artista, ArtistaRequest request) {
        artista.setNome(request.nome());
        artista.setBiografia(request.biografia());
        artista.setPais(request.pais());
        artista.setInicioCarreira(request.inicioCarreira());
    }

    public static ArtistaResponse toResponse(Artista artista) {
        return new ArtistaResponse(
                artista.getId(),
                artista.getNome(),
                artista.getBiografia(),
                artista.getPais(),
                artista.getInicioCarreira(),
                artista.getCriadoEm()
        );
    }
}
