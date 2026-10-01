package io.github.wimdeblauwe.shadleaf.sample01;

import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxRequest;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxResponse;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxReswap;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import io.github.wimdeblauwe.shadleaf.toast.Toast;
import jakarta.validation.Valid;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * The dialog page. A dialog on the page itself, opened by a button, and an edit dialog per team member that htmx
 * fetches into {@code #modal-root}, where {@code <sl:dialog open>} shows it as a modal. The form in it swaps only
 * itself: with errors the dialog stays open, with the focus on the first field with an error. A valid save closes the
 * dialog through {@code HX-Trigger: sl-dialog-close}, swaps nothing where the form is, and updates the member's row out
 * of band.
 * <p>
 * Delete fetches an alert dialog the same way; its Delete button sends {@code hx-delete}, and the answer closes it with
 * the same event, shows a toast ({@code HX-Trigger: sl-toast}), removes the row and moves the focus to the next row.
 * Invite member opens a sheet in the page, whose form swaps itself too; a valid invite answers with an empty form,
 * closes the sheet, appends the new row and shows a toast, both out of band.
 * <p>
 * The row actions are a dropdown menu per member, whose items fetch the edit and delete dialogs. A second menu sorts
 * the table: its radio items are links, and the page marks the chosen one. Rename opens a popover with a form that
 * swaps itself; a valid rename closes it through {@code HX-Trigger: sl-popover-close} and updates the name out of
 * band.
 * <p>
 * Each row has an avatar. Ada Lovelace has a photo, Grace Hopper's photo URL answers 404 (the photo was removed), so
 * Shadleaf's script shows her initials instead of a broken image, and Alan Turing has none, so {@code th:src} renders an
 * empty {@code src} and the stylesheet shows his initials.
 */
@Controller
public class DialogController {

  private static final Map<String, String> SORT_OPTIONS = sortOptions();
  private static final Map<String, Comparator<Member>> SORT_ORDERS = Map.of(
      "added", Comparator.comparingLong(Member::id),
      "name", Comparator.comparing(Member::name, String.CASE_INSENSITIVE_ORDER),
      "email", Comparator.comparing(Member::email, String.CASE_INSENSITIVE_ORDER));

  private final Map<Long, Member> members = new ConcurrentSkipListMap<>(Map.of(
      1L, new Member(1, "Ada Lovelace", "ada@example.com", "/dialog/members/1/photo"),
      2L, new Member(2, "Grace Hopper", "grace@example.com", "/dialog/members/2/photo"),
      3L, new Member(3, "Alan Turing", "alan@example.com")));
  private final AtomicLong nextId = new AtomicLong(4);
  private volatile String teamName = "Team Shadleaf";

  @GetMapping("/dialog")
  public String page(@RequestParam(defaultValue = "added") String sort, Model model) {
    String order = SORT_ORDERS.containsKey(sort) ? sort : "added";
    model.addAttribute("members", sorted(order));
    model.addAttribute("sort", order);
    model.addAttribute("sortOptions", SORT_OPTIONS);
    model.addAttribute("inviteForm", new MemberForm());
    model.addAttribute("teamName", teamName);
    model.addAttribute("renameForm", RenameTeamForm.of(teamName));
    return "dialog";
  }

  @HxRequest
  @PostMapping("/dialog/team")
  public String rename(@Valid @ModelAttribute("renameForm") RenameTeamForm renameForm, BindingResult bindingResult,
      HtmxResponse htmxResponse, Model model) {
    if (bindingResult.hasErrors()) {
      return "dialog :: rename-form";
    }
    teamName = renameForm.getTeamName().strip();
    model.addAttribute("teamName", teamName);
    model.addAttribute("renameForm", RenameTeamForm.of(teamName));
    htmxResponse.addTrigger("sl-popover-close");
    // The form again, with the new name, and the heading out of band.
    return "dialog :: renamed";
  }

  @HxRequest
  @GetMapping("/dialog/members/{id}/edit")
  public String edit(@PathVariable long id, Model model) {
    Member member = member(id);
    model.addAttribute("member", member);
    model.addAttribute("memberForm", MemberForm.of(member));
    return "dialog :: edit-dialog";
  }

  @HxRequest
  @PostMapping("/dialog/members/{id}")
  public String update(@PathVariable long id, @Valid @ModelAttribute("memberForm") MemberForm memberForm,
      BindingResult bindingResult, HtmxResponse htmxResponse, Model model) {
    Member member = member(id);
    if (bindingResult.hasErrors()) {
      model.addAttribute("member", member);
      return "dialog :: edit-form";
    }
    Member updated = member.withNameAndEmail(memberForm.getName(), memberForm.getEmail());
    members.put(id, updated);
    model.addAttribute("member", updated);
    htmxResponse.addTrigger("sl-dialog-close");
    htmxResponse.setReswap(HtmxReswap.none());
    return "dialog :: member-row-oob";
  }

  /** Ada Lovelace's photo (photos/member-1.jpg); for everyone else 404, as for a photo that was removed. */
  @GetMapping(value = "/dialog/members/{id}/photo", produces = MediaType.IMAGE_JPEG_VALUE)
  @ResponseBody
  public Resource photo(@PathVariable long id) {
    Resource photo = new ClassPathResource("photos/member-%d.jpg".formatted(id));
    if (!photo.exists()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    return photo;
  }

  @HxRequest
  @GetMapping("/dialog/members/{id}/delete")
  public String confirmDelete(@PathVariable long id, Model model) {
    model.addAttribute("member", member(id));
    return "dialog :: delete-dialog";
  }

  @HxRequest
  @DeleteMapping("/dialog/members/{id}")
  public String delete(@PathVariable long id, HtmxRequest htmxRequest, HtmxResponse htmxResponse, Model model) {
    Member member = member(id);
    // The rows as the page shows them: htmx sends the page's address, sort order included, in HX-Current-URL.
    List<Member> rows = sorted(sortOf(htmxRequest.getCurrentUrl()));
    int index = rows.indexOf(member);
    members.remove(id);
    htmxResponse.addTrigger("sl-dialog-close");
    htmxResponse.addTrigger("sl-toast", Toast.success("%s was deleted".formatted(member.name())));
    // The row goes with the menu button the focus returns to when the alert dialog closes, so the focus moves to the
    // next row's menu button (the previous row's after the last one), or to Invite member once the table is empty:
    // re-rendered out of band with autofocus, which htmx focuses after the swap. The toast says what happened.
    Member neighbour = index + 1 < rows.size() ? rows.get(index + 1) : index > 0 ? rows.get(index - 1) : null;
    if (neighbour == null) {
      return "dialog :: invite-button-focus";
    }
    model.addAttribute("member", neighbour);
    return "dialog :: member-row-focus";
  }

  @HxRequest
  @PostMapping("/dialog/members")
  public String invite(@Valid @ModelAttribute("inviteForm") MemberForm inviteForm, BindingResult bindingResult,
      HtmxResponse htmxResponse, Model model) {
    if (bindingResult.hasErrors()) {
      return "dialog :: invite-form";
    }
    Member member = new Member(nextId.getAndIncrement(), inviteForm.getName(), inviteForm.getEmail());
    members.put(member.id(), member);
    model.addAttribute("member", member);
    model.addAttribute("inviteForm", new MemberForm());
    htmxResponse.addTrigger("sl-dialog-close");
    // The row and a toast, both out of band.
    return "dialog :: invited";
  }

  private static Map<String, String> sortOptions() {
    Map<String, String> options = new LinkedHashMap<>();
    options.put("added", "Date added");
    options.put("name", "Name");
    options.put("email", "Email");
    return options;
  }

  private List<Member> sorted(String order) {
    return members.values().stream().sorted(SORT_ORDERS.getOrDefault(order, SORT_ORDERS.get("added"))).toList();
  }

  /** The sort order in the address of the page a request came from. */
  private static String sortOf(@Nullable String currentUrl) {
    if (currentUrl == null) {
      return "added";
    }
    String sort = UriComponentsBuilder.fromUriString(currentUrl).build().getQueryParams().getFirst("sort");
    return sort != null ? sort : "added";
  }

  private Member member(long id) {
    Member member = members.get(id);
    if (member == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    return member;
  }
}
