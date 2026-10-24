package io.github.wimdeblauwe.shadleaf.docs;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThat;

import io.github.wimdeblauwe.shadleaf.assets.AlpineVariant;
import io.github.wimdeblauwe.shadleaf.assets.AssetVariant;
import io.github.wimdeblauwe.shadleaf.assets.ShadleafAssets;
import io.github.wimdeblauwe.shadleaf.assets.ViteManifestParser;
import io.github.wimdeblauwe.shadleaf.assets.ViteManifestParser.ViteManifest;
import io.github.wimdeblauwe.shadleaf.component.ComponentRegistry;
import io.github.wimdeblauwe.shadleaf.metadata.ComponentMetadata;
import io.github.wimdeblauwe.shadleaf.metadata.WebTypes;
import io.github.wimdeblauwe.shadleaf.security.ShadleafUser;
import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.FixedUserSource;
import io.github.wimdeblauwe.shadleaf.test.FormModel;
import io.github.wimdeblauwe.shadleaf.test.LibraryComponents;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import io.github.wimdeblauwe.shadleaf.theme.ShadleafThemeScript;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.util.FileSystemUtils;
import org.yaml.snakeyaml.Yaml;
import tools.jackson.databind.json.JsonMapper;

/**
 * Generates everything the docs site ({@code docs/}) derives from the library, into {@value #OUTPUT}:
 * <ul>
 *   <li>{@code previews.json}: every scenario in {@code src/test/resources/previews/*.yaml}, rendered through
 *   {@link ComponentRenderTester}, the same harness the component tests use, so a preview cannot show markup the
 *   library does not emit;</li>
 *   <li>{@code shadleaf/assets/*.css}: the standalone bundle of every skin, found through the Vite manifest, with the
 *   fonts it links;</li>
 *   <li>{@code shadleaf/assets/*.js}: the script of every {@link AlpineVariant}, with the chunks it imports, for the
 *   showcase and the behaviour tests;</li>
 *   <li>{@code components.json} and {@code web-types.json}: the registry export behind the attribute tables, and the
 *   IDE metadata offered for download;</li>
 *   <li>{@code theme-script.json}: the theme script and its CSP hash, for the CSP page.</li>
 * </ul>
 * A scenario's {@code renderSource} can show a photo from {@code previews/photos/} as {@code ${photos.<name>}}, a data:
 * URI, so the previews and the showcase checks never fetch an image. A scenario's {@code pages} are Spring Data
 * pages by variable name ({@code people: {number: 4, size: 10, total: 270}}, {@code number} zero-based, {@code slice:
 * true} for a {@code Slice}), so a pagination renders from a real {@code Page}. A scenario's {@code data} are model
 * variables by name, as YAML writes them (lists, maps, text, numbers), with dates as {@code java.time}: a date
 * ({@code 2024-04-01}) a {@code LocalDate}, a timestamp with an offset or Z an {@code Instant}; a chart's rows, so it
 * renders from the data block an application's would. A scenario's {@code user} is the
 * signed-in user {@code #slUser} gives ({@code name}, {@code username}, {@code email}, and {@code photo}, the name of
 * a photo, or {@code picture}, a URL); without one the visitor is anonymous.
 * {@code docs/scripts/sync.mjs} copies them into the docs project. Adding examples for a component means adding a
 * YAML file; every library component must have one, or be listed under the {@code parts} of its family's file.
 * <p>
 * The layouts of the docs' Layouts section are one YAML file each in {@code previews/layouts/}, a scenario with the id
 * {@code layout--<file name>} and these keys: {@code title}, {@code description}, {@code request} (the page it is
 * rendered for, so {@code #slNav.current} marks one), {@code user} (the signed-in user, as for a component),
 * {@code htmx} ({@code true}: the layout loads htmx and boosts the body), {@code css} (the application's CSS the layout
 * needs), {@code page} ({@code title}, {@code content} and optionally {@code breadcrumb}: what a page passes the
 * layout) and {@code source}: what the layout's body holds between the skip link and the toaster, with {@value
 * #CONTENT_PLACEHOLDER} (and {@value #BREADCRUMB_PLACEHOLDER} for a layout with a breadcrumb) where the page's parts go
 * and {@code ${pageTitle}} for its title. The generator writes the whole layout file and a page that uses it around
 * that source ({@link #layoutTemplate}, {@link #pageTemplate}), and renders the preview with the page's parts in
 * place.
 */
