package io.github.wimdeblauwe.shadleaf.paging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RequestQueryTest {

  @Test
  void keepsOtherParametersAsTheBrowserSentThem() {
    RequestQuery query = RequestQuery.of("/people", "q=ada+l%C3%A9&tag=a&tag=b&sort=name&flag");

    assertThat(query.without("sort").with("sort", "name,desc").href())
        .isEqualTo("/people?q=ada+l%C3%A9&tag=a&tag=b&flag&sort=name,desc");
  }

  @Test
  void decodesNamesAndValues() {
    RequestQuery query = RequestQuery.of("", "q=ada+l%C3%A9&so%72t=a,desc&sort=b&flag&bad=%zz");

    assertThat(query.values("q")).containsExactly("ada lé");
    assertThat(query.values("sort")).containsExactly("a,desc", "b");
    assertThat(query.values("flag")).containsExactly("");
    assertThat(query.values("bad")).containsExactly("%zz");
    assertThat(query.without("sort", "q").href()).isEqualTo("?flag&bad=%zz");
  }

  @Test
  void encodesWhatItAdds() {
    assertThat(RequestQuery.of("/p", null).with("sort", "full name,asc").href()).isEqualTo("/p?sort=full%20name,asc");
    assertThat(RequestQuery.of("/p", "").with("q", "a&b=c").href()).isEqualTo("/p?q=a%26b%3Dc");
  }

  @Test
  void noQueryLeavesThePath() {
    assertThat(RequestQuery.of("/people", "sort=name").without("sort").href()).isEqualTo("/people");
  }
}
