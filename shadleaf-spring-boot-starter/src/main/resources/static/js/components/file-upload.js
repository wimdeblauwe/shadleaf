/*
  sl:file-upload. The server renders a visually hidden file input, a Browse label for it, an empty (hidden) list, a
  status region and two templates: a picked file and a rejected one. The input stays the source of truth: it is what
  the form submits, and the list is drawn from its files. This adds:
  - drop: files dropped anywhere on the component are added, as files picked again with `multiple` are (the browser
    replaces the input's files on every pick; they are put back together with the earlier ones). A file that is there
    already (same name, size and date) is not added twice. Without `multiple`, the new file replaces the old one.
    Every change goes through a DataTransfer and input.files = dt.files, then dispatches input and change, as a pick
    does, so hx-trigger="change" works for a drop too.
  - accept and data-max-size are checked for dropped and picked files (the picker lets one choose "All files"). A file
    that fails shows as a rejected attachment (state="error", the reason as its description) after the picked ones,
    until it is dismissed or files are added again. It is not in the input.
  - each attachment's button removes its file (or dismisses a rejected one); the focus moves to the next button, the
    previous one, or back to the input.
  - added, removed and rejected files are announced once per change, in the status region (polite).
  - the list is drawn again from input.files on init (htmx's history restore renders the markup without the files) and
    on pageshow (the back/forward cache, a browser that restored the form's files).
  - progress, when an htmx request (htmx 2 or 4, see htmx-uploads.js) sends the files: each file is uploading, with
    the share of its bytes sent ("Uploading · 64%"), then processing once the body is out, then the outcome. The
    progress is for the whole request; the files go into the body one after the other, in the list's order, so each
    file's share is read from where its bytes lie in it (the boundaries and other fields scaled away). Meanwhile the
    input is disabled (no adding or removing; htmx has the files already), the remove buttons are hidden and a Cancel
    button takes the Browse label's place: it aborts the request (htmx:abort) and the files are idle again. A failed
    request (an error status, no connection, a timeout) marks them as errors; they stay in the input, to be sent
    again. After a success the swap replaces the component, as a rule; if it is still there, the files show as
    uploaded and leave the input, so they are not sent twice. The start, a failure, a cancel and a success the
    component sees are announced, never the percentages. Without htmx none of this happens: a plain POST shows nothing.
  - previews, when the server rendered the image template (the previews prop): a picked image (image/*, not SVG, up to
    PREVIEW_MAX_SIZE) gets a blob: URL, loaded first in an Image of its own; once it has loaded, the image media takes
    the icon's place, in every state. One that does not load (a type the browser cannot decode, a CSP whose img-src
    does not allow blob:) keeps its icon, and after a CSP has blocked one no other URL is made on the page. Each URL is
    revoked when its file leaves the list (removed, replaced, the next change after an upload) and when the component
    is destroyed. SVG gets none: a blob: URL has the page's origin, and an SVG opened from it ("Open image in new
    tab") would run its script there. htmx's history snapshot gets the icons back: it would hold dead URLs.
  The messages arrive with a plain {0} (and {1}) in them, which this fills in.
*/
import {listener} from '../popup.js';
import {abortRequest, watchUploads} from '../htmx-uploads.js';

const ANNOUNCE_DELAY_MS = 100;
const UNITS = ['byte', 'kilobyte', 'megabyte', 'gigabyte', 'terabyte'];
// Larger images keep their icon: decoding a big photo takes a lot of memory and time.
const PREVIEW_MAX_SIZE = 20 * 1024 * 1024;

// Set once a Content-Security-Policy blocked a blob: image: it blocks every other one on the page.
let blobImagesBlocked = false;

