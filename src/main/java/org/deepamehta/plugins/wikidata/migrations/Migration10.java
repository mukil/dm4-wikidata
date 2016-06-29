package org.deepamehta.plugins.wikidata.migrations;

import de.deepamehta.core.DeepaMehtaType;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.Migration;
import de.deepamehta.workspaces.WorkspacesService;

import java.util.logging.Logger;


/*
 * Assigning new "Common Media" related <code>Topic Type</code> to the "Wikidata" workspace.
 * Adjusting type definition of <code>Wikidata Commons Media</code> Topic Type.
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
        TopicType media_name = dm4.getTopicType(WD_COMMONS_MEDIA_NAME_TYPE_URI);
        assignTypeWorkspace(media_name);
        TopicType media_path = dm4.getTopicType(WD_COMMONS_MEDIA_PATH_TYPE_URI);
        assignTypeWorkspace(media_path);
        TopicType media_type = dm4.getTopicType(WD_COMMONS_MEDIA_TYPE_TYPE_URI);
        assignTypeWorkspace(media_type);
        TopicType media_descr = dm4.getTopicType(WD_COMMONS_MEDIA_DESCR_TYPE_URI);
        assignTypeWorkspace(media_descr);
        TopicType license_name = dm4.getTopicType(WD_COMMONS_AUTHOR_HTML_URI);
        assignTypeWorkspace(license_name);
        TopicType license_info = dm4.getTopicType(WD_COMMONS_LICENSE_HTML_URI);
        assignTypeWorkspace(license_info);
        log.info("1) Assigned alle \"Wikimedia Commons ***\" child-types to \"Wikidata\"-Workspace");
        // 2) Remove (old, unusable) File Association Definion from \"Wikimedia Commons Media\"-Type
        TopicType commonsMedia = dm4.getTopicType(WD_COMMONS_MEDIA_TYPE_URI);
        commonsMedia.removeAssocDef("dm4.files.file");
        // 3) Make all new \"Wikimedia Commons ***\" types part of each \"Wikimedia Commons Media\"-Type
        commonsMedia.addAssocDef(mf.newAssociationDefinitionModel("dm4.core.composition_def",
                commonsMedia.getUri(), WD_COMMONS_MEDIA_NAME_TYPE_URI, "dm4.core.one", "dm4.core.one"));
        commonsMedia.addAssocDef(mf.newAssociationDefinitionModel("dm4.core.composition_def",
                commonsMedia.getUri(), WD_COMMONS_MEDIA_PATH_TYPE_URI, "dm4.core.one", "dm4.core.one"));
        commonsMedia.addAssocDef(mf.newAssociationDefinitionModel("dm4.core.composition_def",
                commonsMedia.getUri(), WD_COMMONS_MEDIA_TYPE_TYPE_URI, "dm4.core.one", "dm4.core.one"));
        commonsMedia.addAssocDef(mf.newAssociationDefinitionModel("dm4.core.composition_def",
                commonsMedia.getUri(), WD_COMMONS_MEDIA_DESCR_TYPE_URI, "dm4.core.one", "dm4.core.one"));
        commonsMedia.addAssocDef(mf.newAssociationDefinitionModel("dm4.core.composition_def",
                commonsMedia.getUri(), WD_COMMONS_AUTHOR_HTML_URI, "dm4.core.one", "dm4.core.one"));
        commonsMedia.addAssocDef(mf.newAssociationDefinitionModel("dm4.core.composition_def",
                commonsMedia.getUri(), WD_COMMONS_LICENSE_HTML_URI, "dm4.core.one", "dm4.core.one"));
        log.info("2) Assigned all \"Wikimedia Commons ***\" child-types to \"Wikimedia Commons Media\"");
    }

    // === Workspace ===


    private void assignTypeWorkspace(DeepaMehtaType topic) {
        wsService.assignTypeToWorkspace(topic, workspaceId);
    }

}
