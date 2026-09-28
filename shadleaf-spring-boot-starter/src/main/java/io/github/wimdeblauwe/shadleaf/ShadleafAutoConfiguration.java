package io.github.wimdeblauwe.shadleaf;

import io.github.wimdeblauwe.shadleaf.assets.ShadleafAssets;
import io.github.wimdeblauwe.shadleaf.assets.ViteManifestParser;
import io.github.wimdeblauwe.shadleaf.dialect.ShadleafDialect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import tools.jackson.databind.json.JsonMapper;

@AutoConfiguration
@EnableConfigurationProperties(ShadleafProperties.class)
public class ShadleafAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public ShadleafAssets shadleafAssets(ShadleafProperties properties,
      ViteManifestParser viteManifestParser) {
    return new ShadleafAssets(properties.dev().viteServerUrl(), viteManifestParser);
  }

  @Bean
  @ConditionalOnMissingBean
  public ViteManifestParser viteManifestParser(JsonMapper jsonMapper) {
    return new ViteManifestParser(jsonMapper);
  }

  @Bean
  @ConditionalOnMissingBean
  public ShadleafDialect shadleafDialect() {
    return new ShadleafDialect();
  }

  @Bean
  @ConditionalOnProperty("shadleaf.dev.templates-path")
  public FileTemplateResolver shadleafDevTemplateResolver(ShadleafProperties properties) {
    FileTemplateResolver resolver = new FileTemplateResolver();
    resolver.setPrefix(properties.dev().templatesPath());
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    resolver.setCharacterEncoding("UTF-8");
    resolver.setCacheable(false);
    resolver.setCheckExistence(true);
    resolver.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return resolver;
  }
}
