import {ng} from "entcore";
import {ParameterService} from "./services";
import {adminParameterController} from "./controllers/admin-parameter"
import * as services from './services';
import {cFilAriane} from "./utils/directives/cFilAriane";
import {navigable} from "./utils/directives/navigable";
import {navigatable} from "./utils/directives/navigatable";
import {navigableCompetences} from "./directives/cNavigableCompetences";
import {tabs} from "./utils/directives/tabs";
import {pane} from "./utils/directives/pane";
import {cSkillNoteDevoir} from "./directives/cSkillNoteDevoir";
import {cSkillsColorColumn} from "./directives/cSkillsColorColumn";
import {cSkillsColorPage} from "./directives/cSkillsColorPage";
import {cSkillsList} from "./directives/cSkillsList";
import {autofocus} from "./utils/directives/autofocus";
import {sticky} from "./utils/directives/sticky";
import {proportionSuiviCompetence} from "./directives/ProportionSuiviCompetence";
import {rzslider} from "./utils/directives/slider";
import {structureLoader} from "./utils/directives/structureLoading";
import {messageLoader} from "./utils/directives/messageLoading";
import {inputTextList} from "./directives/inputTextList";
import {teachingsSkills} from "./directives/teachingsSkills";
import {cSkillsBubble} from "./directives/cSkillsBubble";
ng.addRequiredModule('chart.js');
ng.services.push(ParameterService);
ng.controllers.push(adminParameterController);

ng.directives.push(cFilAriane);
ng.directives.push(navigable);
ng.directives.push(navigatable);
ng.directives.push(navigableCompetences);
ng.directives.push(tabs);
ng.directives.push(pane);
ng.directives.push(cSkillNoteDevoir);
ng.directives.push(cSkillsColorColumn);
ng.directives.push(cSkillsColorPage);
ng.directives.push(cSkillsList);
ng.directives.push(autofocus);
ng.directives.push(sticky);
ng.directives.push(proportionSuiviCompetence);
ng.directives.push(rzslider);
ng.directives.push(structureLoader);
ng.directives.push(messageLoader);
ng.directives.push(inputTextList);
ng.directives.push(teachingsSkills);
ng.directives.push(cSkillsBubble);