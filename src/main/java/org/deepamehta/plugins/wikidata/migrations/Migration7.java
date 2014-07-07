package org.deepamehta.plugins.wikidata.migrations;

import de.deepamehta.core.AssociationType;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.*;
import de.deepamehta.core.service.Migration;
import java.util.logging.Logger;


/*
 * Registering the custom page-renderer for wikidata search entities.
 *
 * @author Malte Reißig (<malte@mikromedia.de>)
 * @website https://github.com/mukil/dm4-wikidata
 * @since 0.0.3-SNAPSHOT
 */

public class Migration7 extends Migration {

    private Logger log = Logger.getLogger(getClass().getName());

    private final static String WD_SEARCH_ENTITY = "org.deepamehta.wikidata.search_entity";

    @Override
    public void run() {

        // 1) Register the new page-renderer
        TopicType searchEntity = dms.getTopicType(WD_SEARCH_ENTITY);
        searchEntity.getViewConfig().addSetting("dm4.webclient.view_config", "dm4.webclient.page_renderer_uri",
                "org.deepamehta.wikidata.search_entity_renderer");

    }

}