class PreviewGeneratorTest {

  static final String OUTPUT = "target/generated-docs";

  private static final Path OUTPUT_DIRECTORY = Path.of(OUTPUT);
  private static final Path PREVIEWS_DIRECTORY = Path.of("src", "test", "resources", "previews");
  private static final Path PHOTOS_DIRECTORY = PREVIEWS_DIRECTORY.resolve("photos");
  private static final Path LAYOUTS_DIRECTORY = PREVIEWS_DIRECTORY.resolve("layouts");
  static final String BREADCRUMB_PLACEHOLDER = "<th:block th:replace=\"${breadcrumb}\"></th:block>";
  static final String CONTENT_PLACEHOLDER = "<div th:replace=\"${content}\"></div>";
  private static final Map<String, String> PHOTO_TYPES = Map.of("jpg", "image/jpeg", "png", "image/png",
      "svg", "image/svg+xml");
  private static final Path BUILT_ASSETS = Path.of("target", "classes", "META-INF", "resources", "shadleaf");
  private static final Path MANIFEST = Path.of("target", "classes").resolve(ShadleafAssets.MANIFEST_LOCATION);
  private static final Pattern STANDALONE_ENTRY = Pattern.compile("css/entries/shadleaf-([^.]+)\\.css");
  private static final String DEFAULT_SKIN = "vega";

  private final JsonMapper jsonMapper = JsonMapper.builder().build();
  private final ComponentRenderTester tester = ComponentRenderTester.create();
  private final Map<String, ComponentRenderTester> testersByRequest = new HashMap<>();
  private final Map<String, String> photos = loadPhotos();

  @Test
  void generatesDocsArtifacts() throws IOException {
    ComponentRegistry registry = LibraryComponents.registry();
    List<Scenario> scenarios = loadScenarios(registry);
    scenarios.addAll(loadLayouts());

    FileSystemUtils.deleteRecursively(OUTPUT_DIRECTORY);
    Files.createDirectories(OUTPUT_DIRECTORY);

    ViteManifest manifest = new ViteManifestParser(jsonMapper).parse(new FileSystemResource(MANIFEST));
    List<Skin> skins = copySkins(manifest);
    Map<String, String> scripts = copyScripts(manifest);
    List<Preview> previews = scenarios.stream().map(this::render).toList();
    String version = System.getProperty("shadleaf.version", ComponentMetadata.libraryVersion());

    write("previews.json", new Previews(version, skins, scripts, previews));
    ComponentMetadata metadata = ComponentMetadata.of(registry, version);
    metadata.write(jsonMapper, OUTPUT_DIRECTORY.resolve("components.json"));
    WebTypes.write(metadata, jsonMapper, OUTPUT_DIRECTORY.resolve("web-types.json"));
    ShadleafThemeScript themeScript = new ShadleafThemeScript("cspNonce");
    write("theme-script.json", new ThemeScript(themeScript.getCspHash(), themeScript.getContent()));

    assertThat(skins).extracting(Skin::name).startsWith(DEFAULT_SKIN).contains("lyra");
    for (Skin skin : skins) {
      assertThat(OUTPUT_DIRECTORY.resolve(skin.css())).exists();
    }
    assertThat(scripts).containsOnlyKeys("bundled", "csp", "external");
  }

  private Preview render(Scenario scenario) {
    Rendered rendered;
    ComponentRenderTester tester;
    if (scenario.user() != null) {
      tester = ComponentRenderTester.builder()
          .requestUri(scenario.request() == null ? "/" : scenario.request())
          .userSource(FixedUserSource.of(user(scenario.user())))
          .build();
    } else if (scenario.request() != null) {
      tester = testersByRequest.computeIfAbsent(scenario.request(),
          request -> ComponentRenderTester.builder().requestUri(request).build());
    } else {
      tester = this.tester;
    }
    try {
      Map<String, Object> variables = new LinkedHashMap<>(scenario.pages());
      variables.putAll(scenario.data());
      variables.put("photos", photos);
      if (scenario.layout() != null) {
        variables.put("pageTitle", scenario.layout().pageTitle());
      }
      if (scenario.form() == null) {
        rendered = tester.render(scenario.renderSource(), variables);
      } else {
        variables.putAll(scenario.form().variables());
        rendered = tester.render(FormModel.wrap(scenario.renderSource()), variables);
      }
    } catch (RuntimeException e) {
      throw new AssertionError("Preview " + scenario.id() + " does not render: " + e.getMessage(), e);
    }
    assertThat(rendered.html()).as("preview %s", scenario.id()).isNotBlank();
    Layout layout = scenario.layout();
    return new Preview(scenario.id(), scenario.component(), scenario.title(), scenario.description(),
        scenario.source(), rendered.html().strip(), rendered.normalizedHtml(), scenario.showcase(), scenario.width(),
        layout != null, layout == null ? null : layout.css(), layout == null ? null : layout.template(),
        layout == null ? null : layout.page());
  }

