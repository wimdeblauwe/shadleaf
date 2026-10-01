/*
  sl:theme-toggle. The choice is stored where the theme script (shadleaf/theme-script.js) reads it before the first
  paint: localStorage 'shadleaf-theme', the plain string 'light' or 'dark' (not Alpine's $persist, which writes JSON),
  and no key at all for the system's theme. Choosing:
  - stores the choice and sets or removes the dark class on <html>, as the theme script does;
  - fires sl-theme-change on the document ({theme: 'light' | 'dark' | 'system', dark}), which every toggle on the page
    listens to, and which an application can use too (a chart that redraws).
  The class follows the system's theme while the choice is System, and the choice made in another tab (the storage
  event) or while the page was in the back/forward cache. Those listeners are added once, by the first toggle.

  Each toggle shows the state: aria-checked on the menu's items (the stored choice), aria-pressed on the button (whether
  the page is dark). The server cannot know either, so this sets them when it starts, and again after an htmx swap or a
  history restore, which start it anew. It never changes the theme when it starts: the page is already in the stored
  one, and a page that sets the class itself (the docs' previews) keeps it.
*/
const STORAGE_KEY = 'shadleaf-theme';
const CHANGE_EVENT = 'sl-theme-change';

const systemDark = window.matchMedia('(prefers-color-scheme: dark)');
let watching = false;

function storedTheme() {
  try {
    const theme = localStorage.getItem(STORAGE_KEY);
    return theme === 'light' || theme === 'dark' ? theme : 'system';
  } catch (error) {
    return 'system';
  }
}

function store(theme) {
  try {
    if (theme === 'system') {
      localStorage.removeItem(STORAGE_KEY);
    } else {
      localStorage.setItem(STORAGE_KEY, theme);
    }
  } catch (error) {
    // Storage blocked: the choice holds for this page only.
  }
}

function isDark() {
  return document.documentElement.classList.contains('dark');
}

function apply(theme) {
  const dark = theme === 'dark' || (theme !== 'light' && systemDark.matches);
  document.documentElement.classList.toggle('dark', dark);
  document.dispatchEvent(new CustomEvent(CHANGE_EVENT, {detail: {theme, dark}}));
}

function choose(theme) {
  store(theme);
  apply(theme);
}

function watch() {
  if (watching) {
    return;
  }
  watching = true;
  systemDark.addEventListener('change', () => {
    if (storedTheme() === 'system') {
      apply('system');
    }
  });
  window.addEventListener('storage', event => {
    // key is null when another tab cleared the whole storage.
    if (event.key === STORAGE_KEY || event.key === null) {
      apply(storedTheme());
    }
  });
  window.addEventListener('pageshow', event => {
    if (event.persisted) {
      apply(storedTheme());
    }
  });
}

export default function themeToggle() {
  return {
    init() {
      watch();
      const root = this.$el;
      const update = () => {
        const theme = storedTheme();
        root.querySelectorAll('[data-theme-choice]').forEach(item =>
            item.setAttribute('aria-checked', String(item.dataset.themeChoice === theme)));
        root.querySelectorAll('.theme-toggle-button').forEach(button =>
            button.setAttribute('aria-pressed', String(isDark())));
      };
      const click = event => {
        const item = event.target.closest('[data-theme-choice]');
        if (item && root.contains(item)) {
          choose(item.dataset.themeChoice);
        } else if (event.target.closest('.theme-toggle-button')) {
          choose(isDark() ? 'light' : 'dark');
        }
      };
      document.addEventListener(CHANGE_EVENT, update);
      root.addEventListener('click', click);
      this.cleanups = [
        () => document.removeEventListener(CHANGE_EVENT, update),
        () => root.removeEventListener('click', click),
      ];
      update();
    },

    destroy() {
      this.cleanups?.forEach(cleanup => cleanup());
    },
  };
}
