package com.example.Splitfy;

import com.example.Splitfy.catalog.entity.Usuario;
import com.example.Splitfy.catalog.repository.UsuarioRepository;
import java.time.LocalDate;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Entrada exclusiva para validar a interface com H2; nunca faz parte do artefato de produção. */
public class InterfaceTestApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(SplitfyApplication.class);
        app.addListeners((ApplicationListener<ApplicationReadyEvent>) event -> {
            var repositorio = event.getApplicationContext().getBean(UsuarioRepository.class);
            var senhas = event.getApplicationContext().getBean(PasswordEncoder.class);
            if (!repositorio.findAll().isEmpty()) throw new IllegalStateException("Use somente um banco H2 vazio para esta entrada de teste.");
            repositorio.save(new Usuario("Admin de teste", "admin-interface@splitfy.test",
                    senhas.encode("InterfaceTeste123!"), LocalDate.of(2000, 1, 1), "ADMIN"));
        });
        app.run(args);
    }
}