  /** Every photo in {@code previews/photos/}, by file name without extension, as a data: URI. */
  private static Map<String, String> loadPhotos() {
    Map<String, String> photos = new LinkedHashMap<>();
    try (Stream<Path> files = Files.list(PHOTOS_DIRECTORY)) {
      for (Path file : files.sorted().toList()) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String type = PHOTO_TYPES.get(name.substring(dot + 1));
        assertThat(type).as("type of %s", file).isNotNull();
        photos.put(name.substring(0, dot),
            "data:" + type + ";base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(file)));
      }
    } catch (IOException e) {
      throw new AssertionError("Cannot read " + PHOTOS_DIRECTORY, e);
    }
    return photos;
  }

  /** The standalone bundle of every skin, default first: the embedded ones carry no reset, so they cannot preview. */
  private List<Skin> copySkins(ViteManifest manifest) throws IOException {
    Set<String> names = new TreeSet<>();
    for (String key : manifest.entries().keySet()) {
      Matcher matcher = STANDALONE_ENTRY.matcher(key);
      if (matcher.matches()) {
        names.add(matcher.group(1));
      }
    }
    List<Skin> skins = new ArrayList<>();
    for (String name : names) {
      ViteManifestParser.ViteManifestEntry entry = manifest.getEntry(AssetVariant.STANDALONE.cssEntry(name));
      String file = entry.file();
      copyAsset(file);
      for (String asset : entry.assets()) {
        if (!Files.exists(OUTPUT_DIRECTORY.resolve("shadleaf").resolve(asset))) {
          copyAsset(asset);
        }
      }
      Skin skin = new Skin(name, "shadleaf/" + file);
      skins.add(name.equals(DEFAULT_SKIN) ? 0 : skins.size(), skin);
    }
    return skins;
  }

  /**
   * The script of every Alpine variant, keyed by its property value, and the chunks the scripts import, statically or
   * on demand (the chart's).
   */
  private Map<String, String> copyScripts(ViteManifest manifest) throws IOException {
    Map<String, String> scripts = new LinkedHashMap<>();
    Set<String> copied = new HashSet<>();
    for (AlpineVariant alpine : AlpineVariant.values()) {
      copyChunk(manifest, alpine.jsEntry(), copied);
      scripts.put(alpine.name().toLowerCase(Locale.ROOT), "shadleaf/" + manifest.getEntry(alpine.jsEntry()).file());
    }
    return scripts;
  }

  private static void copyChunk(ViteManifest manifest, String key, Set<String> copied) throws IOException {
    if (!copied.add(key)) {
      return;
    }
    ViteManifestParser.ViteManifestEntry entry = manifest.getEntry(key);
    copyAsset(entry.file());
    for (String chunk : entry.imports()) {
      copyChunk(manifest, chunk, copied);
    }
    for (String chunk : entry.dynamicImports()) {
      copyChunk(manifest, chunk, copied);
    }
  }

  private static void copyAsset(String file) throws IOException {
    Path target = OUTPUT_DIRECTORY.resolve("shadleaf").resolve(file);
    Files.createDirectories(target.getParent());
    Files.copy(BUILT_ASSETS.resolve(file), target);
  }

