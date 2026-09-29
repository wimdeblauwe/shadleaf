// shadleaf.assets.alpine=csp: Alpine's CSP build, which needs no 'unsafe-eval', with the registrations.
import Alpine from '@alpinejs/csp';
import {startBundled} from '../register.js';

startBundled(Alpine);
