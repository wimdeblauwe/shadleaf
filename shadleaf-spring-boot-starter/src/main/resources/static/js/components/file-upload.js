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
  The messages arrive with a plain {0} (and {1}) in them, which this fills in.
*/
import {listener} from '../popup.js';

const ANNOUNCE_DELAY_MS = 100;
const UNITS = ['byte', 'kilobyte', 'megabyte', 'gigabyte', 'terabyte'];

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
      const cleanups = [];
      const listen = listener(cleanups);
      const language = () => root.closest('[lang]')?.lang || undefined;
      const maxSize = root.dataset.maxSize ? Number(root.dataset.maxSize) : undefined;

      // The files in the input, and those rejected by the last change. The input's files can only be replaced as a
      // whole, so this keeps the earlier ones for the next pick.
      let files = Array.from(input.files);
      let rejected = [];

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

      const render = () => {
        list.replaceChildren(
            ...files.map((file, index) => item(fileTemplate, file, index)),
            ...rejected.map(({file, reason}, index) => item(rejectedTemplate, file, index, reason)));
        list.hidden = list.children.length === 0;
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
        render();
      });

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