  @SuppressWarnings("unchecked")
  private static List<Scenario> loadScenarios(ComponentRegistry registry) throws IOException {
    List<Path> files;
    try (Stream<Path> entries = Files.list(PREVIEWS_DIRECTORY)) {
      files = entries.filter(path -> path.getFileName().toString().endsWith(".yaml")).sorted().toList();
    }
    List<Scenario> scenarios = new ArrayList<>();
    Set<String> ids = new HashSet<>();
    Set<String> components = new TreeSet<>();
    for (Path file : files) {
      Map<String, Object> document;
      try (InputStream in = Files.newInputStream(file)) {
        document = new Yaml().load(in);
      }
      String component = (String) document.get("component");
      assertThat(registry.names()).as("component of %s", file).contains(component);
      components.add(component);
      // A component family (card, card-header, ...) shares one file, which lists the other members as its parts.
      for (String part : (List<String>) document.getOrDefault("parts", List.of())) {
        assertThat(registry.names()).as("part of %s", file).contains(part);
        components.add(part);
      }
      for (Map<String, Object> entry : (List<Map<String, Object>>) document.get("scenarios")) {
        String id = component + "--" + entry.get("id");
        assertThat(ids.add(id)).as("scenario id %s is unique", id).isTrue();
        String source = (String) entry.get("source");
        assertThat(source).as("source of %s", id).isNotBlank();
        String renderSource = (String) entry.getOrDefault("renderSource", source);
        scenarios.add(new Scenario(id, component, (String) entry.get("title"), (String) entry.get("description"),
            source.strip(), renderSource, formModel((Map<String, Object>) entry.get("form")),
            (Boolean) entry.getOrDefault("showcase", true), (String) entry.get("request"),
            pages((Map<String, Map<String, Object>>) entry.getOrDefault("pages", Map.of())),
            (Map<String, Object>) javaTime(entry.getOrDefault("data", Map.of())), (Integer) entry.get("width"), (Map<String, String>) entry.get("user"), null));
      }
    }
    assertThat(components).as("components with previews in %s", PREVIEWS_DIRECTORY)
        .containsExactlyElementsOf(registry.names());
    return scenarios;
  }

  /** The layouts in {@code previews/layouts/}: one scenario per file, off the showcase (a page holds one shell). */
  @SuppressWarnings("unchecked")
  private static List<Scenario> loadLayouts() throws IOException {
    List<Path> files;
    try (Stream<Path> entries = Files.list(LAYOUTS_DIRECTORY)) {
      files = entries.filter(path -> path.getFileName().toString().endsWith(".yaml")).sorted().toList();
    }
    List<Scenario> scenarios = new ArrayList<>();
    for (Path file : files) {
      Map<String, Object> document;
      try (InputStream in = Files.newInputStream(file)) {
        document = new Yaml().load(in);
      }
      String name = file.getFileName().toString().replaceFirst("\\.yaml$", "");
      String id = "layout--" + name;
      String source = ((String) document.get("source")).strip();
      Map<String, String> page = (Map<String, String>) document.get("page");
      @Nullable String breadcrumb = page.get("breadcrumb") == null ? null : page.get("breadcrumb").strip();
      String content = page.get("content").strip();
      assertThat(source).as("source of %s", id).contains(CONTENT_PLACEHOLDER);
      assertThat(source.contains(BREADCRUMB_PLACEHOLDER)).as("breadcrumb of %s", id).isEqualTo(breadcrumb != null);
      String renderSource = "<sl:skip-link/>\n" + source
          .replace(BREADCRUMB_PLACEHOLDER, breadcrumb == null ? "" : breadcrumb)
          .replace(CONTENT_PLACEHOLDER, content);
      String css = ((String) document.get("css")).strip();
      boolean htmx = Boolean.TRUE.equals(document.get("htmx"));
      scenarios.add(new Scenario(id, "layout", (String) document.get("title"), (String) document.get("description"),
          source, renderSource, null, false, (String) document.get("request"), Map.of(), Map.of(), null,
          (Map<String, String>) document.get("user"),
          new Layout(page.get("title"), css, layoutTemplate(source, breadcrumb != null, htmx),
              pageTemplate(page.get("title"), breadcrumb))));
    }
    assertThat(scenarios).as("layouts in %s", LAYOUTS_DIRECTORY).isNotEmpty();
    return scenarios;
  }

