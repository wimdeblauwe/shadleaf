package io.github.wimdeblauwe.shadleaf.sample02;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ShadleafSample02Application {

  public static void main(String[] args) {
    SpringApplication.run(ShadleafSample02Application.class, args);
  }
}
