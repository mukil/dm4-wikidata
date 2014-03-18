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
 * @version 0.0.2-SNAPSHOT
 */

public class Migration6 extends Migration {

    private Logger log = Logger.getLogger(getClass().getName());

    private final static String WS_WIKIDATA_URI = "org.deepamehta.workspaces.wikidata";

    private final static String WD_SEARCH_ENTITY = "org.deepamehta.wikidata.search_entity";

    private final static String WD_CLAIM_EDGE_TYPE_URI = "org.deepamehta.wikidata.claim_edge";

    private final static String WD_TEXT_TYPE_URI = "org.deepamehta.wikidata.text";
    private final static String WD_COMMONS_MEDIA_TYPE_URI = "org.deepamehta.wikidata.commons_media";
    private final static String WD_GLOBE_COORDINATE_TYPE_URI = "org.deepamehta.wikidata.globe_coordinate";

    @Override
    public void run() {

        // 1) Assign all new wikidata types to the \"Wikidata\"-Workspace
        AssociationType claim = dms.getAssociationType(WD_CLAIM_EDGE_TYPE_URI);
        assignWorkspace(claim);
        TopicType coordinate = dms.getTopicType(WD_GLOBE_COORDINATE_TYPE_URI);
        assignWorkspace(coordinate);
        TopicType commons_media = dms.getTopicType(WD_COMMONS_MEDIA_TYPE_URI);
        assignWorkspace(commons_media);
        TopicType text = dms.getTopicType(WD_TEXT_TYPE_URI);
        assignWorkspace(text);
        // 2) Make \"Wikidata Search Entity\" (type=property) part of each \"Wikidata Claim\"-Edge
        TopicType searchEntity = dms.getTopicType(WD_SEARCH_ENTITY);
        claim.addAssocDef(new AssociationDefinitionModel("dm4.core.aggregation_def",
                claim.getUri(), searchEntity.getUri(), "dm4.core.one", "dm4.core.one"));


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
