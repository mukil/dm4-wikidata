package org.deepamehta.plugins.wikidata.migrations;

import de.deepamehta.core.AssociationType;
import de.deepamehta.core.DeepaMehtaType;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.Migration;
import de.deepamehta.workspaces.WorkspacesService;

import java.util.logging.Logger;


/*
 * A very basic plugin for searching and turning wikidata-properties into association-types.
 *
 * @author Malte Reißig (<malte@mikromedia.de>)
 * @website https://github.com/mukil/dm4-wikidata
 */

public class Migration6 extends Migration {

    private Logger log = Logger.getLogger(getClass().getName());

    private final static String WS_WIKIDATA_URI = "org.deepamehta.workspaces.wikidata";

    private final static String WD_SEARCH_ENTITY = "org.deepamehta.wikidata.search_entity";

    private final static String WD_CLAIM_EDGE_TYPE_URI = "org.deepamehta.wikidata.claim_edge";

    private final static String WD_TEXT_TYPE_URI = "org.deepamehta.wikidata.text";
    private final static String WD_COMMONS_MEDIA_TYPE_URI = "org.deepamehta.wikidata.commons_media";
    private final static String WD_GLOBE_COORDINATE_TYPE_URI = "org.deepamehta.wikidata.globe_coordinate";

    private long workspaceId = 0;

    @Inject
    private WorkspacesService wsService = null;

    @Override
    public void run() {

        // 0) Initialize wikidata workspace id
        workspaceId = wsService.getWorkspace(WS_WIKIDATA_URI).getId();
        // 1) Assign all new wikidata types to the \"Wikidata\"-Workspace
        AssociationType claim = dm4.getAssociationType(WD_CLAIM_EDGE_TYPE_URI);
        assignTypeWorkspace(claim);
        TopicType coordinate = dm4.getTopicType(WD_GLOBE_COORDINATE_TYPE_URI);
        assignTypeWorkspace(coordinate);
        TopicType commons_media = dm4.getTopicType(WD_COMMONS_MEDIA_TYPE_URI);
        assignTypeWorkspace(commons_media);
        TopicType text = dm4.getTopicType(WD_TEXT_TYPE_URI);
        assignTypeWorkspace(text);
        log.info("1) Assigned \"Wikidata Text\", \"Wikidata Commons Media\", \"Wikidata Globe Coordinate\" "
                + "to \"Wikidata\"-Workspace");
        // 2) Make \"Wikidata Search Entity\" (type=property) part of each \"Wikidata Claim\"-Edge
        TopicType searchEntity = dm4.getTopicType(WD_SEARCH_ENTITY);
        claim.addAssocDef(mf.newAssociationDefinitionModel("dm4.core.aggregation_def",
                claim.getUri(), searchEntity.getUri(), "dm4.core.one", "dm4.core.one"));
        log.info("2) Assigned \"Wikidata Search Entity\" to \"Wikidata Claim\"");
        // 3) Add "Ordinal Number" to "Search Entity"
        TopicType ordinalNr = dm4.getTopicType("org.deepamehta.wikidata.search_ordinal_nr");
        searchEntity.addAssocDef(mf.newAssociationDefinitionModel("dm4.core.composition_def",
                searchEntity.getUri(), ordinalNr.getUri(), "dm4.core.one", "dm4.core.one"));
        log.info("3) Assigned \"Search Entity Ordinal Nr\" to \"Search Entity\"");

    }

    // === Workspace ===

    private void assignTypeWorkspace(DeepaMehtaType topic) {
        wsService.assignTypeToWorkspace(topic, workspaceId);
    }

}
