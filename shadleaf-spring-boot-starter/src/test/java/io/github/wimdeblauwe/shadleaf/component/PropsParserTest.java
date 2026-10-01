package io.github.wimdeblauwe.shadleaf.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PropsParserTest {

  @Test
  void parsesDeclaredProps() {
    ComponentDefinition definition = parse("""
        <sl:props>
          <sl:prop name="variant" default="primary" values="primary secondary  outline">
            Visual style
            of the button.
          </sl:prop>
          <sl:prop name="disabled" type="boolean">Disable the button.</sl:prop>
          <sl:prop name="stroke-width" type="number" default="1.5"/>
          <sl:prop name="label">Accessible <code>label</code>.</sl:prop>
        </sl:props>
        """);

    assertThat(definition.declared()).isTrue();
    assertThat(definition.props()).containsOnlyKeys("variant", "disabled", "stroke-width", "label");
    assertThat(definition.props().keySet()).containsExactly("variant", "disabled", "stroke-width", "label");

    assertThat(definition.prop("variant")).isEqualTo(new PropDefinition("variant", PropType.ENUM, "primary",
        List.of("primary", "secondary", "outline"), "Visual style of the button."));
    assertThat(definition.prop("disabled").type()).isEqualTo(PropType.BOOLEAN);
    assertThat(definition.prop("disabled").defaultValue()).isEqualTo(false);
    assertThat(definition.prop("stroke-width").type()).isEqualTo(PropType.NUMBER);
    assertThat(definition.prop("stroke-width").defaultValue()).isEqualTo(new BigDecimal("1.5"));
    assertThat(definition.prop("label").type()).isEqualTo(PropType.STRING);
    assertThat(definition.prop("label").defaultValue()).isNull();
    assertThat(definition.prop("label").description()).isEqualTo("Accessible `label`.");
    assertThat(definition.accessibleNameRule()).isNull();
  }

  @Test
  void parsesComponentDescription() {
    ComponentDefinition definition = parse("""
        <sl:props>
          <sl:description>
            A <code>button</code>, or
            an <em>anchor</em>.
          </sl:description>
          <sl:prop name="variant" values="a b">Style.</sl:prop>
        </sl:props>
        """);

    assertThat(definition.description()).isEqualTo("A `button`, or an anchor.");
    assertThat(definition.prop("variant").description()).isEqualTo("Style.");
  }

  @Test
  void descriptionIsEmptyWhenAbsent() {
    assertThat(parse("<sl:props></sl:props>").description()).isEmpty();
  }

  @Test
  void rejectsSecondDescription() {
    assertThatThrownBy(() -> parse("""
        <sl:props><sl:description>a</sl:description><sl:description>b</sl:description></sl:props>
        """))
        .isInstanceOf(ShadleafComponentException.class)
        .hasMessageContaining("more than one <sl:description>");
  }

  @Test
  void templateWithoutPropsBlockIsUndeclared() {
    ComponentDefinition definition = PropsParser.parse("raw", """
        <html><body><div th:fragment="raw">x</div></body></html>
        """, "raw.html");

    assertThat(definition.declared()).isFalse();
    assertThat(definition.props()).isEmpty();
  }

  @Test
  void emptyPropsBlockIsDeclared() {
    assertThat(parse("<sl:props></sl:props>").declared()).isTrue();
  }

  @Test
  void parsesAccessibleNameRule() {
    ComponentDefinition definition = parse("""
        <sl:props>
          <sl:prop name="size" default="default" values="default icon icon-sm"/>
          <sl:accessible-name required-when="size=icon icon-sm"/>
        </sl:props>
        """);

    AccessibleNameRule rule = definition.accessibleNameRule();
    assertThat(rule.prop()).isEqualTo("size");
    assertThat(rule.values()).containsExactlyInAnyOrder("icon", "icon-sm");
    assertThat(rule.appliesTo("icon")).isTrue();
    assertThat(rule.appliesTo("default")).isFalse();
  }

  @Test
  void accessibleNameWithoutConditionIsAlwaysRequired() {
    ComponentDefinition definition = parse("<sl:props><sl:accessible-name/></sl:props>");

    assertThat(definition.accessibleNameRule()).isEqualTo(AccessibleNameRule.always());
    assertThat(definition.accessibleNameRule().appliesTo(null)).isTrue();
  }

  @Test
  void rejectsPropNamesOfStandardProcessors() {
    for (String reserved : new String[]{"text", "if", "each", "with", "attr", "href", "value", "src", "action",
        "remove", "fragment", "field", "class"}) {
      assertThatThrownBy(() -> parse("<sl:props><sl:prop name=\"" + reserved + "\"/></sl:props>"))
          .as(reserved)
          .isInstanceOf(ShadleafComponentException.class)
          .hasMessageContaining("prop name '" + reserved + "' is reserved");
    }
  }

  @Test
  void reservedNamesComeFromTheStandardDialect() {
    assertThat(ReservedPropNames.NAMES)
        .contains("text", "utext", "if", "each", "with", "attr", "href", "value", "src", "action", "remove",
            "fragment", "classappend", "insert", "replace", "object", "field", "errors", "class")
        // plain attribute setters: th:size sets size either way, so a prop may use the name
        .doesNotContain("size", "type", "disabled", "name", "title", "onclick")
        .doesNotContain("variant", "hx-post");
  }

  @Test
  void rejectsIllegalDefault() {
    assertThatThrownBy(() -> parse("""
        <sl:props><sl:prop name="variant" default="primry" values="primary outline"/></sl:props>
        """))
        .hasMessage("Invalid <sl:props> in test-component.html: invalid default for prop 'variant': "
            + "Invalid value 'primry' for prop 'variant' of <sl:test-component>. Must be one of: primary, outline.");
  }

  @Test
  void rejectsUnknownPropAttribute() {
    assertThatThrownBy(() -> parse("<sl:props><sl:prop name=\"variant\" valeus=\"a b\"/></sl:props>"))
        .hasMessageContaining("unknown attribute 'valeus' on prop 'variant'; allowed are default, name, required, type, values");
  }

  @Test
  void parsesRequired() {
    ComponentDefinition definition = parse("""
        <sl:props>
          <sl:prop name="id" required/>
          <sl:prop name="target" required="true"/>
          <sl:prop name="label" required="false"/>
          <sl:prop name="title"/>
        </sl:props>
        """);

    assertThat(definition.prop("id").required()).isTrue();
    assertThat(definition.prop("target").required()).isTrue();
    assertThat(definition.prop("label").required()).isFalse();
    assertThat(definition.prop("title").required()).isFalse();
  }

  @Test
  void rejectsRequiredWithDefault() {
    assertThatThrownBy(() -> parse("<sl:props><sl:prop name=\"id\" required default=\"x\"/></sl:props>"))
        .hasMessageContaining("prop 'id' is required and cannot have a default");
  }

  @Test
  void rejectsRequiredBoolean() {
    assertThatThrownBy(() -> parse("<sl:props><sl:prop name=\"open\" type=\"boolean\" required/></sl:props>"))
        .hasMessageContaining("prop 'open' is a boolean and cannot be required");
  }

  @Test
  void rejectsUnknownRequiredValue() {
    assertThatThrownBy(() -> parse("<sl:props><sl:prop name=\"id\" required=\"yes\"/></sl:props>"))
        .hasMessageContaining("prop 'id' has required=\"yes\"");
  }

  @Test
  void rejectsUnknownType() {
    assertThatThrownBy(() -> parse("<sl:props><sl:prop name=\"count\" type=\"int\"/></sl:props>"))
        .hasMessageContaining("prop 'count' has unknown type 'int'");
  }

  @Test
  void parsesObjectProp() {
    ComponentDefinition definition = parse("<sl:props><sl:prop name=\"page\" type=\"object\"/></sl:props>");

    assertThat(definition.prop("page").type()).isEqualTo(PropType.OBJECT);
    assertThat(definition.prop("page").defaultValue()).isNull();
  }

  @Test
  void rejectsDefaultOnObject() {
    assertThatThrownBy(() -> parse("<sl:props><sl:prop name=\"page\" type=\"object\" default=\"x\"/></sl:props>"))
        .hasMessageContaining("prop 'page' is an object and cannot have a default");
  }

  @Test
  void rejectsValuesOnObject() {
    assertThatThrownBy(() -> parse("<sl:props><sl:prop name=\"page\" type=\"object\" values=\"a\"/></sl:props>"))
        .hasMessageContaining("prop 'page' is of type object and cannot declare values");
  }

  @Test
  void rejectsValuesOnBoolean() {
    assertThatThrownBy(() -> parse("<sl:props><sl:prop name=\"open\" type=\"boolean\" values=\"a\"/></sl:props>"))
        .hasMessageContaining("prop 'open' is of type boolean and cannot declare values");
  }

  @Test
  void rejectsDuplicateProp() {
    assertThatThrownBy(() -> parse("<sl:props><sl:prop name=\"size\"/><sl:prop name=\"size\"/></sl:props>"))
        .hasMessageContaining("prop 'size' is declared twice");
  }

  @Test
  void rejectsPropWithoutName() {
    assertThatThrownBy(() -> parse("<sl:props><sl:prop type=\"boolean\"/></sl:props>"))
        .hasMessageContaining("<sl:prop> without a name");
  }

  @Test
  void rejectsNonKebabCaseName() {
    assertThatThrownBy(() -> parse("<sl:props><sl:prop name=\"strokeWidth\"/></sl:props>"))
        .hasMessageContaining("must be lower-case kebab-case");
  }

  @Test
  void rejectsUnexpectedElement() {
    assertThatThrownBy(() -> parse("<sl:props><sl:attr name=\"size\"/></sl:props>"))
        .hasMessageContaining("unexpected <sl:attr>");
  }

  @Test
  void rejectsAccessibleNameReferringToUnknownProp() {
    assertThatThrownBy(() -> parse("<sl:props><sl:accessible-name required-when=\"size=icon\"/></sl:props>"))
        .hasMessageContaining("refers to undeclared prop 'size'");
  }

  @Test
  void rejectsAccessibleNameReferringToIllegalValue() {
    assertThatThrownBy(() -> parse("""
        <sl:props>
          <sl:prop name="size" values="default icon"/>
          <sl:accessible-name required-when="size=icon icn"/>
        </sl:props>
        """))
        .hasMessageContaining("refers to value 'icn', which is not a legal value of prop 'size' (default, icon)");
  }

  private static ComponentDefinition parse(String propsBlock) {
    String template = """
        <!doctype html>
        <html xmlns:th="http://www.thymeleaf.org" xmlns:sl="https://shadleaf.dev/sl">
        <head>
        %s
        </head>
        <body>
          <span th:fragment="test-component">x</span>
        </body>
        </html>
        """.formatted(propsBlock);
    return PropsParser.parse("test-component", template, "test-component.html");
  }
}