export default function fileUpload() {
  return {
    // Declared, so init() sets them here: Alpine puts a property no scope has on the outermost x-data, where every
    // component nested in that one would share it.
    cleanups: [],
    timer: undefined,

    init() {
      const root = this.$el;
      const input = root.querySelector(':scope > .file-upload-input');
      const list = root.querySelector(':scope > .file-upload-list');
      const status = root.querySelector(':scope > .file-upload-status');
      const fileTemplate = root.querySelector(':scope > template[data-kind="file"]');
      const rejectedTemplate = root.querySelector(':scope > template[data-kind="rejected"]');
      const imageTemplate = root.querySelector(':scope > template[data-kind="image"]');
      const cancel = root.querySelector(':scope > .file-upload-dropzone > .empty-content > .file-upload-cancel');
      const cleanups = [];
      const listen = listener(cleanups);
      const language = () => root.closest('[lang]')?.lang || undefined;
      const maxSize = root.dataset.maxSize ? Number(root.dataset.maxSize) : undefined;

      // The files in the input, and those rejected by the last change. The input's files can only be replaced as a
      // whole, so this keeps the earlier ones for the next pick.
      let files = Array.from(input.files);
      let rejected = [];
      // The upload in progress or last done: its state (uploading, processing, error, done) and each file's share
      // sent. Cleared by the next change. uploaded holds the files of a success the component saw, out of the input.
      let upload;
      let uploaded = [];

      // A history snapshot taken while uploading kept the disabled input and the marker: neither holds any more.
      if (root.dataset.uploading) {
        delete root.dataset.uploading;
        input.disabled = false;
      }

      const announce = parts => {
        const text = parts.filter(Boolean).join(' ');
        if (!text) {
          return;
        }
        // Emptied first, so the same text twice in a row is announced twice.
        status.textContent = '';
        clearTimeout(this.timer);
        this.timer = setTimeout(() => {
          status.textContent = text;
        }, ANNOUNCE_DELAY_MS);
      };

      const names = items => new Intl.ListFormat(language(), {type: 'conjunction'})
          .format(items.map(item => item.name));

      // The previews by file (keyed as same() compares them: the input may hand out other File objects for the same
      // files): the blob: URL, and whether it has loaded. A failed one keeps its entry without a URL, so it is not
      // tried again.
      const previews = new Map();
      const previewKey = file => `${file.name}\u0000${file.size}\u0000${file.lastModified}`;
      const previewed = new WeakMap(); // a file's list item → its key

      const showPreview = (element, url) => {
        const media = imageTemplate.content.firstElementChild.cloneNode(true);
        media.querySelector('img').src = url;
        element.querySelector('.attachment-media').replaceWith(media);
      };

      const preview = (element, file) => {
        if (!imageTemplate) {
          return;
        }
        const key = previewKey(file);
        let entry = previews.get(key);
        if (!entry) {
          const type = (file.type ?? '').toLowerCase();
          if (blobImagesBlocked || !type.startsWith('image/') || type === 'image/svg+xml'
              || file.size > PREVIEW_MAX_SIZE) {
            return;
          }
          entry = {url: URL.createObjectURL(file), loaded: false};
          previews.set(key, entry);
          const probe = new Image();
          probe.decoding = 'async';
          probe.addEventListener('load', () => {
            if (previews.get(key) !== entry) {
              return;
            }
            entry.loaded = true;
            list.querySelectorAll(':scope > .file-upload-item[data-kind="file"]').forEach(other => {
              if (previewed.get(other) === key) {
                showPreview(other, entry.url);
              }
            });
          }, {once: true});
          probe.addEventListener('error', () => {
            if (previews.get(key) !== entry) {
              return;
            }
            URL.revokeObjectURL(entry.url);
            entry.url = undefined;
          }, {once: true});
          probe.src = entry.url;
        }
        previewed.set(element, key);
        if (entry.loaded) {
          showPreview(element, entry.url);
        }
      };

      // Revokes the URLs of the files no longer listed (all of them, with none listed).
      const prunePreviews = (listed = []) => {
        const keys = new Set(listed.map(previewKey));
        for (const [key, entry] of previews) {
          if (!keys.has(key)) {
            if (entry.url) {
              URL.revokeObjectURL(entry.url);
            }
            previews.delete(key);
          }
        }
      };
      cleanups.push(() => prunePreviews());

      const item = (template, file, index, reason) => {
        const element = template.content.firstElementChild.cloneNode(true);
        element.dataset.kind = template.dataset.kind;
        element.dataset.index = String(index);
        element.querySelector('.attachment-title').textContent = file.name;
        element.querySelector('.attachment-description').textContent = reason ?? describe(file, language());
        const action = element.querySelector('.attachment-action');
        action.setAttribute('aria-label', format(action.getAttribute('aria-label'), file.name));
        return element;
      };

      const percent = share => new Intl.NumberFormat(language(), {style: 'percent', maximumFractionDigits: 0})
          .format(Math.floor(share * 100) / 100);

      // Sets the files' attachments to the upload's state, as attachment.html renders it: done is the default (no
      // data-state), uploading and processing are busy.
      const paint = () => {
        const messages = fileTemplate.dataset;
        list.querySelectorAll(':scope > .file-upload-item[data-kind="file"]').forEach(element => {
          const index = Number(element.dataset.index);
          const state = element.dataset.uploaded ? 'done' : upload?.state;
          if (!state) {
            return;
          }
          const attachment = element.querySelector('.attachment');
          if (state === 'done') {
            attachment.removeAttribute('data-state');
          } else {
            attachment.dataset.state = state;
          }
          if (state === 'uploading' || state === 'processing') {
            attachment.setAttribute('aria-busy', 'true');
          } else {
            attachment.removeAttribute('aria-busy');
          }
          const share = upload?.shares?.[index];
          element.querySelector('.attachment-description').textContent = {
            uploading: share === undefined ? messages.messageUploadingIndeterminate
                : format(messages.messageUploading, percent(share)),
            processing: messages.messageProcessing,
            error: messages.messageFailed,
            done: messages.messageDone,
          }[state];
        });
      };

      const render = () => {
        list.replaceChildren(
            ...files.map((file, index) => {
              const element = item(fileTemplate, file, index);
              preview(element, file);
              return element;
            }),
            ...uploaded.map((file, index) => {
              const element = item(fileTemplate, file, index);
              element.dataset.uploaded = 'true';
              element.querySelector('.attachment-actions').remove();
              preview(element, file);
              return element;
            }),
            ...rejected.map(({file, reason}, index) => item(rejectedTemplate, file, index, reason)));
        list.hidden = list.children.length === 0;
        prunePreviews([...files, ...uploaded]);
        paint();
      };

      const write = () => {
        const transfer = new DataTransfer();
        files.forEach(file => transfer.items.add(file));
        input.files = transfer.files;
        // Taken from the input, which may refuse a file: what is listed is what is submitted.
        files = Array.from(input.files);
        input.dispatchEvent(new Event('input', {bubbles: true}));
        input.dispatchEvent(new Event('change', {bubbles: true}));
      };

      const reasonFor = file => {
        if (!accepts(file, input.accept)) {
          return rejectedTemplate.dataset.messageType;
        }
        if (maxSize !== undefined && file.size > maxSize) {
          return format(rejectedTemplate.dataset.messageSize, size(maxSize, language()));
        }
        return undefined;
      };

      // Adds the picked or dropped files to the ones there, as far as they pass.
      const add = (previous, incoming) => {
        const accepted = [];
        rejected = [];
        uploaded = [];
        upload = undefined;
        for (const file of incoming) {
          const reason = reasonFor(file);
          if (reason) {
            rejected.push({file, reason});
          } else if (!input.multiple && accepted.length > 0) {
            rejected.push({file, reason: rejectedTemplate.dataset.messageCount});
          } else if (![...previous, ...accepted].some(other => same(other, file))) {
            accepted.push(file);
          }
        }
        files = input.multiple ? [...previous, ...accepted] : (accepted.length > 0 ? accepted : previous);
        write();
        render();
        const replaced = input.multiple || accepted.length === 0 ? [] : previous.filter(file => !same(file, accepted[0]));
        announce([
          accepted.length > 0 ? format(status.dataset.messageAdded, names(accepted)) : '',
          replaced.length > 0 ? format(status.dataset.messageRemoved, names(replaced)) : '',
          ...groupBy(rejected, entry => entry.reason).map(([reason, entries]) =>
              format(status.dataset.messageRejected, names(entries.map(entry => entry.file)), reason)),
        ]);
      };

      // A pick replaced the input's files. Writing them back dispatches change again: ignored, as it holds our files.
      let writing = false;
      listen(input, 'change', () => {
        if (writing) {
          return;
        }
        const picked = Array.from(input.files);
        // An older browser empties the input when the picker is cancelled: keep what was there.
        writing = true;
        try {
          if (picked.length === 0) {
            if (files.length > 0) {
              write();
            }
            return;
          }
          add(files, picked);
        } finally {
          writing = false;
        }
      });

      // Drop. dragover must be cancelled for the drop to come, and a drop not taken here would open the file in the
      // tab. The counter keeps data-dragging on while the pointer crosses the children.
      let depth = 0;
      const carriesFiles = event => Array.from(event.dataTransfer?.types ?? []).includes('Files');
      listen(root, 'dragenter', event => {
        if (!carriesFiles(event)) {
          return;
        }
        event.preventDefault();
        depth++;
        if (!input.disabled) {
          root.dataset.dragging = 'true';
        }
      });
      listen(root, 'dragover', event => {
        if (!carriesFiles(event)) {
          return;
        }
        event.preventDefault();
        event.dataTransfer.dropEffect = input.disabled ? 'none' : 'copy';
      });
      listen(root, 'dragleave', event => {
        if (!carriesFiles(event)) {
          return;
        }
        depth = Math.max(0, depth - 1);
        if (depth === 0) {
          delete root.dataset.dragging;
        }
      });
      listen(root, 'drop', event => {
        if (!carriesFiles(event)) {
          return;
        }
        event.preventDefault();
        depth = 0;
        delete root.dataset.dragging;
        if (input.disabled) {
          return;
        }
        writing = true;
        try {
          add(files, Array.from(event.dataTransfer.files));
        } finally {
          writing = false;
        }
      });

      listen(list, 'click', event => {
        const action = event.target instanceof Element ? event.target.closest('.attachment-action') : null;
        const entry = action?.closest('.file-upload-item');
        if (!entry || !list.contains(entry)) {
          return;
        }
        const index = Number(entry.dataset.index);
        const actions = Array.from(list.querySelectorAll('.attachment-action'));
        const position = actions.indexOf(action);
        if (entry.dataset.kind === 'rejected') {
          rejected.splice(index, 1);
          render();
        } else {
          upload = undefined;
          const [removed] = files.splice(index, 1);
          writing = true;
          try {
            write();
          } finally {
            writing = false;
          }
          render();
          announce([format(status.dataset.messageRemoved, removed.name)]);
        }
        const remaining = Array.from(list.querySelectorAll('.attachment-action'));
        (remaining[position] ?? remaining[position - 1] ?? input).focus();
      });

      // The back/forward cache keeps the page as it was, but a browser may have put files back into the input.
      listen(window, 'pageshow', () => {
        files = Array.from(input.files);
        rejected = [];
        uploaded = [];
        if (!root.dataset.uploading) {
          upload = undefined;
        }
        render();
      });

      if (imageTemplate) {
        listen(document, 'securitypolicyviolation', event => {
          if (event.blockedURI.startsWith('blob') && event.effectiveDirective.startsWith('img-src')) {
            blobImagesBlocked = true;
          }
        });
        // The snapshot is taken right after this event, as markup: it gets the icons, which the page gets back after
        // it, when the swap that follows has left the component there.
        listen(document, 'htmx:beforeHistorySave', () => {
          const icon = fileTemplate.content.querySelector('.attachment-media');
          list.querySelectorAll(':scope > .file-upload-item > .attachment > .attachment-media[data-variant="image"]')
              .forEach(media => media.replaceWith(icon.cloneNode(true)));
          setTimeout(() => {
            if (root.isConnected) {
              render();
            }
          });
        });
      }

      // A request body carries the files when it holds a file of the input's under its name.
      const carries = body => body instanceof FormData && input.name !== '' && files.length > 0
          && body.getAll(input.name).some(value => value instanceof File && files.some(file => same(file, value)));

      watchUploads(listen, carries, elt => {
        const sending = files.slice();
        const sizes = sending.reduce((sum, file) => sum + file.size, 0);
        upload = {state: 'uploading', shares: undefined, elt};
        rejected = [];
        uploaded = [];
        root.dataset.uploading = 'true';
        input.disabled = true;
        render();
        announce([sending.length === 1
          ? format(status.dataset.messageUploadingOne, sending[0].name)
          : format(status.dataset.messageUploading, new Intl.NumberFormat(language()).format(sending.length))]);

        const finish = () => {
          delete root.dataset.uploading;
          input.disabled = false;
          // The Cancel button is hidden again: a focus on it goes back to the input.
          if (document.activeElement === cancel) {
            input.focus();
          }
        };
        return {
          // The bytes sent, mapped onto the files in their order: the share of the body that is files' bytes is
          // taken as spread evenly, which is near enough, as the boundaries and the other fields are small.
          progress(loaded, total) {
            if (!total || sizes === 0) {
              upload.shares = undefined;
            } else {
              let offset = 0;
              const sent = loaded * sizes / total;
              upload.shares = sending.map(file => {
                const share = file.size === 0 ? (sent >= offset ? 1 : 0)
                    : Math.min(1, Math.max(0, (sent - offset) / file.size));
                offset += file.size;
                return share;
              });
            }
            paint();
          },
          sent() {
            upload.state = 'processing';
            paint();
          },
          end(how) {
            finish();
            if (!root.isConnected) {
              return;
            }
            if (how === 'done') {
              // Out of the input without input or change: hx-trigger="change" would send the empty input.
              uploaded = sending;
              files = [];
              input.value = '';
              upload = undefined;
              render();
              announce([sending.length === 1
                ? format(status.dataset.messageUploadedOne, sending[0].name)
                : format(status.dataset.messageUploaded, new Intl.NumberFormat(language()).format(sending.length))]);
            } else if (how === 'error') {
              upload = {state: 'error'};
              render();
              announce([status.dataset.messageFailed]);
            } else {
              upload = undefined;
              render();
              announce([status.dataset.messageCancelled]);
            }
          },
        };
      });

      // An application's copy of the template from before the Cancel button has none.
      if (cancel) {
        listen(cancel, 'click', () => {
          if (upload?.elt) {
            abortRequest(upload.elt);
          }
        });
      }

      render();
      this.cleanups = cleanups;
    },

    destroy() {
      clearTimeout(this.timer);
      this.cleanups.forEach(cleanup => cleanup());
    },
  };
}

