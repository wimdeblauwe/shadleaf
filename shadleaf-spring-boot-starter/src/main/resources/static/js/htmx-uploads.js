/*
  Watches the htmx requests that send a component's files, for sl:file-upload's progress. Works with htmx 2 and htmx 4
  (whichever is on the page fires its own events; without htmx none fire and nothing happens):
  - htmx 2 sends with an XMLHttpRequest and hands it over in htmx:beforeSend. Its own htmx:xhr:progress cannot be
    used: it fires for the upload and the download alike and does not say which. So the listeners go on xhr.upload.
    htmx:configRequest has the request's FormData, to tell whether it carries the files.
  - htmx 4 sends with fetch(), which reports no upload progress. A request whose body carries the files gets a fetch
    of its own (ctx.fetch, read after htmx:before:request) that sends it with an XMLHttpRequest and answers with a
    Response. htmx keeps doing everything else: the swap, the HX-* headers, history, the timeout and htmx:abort,
    through the request's signal.
  One request at a time per component: another one that carries the files while one is going is not watched.

  `carries(body)` says whether a request body (a FormData, or anything else) holds the files. `start(elt)` is called
  when such a request is sent, with the element that made it, and returns {progress(loaded, total), sent(), end(how)}:
  progress as the bytes go (total 0 when the size is not known), sent once the body is out, and end with 'done',
  'error' (an error status, no connection, a timeout) or 'aborted'.
*/
export function watchUploads(listen, carries, start) {
  let pending; // htmx 2: the request config that carries the files, between htmx:configRequest and htmx:beforeSend
  let current; // {key, aborted, handlers}: the htmx 2 xhr or the htmx 4 ctx

  const begin = (elt, xhr, key) => {
    const request = {key, aborted: false, handlers: start(elt)};
    current = request;
    xhr.upload.addEventListener('progress', event => request.handlers.progress(event.loaded,
        event.lengthComputable ? event.total : 0));
    xhr.upload.addEventListener('load', () => request.handlers.sent());
    xhr.addEventListener('abort', () => {
      request.aborted = true;
    });
  };

  const end = (key, how) => {
    if (!current || current.key !== key) {
      return;
    }
    const request = current;
    current = undefined;
    request.handlers.end(request.aborted ? 'aborted' : how);
  };

  // htmx 2. htmx:afterRequest comes after the swap, and again on the closest parent left when the swap removed the
  // element that made the request: the second one is ignored. After an abort, no connection or a timeout it has
  // neither successful nor failed set, and comes before the event that says which (htmx's xhr.onabort runs before
  // any listener added here).
  listen(document, 'htmx:configRequest', event => {
    const detail = event.detail ?? {};
    pending = !current && !detail.useUrlParams && carries(detail.formData) ? detail : undefined;
  });
  listen(document, 'htmx:beforeSend', event => {
    const {xhr, requestConfig, elt} = event.detail ?? {};
    if (!pending || requestConfig !== pending || !(xhr instanceof XMLHttpRequest)) {
      return;
    }
    pending = undefined;
    begin(elt, xhr, xhr);
  });
  listen(document, 'htmx:afterRequest', event => {
    const {xhr, successful, failed} = event.detail ?? {};
    if (successful || failed) {
      end(xhr, successful ? 'done' : 'error');
    }
  });
  listen(document, 'htmx:sendAbort', event => end(event.detail?.xhr, 'aborted'));
  listen(document, 'htmx:sendError', event => end(event.detail?.xhr, 'error'));
  listen(document, 'htmx:timeout', event => end(event.detail?.xhr, 'error'));

  // htmx 4. A multipart body is a FormData by htmx:before:request. htmx:finally:request comes after the swap, or after
  // htmx:error (no connection, a timeout, an abort: the fetch rejected).
  listen(document, 'htmx:before:request', event => {
    const ctx = event.detail?.ctx;
    if (!ctx?.request || current || !carries(ctx.request.body)) {
      return;
    }
    const elt = ctx.sourceElement;
    ctx.fetch = (url, init) => send(url, init, xhr => begin(elt, xhr, ctx));
  });
  listen(document, 'htmx:finally:request', event => {
    const ctx = event.detail?.ctx;
    const failed = String(ctx?.status ?? '').startsWith('error') || (ctx?.response?.status ?? 0) >= 400;
    end(ctx, failed ? 'error' : 'done');
  });
}

/** Asks the element that made a request to abort it: htmx 2 listens on the body (and reads detail.elt), 4 on it. */
export function abortRequest(elt) {
  elt.dispatchEvent(new CustomEvent('htmx:abort', {bubbles: true, detail: {}}));
}

const NULL_BODY_STATUSES = [101, 103, 204, 205, 304];

/**
 * fetch() for htmx 4's ctx.fetch, sent with an XMLHttpRequest for its upload progress. It takes what htmx passes:
 * method, headers, body, credentials and signal (an abort rejects, as fetch's does); mode is left to the browser,
 * which keeps an XMLHttpRequest to the page's origin without CORS anyway.
 */
function send(url, init, attach) {
  return new Promise((resolve, reject) => {
    const signal = init.signal;
    if (signal?.aborted) {
      reject(signal.reason);
      return;
    }
    const xhr = new XMLHttpRequest();
    xhr.open(init.method ?? 'GET', url);
    xhr.responseType = 'blob';
    xhr.withCredentials = init.credentials === 'include';
    new Headers(init.headers).forEach((value, name) => xhr.setRequestHeader(name, value));
    const abort = () => xhr.abort();
    signal?.addEventListener('abort', abort, {once: true});
    const settle = () => signal?.removeEventListener('abort', abort);
    xhr.addEventListener('load', () => {
      settle();
      const response = new Response(NULL_BODY_STATUSES.includes(xhr.status) ? null : xhr.response, {
        status: xhr.status,
        statusText: xhr.statusText,
        headers: responseHeaders(xhr),
      });
      // A Response made here has no URL; htmx reads it after a redirect.
      Object.defineProperty(response, 'url', {value: xhr.responseURL});
      resolve(response);
    });
    xhr.addEventListener('error', () => {
      settle();
      reject(new TypeError('Failed to fetch'));
    });
    xhr.addEventListener('abort', () => {
      settle();
      reject(signal?.reason ?? new DOMException('The request was aborted.', 'AbortError'));
    });
    attach(xhr);
    xhr.send(init.body ?? null);
  });
}

function responseHeaders(xhr) {
  const headers = new Headers();
  for (const line of xhr.getAllResponseHeaders().trim().split(/[\r\n]+/)) {
    const colon = line.indexOf(':');
    if (colon > 0) {
      headers.append(line.slice(0, colon).trim(), line.slice(colon + 1).trim());
    }
  }
  return headers;
}
