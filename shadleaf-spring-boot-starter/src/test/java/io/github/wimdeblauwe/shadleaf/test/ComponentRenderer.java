package io.github.wimdeblauwe.shadleaf.test;

import io.github.wimdeblauwe.shadleaf.component.ClasspathComponentDefinitionSource;
import io.github.wimdeblauwe.shadleaf.component.ComponentRegistry;
import io.github.wimdeblauwe.shadleaf.dialect.ShadleafDialect;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.thymeleaf.context.Context;
import org.thymeleaf.dialect.IDialect;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;

/**
 * Renders a template snippet such as {@code <sl:button variant="outline">Save</sl:button>} to HTML, without a Spring
 * application context.
 * <p>
 * It uses {@link SpringTemplateEngine}, so expressions are SpEL, exactly as in a consuming application. Component
 * templates resolve from {@code templates/sl/**} on the classpath; the snippet itself is the template.
 * <p>
 * This is the seed of {@code ComponentRenderTester} (M4) and of the docs preview generator (M5), which should share
 * this one definition of "render a component".
 */
public final class ComponentRenderer {

  private final SpringTemplateEngine engine;

  public ComponentRenderer() {
    this(new ComponentRegistry(List.of(
        new ClasspathComponentDefinitionSource(ComponentRenderer.class.getClassLoader()))));
  }

  public ComponentRenderer(ComponentRegistry registry) {
    this(new ShadleafDialect(registry));
  }

  public ComponentRenderer(IDialect shadleafDialect) {
    ClassLoaderTemplateResolver componentResolver = new ClassLoaderTemplateResolver();
    componentResolver.setPrefix("templates/");
    componentResolver.setSuffix(".html");
    componentResolver.setTemplateMode(TemplateMode.HTML);
    componentResolver.setCharacterEncoding("UTF-8");
    componentResolver.setResolvablePatterns(Set.of(ShadleafDialect.PREFIX + "/*"));
    componentResolver.setOrder(1);

    StringTemplateResolver snippetResolver = new StringTemplateResolver();
    snippetResolver.setTemplateMode(TemplateMode.HTML);
    snippetResolver.setOrder(2);

    engine = new SpringTemplateEngine();
    engine.addTemplateResolver(componentResolver);
    engine.addTemplateResolver(snippetResolver);
    engine.addDialect(shadleafDialect);
  }

  public String render(String snippet) {
    return render(snippet, Map.of());
  }

  public String render(String snippet, Map<String, ?> variables) {
    Context context = new Context();
    variables.forEach(context::setVariable);
    return engine.process(snippet, context);
  }

  public SpringTemplateEngine engine() {
    return engine;
  }
}
