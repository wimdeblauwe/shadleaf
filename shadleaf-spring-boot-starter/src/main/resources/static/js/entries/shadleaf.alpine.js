// shadleaf.assets.alpine=bundled: Alpine's standard build with the registrations.
import Alpine from 'alpinejs';
import {watchAvatarImages} from '../avatar.js';
import {watchIndeterminateCheckboxes} from '../checkbox.js';
import {startBundled} from '../register.js';

watchAvatarImages();
watchIndeterminateCheckboxes();
startBundled(Alpine);
