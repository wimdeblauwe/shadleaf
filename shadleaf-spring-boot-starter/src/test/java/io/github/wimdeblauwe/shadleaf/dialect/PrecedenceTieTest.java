package io.github.wimdeblauwe.shadleaf.dialect;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wimdeblauwe.shadleaf.component.ClasspathComponentDefinitionSource;
import io.github.wimdeblauwe.shadleaf.component.ComponentRegistry;
import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.processor.IProcessor;
import org.thymeleaf.processor.element.IElementProcessor;
import org.thymeleaf.spring6.processor.SpringActionTagProcessor;
import org.thymeleaf.spring6.processor.SpringHrefTagProcessor;
import org.thymeleaf.spring6.processor.SpringSrcTagProcessor;
import org.thymeleaf.spring6.processor.SpringValueTagProcessor;
import org.thymeleaf.standard.StandardDialect;
import org.thymeleaf.standard.processor.StandardConditionalFixedValueTagProcessor;
import org.thymeleaf.standard.processor.StandardNonRemovableAttributeTagProcessor;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.util.ProcessorConfigurationUtils;

/**
 * The spike from the plan: {@code th:href}, {@code th:src} and {@code th:action} have precedence 1000, exactly like
 * the component processor. So do the plain attribute setters such as {@code th:size} and {@code th:disabled}, which a
 * prop may share its name with. ({@code th:value} ties only in the plain standard dialect; under Spring it is 1010.)
 * <p>
 * How Thymeleaf breaks the tie: dialect precedence first (both dialects are 1000), then processor precedence (both
 * 1000), then the <em>class name</em>. {@code io.github.wimdeblauwe...} sorts before {@code org.thymeleaf...}, so the
 * component processor runs first today. That is an accident of naming, so this test proves both orders render the
 * same HTML: either the standard processor sets the attribute and the component reads (or forwards) the literal, or
 * the component evaluates {@code th:<prop>} itself and forwards the other {@code th:*} attributes to the rendered
 * element, where they are evaluated.
 */
class PrecedenceTieTest {

  private static final String SNIPPET = """
      <sl:test-link th:href="${url}" th:src="${src}" th:value="${value}" th:action="${action}"
                    th:variant="${variant}" th:size="${size}" th:disabled="${disabled}">Go</sl:test-link>""";

  private static final Map<String, Object> VARIABLES = Map.of(
      "url", "/orders/42", "src", "/img/a.png", "value", "v1", "action", "/submit",
      "variant", "muted", "size", "sm", "disabled", true);

  @Test
  void componentProcessorCurrentlyWinsTheTieByClassName() {
    ComponentRenderTester renderer = ComponentRenderTester.create();
    List<IElementProcessor> processors = renderer.engine().getConfiguration()
        .getElementProcessors(TemplateMode.HTML).stream()
        .map(ProcessorConfigurationUtils::unwrap)
        .toList();

    int component = indexOf(processors, ComponentElementProcessor.class);
    for (Class<?> tied : List.of(SpringHrefTagProcessor.class, SpringSrcTagProcessor.class,
        SpringActionTagProcessor.class, StandardNonRemovableAttributeTagProcessor.class,
        StandardConditionalFixedValueTagProcessor.class)) {
      IElementProcessor processor = processors.get(indexOf(processors, tied));
      assertThat(processor.getPrecedence()).as(tied.getSimpleName()).isEqualTo(StandardDialect.PROCESSOR_PRECEDENCE);
      assertThat(component).as("component processor runs before " + tied.getSimpleName())
          .isLessThan(indexOf(processors, tied));
    }
  }

  @Test
  void springThValueDoesNotTie() {
    // StandardValueTagProcessor is at 1000, but the Spring dialect replaces it with one at 1010.
    List<IElementProcessor> processors = ComponentRenderTester.create().engine().getConfiguration()
        .getElementProcessors(TemplateMode.HTML).stream()
        .map(ProcessorConfigurationUtils::unwrap)
        .toList();

    assertThat(processors.get(indexOf(processors, SpringValueTagProcessor.class)).getPrecedence())
        .isEqualTo(SpringValueTagProcessor.ATTR_PRECEDENCE)
        .isEqualTo(1010);
  }

  @ParameterizedTest(name = "component processor precedence {0}")
  @ValueSource(ints = {1000, 1001})
  void rendersTheSameWhicheverProcessorRunsFirst(int componentPrecedence) {
    ComponentRenderTester renderer = ComponentRenderTester.builder().dialect(new TestDialect(componentPrecedence)).build();

    Element link = render(renderer, SNIPPET, VARIABLES);

    assertThat(link.tagName()).isEqualTo("a");
    assertThat(link.attr("href")).isEqualTo("/orders/42");
    assertThat(link.attr("src")).isEqualTo("/img/a.png");
    assertThat(link.attr("value")).isEqualTo("v1");
    assertThat(link.attr("action")).isEqualTo("/submit");
    assertThat(link.attr("data-variant")).isEqualTo("muted");
    assertThat(link.attr("data-size")).isEqualTo("sm");
    assertThat(link.attr("aria-disabled")).isEqualTo("true");
    assertThat(link.attributes().asList()).extracting(a -> a.getKey())
        .doesNotContain("variant", "size", "disabled")
        .noneMatch(key -> key.startsWith("th:"));
    assertThat(link.text()).isEqualTo("Go");
  }

  @ParameterizedTest(name = "component processor precedence {0}")
  @ValueSource(ints = {1000, 1001})
  void falseBooleanSurvivesEitherOrder(int componentPrecedence) {
    // When th:disabled runs first, false removes the attribute, so the prop falls back to its default.
    ComponentRenderTester renderer = ComponentRenderTester.builder().dialect(new TestDialect(componentPrecedence)).build();

    Element link = render(renderer, "<sl:test-link th:disabled=\"${false}\">Go</sl:test-link>", Map.of());

    assertThat(link.hasAttr("aria-disabled")).isFalse();
    assertThat(link.hasAttr("disabled")).isFalse();
  }

  private static Element render(ComponentRenderTester renderer, String snippet, Map<String, ?> variables) {
    return renderer.render(snippet, variables).root();
  }

  private static int indexOf(List<IElementProcessor> processors, Class<?> type) {
    for (int i = 0; i < processors.size(); i++) {
      if (type.isInstance(processors.get(i))) {
        return i;
      }
    }
    throw new AssertionError("No " + type.getSimpleName() + " registered");
  }

  /** The Shadleaf dialect with the component processor moved to another precedence. */
  private static final class TestDialect extends AbstractProcessorDialect {

    private final ComponentRegistry registry = new ComponentRegistry(List.of(
        new ClasspathComponentDefinitionSource(PrecedenceTieTest.class.getClassLoader())));
    private final int componentPrecedence;

    TestDialect(int componentPrecedence) {
      super("Shadleaf (test)", ShadleafDialect.PREFIX, StandardDialect.PROCESSOR_PRECEDENCE);
      this.componentPrecedence = componentPrecedence;
    }

    @Override
    public Set<IProcessor> getProcessors(String dialectPrefix) {
      return Set.of(
          new ComponentElementProcessor(dialectPrefix, registry, componentPrecedence),
          new AttrsAttributeProcessor(dialectPrefix),
          new SlotElementProcessor(dialectPrefix));
    }
  }
}
