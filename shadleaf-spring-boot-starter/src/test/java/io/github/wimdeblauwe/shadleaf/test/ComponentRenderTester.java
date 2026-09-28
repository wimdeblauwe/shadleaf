package io.github.wimdeblauwe.shadleaf.test;

import io.github.wimdeblauwe.shadleaf.component.ClasspathComponentDefinitionSource;
import io.github.wimdeblauwe.shadleaf.component.ComponentRegistry;
import io.github.wimdeblauwe.shadleaf.dialect.ShadleafDialect;
import io.github.wimdeblauwe.shadleaf.i18n.ShadleafMessageSource;
import io.github.wimdeblauwe.shadleaf.icon.IconRegistry;
import io.github.wimdeblauwe.shadleaf.icon.IconSource;
import io.github.wimdeblauwe.shadleaf.icon.LucideIconSource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.dialect.IDialect;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import tools.jackson.databind.json.JsonMapper;

/**
 * Renders a template snippet such as {@code <sl:button variant="outline">Save</sl:button>} without a Spring
 * application context, and returns it as {@link Rendered} for {@link ShadleafAssertions}.
 * <p>
 * It uses {@link SpringTemplateEngine}, so expressions are SpEL, exactly as in a consuming application. Component
 * templates resolve from {@code templates/sl/**} on the classpath, so an application's own copy of a template wins
 * here just as it does at runtime; the snippet itself is the template. Messages come from the built-in
 * {@code shadleaf/messages.properties} unless {@link Builder#messageSource(MessageSource)} says otherwise.
 * <p>
 * Every render runs in a mock servlet web exchange, so {@code @{/orders}} resolves against the context path and
 * request attributes (such as a CSP nonce) are context variables, as in a request.
 * <p>
 * This is the one definition of "render a component": the component tests, the approval tests and the docs preview
 * generator (M5) all use it.
 */
public final class ComponentRenderTester {

  private final SpringTemplateEngine engine;
  private final JakartaServletWebApplication webApplication;
  private final MockServletContext servletContext;
  private final Locale locale;
  private final String contextPath;
  private final Map<String, Object> requestAttributes;

  private ComponentRenderTester(Builder builder) {
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
    engine.addDialect(builder.dialect != null ? builder.dialect : defaultDialect(builder.iconSources));
    engine.setTemplateEngineMessageSource(
        builder.messageSource != null ? builder.messageSource : new ShadleafMessageSource());

    servletContext = new MockServletContext();
    servletContext.setContextPath(builder.contextPath);
    webApplication = JakartaServletWebApplication.buildApplication(servletContext);
    locale = builder.locale;
    contextPath = builder.contextPath;
    requestAttributes = Map.copyOf(builder.requestAttributes);
  }

  /** The library's components, the bundled lucide icons and the built-in messages, at context path {@code ""}. */
  public static ComponentRenderTester create() {
    return builder().build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public Rendered render(String snippet) {
    return render(snippet, Map.of());
  }

  public Rendered render(String snippet, Map<String, ?> variables) {
    MockHttpServletRequest request = new MockHttpServletRequest(servletContext, "GET", contextPath + "/");
    request.setContextPath(contextPath);
    requestAttributes.forEach(request::setAttribute);
    WebContext context = new WebContext(webApplication.buildExchange(request, new MockHttpServletResponse()), locale);
    variables.forEach(context::setVariable);
    return new Rendered(engine.process(snippet, context));
  }

  /** The engine, for tests that inspect its configuration. */
  public SpringTemplateEngine engine() {
    return engine;
  }

  private static ShadleafDialect defaultDialect(List<IconSource> applicationIconSources) {
    ComponentRegistry registry = new ComponentRegistry(List.of(
        new ClasspathComponentDefinitionSource(ComponentRenderTester.class.getClassLoader())));
    List<IconSource> iconSources = new ArrayList<>(applicationIconSources);
    iconSources.add(new LucideIconSource(JsonMapper.builder().build()));
    return new ShadleafDialect(registry, new IconRegistry(iconSources));
  }

  public static final class Builder {

    private final List<IconSource> iconSources = new ArrayList<>();
    private final Map<String, Object> requestAttributes = new LinkedHashMap<>();
    private @Nullable IDialect dialect;
    private @Nullable MessageSource messageSource;
    private Locale locale = Locale.ENGLISH;
    private String contextPath = "";

    private Builder() {
    }

    /** Application icon sources, asked in this order before the bundled lucide icons. */
    public Builder iconSources(IconSource... sources) {
      iconSources.addAll(List.of(sources));
      return this;
    }

    /** Replaces the Shadleaf dialect, for engine tests; {@link #iconSources} is then ignored. */
    public Builder dialect(IDialect dialect) {
      this.dialect = dialect;
      return this;
    }

    /**
     * The messages the templates see. Attach the built-in strings with
     * {@link ShadleafMessageSource#attachTo}, as the auto-configuration does.
     */
    public Builder messageSource(MessageSource messageSource) {
      this.messageSource = messageSource;
      return this;
    }

    public Builder locale(Locale locale) {
      this.locale = locale;
      return this;
    }

    /** The servlet context path, which {@code @{/...}} links start with. */
    public Builder contextPath(String contextPath) {
      this.contextPath = contextPath;
      return this;
    }

    /** A request attribute, visible to templates as a context variable, like a CSP nonce. */
    public Builder requestAttribute(String name, Object value) {
      requestAttributes.put(name, value);
      return this;
    }

    public ComponentRenderTester build() {
      return new ComponentRenderTester(this);
    }
  }
}
