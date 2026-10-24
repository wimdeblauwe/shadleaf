// shadleaf.assets.alpine=external: the registrations only, for the Alpine the application loads itself.
import {watchAvatarImages} from '../avatar.js';
import {watchCharts} from '../chart/loader.js';
import {watchIndeterminateCheckboxes} from '../checkbox.js';
import {watchInputGroupAddons} from '../input-group.js';
import {registerWithApplicationAlpine} from '../register.js';

watchAvatarImages();
watchCharts();
watchIndeterminateCheckboxes();
watchInputGroupAddons();
registerWithApplicationAlpine();
