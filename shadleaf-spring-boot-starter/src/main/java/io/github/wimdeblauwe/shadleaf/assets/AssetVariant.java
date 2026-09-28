package io.github.wimdeblauwe.shadleaf.assets;

/**
 * Which build of the stylesheet an application loads, set with {@code shadleaf.assets.variant}.
 */
public enum AssetVariant {

  /**
   * Includes Tailwind's preflight reset and the page base styles. For applications that do not compile Tailwind
   * themselves.
   */
  STANDALONE(""),

  /**
   * Without preflight, page base styles or Tailwind theme variables. For applications that compile Tailwind
   * themselves, so the page has exactly one reset and the application's theme stays its own.
   */
  EMBEDDED(".embedded");

  private final String fileSuffix;

  AssetVariant(String fileSuffix) {
    this.fileSuffix = fileSuffix;
  }

  /**
   * The CSS entry point for the given skin, relative to the Vite root: also the key of its Vite manifest entry.
   */
  public String cssEntry(String skin) {
    return "css/entries/shadleaf-" + skin + fileSuffix + ".css";
  }
}
