package io.github.wimdeblauwe.shadleaf.test;

import io.github.wimdeblauwe.shadleaf.component.ComponentRegistry;
import io.github.wimdeblauwe.shadleaf.component.PropDefinition;
import io.github.wimdeblauwe.shadleaf.component.PropType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.select.Elements;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/**
 * Renders one component on its own, with whatever it needs to render at all: {@code aria-label} satisfies any
 * accessible-name rule, {@code name} satisfies {@code <sl:icon>}, every other required prop gets {@code x} (a number
 * {@code 1}), an object prop gets {@link #SAMPLE_PAGE} ({@code th:page}, and a chart's {@code th:data} takes its rows),
 * a {@code currency} prop gets {@code EUR}, which the chart parts' currency formats need. It renders inside a form
 * object, for the components that read one ({@code sl:form-errors}).
 */
public final class IsolatedRender {

  /** The page an object prop gets: page 2 of 3, so every pagination link is there. */
  public static final Page<String> SAMPLE_PAGE = new PageImpl<>(List.of("x"), PageRequest.of(1, 1), 3);

  private static final FormModel FORM = FormModel.of(Map.of(), Map.of());

  private final ComponentRegistry registry;
  private final ComponentRenderTester renderer;

  public IsolatedRender(ComponentRegistry registry, ComponentRenderTester renderer) {
    this.registry = registry;
    this.renderer = renderer;
  }

  /**
   * @param attributes the props to set, by name, as literal values; the required ones not among them are filled in
   * @param content    what goes between the tags: {@code x}, or named slots
   * @return the top-level elements the component rendered; empty for one that renders no element of its own
   */
  public Elements render(String component, Map<String, String> attributes, String content) {
    StringBuilder attributeText = new StringBuilder();
    attributes.forEach((name, value) -> attributeText.append("%s=\"%s\" ".formatted(name, value)));
    Map<String, PropDefinition> props = registry.get(component).props();
    props.values().stream()
        .filter(PropDefinition::required)
        .filter(required -> !attributes.containsKey(required.name()) && !required.name().equals("name"))
        .forEach(required -> attributeText.append("%s=\"%s\" ".formatted(required.name(),
            required.type() == PropType.NUMBER ? "1" : "x")));
    if (props.containsKey("currency") && !attributes.containsKey("currency")) {
      attributeText.append("currency=\"EUR\" ");
    }
    props.values().stream()
        .filter(object -> object.type() == PropType.OBJECT && !attributes.containsKey(object.name()))
        .forEach(object -> attributeText.append("th:%s=\"${samplePage}\" ".formatted(object.name())));
    Map<String, Object> variables = new LinkedHashMap<>(FORM.variables());
    variables.put("samplePage", SAMPLE_PAGE);
    return renderer.render(FormModel.wrap("<sl:%s %saria-label=\"x\" name=\"x\">%s</sl:%s>"
        .formatted(component, attributeText, content, component)), variables).document().body().children();
  }
}
