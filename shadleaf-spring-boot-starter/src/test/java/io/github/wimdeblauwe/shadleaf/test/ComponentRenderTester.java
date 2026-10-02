package io.github.wimdeblauwe.shadleaf.test;

import io.github.wimdeblauwe.shadleaf.component.ClasspathComponentDefinitionSource;
import io.github.wimdeblauwe.shadleaf.component.ComponentRegistry;
import io.github.wimdeblauwe.shadleaf.dialect.ShadleafDialect;
import io.github.wimdeblauwe.shadleaf.i18n.ShadleafMessageSource;
import io.github.wimdeblauwe.shadleaf.icon.IconRegistry;
import io.github.wimdeblauwe.shadleaf.icon.IconSource;
import io.github.wimdeblauwe.shadleaf.icon.LucideIconSource;
import io.github.wimdeblauwe.shadleaf.paging.PagingParameters;
import io.github.wimdeblauwe.shadleaf.security.UserSource;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.dialect.IDialect;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.context.webmvc.SpringWebMvcThymeleafRequestContext;
import org.thymeleaf.spring6.naming.SpringContextVariableNames;
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
 * Who is signed in comes from Spring Security's request post-processors, as with MockMvc:
 * {@code builder().with(oidcLogin())}, {@code with(user("ada"))}; without one the visitor is anonymous.
 * <p>
 * Every render runs in a mock servlet web exchange, so {@code @{/orders}} resolves against the context path and
 * request attributes (such as a CSP nonce) are context variables, as in a request. It also carries Spring MVC's
 * {@code RequestContext}, as {@code ThymeleafView} sets it up, so {@code th:field}, {@code th:errors} and
 * {@code #fields} find a {@code BindingResult} passed in the variables under
 * {@code BindingResult.MODEL_KEY_PREFIX + name}.
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
  private final String requestUri;
  private final Map<String, Object> requestAttributes;
  private final Map<String, String> cookies;
  private final List<RequestPostProcessor> requestPostProcessors;

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
    snippetResolver.setCacheable(builder.cacheSnippets);
    snippetResolver.setOrder(2);

    engine = new SpringTemplateEngine();
    engine.addTemplateResolver(componentResolver);
    engine.addTemplateResolver(snippetResolver);
    engine.addDialect(builder.dialect != null
        ? builder.dialect
        : defaultDialect(builder.iconSources, builder.pagingParameters,
            builder.userSource != null ? builder.userSource : TesterSecurity.defaultUserSource()));
    MessageSource messageSource = builder.messageSource != null ? builder.messageSource : new ShadleafMessageSource();
    engine.setTemplateEngineMessageSource(messageSource);

    servletContext = new MockServletContext();
    servletContext.setContextPath(builder.contextPath);
    // RequestContext needs a web application context; its message source resolves binding error messages, as the
    // application's messageSource bean does.
    GenericWebApplicationContext applicationContext = new GenericWebApplicationContext(servletContext);
    applicationContext.getBeanFactory().registerSingleton("messageSource", messageSource);
    TesterSecurity.registerBeans(applicationContext.getBeanFactory());
    applicationContext.refresh();
    servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, applicationContext);
    webApplication = JakartaServletWebApplication.buildApplication(servletContext);
    locale = builder.locale;
    contextPath = builder.contextPath;
    requestUri = builder.requestUri;
    requestAttributes = Map.copyOf(builder.requestAttributes);
    cookies = Map.copyOf(builder.cookies);
    requestPostProcessors = List.copyOf(builder.requestPostProcessors);
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
    int query = requestUri.indexOf('?');
    String path = query < 0 ? requestUri : requestUri.substring(0, query);
    MockHttpServletRequest request = new MockHttpServletRequest(servletContext, "GET", contextPath + path);
    request.setContextPath(contextPath);
    if (query >= 0) {
      String queryString = requestUri.substring(query + 1);
      request.setQueryString(queryString);
      UriComponentsBuilder.fromUriString("?" + queryString).build().getQueryParams().forEach((name, values) ->
          values.forEach(value -> request.addParameter(UriUtils.decode(name, StandardCharsets.UTF_8),
              value == null ? "" : UriUtils.decode(value, StandardCharsets.UTF_8))));
    }
    request.addPreferredLocale(locale);
    requestAttributes.forEach(request::setAttribute);
    if (!cookies.isEmpty()) {
      request.setCookies(cookies.entrySet().stream()
          .map(cookie -> new Cookie(cookie.getKey(), cookie.getValue()))
          .toArray(Cookie[]::new));
    }
    MockHttpServletRequest processed = request;
    for (RequestPostProcessor postProcessor : requestPostProcessors) {
      processed = postProcessor.postProcessRequest(processed);
    }
    try {
      TesterSecurity.loadContext(processed);
      return render(snippet, variables, processed);
    } finally {
      TesterSecurity.clearContext();
    }
  }

  private Rendered render(String snippet, Map<String, ?> variables, MockHttpServletRequest request) {
    MockHttpServletResponse response = new MockHttpServletResponse();
    WebContext context = new WebContext(webApplication.buildExchange(request, response), locale);
    variables.forEach(context::setVariable);
    RequestContext requestContext = new RequestContext(request, response, servletContext,
        new LinkedHashMap<String, Object>(variables));
    context.setVariable(SpringContextVariableNames.SPRING_REQUEST_CONTEXT, requestContext);
    context.setVariable(SpringContextVariableNames.THYMELEAF_REQUEST_CONTEXT,
        new SpringWebMvcThymeleafRequestContext(requestContext, request));
    return new Rendered(engine.process(snippet, context));
  }

  /** The engine, for tests that inspect its configuration. */
  public SpringTemplateEngine engine() {
    return engine;
  }

  private static ShadleafDialect defaultDialect(List<IconSource> applicationIconSources,
      PagingParameters pagingParameters, UserSource userSource) {
    ComponentRegistry registry = new ComponentRegistry(List.of(
        new ClasspathComponentDefinitionSource(ComponentRenderTester.class.getClassLoader())));
    List<IconSource> iconSources = new ArrayList<>(applicationIconSources);
    iconSources.add(new LucideIconSource(JsonMapper.builder().build()));
    return new ShadleafDialect(registry, new IconRegistry(iconSources), pagingParameters, userSource);
  }

  public static final class Builder {

    private final List<IconSource> iconSources = new ArrayList<>();
    private final Map<String, Object> requestAttributes = new LinkedHashMap<>();
    private final Map<String, String> cookies = new LinkedHashMap<>();
    private final List<RequestPostProcessor> requestPostProcessors = new ArrayList<>();
    private @Nullable IDialect dialect;
    private @Nullable UserSource userSource;
    private @Nullable MessageSource messageSource;
    private Locale locale = Locale.ENGLISH;
    private String contextPath = "";
    private String requestUri = "/";
    private PagingParameters pagingParameters = PagingParameters.defaults();
    private boolean cacheSnippets;

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

    /**
     * The path and query string of the request every render runs in ({@code /people?sort=name,desc}), within the
     * context path. Defaults to {@code /}.
     */
    public Builder requestUri(String requestUri) {
      this.requestUri = requestUri;
      return this;
    }

    /** The request parameter names of the sort and page links, as {@code spring.data.web.*} sets them. */
    public Builder pagingParameters(PagingParameters pagingParameters) {
      this.pagingParameters = pagingParameters;
      return this;
    }

    /** A request attribute, visible to templates as a context variable, like a CSP nonce. */
    public Builder requestAttribute(String name, Object value) {
      requestAttributes.put(name, value);
      return this;
    }

    /** A cookie the request sends, such as the sidebar's {@code sl-sidebar-state}. */
    public Builder cookie(String name, String value) {
      cookies.put(name, value);
      return this;
    }

    /**
     * Prepares every render's request, as {@code MockMvc}'s {@code with()}: Spring Security's {@code oidcLogin()},
     * {@code oauth2Login()} or {@code user("ada")} sign a user in for the render. Without one the visitor is
     * anonymous.
     */
    public Builder with(RequestPostProcessor postProcessor) {
      requestPostProcessors.add(postProcessor);
      return this;
    }

    /**
     * Where {@code #slUser} gets the user and the sign-in and sign-out URLs from. Defaults to what an application with
     * Spring Security and the default resolver gets (anonymous without Spring Security), with {@code /login} and
     * {@code /logout}.
     */
    public Builder userSource(UserSource userSource) {
      this.userSource = userSource;
      return this;
    }

    /**
     * Caches the parsed snippet, keyed by its text, as an application's page templates are cached in production.
     * Off by default, so every render parses the snippet again.
     */
    public Builder cacheSnippets() {
      this.cacheSnippets = true;
      return this;
    }

    public ComponentRenderTester build() {
      return new ComponentRenderTester(this);
    }
  }
}