/** Fills in {0}, {1}, ... as the server left them in a message. */
function format(pattern, ...values) {
  return (pattern ?? '').replace(/\{(\d+)\}/g, (match, index) => values[Number(index)] ?? match);
}

function same(a, b) {
  return a.name === b.name && a.size === b.size && a.lastModified === b.lastModified;
}

/** Whether a file matches an accept attribute: extensions, MIME types and type/* wildcards, as the picker reads it. */
function accepts(file, accept) {
  const tokens = (accept ?? '').split(',').map(token => token.trim().toLowerCase()).filter(Boolean);
  if (tokens.length === 0) {
    return true;
  }
  const name = file.name.toLowerCase();
  const type = (file.type ?? '').toLowerCase();
  return tokens.some(token => {
    if (token.startsWith('.')) {
      return name.endsWith(token);
    }
    if (token.endsWith('/*')) {
      return type.startsWith(token.slice(0, -1));
    }
    return type === token;
  });
}

/** "PDF · 2.4 MB": the extension, when there is one, and the size. */
function describe(file, language) {
  const dot = file.name.lastIndexOf('.');
  const extension = dot > 0 && dot < file.name.length - 1 ? file.name.slice(dot + 1).toUpperCase() : '';
  return extension ? `${extension} · ${size(file.size, language)}` : size(file.size, language);
}

/** A size in the page's language, in binary units as Spring reads max-size: 10MB is 10 × 1024 × 1024 bytes. */
function size(bytes, language) {
  let value = bytes;
  let unit = 0;
  while (value >= 1024 && unit < UNITS.length - 1) {
    value /= 1024;
    unit++;
  }
  return new Intl.NumberFormat(language, {
    style: 'unit',
    unit: UNITS[unit],
    unitDisplay: unit === 0 ? 'long' : 'short',
    maximumFractionDigits: unit === 0 ? 0 : 1,
  }).format(value);
}

function groupBy(items, key) {
  const groups = new Map();
  for (const item of items) {
    const value = key(item);
    groups.set(value, [...(groups.get(value) ?? []), item]);
  }
  return Array.from(groups);
}
