package io.github.wimdeblauwe.shadleaf.sample03;

import java.time.Duration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

/**
 * One Keycloak for every test class, with the realm compose.yaml imports. Spring Security reads the issuer's discovery
 * document when the context starts, so the container runs before any context: tests call {@link #register} from a
 * {@code @DynamicPropertySource} method.
 */
final class KeycloakContainer {

  static final String REALM = "shadleaf";

  private static final GenericContainer<?> KEYCLOAK = start();

  private KeycloakContainer() {
  }

  @SuppressWarnings("resource") // stopped by Testcontainers' Ryuk when the JVM ends
  private static GenericContainer<?> start() {
    GenericContainer<?> keycloak = new GenericContainer<>(System.getProperty("keycloak.image",
        "quay.io/keycloak/keycloak:26.8.0"))
        .withCommand("start-dev", "--import-realm")
        .withEnv("KC_HEALTH_ENABLED", "true")
        .withCopyFileToContainer(MountableFile.forHostPath("keycloak/shadleaf-realm.json"),
            "/opt/keycloak/data/import/shadleaf-realm.json")
        .withExposedPorts(8080, 9000)
        .waitingFor(Wait.forHttp("/health/ready").forPort(9000).withStartupTimeout(Duration.ofMinutes(3)));
    keycloak.start();
    return keycloak;
  }

  /** The issuer as the browser and the application both see it: {@code http://localhost:<mapped port>/realms/...}. */
  static String issuer() {
    return "http://" + KEYCLOAK.getHost() + ":" + KEYCLOAK.getMappedPort(8080) + "/realms/" + REALM;
  }

  static void register(DynamicPropertyRegistry registry) {
    registry.add("spring.security.oauth2.client.provider.keycloak.issuer-uri", KeycloakContainer::issuer);
  }
}
