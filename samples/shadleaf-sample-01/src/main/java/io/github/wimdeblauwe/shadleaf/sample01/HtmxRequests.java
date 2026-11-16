package io.github.wimdeblauwe.shadleaf.sample01;

import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxRequest;

/**
 * Whether an htmx request asks for part of a page. The layout boosts the body ({@code hx-boost:inherited="true"}), so
 * every link and form htmx does not handle otherwise is sent by htmx too, with {@code HX-Request} and
 * {@code HX-Boosted}, and swaps the whole body: htmx 4 sends {@code HX-Target: body} for it (htmx 2 sent no
 * {@code HX-Target}, the body having no id), and it wants the whole page, as a history restore does. A boosted link
 * inside an element with a target of its own ({@code div#people-results}) and every {@code hx-get}/{@code hx-post}
 * want their fragment. {@code HX-Request-Type} cannot tell them apart: htmx 4 sends {@code full} for a request with an
 * {@code hx-select} too.
 */
final class HtmxRequests {

  private HtmxRequests() {
  }

  static boolean wantsFragment(HtmxRequest request) {
    String target = request.getTarget();
    return request.isHtmxRequest()
        && !request.isHistoryRestoreRequest()
        && !(request.isBoosted() && (target == null || target.equals("body") || target.startsWith("body#")));
  }
}
