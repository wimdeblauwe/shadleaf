// shadleaf.assets.alpine=csp: Alpine's CSP build, which needs no 'unsafe-eval', with the registrations.
import Alpine from '@alpinejs/csp';
import {watchAvatarImages} from '../avatar.js';
import {watchIndeterminateCheckboxes} from '../checkbox.js';
import {watchInputGroupAddons} from '../input-group.js';
import {startBundled} from '../register.js';

watchAvatarImages();
watchIndeterminateCheckboxes();
watchInputGroupAddons();
startBundled(Alpine);
