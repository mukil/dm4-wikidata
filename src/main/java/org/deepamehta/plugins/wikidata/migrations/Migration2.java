package org.deepamehta.plugins.wikidata.migrations;

import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.service.Migration;
import java.util.logging.Logger;


/*
 * A very basic plugin for searching and turning wikidata-properties into association-types.
 *
 * @author Malte Reißig (<malte@mikromedia.de>)
 * @website https://github.com/mukil/dm4-wikidata
 * @version 0.0.2-SNAPSHOT
 */

public class Migration2 extends Migration {

    private Logger log = Logger.getLogger(getClass().getName());

    private final static String WD_SEARCH_BUCKET = "org.deepamehta.wikidata.search_bucket";
    private final static String WD_SEARCH_ENTITY = "org.deepamehta.wikidata.search_entity";
    private final static String WD_ISO_LANGUAGE_CODE = "org.deepamehta.wikidata.language";

    private final static String DEEPAMEHTA_USERNAME_URI = "dm4.accesscontrol.username";
    private final static String DEEPAMEHTA_ADMIN_USERNAME = "admin";

    private final String WS_WIKIDATA_URI = "org.deepamehta.workspaces.wikidata";

    @Override
    public void run() {

        // 1) create \"Wikidata\"-Workspace
        TopicModel workspace = new TopicModel(WS_WIKIDATA_URI, "dm4.workspaces.workspace");
        Topic ws = dms.createTopic(workspace, null);
        ws.setSimpleValue("Wikidata");
        log.info("1) Created WIKIDATA Workspace ..");
        // 2) assign "admin" username to \"Wikidata\"-Workspace
        Topic administrator = dms.getTopic(DEEPAMEHTA_USERNAME_URI, new SimpleValue(DEEPAMEHTA_ADMIN_USERNAME), true);
        assignWorkspace(administrator);
        log.info("2) Assigned admin to WIKIDATA Workspace ..");
        // 3) assign all types to our new workspace
        TopicType searchType = dms.getTopicType(WD_SEARCH_BUCKET);
        TopicType seachEntity = dms.getTopicType(WD_SEARCH_ENTITY);
        TopicType languageCode = dms.getTopicType(WD_ISO_LANGUAGE_CODE);
        assignWorkspace(searchType);
        assignWorkspace(seachEntity);
        log.info("3) Assigned Wikidata Search Types to \"Wikidata\"-Workspace ..");
        assignWorkspace(languageCode);
        log.info("4) Assigned Wikidata Language Type to \"Wikidata\"-Workspace ..");

    }

    // === Workspace ===

    private void assignWorkspace(Topic topic) {
        Topic defaultWorkspace = dms.getTopic("uri", new SimpleValue(WS_WIKIDATA_URI), false);
        dms.createAssociation(new AssociationModel("dm4.core.aggregation",
            new TopicRoleModel(topic.getId(), "dm4.core.parent"),
            new TopicRoleModel(defaultWorkspace.getId(), "dm4.core.child")
        ), null);
    }

}
