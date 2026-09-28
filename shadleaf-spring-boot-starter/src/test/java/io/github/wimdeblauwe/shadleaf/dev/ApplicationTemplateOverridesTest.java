package io.github.wimdeblauwe.shadleaf.dev;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * The library's templates are in target/classes; the test-* ones in target/test-classes, which plays the application.
 */
class ApplicationTemplateOverridesTest {

  private final ApplicationTemplateOverrides overrides =
      new ApplicationTemplateOverrides(ApplicationTemplateOverridesTest.class.getClassLoader());

  @Test
  void libraryOnlyTemplateIsNotOverridden() {
    assertThat(overrides.isOverridden("sl/components/button")).isFalse();
    assertThat(overrides.isOverridden("sl/layout")).isFalse();
  }

  @Test
  void templateFromAnotherClasspathRootIsAnOverride() {
    assertThat(overrides.isOverridden("sl/components/test-chip")).isTrue();
    assertThat(overrides.isOverridden("sl/components/test-chip.html")).isTrue();
  }

  @Test
  void missingTemplateIsNotOverridden() {
    assertThat(overrides.isOverridden("sl/components/nope")).isFalse();
  }
}
