package io.github.wimdeblauwe.shadleaf.popup;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@code <sl:dropdown-menu>} and its parts, alone and composed, rendered into
 * {@code src/test/resources/approved/dropdown-menu.approved.html}.
 */
class DropdownMenuApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void dropdownMenuAndParts() {
    HtmlApproval.verifyRenders("dropdown-menu", tester, List.of(
        """
            <sl:dropdown-menu id="m1">
              <sl:dropdown-menu-trigger variant="outline">Open</sl:dropdown-menu-trigger>
              <sl:dropdown-menu-content>
                <sl:dropdown-menu-item>Profile</sl:dropdown-menu-item>
              </sl:dropdown-menu-content>
            </sl:dropdown-menu>""",
        """
            <sl:dropdown-menu th:id="|member-${42}-actions|">
              <sl:dropdown-menu-trigger variant="ghost" size="icon-sm" aria-label="Actions">
                <sl:icon name="ellipsis"/>
              </sl:dropdown-menu-trigger>
              <sl:dropdown-menu-content side="left" align="end" class="w-40">
                <sl:dropdown-menu-item>Edit</sl:dropdown-menu-item>
              </sl:dropdown-menu-content>
            </sl:dropdown-menu>""",
        """
            <sl:dropdown-menu id="m3">
              <sl:dropdown-menu-content side="top" align="center">Items</sl:dropdown-menu-content>
            </sl:dropdown-menu>""",
        "<sl:dropdown-menu-item>Profile</sl:dropdown-menu-item>",
        "<sl:dropdown-menu-item as=\"a\" href=\"/settings\">Settings</sl:dropdown-menu-item>",
        "<sl:dropdown-menu-item as=\"a\" href=\"/settings\" disabled>Settings</sl:dropdown-menu-item>",
        "<sl:dropdown-menu-item disabled>Archive</sl:dropdown-menu-item>",
        "<sl:dropdown-menu-item variant=\"destructive\" hx-delete=\"/members/1\">Delete</sl:dropdown-menu-item>",
        "<sl:dropdown-menu-item type=\"submit\" inset>Log out</sl:dropdown-menu-item>",
        """
            <sl:dropdown-menu-item><sl:icon name="user"/>Profile
              <sl:dropdown-menu-shortcut>⇧⌘P</sl:dropdown-menu-shortcut></sl:dropdown-menu-item>""",
        "<sl:dropdown-menu-checkbox-item checked>Status bar</sl:dropdown-menu-checkbox-item>",
        "<sl:dropdown-menu-checkbox-item as=\"a\" href=\"?panel=true\">Panel</sl:dropdown-menu-checkbox-item>",
        """
            <sl:dropdown-menu-radio-group aria-label="Sort by">
              <sl:dropdown-menu-radio-item as="a" href="?sort=name" checked>Name</sl:dropdown-menu-radio-item>
              <sl:dropdown-menu-radio-item as="a" href="?sort=date" disabled>Date</sl:dropdown-menu-radio-item>
            </sl:dropdown-menu-radio-group>""",
        "<sl:dropdown-menu-label>My account</sl:dropdown-menu-label>",
        "<sl:dropdown-menu-label inset>My account</sl:dropdown-menu-label>",
        "<sl:dropdown-menu-group><sl:dropdown-menu-item>Team</sl:dropdown-menu-item></sl:dropdown-menu-group>",
        "<sl:dropdown-menu-separator/>",
        "<sl:dropdown-menu-shortcut>⌘S</sl:dropdown-menu-shortcut>"));
  }

  /** The trigger as each part it can be: {@code dropdown-menu-trigger.approved.html}. */
  @Test
  void triggerKinds() {
    HtmlApproval.verifyRenders("dropdown-menu-trigger", tester, List.of(
        """
            <sl:dropdown-menu id="sort">
              <sl:dropdown-menu-trigger variant="outline" size="sm">
                <sl:slot name="icon-start"><sl:icon name="arrow-up-down"/></sl:slot>
                Sort
                <sl:slot name="icon-end"><sl:icon name="chevron-down"/></sl:slot>
              </sl:dropdown-menu-trigger>
            </sl:dropdown-menu>""",
        """
            <sl:sidebar-menu-item>
              <sl:dropdown-menu id="teams">
                <sl:dropdown-menu-trigger as="sidebar-menu-button" size="lg" tooltip="Acme Inc.">
                  <sl:slot name="icon-start"><sl:icon name="gallery-vertical-end"/></sl:slot>
                  <strong>Acme Inc.</strong>
                  <small>Enterprise</small>
                  <sl:slot name="icon-end"><sl:icon name="chevrons-up-down"/></sl:slot>
                </sl:dropdown-menu-trigger>
                <sl:dropdown-menu-content side="right">
                  <sl:dropdown-menu-label>Teams</sl:dropdown-menu-label>
                  <sl:dropdown-menu-radio-group aria-label="Teams">
                    <sl:dropdown-menu-radio-item as="a" href="/teams/acme" checked>Acme Inc.</sl:dropdown-menu-radio-item>
                    <sl:dropdown-menu-radio-item as="a" href="/teams/evil">Evil Corp.</sl:dropdown-menu-radio-item>
                  </sl:dropdown-menu-radio-group>
                </sl:dropdown-menu-content>
              </sl:dropdown-menu>
            </sl:sidebar-menu-item>""",
        """
            <sl:dropdown-menu id="version">
              <sl:dropdown-menu-trigger as="sidebar-menu-button" variant="outline" size="sm">v1.0.0</sl:dropdown-menu-trigger>
            </sl:dropdown-menu>""",
        """
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button href="/projects/design">Design Engineering</sl:sidebar-menu-button>
              <sl:dropdown-menu id="design-more">
                <sl:dropdown-menu-trigger as="sidebar-menu-action" show-on-hover aria-label="More for Design Engineering">
                  <sl:icon name="ellipsis"/>
                </sl:dropdown-menu-trigger>
                <sl:dropdown-menu-content side="right">
                  <sl:dropdown-menu-item as="a" href="/projects/design">View project</sl:dropdown-menu-item>
                </sl:dropdown-menu-content>
              </sl:dropdown-menu>
            </sl:sidebar-menu-item>"""));
  }
}
