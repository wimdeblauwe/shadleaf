/*
  The moment before htmx saves the page in its history cache, for the components that tidy up what a snapshot must not
  keep (an open dialog, toasts, a chart's drawing). htmx 2 fires htmx:beforeHistorySave; htmx 4 keeps no cache, but its
  hx-history-cache extension does, and fires htmx:history:cache:before:save. Both are taken right after the event.
  htmx 4's htmx-2-compat extension fires htmx:beforeHistorySave on every history update as well, in the same task as
  the extension's event when both are loaded: the handler then runs once.
  Not with hx-alpine-compat (htmx 4.0.1), which Shadleaf needs on htmx 4: its own hook on the cache event runs first
  and destroys every Alpine component on the page, which removes these listeners. The tests in dialog.spec.ts and
  toast.spec.ts fail until that changes (https://github.com/bigskysoftware/htmx/issues/4221).
*/
const EVENTS = ['htmx:beforeHistorySave', 'htmx:history:cache:before:save'];

/** Adds `handler` for the events through `listen(target, type, handler)`, so it is removed with the component. */
export function onHistorySave(listen, handler) {
  let ran = false;
  const once = () => {
    if (ran) {
      return;
    }
    ran = true;
    queueMicrotask(() => ran = false);
    handler();
  };
  EVENTS.forEach(type => listen(document, type, once));
}
