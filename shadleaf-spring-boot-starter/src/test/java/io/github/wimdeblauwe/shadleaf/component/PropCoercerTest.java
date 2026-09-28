package io.github.wimdeblauwe.shadleaf.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PropCoercerTest {

  private static final PropDefinition VARIANT =
      new PropDefinition("variant", PropType.ENUM, "primary", List.of("primary", "outline", "ghost"), "");
  private static final PropDefinition DISABLED =
      new PropDefinition("disabled", PropType.BOOLEAN, false, List.of(), "");
  private static final PropDefinition STROKE_WIDTH =
      new PropDefinition("stroke-width", PropType.NUMBER, new BigDecimal("2"), List.of(), "");
  private static final PropDefinition LABEL =
      new PropDefinition("label", PropType.STRING, null, List.of(), "");

  @Test
  void absentValueFallsBackToDefault() {
    assertThat(PropCoercer.bind("button", VARIANT, null)).isEqualTo("primary");
    assertThat(PropCoercer.bind("button", DISABLED, null)).isEqualTo(false);
    assertThat(PropCoercer.bind("icon", STROKE_WIDTH, null)).isEqualTo(new BigDecimal("2"));
    assertThat(PropCoercer.bind("icon", LABEL, null)).isNull();
  }

  @Test
  void enumAcceptsLegalValue() {
    assertThat(PropCoercer.bind("button", VARIANT, "outline")).isEqualTo("outline");
  }

  @Test
  void enumRejectsIllegalValueListingTheLegalOnes() {
    assertThatThrownBy(() -> PropCoercer.bind("button", VARIANT, "outlin"))
        .isInstanceOf(ShadleafComponentException.class)
        .hasMessage("Invalid value 'outlin' for prop 'variant' of <sl:button>. Must be one of: primary, outline, ghost.");
  }

  @Test
  void booleanFollowsHtmlConventions() {
    assertThat(PropCoercer.bind("button", DISABLED, "")).isEqualTo(true);
    assertThat(PropCoercer.bind("button", DISABLED, "disabled")).isEqualTo(true);
    assertThat(PropCoercer.bind("button", DISABLED, "true")).isEqualTo(true);
    assertThat(PropCoercer.bind("button", DISABLED, "false")).isEqualTo(false);
    assertThat(PropCoercer.bind("button", DISABLED, Boolean.TRUE)).isEqualTo(true);
    assertThat(PropCoercer.bind("button", DISABLED, Boolean.FALSE)).isEqualTo(false);
  }

  @Test
  void booleanRejectsOtherValues() {
    assertThatThrownBy(() -> PropCoercer.bind("button", DISABLED, "yes"))
        .hasMessage("Invalid value 'yes' for boolean prop 'disabled' of <sl:button>. "
            + "Use disabled, disabled=\"true\" or disabled=\"false\".");
  }

  @Test
  void numberAcceptsStringsAndNumbers() {
    assertThat(PropCoercer.bind("icon", STROKE_WIDTH, "1.5")).isEqualTo(new BigDecimal("1.5"));
    assertThat(PropCoercer.bind("icon", STROKE_WIDTH, " 3 ")).isEqualTo(new BigDecimal("3"));
    assertThat(PropCoercer.bind("icon", STROKE_WIDTH, 3)).isEqualTo(new BigDecimal("3"));
    assertThat(PropCoercer.bind("icon", STROKE_WIDTH, 1.25d)).isEqualTo(new BigDecimal("1.25"));
  }

  @Test
  void numberRejectsText() {
    assertThatThrownBy(() -> PropCoercer.bind("icon", STROKE_WIDTH, "thick"))
        .hasMessage("Invalid value 'thick' for number prop 'stroke-width' of <sl:icon>.");
  }

  @Test
  void stringAcceptsAnything() {
    assertThat(PropCoercer.bind("icon", LABEL, "Delete")).isEqualTo("Delete");
    assertThat(PropCoercer.bind("icon", LABEL, 42)).isEqualTo("42");
  }
}
