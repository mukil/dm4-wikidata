package org.deepamehta.plugins.wikidata.migrations;

import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.*;
import de.deepamehta.core.service.Migration;
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

    @Override
    public void run() {

        // 1) Assign all new wikidata types to the \"Wikidata\"-Workspace
        TopicType media_name = dms.getTopicType(WD_COMMONS_MEDIA_NAME_TYPE_URI);
        assignWorkspace(media_name);
        TopicType media_path = dms.getTopicType(WD_COMMONS_MEDIA_PATH_TYPE_URI);
        assignWorkspace(media_path);
        TopicType media_type = dms.getTopicType(WD_COMMONS_MEDIA_TYPE_TYPE_URI);
        assignWorkspace(media_type);
        TopicType media_descr = dms.getTopicType(WD_COMMONS_MEDIA_DESCR_TYPE_URI);
        assignWorkspace(media_descr);
        TopicType license_name = dms.getTopicType(WD_COMMONS_AUTHOR_HTML_URI);
        assignWorkspace(license_name);
        TopicType license_info = dms.getTopicType(WD_COMMONS_LICENSE_HTML_URI);
        assignWorkspace(license_info);
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

    private void assignWorkspace(Topic topic) {
        Topic defaultWorkspace = dms.getTopic("uri", new SimpleValue(WS_WIKIDATA_URI));
        dms.createAssociation(new AssociationModel("dm4.core.aggregation",
            new TopicRoleModel(topic.getId(), "dm4.core.parent"),
            new TopicRoleModel(defaultWorkspace.getId(), "dm4.core.child")
        ));
    }

}
