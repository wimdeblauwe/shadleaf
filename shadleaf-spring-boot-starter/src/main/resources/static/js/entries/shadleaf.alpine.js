// shadleaf.assets.alpine=bundled: Alpine's standard build with the registrations.
import Alpine from 'alpinejs';
import {watchAvatarImages} from '../avatar.js';
import {startBundled} from '../register.js';

watchAvatarImages();
startBundled(Alpine);
