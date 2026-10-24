// shadleaf.assets.alpine=external: the registrations only, for the Alpine the application loads itself.
import {watchAvatarImages} from '../avatar.js';
import {watchIndeterminateCheckboxes} from '../checkbox.js';
import {watchInputGroupAddons} from '../input-group.js';
import {registerWithApplicationAlpine} from '../register.js';

watchAvatarImages();
watchIndeterminateCheckboxes();
watchInputGroupAddons();
registerWithApplicationAlpine();
