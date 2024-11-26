/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2024 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package com.adobe.aem.core.utils;

import java.util.Arrays;
import java.util.List;

public class AdaptiveFormConstants {

    public static final String GUIDE_CONTAINER = "guideContainer";
    public static final String GUIDE_CONTAINER_WRAPPER = "guideContainerWrapper";
    public static final String GUIDE_FRAGMENT_CONTAINER = "guideFragmentContainer";
    public static final String FOUNDATION_COMPONENTS_PREFIX = "fd/af/components/";
    public static final String FOUNDATION_LAYOUTS_PREFIX = "fd/af/layouts/panel/";
    public static final String GUIDE_CONTAINER_RESOURCE_TYPE = FOUNDATION_COMPONENTS_PREFIX + GUIDE_CONTAINER;
    public static final String GUIDE_CONTAINER_WRAPPER_RESOURCE_TYPE = FOUNDATION_COMPONENTS_PREFIX + GUIDE_CONTAINER_WRAPPER;
    public static final String GUIDE_FRAGMENT_CONTAINER_RESOURCE_TYPE = FOUNDATION_COMPONENTS_PREFIX + GUIDE_FRAGMENT_CONTAINER;
    public static final String PANEL = "panel";
    public static final String ROOT_PANEL = FOUNDATION_COMPONENTS_PREFIX + "rootPanel";
    public static final String GUIDE_PANEL = FOUNDATION_COMPONENTS_PREFIX + PANEL;
    public static final String GUIDE_NUMERIC_BOX = FOUNDATION_COMPONENTS_PREFIX + "guidenumericbox";
    public static final String AF_FORM_TITLE = FOUNDATION_COMPONENTS_PREFIX + "afFormTitle";
    public static final String GUIDE_TEXT_BOX = FOUNDATION_COMPONENTS_PREFIX + "guidetextbox";
    public static final String GUIDE_TEXT_DRAW = FOUNDATION_COMPONENTS_PREFIX + "guidetextdraw";
    public static final String GUIDE_EMAIL = FOUNDATION_COMPONENTS_PREFIX + "guideemail";
    public static final String GUIDE_CHECK_BOX = FOUNDATION_COMPONENTS_PREFIX + "guidecheckbox";
    public static final String GUIDE_DROPDOWN_LIST = FOUNDATION_COMPONENTS_PREFIX + "guidedropdownlist";
    public static final String GUIDE_RADIO_BUTTON = FOUNDATION_COMPONENTS_PREFIX + "guideradiobutton";
    public static final String GUIDE_SWITCH = FOUNDATION_COMPONENTS_PREFIX + "guideswitch";
    public static final String GUIDE_BUTTON = FOUNDATION_COMPONENTS_PREFIX + "guidebutton";
    public static final String GUIDE_TABLE = FOUNDATION_COMPONENTS_PREFIX + "table";
    public static final String GUIDE_DATE_INPUT = FOUNDATION_COMPONENTS_PREFIX + "guidedateinput";
    public static final String GUIDE_NUMERIC_STEPPER = FOUNDATION_COMPONENTS_PREFIX + "guidenumericstepper";
    public static final String GUIDE_PASSWORD_BOX = FOUNDATION_COMPONENTS_PREFIX + "guidepasswordbox";
    public static final String GUIDE_DATE_PICKER = FOUNDATION_COMPONENTS_PREFIX + "guidedatepicker";
    public static final String GUIDE_TELEPHONE = FOUNDATION_COMPONENTS_PREFIX + "guidetelephone";
    public static final String GUIDE_IMAGE = FOUNDATION_COMPONENTS_PREFIX + "guideimage";
    public static final String GUIDE_CAPTCHA = FOUNDATION_COMPONENTS_PREFIX + "guideCaptcha";
    public static final String GUIDE_FILE_UPLOAD = FOUNDATION_COMPONENTS_PREFIX + "guidefileupload";
    public static final String GUIDE_HEADER = FOUNDATION_COMPONENTS_PREFIX + "guideheader";
    public static final String GUIDE_FOOTER = FOUNDATION_COMPONENTS_PREFIX + "guidefooter";
    public static final String GUIDE_TERMS_AND_CONDITIONS = FOUNDATION_COMPONENTS_PREFIX + "guidetermsandconditions";
    public static final String GUIDE_SEPARATOR = FOUNDATION_COMPONENTS_PREFIX + "guideseparator";
    public static final String SUBMIT_ACTION = FOUNDATION_COMPONENTS_PREFIX + "actions/submit";
    public static final String RESET_ACTION = FOUNDATION_COMPONENTS_PREFIX + "actions/reset";
    public static final String WIZARD = "wizard";
    public static final String VERTICAL_TAB_LAYOUT = "verticalTabbedPanelLayout";
    public static final String TABS_ON_TOP_LAYOUT = "tabbedPanelLayout";
    public static final String ACCORDION = "accordion";
    public static final String WIZARD_RESOURCE_TYPE = FOUNDATION_LAYOUTS_PREFIX + WIZARD;
    public static final String VERTICAL_TABS_RESOURCE_TYPE = FOUNDATION_LAYOUTS_PREFIX + VERTICAL_TAB_LAYOUT;
    public static final String TABS_ON_TOP_RESOURCE_TYPE = FOUNDATION_LAYOUTS_PREFIX + TABS_ON_TOP_LAYOUT;
    public static final String ACCORDION_RESOURCE_TYPE = FOUNDATION_LAYOUTS_PREFIX + ACCORDION;
    public static final String ITEMS = "items";
    public static final String TEMP_CONTAINER_NODE = "container";
    public static final String LAYOUT_PROPERTY = "layout";
    public static final String FRAGMENT_PATH = "fragmentPath";
    public static final String FRAGMENT_REF = "fragRef";
    public static final String TEMPLATE_REF = "templateRef";
    public static final String V2_COMPONENT_TEMP_PATH = "v2Path";
    public static final String BIND_REF = "bindRef";
    public static final String DATA_REF = "dataRef";
    public static final String REPEATABLE = "repeatable";
    public static final String MIN_OCCUR = "minOccur";
    public static final String MAX_OCCUR = "maxOccur";
    public static final String FIELD_TYPE = "fieldType";
    public static final String FD_VERSION = "fd:version";
    public static final String THEME_REF = "themeRef";
    public static final String FD_TYPE = "fd:type";
    public static final String FD_RULES = "fd:rules";
    public static final String FD_EVENTS = "fd:events";
    public static final String CQ_TEMPLATE = "cq:template";
    public static final String METADATA = "metadata";
    public static final String GUIDE_NODE_CLASS = "guideNodeClass";
    public static final String GUIDE_CSS = "guideCss";
    public static final String VIEW = "view";
    public static final String HIDE_TITLE = "hideTitle";
    public static final String FD_VIEW = "fd:view";
    public static final String FIELD_TYPE_FORM = "form";
    public static final String TYPE = "type";
    public static final String FIELD_TYPE_FRAGMENT = "fragment";
    public static final String CORE_COMPONENT_VERSION = "2.1";
    public static final String CANVAS_THEME_PATH = "/libs/fd/af/themes/canvas";
    public static final String JCR_CONTENT_GUIDE_CONTAINER_PATH = "/jcr:content/guideContainer";
    public static final String FORMS_PAGE_PREFIX = "/content/forms/af";
    public static final String FORMS_ASSET_PREFIX = "/content/dam/formsanddocuments";
    public static final String IS_MIGRATED = "isMigrated";
    public static final String JCR_TITLE = "jcr:title";
    public static final String JCR_DESCRIPTION = "jcr:description";
    public static final String NAME = "name";
    public static final String ACTION_SUBMIT = "submit";
    public static final String ACTION_RESET = "reset";
    public static final String ENUM = "enum";
    public static final String ENUM_NAMES = "enumNames";
    public static final String OPTIONS = "options";
    public static final String _VALUE = "_value";
    public static final String DEFAULT = "default";
    public static final String MINIMUM = "minimum";
    public static final String MAXIMUM = "maximum";
    public static final String MINIMUM_DATE = "minimumDate";
    public static final String MAXIMUM_DATE = "maximumDate";
    public static final String MANDATORY = "mandatory";
    public static final String REQUIRED = "required";
    public static final String MANDATORY_MESSAGE = "mandatoryMessage";
    public static final String VALIDATE_EXP_MESSAGE = "validateExpMessage";
    public static final String REVIEW_STATUS = "reviewStatus";
    public static final String IS_DISPLAY_SAME_AS_VALIDATE = "displayIsSameAsValidate";
    public static final String DISPLAY_PICTURE_CLAUSE = "displayPictureClause";
    public static final String VALIDATE_PICTURE_CLAUSE = "validatePictureClause";
    public static final String VALIDATION_PATTERN_TYPE = "validationPatternType";
    public static final String ALIGNMENT = "alignment";
    public static final String OLD_VERTICAL_ALIGNMENT = "guideFieldVerticalAlignment";
    public static final String OLD_HORIZONTAL_ALIGNMENT = "guideFieldHorizontalAlignment";
    public static final String NEW_VERTICAL_ALIGNMENT = "vertical";
    public static final String NEW_HORIZONTAL_ALIGNMENT = "horizontal";
    public static final String CUSTOM = "custom";
    public static final String CQ_RESPONSIVE = "cq:responsive";
    public static final String OFFSET = "offset";
    public static final String WIDTH = "width";
    public static final String RESPONSIVE_GRID = "responsiveGrid";
    public static final String V1_FRAGMENT_TEMPLATE_REF = "/libs/fd/af/templateForFragment/defaultFragmentTemplate";
    public static final String DEFAULT_COMPONENT_PATH_PREFIX = "forms-components-examples/components/form/";
    public static final String CORE_FORM_TEMPLATE_PATH = "${formTemplatePath}";
    public static final String CORE_FRAGMENT_TEMPLATE_PATH = "${fragmentTemplatePath}";
    public static final String APP_ID = "${appId}";
    public static final String COMPONENT_PATH_PREFIX = (APP_ID.contains("${")) ? DEFAULT_COMPONENT_PATH_PREFIX : APP_ID + "/components/adaptiveForm/";
    public static final String CORE_PAGE_RESOURCE_TYPE = DEFAULT_COMPONENT_PATH_PREFIX.equals(COMPONENT_PATH_PREFIX) ? "forms-components-examples/components/page" : COMPONENT_PATH_PREFIX + "page";
    public static final String CORE_FORM_CONTAINER_RESOURCE_TYPE = DEFAULT_COMPONENT_PATH_PREFIX.equals(COMPONENT_PATH_PREFIX) ? COMPONENT_PATH_PREFIX + "container" : COMPONENT_PATH_PREFIX + "formcontainer";
    public static final String CORE_FRAGMENT_CONTAINER_RESOURCE_TYPE = COMPONENT_PATH_PREFIX + "fragmentcontainer";
    public static final String CORE_PANEL_RESOURCE_TYPE = COMPONENT_PATH_PREFIX + "panelcontainer";
    public static final String CORE_FRAGMENT_RESOURCE_TYPE = COMPONENT_PATH_PREFIX + "fragment";
    public static final String CORE_WIZARD_RESOURCE_TYPE = COMPONENT_PATH_PREFIX + "wizard";
    public static final String CORE_TABS_ON_TOP_RESOURCE_TYPE = COMPONENT_PATH_PREFIX + "tabsontop";
    public static final String CORE_VERTICAL_TABS_RESOURCE_TYPE = COMPONENT_PATH_PREFIX + "verticaltabs";
    public static final String CORE_ACCORDION_RESOURCE_TYPE = COMPONENT_PATH_PREFIX + "accordion";
    public static final String CORE_SUBMIT_ACTION_RESOURCE_TYPE = COMPONENT_PATH_PREFIX + "actions/submit";
    public static final String CORE_RESET_ACTION_RESOURCE_TYPE = COMPONENT_PATH_PREFIX + "actions/reset";
    public final static List<String> GRID_LAYOUTS = Arrays.asList("fd/af/layouts/gridFluidLayout", "fd/af/layouts/gridFluidLayout2", "fd/af/layouts/defaultGuideLayout");
    public final static List<String> PANEL_NODES_TO_IGNORE = Arrays.asList("layout", "items");
    public final static List<String> CONTAINER_NODES_TO_IGNORE = Arrays.asList("parsys1", "parsys2", "layout", "rootPanel");
}