  /**
   * The whole layout file around a layout's body: the head every layout needs (theme script, Shadleaf's assets, the
   * application's CSS, and htmx if the layout uses it), the skip link first in the body, then the body, the root htmx
   * puts dialogs in and the toaster. As the Application shell guide's checklist says.
   */
  private static String layoutTemplate(String source, boolean breadcrumb, boolean htmx) {
    return """
        <!DOCTYPE html>
        <html lang="en" xmlns:th="http://www.thymeleaf.org" xmlns:sl="https://shadleaf.dev/sl"
              th:fragment="layout({parameters})">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1">
          <title th:text="${pageTitle}">Page</title>
          <th:block th:replace="~{sl/layout :: theme-script}"></th:block>
          <th:block th:replace="~{sl/layout :: assets}"></th:block>
        {htmxScript}  <link rel="stylesheet" th:href="@{/css/application.css}">
        </head>
        <body{bodyAttributes}>
        <sl:skip-link/>
        {source}
        <div id="modal-root"></div>
        <sl:toaster>
          <sl:toast th:each="toast : ${toasts}" th:title="${toast.title}" th:description="${toast.description}"
                    th:variant="${toast.variant}" th:duration="${toast.duration}"/>
        </sl:toaster>
        </body>
        </html>
        """.replace("{parameters}", breadcrumb ? "pageTitle, breadcrumb, content" : "pageTitle, content")
        .replace("{htmxScript}", htmx ? "  <script th:src=\"@{/webjars/htmx.org/dist/htmx.min.js}\" defer></script>\n" : "")
        .replace("{bodyAttributes}", htmx ? " hx-boost=\"true\"" : "")
        .replace("{source}", source);
  }

  /** A page that uses the layout (as {@code layout/main.html}): its title, its breadcrumb's levels and its content. */
  private static String pageTemplate(String title, @Nullable String breadcrumb) {
    if (breadcrumb == null) {
      return """
          <html xmlns:th="http://www.thymeleaf.org" xmlns:sl="https://shadleaf.dev/sl"
                th:replace="~{layout/main :: layout('{title}', ~{::#page-content})}">
          <body>
          <div id="page-content">
            ...
          </div>
          </body>
          </html>
          """.replace("{title}", title);
    }
    return """
        <html xmlns:th="http://www.thymeleaf.org" xmlns:sl="https://shadleaf.dev/sl"
              th:replace="~{layout/main :: layout('{title}', ~{::breadcrumb}, ~{::#page-content})}">
        <body>
        <th:block th:ref="breadcrumb">
        {breadcrumb}
        </th:block>
        <div id="page-content">
          ...
        </div>
        </body>
        </html>
        """.replace("{title}", title).replace("{breadcrumb}", breadcrumb.indent(2).stripTrailing());
  }

  /**
   * A scenario's {@code form}: {@code values} and {@code errors} (a message or a list of them), by field name, and
   * {@code globalErrors} (a message or a list of them) for the whole form. The
   * snippet then renders inside {@code th:object="${form}"}, so {@code th:field} works as in an application.
   */
  @SuppressWarnings("unchecked")
  private static @Nullable FormModel formModel(@Nullable Map<String, Object> form) {
    if (form == null) {
      return null;
    }
    Map<String, List<String>> errors = new LinkedHashMap<>();
    ((Map<String, Object>) form.getOrDefault("errors", Map.of())).forEach((field, messages) ->
        errors.put(field, messages instanceof List<?> list ? (List<String>) list : List.of((String) messages)));
    Object globalErrors = form.getOrDefault("globalErrors", List.of());
    return FormModel.of((Map<String, Object>) form.getOrDefault("values", Map.of()), errors,
        globalErrors instanceof List<?> list ? (List<String>) list : List.of((String) globalErrors));
  }

  /** A scenario's {@code user}: its picture is a photo's data: URI ({@code photo}) or a URL ({@code picture}). */
  private ShadleafUser user(Map<String, String> user) {
    String photo = user.get("photo");
    if (photo != null) {
      assertThat(photos).as("photo of user %s", user.get("name")).containsKey(photo);
    }
    return ShadleafUser.of(user.get("name"), user.get("username"), user.get("email"),
        photo != null ? photos.get(photo) : user.get("picture"));
  }

