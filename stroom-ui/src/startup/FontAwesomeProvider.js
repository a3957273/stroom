import { lifecycle } from 'recompose';
import { library } from '@fortawesome/fontawesome-svg-core';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import {
  faFolder,
  faAngleRight,
  faAngleUp,
  faAngleDoubleDown,
  faArrowLeft,
  faSearch,
  faHistory,
  faExclamationCircle,
  faExclamationTriangle,
  faBars,
  faCircle,
  faTrash,
  faTasks,
  faUsers,
  faUser,
  faHome,
  faKey,
  faCaretDown,
  faCaretRight,
  faInfoCircle,
  faBomb,
  faFile,
  faDatabase,
  faPlay,
  faQuestionCircle,
  faPlus,
  faTimes,
  faCogs,
  faQuestion,
} from '@fortawesome/free-solid-svg-icons';

export default lifecycle({
  componentWillMount() {
    library.add(
      faFolder,
      faAngleRight,
      faAngleUp,
      faAngleDoubleDown,
      faArrowLeft,
      faSearch,
      faHistory,
      faExclamationCircle,
      faExclamationTriangle,
      faBars,
      faPlus,
      faCircle,
      faTimes,
      faCogs,
      faTrash,
      faTasks,
      faUsers,
      faUser,
      faHome,
      faKey,
      faCaretDown,
      faCaretRight,
      faInfoCircle,
      faBomb,
      faFile,
      faDatabase,
      faPlay,
      faQuestionCircle,
      faQuestion,
    );
  },
});
