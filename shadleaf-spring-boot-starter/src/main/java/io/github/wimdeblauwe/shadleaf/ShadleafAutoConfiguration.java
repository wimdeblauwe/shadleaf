package io.github.wimdeblauwe.shadleaf;

import io.github.wimdeblauwe.shadleaf.assets.ShadleafAssets;
import io.github.wimdeblauwe.shadleaf.assets.ViteManifestParser;
import io.github.wimdeblauwe.shadleaf.component.ClasspathComponentDefinitionSource;
import io.github.wimdeblauwe.shadleaf.component.ComponentDefinitionSource;
import io.github.wimdeblauwe.shadleaf.component.ComponentRegistry;
import io.github.wimdeblauwe.shadleaf.component.FileSystemComponentDefinitionSource;
import io.github.wimdeblauwe.shadleaf.dev.ApplicationTemplateOverrides;
import io.github.wimdeblauwe.shadleaf.dev.ShadleafDevTemplateResolver;
import io.github.wimdeblauwe.shadleaf.dev.WebTypesFileWriter;
import io.github.wimdeblauwe.shadleaf.dialect.ShadleafDialect;
import io.github.wimdeblauwe.shadleaf.i18n.ShadleafMessageSource;
import io.github.wimdeblauwe.shadleaf.icon.IconRegistry;
import io.github.wimdeblauwe.shadleaf.icon.IconSource;
import io.github.wimdeblauwe.shadleaf.icon.LucideIconSource;
import io.github.wimdeblauwe.shadleaf.i18n.ShadleafMessageSourcePostProcessor;
import io.github.wimdeblauwe.shadleaf.theme.ShadleafThemeScript;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.util.StringUtils;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import tools.jackson.databind.json.JsonMapper;

// After MessageSourceAutoConfiguration, so the fallback messageSource below only applies when it backed off.
@AutoConfiguration(after = MessageSourceAutoConfiguration.class)
@EnableConfigurationProperties(ShadleafProperties.class)
public class ShadleafAutoConfiguration {

  private static final Duration WEB_TYPES_RECHECK_INTERVAL = Duration.ofSeconds(2);

  @Bean
  @ConditionalOnMissingBean
  public ShadleafAssets shadleafAssets(ShadleafProperties properties,
      ViteManifestParser viteManifestParser) {
    return new ShadleafAssets(properties.skin(), properties.assets().variant(), properties.assets().alpine(),
        properties.dev().viteServerUrl(), viteManifestParser);
  }

  @Bean
  @ConditionalOnMissingBean
  public ShadleafThemeScript shadleafThemeScript(ShadleafProperties properties) {
    return new ShadleafThemeScript(properties.csp().nonceAttribute());
  }

  /**
   * Adds the components' built-in strings ({@code shadleaf/messages.properties}) behind the application's own
   * {@code messageSource}.
   */
  @Bean
  public static ShadleafMessageSourcePostProcessor shadleafMessageSourcePostProcessor() {
    return new ShadleafMessageSourcePostProcessor();
  }

  /**
   * The message source of an application that has none: Spring Boot only configures one when the application has a
   * {@code messages.properties}.
   */
  @Bean(AbstractApplicationContext.MESSAGE_SOURCE_BEAN_NAME)
  @ConditionalOnMissingBean(name = AbstractApplicationContext.MESSAGE_SOURCE_BEAN_NAME, search = SearchStrategy.CURRENT)
  public MessageSource messageSource() {
    return new ShadleafMessageSource();
  }

  @Bean
  @ConditionalOnMissingBean
  public ViteManifestParser viteManifestParser(JsonMapper jsonMapper) {
    return new ViteManifestParser(jsonMapper);
  }

  /**
   * The components the dialect can render. Sources are asked in order: the templates on disk when
   * {@code shadleaf.dev.templates-path} is set (re-read when they change, and skipped for a component the application
   * overrides with its own template), then any {@link ComponentDefinitionSource} beans, then the component templates on
   * the classpath.
   */
  @Bean
  @ConditionalOnMissingBean
  public ComponentRegistry shadleafComponentRegistry(ShadleafProperties properties,
      ObjectProvider<ComponentDefinitionSource> additionalSources, ResourceLoader resourceLoader) {
    List<ComponentDefinitionSource> sources = new ArrayList<>();
    String templatesPath = properties.dev().templatesPath();
    if (StringUtils.hasText(templatesPath)) {
      sources.add(FileSystemComponentDefinitionSource.forTemplatesPath(templatesPath,
          new ApplicationTemplateOverrides(resourceLoader.getClassLoader())));
    }
    additionalSources.orderedStream().forEach(sources::add);
    sources.add(new ClasspathComponentDefinitionSource(ResourcePatternUtils.getResourcePatternResolver(resourceLoader),
        ClasspathComponentDefinitionSource.DEFAULT_LOCATION_PATTERN));
    return new ComponentRegistry(sources);
  }

  /**
   * The icons {@code <sl:icon>} can render: any {@link IconSource} beans, in order, then the bundled lucide icons. The
   * first source that knows a name wins, so an application can add icons and replace bundled ones.
   */
  @Bean
  @ConditionalOnMissingBean
  public IconRegistry shadleafIconRegistry(ObjectProvider<IconSource> iconSources, JsonMapper jsonMapper) {
    List<IconSource> sources = new ArrayList<>();
    iconSources.orderedStream().forEach(sources::add);
    sources.add(new LucideIconSource(jsonMapper));
    return new IconRegistry(sources);
  }

  @Bean
  @ConditionalOnMissingBean
  public ShadleafDialect shadleafDialect(ComponentRegistry componentRegistry, IconRegistry iconRegistry) {
    return new ShadleafDialect(componentRegistry, iconRegistry);
  }

  /**
   * Writes the IDE metadata for the application's components to {@code shadleaf.dev.web-types-file}: at startup, and
   * every two seconds while {@code shadleaf.dev.templates-path} lets templates change without a restart.
   */
  @Bean
  @ConditionalOnProperty("shadleaf.dev.web-types-file")
  public WebTypesFileWriter shadleafWebTypesFileWriter(ShadleafProperties properties,
      ComponentRegistry componentRegistry, JsonMapper jsonMapper) {
    ShadleafProperties.DevProperties dev = properties.dev();
    return new WebTypesFileWriter(componentRegistry, jsonMapper, Path.of(dev.webTypesFile()),
        StringUtils.hasText(dev.templatesPath()) ? WEB_TYPES_RECHECK_INTERVAL : null);
  }

  @Bean
  @ConditionalOnProperty("shadleaf.dev.templates-path")
  public FileTemplateResolver shadleafDevTemplateResolver(ShadleafProperties properties,
      ResourceLoader resourceLoader) {
    FileTemplateResolver resolver = new ShadleafDevTemplateResolver(
        new ApplicationTemplateOverrides(resourceLoader.getClassLoader()));
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