  /** A scenario's {@code pages}: a {@code Page} (or with {@code slice: true} a {@code Slice}) by variable name. */
  private static Map<String, Object> pages(Map<String, Map<String, Object>> pages) {
    Map<String, Object> variables = new LinkedHashMap<>();
    pages.forEach((name, page) -> {
      int number = (Integer) page.get("number");
      int size = (Integer) page.get("size");
      if (Boolean.TRUE.equals(page.get("slice"))) {
        variables.put(name, new SliceImpl<>(Collections.nCopies(size, name), PageRequest.of(number, size), true));
      } else {
        long total = ((Number) page.get("total")).longValue();
        int rows = (int) Math.max(0, Math.min(size, total - (long) number * size));
        variables.put(name, new PageImpl<>(Collections.nCopies(rows, name), PageRequest.of(number, size), total));
      }
    });
    return variables;
  }

  /**
   * A scenario's {@code data} with SnakeYAML's dates as {@code java.time}: it reads {@code 2024-04-01} as a
   * {@code java.util.Date} at midnight UTC, which Jackson would write as an instant.
   */
  private static Object javaTime(Object value) {
    if (value instanceof Date date) {
      Instant instant = date.toInstant();
      return instant.equals(instant.truncatedTo(ChronoUnit.DAYS)) ? LocalDate.ofInstant(instant, ZoneOffset.UTC)
          : instant;
    }
    if (value instanceof Map<?, ?> map) {
      Map<Object, Object> converted = new LinkedHashMap<>();
      map.forEach((key, item) -> converted.put(key, javaTime(item)));
      return converted;
    }
    if (value instanceof List<?> list) {
      return list.stream().map(PreviewGeneratorTest::javaTime).toList();
    }
    return value;
  }

  private void write(String name, Object value) throws IOException {
    Files.writeString(OUTPUT_DIRECTORY.resolve(name),
        jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value) + "\n", StandardCharsets.UTF_8);
  }

  /**
   * @param source       the snippet shown to the reader
   * @param renderSource what is rendered, when it has to differ from the snippet (e.g. model variables replaced by
   *                     literals); the {@code source} otherwise
   * @param form         the form object {@code th:field} binds to, if the scenario has one
   * @param showcase     whether the scenario is on the showcase page too; {@code false} for one that would get in the
   *                     way of the other components' tests there (a toast shown when the page loads)
   * @param request      the path and query of the request it renders in ({@code /people?sort=name,desc}), for links
   *                     built from the request such as a table's sort links; {@code /} when absent
   * @param pages        Spring Data pages by variable name, for a pagination
   * @param data         other model variables by name, such as a chart's rows
   * @param user         the signed-in user, as written in the YAML; anonymous when absent
   * @param layout       what a layout of the Layouts section has besides its source; {@code null} for a component's
   *                     scenario
   */
  private record Scenario(String id, String component, @Nullable String title, @Nullable String description,
                          String source, String renderSource, @Nullable FormModel form, boolean showcase,
                          @Nullable String request, Map<String, Object> pages, Map<String, Object> data,
                          @Nullable Integer width,
                          @Nullable Map<String, String> user, @Nullable Layout layout) {

  }

  /**
   * @param pageTitle the title of the page the preview shows
   * @param css       the application's CSS the layout needs
   * @param template  the whole layout file
   * @param page      a page that uses it
   */
  private record Layout(String pageTitle, String css, String template, String page) {

  }

  /**
   * @param scripts the script of each Alpine variant ({@code bundled}, {@code csp}, {@code external}), relative to
   *                the output directory
   */
  private record Previews(String version, List<Skin> skins, Map<String, String> scripts, List<Preview> scenarios) {

  }

  /**
   * @param css path relative to the output directory, e.g. {@code shadleaf/assets/shadleaf-lyra-abc.css}
   */
  private record Skin(String name, String css) {

  }

  /**
   * @param html           the rendered markup, as it goes into the preview frame
   * @param normalizedHtml one element per line, for the "rendered HTML" disclosure
   * @param showcase       whether the showcase page shows it
   * @param layout         whether it is a layout of the Layouts section: a whole page, previewed at a desktop and a
   *                       phone width
   * @param css            a layout's application CSS
   * @param template       a layout's whole file
   * @param page           a page that uses the layout
   */
  private record Preview(String id, String component, @Nullable String title, @Nullable String description,
                         String source, String html, String normalizedHtml, boolean showcase,
                         @Nullable Integer width, boolean layout, @Nullable String css, @Nullable String template,
                         @Nullable String page) {

  }

  private record ThemeScript(String cspHash, String script) {

  }
}
