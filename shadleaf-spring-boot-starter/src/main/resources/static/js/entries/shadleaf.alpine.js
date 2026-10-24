// shadleaf.assets.alpine=bundled: Alpine's standard build with the registrations.
import Alpine from 'alpinejs';
import {watchAvatarImages} from '../avatar.js';
import {watchCharts} from '../chart/loader.js';
import {watchIndeterminateCheckboxes} from '../checkbox.js';
import {watchInputGroupAddons} from '../input-group.js';
import {startBundled} from '../register.js';

watchAvatarImages();
watchCharts();
watchIndeterminateCheckboxes();
watchInputGroupAddons();
startBundled(Alpine);
