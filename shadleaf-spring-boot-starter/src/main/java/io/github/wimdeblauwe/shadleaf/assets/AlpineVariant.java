package io.github.wimdeblauwe.shadleaf.assets;

/**
 * Where the page's Alpine.js comes from, set with {@code shadleaf.assets.alpine}. A page must have exactly one Alpine,
 * so an application that loads its own uses {@link #EXTERNAL}.
 */
public enum AlpineVariant {

  /** Alpine's standard build, started by Shadleaf. The application can use Alpine in its own markup freely. */
  BUNDLED("shadleaf.alpine"),

  /**
   * Alpine's CSP build, started by Shadleaf, for a Content-Security-Policy without {@code 'unsafe-eval'}. The
   * application's own Alpine expressions are then limited to what the CSP build accepts too.
   */
  CSP("shadleaf.alpine-csp"),

  /**
   * Only the registrations of Shadleaf's components, for an application that loads Alpine (and the plugins the
   * components need) itself. The Shadleaf script must run before that Alpine starts.
   */
  EXTERNAL("shadleaf");

  private final String entryName;

  AlpineVariant(String entryName) {
    this.entryName = entryName;
  }

  /** The JS entry point, relative to the Vite root: also the key of its Vite manifest entry. */
  public String jsEntry() {
    return "js/entries/" + entryName + ".js";
  }
}
