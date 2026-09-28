package io.github.wimdeblauwe.shadleaf.sample01;

import io.github.wimdeblauwe.shadleaf.icon.IconSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ShadleafSample01Application {

  public static void main(String[] args) {
    SpringApplication.run(ShadleafSample01Application.class, args);
  }

  /**
   * The application's own icons, from src/main/resources/icons/*.svg. Asked before the bundled lucide icons, so
   * icons/leaf.svg replaces lucide's leaf.
   */
  @Bean
  IconSource applicationIcons() {
    return IconSource.classpathDirectory("icons/");
  }
}
