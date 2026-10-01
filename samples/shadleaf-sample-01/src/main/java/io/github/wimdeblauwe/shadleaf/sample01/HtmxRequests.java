package io.github.wimdeblauwe.shadleaf.sample01;

import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxRequest;

/**
 * Whether an htmx request asks for part of a page. The layout boosts the body ({@code hx-boost="true"}), so every link
 * and form htmx does not handle otherwise is sent by htmx too, with {@code HX-Request} and {@code HX-Boosted}, and swaps
 * the whole body: such a request has no {@code HX-Target} (the body has no id) and wants the whole page, as a history
 * restore does. A boosted link inside an element with a target of its own ({@code #people-results}) and every
 * {@code hx-get}/{@code hx-post} want their fragment.
 */
final class HtmxRequests {

  private HtmxRequests() {
  }

  static boolean wantsFragment(HtmxRequest request) {
    return request.isHtmxRequest()
        && !request.isHistoryRestoreRequest()
        && !(request.isBoosted() && request.getTarget() == null);
  }
}
