package com.example.Splitfy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.config.location=file:src/main/resources/application.properties",
        "splitfy.jwt.secret=chave-exclusiva-para-testes-com-mais-de-32-bytes",
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:1/indisponivel",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.datasource.hikari.connection-timeout=1000"
})
class InicializacaoHttpIntegracaoTest {
    @LocalServerPort int port;
    @Autowired ConfigurableApplicationContext context;

    @Test
    void atendeHttpComSegurancaAntesDeConectarAoBanco() throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var home = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/")).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(home.statusCode()).isEqualTo(200);
            var cadastro = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/musicas"))
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{}")).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(cadastro.statusCode()).isEqualTo(401);
            assertThat(context.getBeanFactory().containsSingleton("entityManagerFactory")).isFalse();
            assertThat(context.getBeanFactory().containsSingleton("flywayInitializer")).isFalse();
        }
    }
}
