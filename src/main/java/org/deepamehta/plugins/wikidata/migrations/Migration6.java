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

    private final String WS_WIKIDATA_URI = "org.deepamehta.workspaces.wikidata";

    @Override
    public void run() {

        TopicType coordinate = dms.getTopicType("org.deepamehta.wikidata.globe_coordinate");
        assignWorkspace(coordinate);
        TopicType commons_media = dms.getTopicType("org.deepamehta.wikidata.commons_media");
        assignWorkspace(commons_media);
        TopicType text = dms.getTopicType("org.deepamehta.wikidata.text");
        assignWorkspace(text);
        AssociationType claim = dms.getAssociationType("org.deepamehta.wikidata.claim_edge");
        assignWorkspace(claim);

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
