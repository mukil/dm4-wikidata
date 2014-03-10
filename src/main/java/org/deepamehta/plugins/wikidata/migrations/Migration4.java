package org.deepamehta.plugins.wikidata.migrations;

import de.deepamehta.core.AssociationType;
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
 * @version 0.0.1
 */

public class Migration4 extends Migration {

    private Logger log = Logger.getLogger(getClass().getName());

    private final static String WD_SEARCH_ENTITY_TYPE = "org.deepamehta.wikidata.search_entity_type";
    private final static String WD_SEARCH_ENTITY = "org.deepamehta.wikidata.search_entity";

    private final String WS_WIKIDATA_URI = "org.deepamehta.workspaces.wikidata";

    @Override
    public void run() {

        // 1) assign "admin" username to \"Wikidata\"-Workspace
        TopicType searchEntity = dms.getTopicType(WD_SEARCH_ENTITY);
        TopicType searchEntityType = dms.getTopicType(WD_SEARCH_ENTITY_TYPE);
        searchEntity.addAssocDef(new AssociationDefinitionModel("dm4.core.composition_def",
                searchEntity.getUri(), searchEntityType.getUri(), "dm4.core.one", "dm4.core.one"));
        log.info("1) Assigned \"Search Entity Type\" to \"Search Entity\"");
        // "dm4.webclient.page_renderer_uri" : "org.deepamehta.wikidata.search_entity_renderer"
        // log.info("2) Assigned new search entity renderer to \"Search Entity\" Topic Type");
        // 3) remove "org.deepamehta.wikidata.language" from WD_SEARCH_ENTITY
        searchEntity.removeAssocDef("org.deepamehta.wikidata.language");

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
