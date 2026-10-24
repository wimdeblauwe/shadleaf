package io.github.wimdeblauwe.shadleaf.form;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@code <sl:combobox>} and its parts, rendered into {@code src/test/resources/approved/combobox.approved.html}. The
 * behaviour with {@code th:field} is in {@link ComboboxBindingTest}.
 */
class ComboboxApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void comboboxAndParts() {
    HtmlApproval.verifyRenders("combobox", tester, List.of(
        """
            <sl:combobox name="framework" aria-label="Framework">
              <sl:combobox-item value="next">Next.js</sl:combobox-item>
            </sl:combobox>""",
        """
            <sl:combobox id="framework" name="framework" placeholder="Select a framework" class="w-full" required
                         show-clear auto-highlight empty="No frameworks found.">
              <sl:combobox-group label="React">
                <sl:combobox-item value="next">Next.js</sl:combobox-item>
                <sl:combobox-item value="remix" disabled>Remix</sl:combobox-item>
              </sl:combobox-group>
              <sl:combobox-separator/>
              <sl:combobox-group label="Vue">
                <sl:combobox-item value="nuxt" selected>Nuxt.js</sl:combobox-item>
              </sl:combobox-group>
            </sl:combobox>""",
        """
            <sl:combobox name="frameworks" aria-label="Frameworks" multiple placeholder="Add a framework"
                         show-trigger="false" disabled>
              <sl:combobox-item value="next" selected>Next.js</sl:combobox-item>
              <sl:combobox-item value="astro"><sl:icon name="rocket"/>Astro</sl:combobox-item>
            </sl:combobox>""",
        """
            <sl:combobox name="country" aria-label="Country" search-url="/countries" show-trigger="false"
                         hx-post="/country" hx-trigger="change">
              <sl:combobox-item value="be" selected>Belgium</sl:combobox-item>
            </sl:combobox>""",
        "<sl:combobox-item th:value=\"${'astro'}\">Astro</sl:combobox-item>",
        "<sl:combobox-group th:label=\"${'Vue'}\" class=\"x\"><sl:combobox-item value=\"a\">A</sl:combobox-item></sl:combobox-group>",
        "<sl:combobox-separator/>"));
  }
}
