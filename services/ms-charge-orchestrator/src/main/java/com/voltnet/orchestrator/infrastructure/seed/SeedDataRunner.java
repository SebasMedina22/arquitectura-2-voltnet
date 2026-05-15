package com.voltnet.orchestrator.infrastructure.seed;

import com.voltnet.orchestrator.domain.model.User;
import com.voltnet.orchestrator.domain.model.UserId;
import com.voltnet.orchestrator.domain.port.out.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

/**
 * Seed de la proyeccion local de solvencia. Permite demos inmediatas sin
 * depender de eventos previos de MS-Billing:
 *  - USR-001: solvente (0 dias de mora, metodo de pago activo) -> R2 OK.
 *  - USR-002: deuda 15 dias (< 30) -> R2 OK (caso "vencido pero no bloqueante").
 *  - USR-003: deuda 45 dias (> 30) -> R2 bloquea.
 */
@Component
@ConditionalOnProperty(name = "orchestrator.seed.enabled", havingValue = "true", matchIfMissing = true)
public class SeedDataRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDataRunner.class);

    private final UserRepository userRepository;
    private final Clock clock;

    public SeedDataRunner(UserRepository userRepository, Clock clock) {
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Override
    public void run(String... args) {
        Instant now = Instant.now(clock);
        seed(new User(new UserId("USR-001"), 0,  true, now), "solvente");
        seed(new User(new UserId("USR-002"), 15, true, now), "deuda 15 dias (no bloquea)");
        seed(new User(new UserId("USR-003"), 45, true, now), "deuda 45 dias (R2 bloquea)");
    }

    private void seed(User u, String descripcion) {
        if (userRepository.findById(u.id()).isEmpty()) {
            userRepository.save(u);
            log.info("Seed: usuario {} creado ({})", u.id(), descripcion);
        }
    }
}
