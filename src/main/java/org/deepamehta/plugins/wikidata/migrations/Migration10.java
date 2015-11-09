package org.deepamehta.plugins.wikidata.migrations;

import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.Type;
import de.deepamehta.core.model.*;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.Migration;
import de.deepamehta.plugins.workspaces.WorkspacesService;

import java.util.logging.Logger;


/*
 * A very basic plugin for searching and turning wikidata-properties into association-types.
 *
 * @author Malte Reißig (<malte@mikromedia.de>)
 * @website https://github.com/mukil/dm4-wikidata
 */

public class Migration10 extends Migration {

    private Logger log = Logger.getLogger(getClass().getName());

    private final String WS_WIKIDATA_URI = "org.deepamehta.workspaces.wikidata";

    private final String WD_COMMONS_MEDIA_TYPE_URI = "org.deepamehta.wikidata.commons_media";
    private final String WD_COMMONS_MEDIA_NAME_TYPE_URI = "org.deepamehta.wikidata.commons_media_name";
    private final String WD_COMMONS_MEDIA_PATH_TYPE_URI = "org.deepamehta.wikidata.commons_media_path";
    private final String WD_COMMONS_MEDIA_TYPE_TYPE_URI = "org.deepamehta.wikidata.commons_media_type";
    private final String WD_COMMONS_MEDIA_DESCR_TYPE_URI = "org.deepamehta.wikidata.commons_media_descr";
    private final String WD_COMMONS_AUTHOR_HTML_URI = "org.deepamehta.wikidata.commons_author_html";
    private final String WD_COMMONS_LICENSE_HTML_URI = "org.deepamehta.wikidata.commons_license_html";

    private long workspaceId = 0;

    @Inject
    private WorkspacesService wsService = null;

    @Override
    public void run() {

        // 0) Initialize wikidata workspace id
        workspaceId = wsService.getWorkspace(WS_WIKIDATA_URI).getId();
        // 1) Assign all new wikidata types to the \"Wikidata\"-Workspace
        TopicType media_name = dms.getTopicType(WD_COMMONS_MEDIA_NAME_TYPE_URI);
        assignTypeWorkspace(media_name);
        TopicType media_path = dms.getTopicType(WD_COMMONS_MEDIA_PATH_TYPE_URI);
        assignTypeWorkspace(media_path);
        TopicType media_type = dms.getTopicType(WD_COMMONS_MEDIA_TYPE_TYPE_URI);
        assignTypeWorkspace(media_type);
        TopicType media_descr = dms.getTopicType(WD_COMMONS_MEDIA_DESCR_TYPE_URI);
        assignTypeWorkspace(media_descr);
        TopicType license_name = dms.getTopicType(WD_COMMONS_AUTHOR_HTML_URI);
        assignTypeWorkspace(license_name);
        TopicType license_info = dms.getTopicType(WD_COMMONS_LICENSE_HTML_URI);
        assignTypeWorkspace(license_info);
        log.info("1) Assigned alle \"Wikimedia Commons ***\" child-types to \"Wikidata\"-Workspace");
        // 2) Remove (old, unusable) File Association Definion from \"Wikimedia Commons Media\"-Type
        TopicType commonsMedia = dms.getTopicType(WD_COMMONS_MEDIA_TYPE_URI);
        commonsMedia.removeAssocDef("dm4.files.file");
        // 3) Make all new \"Wikimedia Commons ***\" types part of each \"Wikimedia Commons Media\"-Type
        commonsMedia.addAssocDef(new AssociationDefinitionModel("dm4.core.composition_def",
                commonsMedia.getUri(), WD_COMMONS_MEDIA_NAME_TYPE_URI, "dm4.core.one", "dm4.core.one"));
        commonsMedia.addAssocDef(new AssociationDefinitionModel("dm4.core.composition_def",
                commonsMedia.getUri(), WD_COMMONS_MEDIA_PATH_TYPE_URI, "dm4.core.one", "dm4.core.one"));
        commonsMedia.addAssocDef(new AssociationDefinitionModel("dm4.core.composition_def",
                commonsMedia.getUri(), WD_COMMONS_MEDIA_TYPE_TYPE_URI, "dm4.core.one", "dm4.core.one"));
        commonsMedia.addAssocDef(new AssociationDefinitionModel("dm4.core.composition_def",
                commonsMedia.getUri(), WD_COMMONS_MEDIA_DESCR_TYPE_URI, "dm4.core.one", "dm4.core.one"));
        commonsMedia.addAssocDef(new AssociationDefinitionModel("dm4.core.composition_def",
                commonsMedia.getUri(), WD_COMMONS_AUTHOR_HTML_URI, "dm4.core.one", "dm4.core.one"));
        commonsMedia.addAssocDef(new AssociationDefinitionModel("dm4.core.composition_def",
                commonsMedia.getUri(), WD_COMMONS_LICENSE_HTML_URI, "dm4.core.one", "dm4.core.one"));
        log.info("2) Assigned all \"Wikimedia Commons ***\" child-types to \"Wikimedia Commons Media\"");
    }

    // === Workspace ===


    private void assignTypeWorkspace(Type topic) {
        wsService.assignTypeToWorkspace(topic, workspaceId);
    }

}
