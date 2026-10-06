package com.example.Splitfy.seguranca;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class ConfiguracaoSeguranca {

    @Bean
    SecurityFilterChain filtroSeguranca(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basico -> basico.disable())
                .formLogin(formulario -> formulario.disable())
                .authorizeHttpRequests(rotas -> rotas
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/usuarios").permitAll()
                        .requestMatchers("/", "/index.html", "/assets/**", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/usuarios", "/api/usuarios/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/usuarios/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/usuarios/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/usuarios/*/ativar").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/musicas/*/play").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/artistas", "/api/albuns", "/api/generos", "/api/musicas").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/artistas/*", "/api/albuns/*", "/api/generos/*", "/api/musicas/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/artistas/*", "/api/albuns/*", "/api/generos/*", "/api/musicas/*").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(conversorJwt())));
        return http.build();
    }

    @Bean
    PasswordEncoder codificadorSenha() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecretKey chaveJwt(@Value("${splitfy.jwt.secret}") String segredo) {
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalArgumentException("SPLITFY_JWT_SECRET deve ter pelo menos 32 bytes.");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder codificadorJwt(SecretKey chaveJwt) {
        return NimbusJwtEncoder.withSecretKey(chaveJwt).build();
    }

    @Bean
    JwtDecoder decodificadorJwt(SecretKey chaveJwt) {
        return NimbusJwtDecoder.withSecretKey(chaveJwt).macAlgorithm(MacAlgorithm.HS256).build();
    }

    private JwtAuthenticationConverter conversorJwt() {
        JwtGrantedAuthoritiesConverter permissoes = new JwtGrantedAuthoritiesConverter();
        permissoes.setAuthoritiesClaimName("role");
        permissoes.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(permissoes);
        return conversor;
    }
}
