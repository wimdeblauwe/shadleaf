/*
  The body of a dialog or sheet (sl:dialog-body, sl:sheet-body) as an htmx target. Its content has the id
  <dialog id>-body; for a request that targets it (or an element in it):
  - a request from outside opens the dialog at once (showModal, so focus returns to where it was when it closes). A
    request from inside a closed dialog, such as hx-trigger="load", opens nothing;
  - while it runs, the body has data-loading (the stylesheet covers the content and shows a spinner after a moment)
    and the content aria-busy, and further requests into the body are cancelled (htmx:confirm), so a second click on
    Save sends nothing;
  - when it fails (an error status, no connection, a timeout), the body's alert shows; for the request that opened the
    dialog, unless data-on-load-error="inline" or the page has no toaster, the dialog closes and the alert's text is
    shown as an error toast (sl-toast) instead;
  - once the answer to the request that opened the dialog is in, focus moves into it, as showModal() would have moved
    it had the content been there: to an [autofocus] element, else the first control (not when the focus is in the
    content already);
  - closing the dialog aborts a request still running (not one whose answer is in, which may be what closed it);
  - content marked data-clear-on-close (the body was empty in the page) is emptied once the dialog has closed, and at
    once when a request opens the dialog again or htmx saves the page in its history cache, so it never shows the
    previous content.
*/

import {firstTabbable} from './popup.js';

/** Watches the body of `dialog`, if it has one; `listen` adds a listener that is removed with the component. */
export function watchBody(dialog, listen) {
  // The request into the body that is running: its XMLHttpRequest, and whether it opened the dialog.
  let request = null;
  // The showModal() waiting for the click that sent the request to be handled.
  let opening = null;

  const parts = () => {
    const content = document.getElementById(`${dialog.id}-body`);
    if (!content || content.closest('dialog') !== dialog) {
      return null;
    }
    const body = content.parentElement;
    return {content, body, error: body.querySelector(':scope > [role=alert]')};
  };
  const owns = (event, body) => event.detail?.target instanceof Element && body.content.contains(event.detail.target);

  const setLoading = (body, loading) => {
    if (loading) {
      body.body.setAttribute('data-loading', 'true');
      body.content.setAttribute('aria-busy', 'true');
    } else {
      body.body.removeAttribute('data-loading');
      body.content.removeAttribute('aria-busy');
    }
  };
  const showError = (body, shown) => {
    if (body.error) {
      body.error.hidden = !shown;
    }
  };
  const clear = body => {
    if (body.content.dataset.clearOnClose === 'true') {
      body.content.replaceChildren();
    }
  };
  const abort = () => {
    const xhr = request?.xhr;
    request = null;
    if (xhr && xhr.readyState !== XMLHttpRequest.DONE) {
      xhr.abort();
    }
  };

  listen(document, 'htmx:confirm', event => {
    const body = parts();
    if (request && body && owns(event, body)) {
      event.preventDefault();
    }
  });

  listen(document, 'htmx:beforeRequest', event => {
    const body = parts();
    if (!body || !owns(event, body)) {
      return;
    }
    const fromOutside = !(event.detail.elt instanceof Node && dialog.contains(event.detail.elt));
    if (!dialog.open && !fromOutside) {
      return;
    }
    const opens = !dialog.open;
    showError(body, false);
    if (opens) {
      clear(body);
      // A task later, once the click that sent the request is handled: a dropdown menu moves the focus from its item
      // back to its trigger after the item's own listeners (htmx's), and showModal() remembers where the focus is to
      // return it there.
      const thisOpening = opening = {};
      setTimeout(() => {
        if (opening === thisOpening) {
          opening = null;
          if (!dialog.open) {
            dialog.showModal();
          }
        }
      });
    }
    request = {xhr: event.detail.xhr, opened: opens};
    setLoading(body, true);
  });

  const end = (event, failed) => {
    if (!request || event.detail?.xhr !== request.xhr) {
      return;
    }
    const {opened} = request;
    request = null;
    const body = parts();
    if (!body) {
      return;
    }
    setLoading(body, false);
    if (!failed) {
      if (opened && dialog.open && !body.content.contains(document.activeElement)) {
        (body.content.querySelector('[autofocus]') ?? firstTabbable(body.content))?.focus();
      }
      return;
    }
    if (opened && body.body.dataset.onLoadError !== 'inline' && document.querySelector('.toaster')) {
      const title = body.error?.textContent.trim() ?? '';
      opening = null;
      dialog.close();
      dialog.dispatchEvent(new CustomEvent('sl-toast', {bubbles: true, detail: {title, variant: 'error'}}));
    } else {
      showError(body, true);
    }
  };
  // After an error status afterRequest has failed; after no connection, a timeout or an abort it has neither, and the
  // event that follows says which.
  listen(document, 'htmx:afterRequest', event => {
    if (event.detail?.successful === true) {
      end(event, false);
    } else if (event.detail?.failed === true) {
      end(event, true);
    }
  });
  listen(document, 'htmx:sendError', event => end(event, true));
  listen(document, 'htmx:timeout', event => end(event, true));
  listen(document, 'htmx:sendAbort', event => end(event, false));

  listen(dialog, 'close', () => {
    abort();
    const body = parts();
    if (!body) {
      return;
    }
    setLoading(body, false);
    afterTransitions(dialog).then(() => {
      const closed = parts();
      if (closed && !dialog.open) {
        showError(closed, false);
        clear(closed);
      }
    });
  });

  listen(document, 'htmx:beforeHistorySave', () => {
    opening = null;
    abort();
    const body = parts();
    if (body) {
      setLoading(body, false);
      showError(body, false);
      clear(body);
    }
  });
}

/** Resolves once the dialog's own closing transitions (fade, slide) are over; at once under reduced motion. */
function afterTransitions(dialog) {
  return new Promise(resolve => requestAnimationFrame(resolve))
      .then(() => Promise.allSettled(dialog.getAnimations()
          .filter(animation => animation.effect?.getComputedTiming().endTime !== Infinity)
          .map(animation => animation.finished)));
}
