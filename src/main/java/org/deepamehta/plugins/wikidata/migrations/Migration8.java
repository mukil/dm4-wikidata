package org.deepamehta.plugins.wikidata.migrations;

import de.deepamehta.core.TopicType;
import de.deepamehta.core.service.Migration;
import java.util.logging.Logger;


/*
 * Registering the custom page-renderer for wikidata search entities.
 *
 * @author Malte Reißig (<malte@mikromedia.de>)
 * @website https://github.com/mukil/dm4-wikidata
 * @since 0.0.3-SNAPSHOT
 */

public class Migration8 extends Migration {

    private Logger log = Logger.getLogger(getClass().getName());

    private final static String WD_COMMONS_MEDIA = "org.deepamehta.wikidata.commons_media";

    @Override
    public void run() {

        // 1) Register the new page-renderer
        TopicType searchEntity = dms.getTopicType(WD_COMMONS_MEDIA);
        searchEntity.getViewConfig().addSetting("dm4.webclient.view_config", "dm4.webclient.page_renderer_uri",
                "org.deepamehta.wikidata.commons_media_renderer");

    }

}
