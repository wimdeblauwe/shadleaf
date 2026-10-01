package io.github.wimdeblauwe.shadleaf.paging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class PagingParametersTest {

  @Test
  void springDataDefaultsWithoutProperties() {
    PagingParameters parameters = PagingParameters.from(new MockEnvironment());

    assertThat(parameters).isEqualTo(PagingParameters.defaults());
    assertThat(parameters.page(null)).isEqualTo("page");
    assertThat(parameters.size(null)).isEqualTo("size");
    assertThat(parameters.sort(null)).isEqualTo("sort");
  }

  @Test
  void readsSpringBootsProperties() {
    MockEnvironment environment = new MockEnvironment()
        .withProperty("spring.data.web.pageable.page-parameter", "p")
        .withProperty("spring.data.web.pageable.size-parameter", "s")
        .withProperty("spring.data.web.pageable.one-indexed-parameters", "true")
        .withProperty("spring.data.web.pageable.prefix", "x_")
        .withProperty("spring.data.web.pageable.qualifier-delimiter", "__")
        .withProperty("spring.data.web.sort.sort-parameter", "order");

    assertThat(PagingParameters.from(environment))
        .isEqualTo(new PagingParameters("p", "s", true, "x_", "__", "order"));
  }

  @Test
  void namesFollowSpringDatasResolvers() {
    PagingParameters parameters = new PagingParameters("page", "size", false, "x_", "__", "sort");

    // The prefix goes before the qualifier, on the page and size parameters only; the sort parameter's delimiter is
    // the sort resolver's own "_", which Spring Boot does not configure.
    assertThat(parameters.page("members")).isEqualTo("x_members__page");
    assertThat(parameters.size("members")).isEqualTo("x_members__size");
    assertThat(parameters.sort("members")).isEqualTo("members_sort");
    assertThat(parameters.page("")).isEqualTo("x_page");
  }
}
