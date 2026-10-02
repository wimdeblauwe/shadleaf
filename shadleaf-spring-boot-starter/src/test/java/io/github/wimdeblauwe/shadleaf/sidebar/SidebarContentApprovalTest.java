package io.github.wimdeblauwe.shadleaf.sidebar;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The sidebar's content parts, alone, in every enum value and state, and composed as a sidebar, rendered into
 * {@code src/test/resources/approved/sidebar-content.approved.html}. Groups and items with badges get an id, so
 * nothing is generated.
 */
class SidebarContentApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.builder().requestUri("/inbox").build();

  @Test
  void contentParts() {
    HtmlApproval.verifyRenders("sidebar-content", tester, List.of(
        "<sl:sidebar-header>Acme</sl:sidebar-header>",
        "<sl:sidebar-content>Groups</sl:sidebar-content>",
        "<sl:sidebar-footer>Help</sl:sidebar-footer>",
        "<sl:sidebar-separator/>",
        "<sl:sidebar-group>Content</sl:sidebar-group>",
        "<sl:sidebar-group id=\"platform\"><sl:sidebar-group-label>Platform</sl:sidebar-group-label></sl:sidebar-group>",
        "<sl:sidebar-group-label>Platform</sl:sidebar-group-label>",
        "<sl:sidebar-group-action aria-label=\"Add project\"><sl:icon name=\"plus\"/></sl:sidebar-group-action>",
        "<sl:sidebar-group-action as=\"button\" type=\"submit\" aria-label=\"Add project\">+</sl:sidebar-group-action>",
        "<sl:sidebar-group-action as=\"a\" href=\"/projects/new\" aria-label=\"Add project\">+</sl:sidebar-group-action>",
        "<sl:sidebar-group-content>Menu</sl:sidebar-group-content>",
        "<sl:sidebar-menu>Items</sl:sidebar-menu>",
        "<sl:sidebar-menu-item>Button</sl:sidebar-menu-item>",
        "<sl:sidebar-menu-item id=\"inbox\">Button</sl:sidebar-menu-item>",
        "<sl:sidebar-menu-button href=\"/\">Home</sl:sidebar-menu-button>",
        "<sl:sidebar-menu-button as=\"a\" href=\"/\">Home</sl:sidebar-menu-button>",
        "<sl:sidebar-menu-button as=\"button\">Compose</sl:sidebar-menu-button>",
        "<sl:sidebar-menu-button as=\"button\" type=\"submit\">Compose</sl:sidebar-menu-button>",
        "<sl:sidebar-menu-button as=\"button\" type=\"reset\">Compose</sl:sidebar-menu-button>",
        "<sl:sidebar-menu-button href=\"/\" active>Home</sl:sidebar-menu-button>",
        "<sl:sidebar-menu-button as=\"button\" active>Drafts</sl:sidebar-menu-button>",
        "<sl:sidebar-menu-button href=\"/\" variant=\"default\">Home</sl:sidebar-menu-button>",
        "<sl:sidebar-menu-button href=\"/\" variant=\"outline\">Home</sl:sidebar-menu-button>",
        "<sl:sidebar-menu-button href=\"/\" size=\"default\">Home</sl:sidebar-menu-button>",
        "<sl:sidebar-menu-button href=\"/\" size=\"sm\">Home</sl:sidebar-menu-button>",
        "<sl:sidebar-menu-button href=\"/\" size=\"lg\">Home</sl:sidebar-menu-button>",
        "<sl:sidebar-menu-button href=\"/billing\" disabled>Billing</sl:sidebar-menu-button>",
        "<sl:sidebar-menu-button as=\"button\" disabled>Archive</sl:sidebar-menu-button>",
        """
            <sl:sidebar-menu-button href="/inbox" th:active="${#slNav.current('/inbox')}">
              <sl:slot name="icon-start"><sl:icon name="inbox"/></sl:slot>
              Inbox
              <sl:slot name="icon-end"><sl:icon name="chevron-right"/></sl:slot>
            </sl:sidebar-menu-button>""",
        """
            <sl:sidebar-menu-button href="/" size="lg">
              <sl:slot name="icon-start"><sl:icon name="gallery-vertical-end"/></sl:slot>
              <strong>Acme Inc.</strong>
              <small>Enterprise</small>
            </sl:sidebar-menu-button>""",
        "<sl:sidebar-menu-action aria-label=\"More for Design\"><sl:icon name=\"ellipsis\"/></sl:sidebar-menu-action>",
        "<sl:sidebar-menu-action aria-label=\"More for Design\" show-on-hover>…</sl:sidebar-menu-action>",
        "<sl:sidebar-menu-action as=\"button\" type=\"submit\" aria-label=\"Remove Design\">×</sl:sidebar-menu-action>",
        "<sl:sidebar-menu-action as=\"a\" href=\"/design\" aria-label=\"Open Design\">→</sl:sidebar-menu-action>",
        "<sl:sidebar-menu-badge>12</sl:sidebar-menu-badge>",
        "<sl:sidebar-menu-badge label=\"12 unread messages\">12</sl:sidebar-menu-badge>",
        "<sl:sidebar-menu-button href=\"/\" tooltip=\"Home\">Home</sl:sidebar-menu-button>",
        "<sl:sidebar-menu-button as=\"button\" tooltip=\"Compose\">Compose</sl:sidebar-menu-button>",
        "<sl:sidebar-menu-item collapsible>Content</sl:sidebar-menu-item>",
        "<sl:sidebar-menu-item collapsible open id=\"settings\">Content</sl:sidebar-menu-item>",
        """
            <sl:sidebar-menu-item collapsible open>
              <sl:sidebar-menu-button tooltip="Settings" variant="outline" size="sm">
                <sl:slot name="icon-start"><sl:icon name="settings"/></sl:slot>
                Settings
              </sl:sidebar-menu-button>
              <sl:sidebar-menu-sub>
                <sl:sidebar-menu-sub-item>
                  <sl:sidebar-menu-sub-button href="/settings" th:active="${#slNav.current('/settings')}">
                    General
                  </sl:sidebar-menu-sub-button>
                </sl:sidebar-menu-sub-item>
              </sl:sidebar-menu-sub>
            </sl:sidebar-menu-item>""",
        "<sl:sidebar-menu-sub>Items</sl:sidebar-menu-sub>",
        "<sl:sidebar-menu-sub-item>Button</sl:sidebar-menu-sub-item>",
        "<sl:sidebar-menu-sub-button href=\"/a\">Link</sl:sidebar-menu-sub-button>",
        "<sl:sidebar-menu-sub-button as=\"a\" href=\"/a\">Link</sl:sidebar-menu-sub-button>",
        "<sl:sidebar-menu-sub-button as=\"button\">Action</sl:sidebar-menu-sub-button>",
        "<sl:sidebar-menu-sub-button as=\"button\" type=\"submit\">Action</sl:sidebar-menu-sub-button>",
        "<sl:sidebar-menu-sub-button as=\"button\" type=\"reset\">Action</sl:sidebar-menu-sub-button>",
        "<sl:sidebar-menu-sub-button href=\"/a\" active>Link</sl:sidebar-menu-sub-button>",
        "<sl:sidebar-menu-sub-button as=\"button\" active>Action</sl:sidebar-menu-sub-button>",
        "<sl:sidebar-menu-sub-button href=\"/a\" size=\"md\">Link</sl:sidebar-menu-sub-button>",
        "<sl:sidebar-menu-sub-button href=\"/a\" size=\"sm\">Link</sl:sidebar-menu-sub-button>",
        "<sl:sidebar-menu-sub-button href=\"/a\" disabled>Link</sl:sidebar-menu-sub-button>",
        "<sl:sidebar-menu-sub-button as=\"button\" disabled>Action</sl:sidebar-menu-sub-button>",
        """
            <sl:sidebar-menu-sub-button href="/a">
              <sl:slot name="icon-start"><sl:icon name="file"/></sl:slot>
              With an icon
            </sl:sidebar-menu-sub-button>""",
        """
            <sl:sidebar>
              <sl:sidebar-header>
                <sl:sidebar-menu>
                  <sl:sidebar-menu-item>
                    <sl:sidebar-menu-button href="/" size="lg">
                      <sl:slot name="icon-start"><sl:icon name="gallery-vertical-end"/></sl:slot>
                      <strong>Acme Inc.</strong>
                      <small>Enterprise</small>
                    </sl:sidebar-menu-button>
                  </sl:sidebar-menu-item>
                </sl:sidebar-menu>
              </sl:sidebar-header>
              <sl:sidebar-content>
                <sl:sidebar-group id="platform">
                  <sl:sidebar-group-label>Platform</sl:sidebar-group-label>
                  <sl:sidebar-group-content>
                    <sl:sidebar-menu>
                      <sl:sidebar-menu-item>
                        <sl:sidebar-menu-button href="/" th:active="${#slNav.current('/')}">
                          <sl:slot name="icon-start"><sl:icon name="house"/></sl:slot>
                          Home
                        </sl:sidebar-menu-button>
                      </sl:sidebar-menu-item>
                      <sl:sidebar-menu-item id="inbox">
                        <sl:sidebar-menu-button href="/inbox" th:active="${#slNav.current('/inbox')}">
                          <sl:slot name="icon-start"><sl:icon name="inbox"/></sl:slot>
                          Inbox
                        </sl:sidebar-menu-button>
                        <sl:sidebar-menu-badge>12</sl:sidebar-menu-badge>
                      </sl:sidebar-menu-item>
                    </sl:sidebar-menu>
                  </sl:sidebar-group-content>
                </sl:sidebar-group>
                <sl:sidebar-group id="projects">
                  <sl:sidebar-group-label>Projects</sl:sidebar-group-label>
                  <sl:sidebar-group-action aria-label="Add project"><sl:icon name="plus"/></sl:sidebar-group-action>
                  <sl:sidebar-group-content>
                    <sl:sidebar-menu>
                      <sl:sidebar-menu-item>
                        <sl:sidebar-menu-button href="/projects/design">Design</sl:sidebar-menu-button>
                        <sl:sidebar-menu-action aria-label="More for Design" show-on-hover>
                          <sl:icon name="ellipsis"/>
                        </sl:sidebar-menu-action>
                      </sl:sidebar-menu-item>
                    </sl:sidebar-menu>
                  </sl:sidebar-group-content>
                </sl:sidebar-group>
              </sl:sidebar-content>
              <sl:sidebar-separator/>
              <sl:sidebar-footer>
                <sl:sidebar-menu>
                  <sl:sidebar-menu-item>
                    <sl:sidebar-menu-button href="/help" size="sm">Help</sl:sidebar-menu-button>
                  </sl:sidebar-menu-item>
                </sl:sidebar-menu>
              </sl:sidebar-footer>
            </sl:sidebar>"""));
  }
}
