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
 * @version 0.0.2
 */

public class Migration4 extends Migration {

    private Logger log = Logger.getLogger(getClass().getName());

    private final static String WD_SEARCH_ENTITY_TYPE = "org.deepamehta.wikidata.search_entity_type";
    private final static String WD_SEARCH_ENTITY = "org.deepamehta.wikidata.search_entity";

    @Override
    public void run() {

        // 1) fetch my two topic-types
        TopicType searchEntity = dms.getTopicType(WD_SEARCH_ENTITY);
        TopicType searchEntityType = dms.getTopicType(WD_SEARCH_ENTITY_TYPE);
        // 2) relate my new topic-type to the existing one
        searchEntity.addAssocDef(new AssociationDefinitionModel("dm4.core.composition_def",
                searchEntity.getUri(), searchEntityType.getUri(), "dm4.core.one", "dm4.core.one"));
        log.info("1) Assigned \"Search Entity Type\" to \"Search Entity\"");
        // "dm4.webclient.page_renderer_uri" : "org.deepamehta.wikidata.search_entity_renderer"
        // log.info("2) Assigned new search entity renderer to \"Search Entity\" Topic Type");
        // ### Do so when dm4-core is fixed: remove assocDef "org.deepamehta.wikidata.language" from WD_SEARCH_ENTITY
        // searchEntity.removeAssocDef("org.deepamehta.wikidata.language");

    }

}
