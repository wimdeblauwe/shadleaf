package io.github.wimdeblauwe.shadleaf.assets;

import java.util.Locale;

/**
 * The visual identity of the components, set with {@code shadleaf.skin}. Same markup and tokens, different geometry,
 * spacing, type size and shadow.
 */
public enum Skin {

  /** The classic look, the default. */
  VEGA,

  /** Boxy and sharp. */
  LYRA,

  /** Reduced padding and margins. */
  NOVA,

  /** Rounded, with generous spacing. */
  MAIA,

  /** Compact. */
  MIRA,

  /** Fluid and soft. */
  LUMA,

  /** A compact {@link #LUMA}. */
  RHEA,

  /** Editorial and typographic. */
  SERA,

  /** Looks hand-drawn, for mockups. */
  SKETCH;

  /** The name used in the asset file names and in the Vite manifest, e.g. {@code vega}. */
  public String id() {
    return name().toLowerCase(Locale.ROOT);
  }
}
