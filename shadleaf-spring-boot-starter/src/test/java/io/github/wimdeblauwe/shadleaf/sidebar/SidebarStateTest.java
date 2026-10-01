package io.github.wimdeblauwe.shadleaf.sidebar;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.thymeleaf.context.ExpressionContext;
import org.thymeleaf.spring6.SpringTemplateEngine;

/** Outside a web request (an e-mail template) there is no cookie: the state is the default. */
class SidebarStateTest {

  private final SidebarState state =
      new SidebarState(new ExpressionContext(new SpringTemplateEngine().getConfiguration()));

  @Test
  void withoutARequestTheDefaultApplies() {
    assertThat(state.state(null)).isEqualTo("expanded");
    assertThat(state.state("collapsed")).isEqualTo("collapsed");
    assertThat(state.state("sideways")).isEqualTo("expanded");
  }
}
