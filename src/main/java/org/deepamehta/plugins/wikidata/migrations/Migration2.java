package org.deepamehta.plugins.wikidata.migrations;

import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.Migration;
import de.deepamehta.core.service.accesscontrol.SharingMode;
import de.deepamehta.plugins.accesscontrol.AccessControlService;
import de.deepamehta.plugins.workspaces.WorkspacesService;

import java.util.logging.Logger;


/*
 * A very basic plugin for searching and turning wikidata-properties into association-types.
 *
 * @author Malte Reißig (<malte@mikromedia.de>)
 * @website https://github.com/mukil/dm4-wikidata
 */

public class Migration2 extends Migration {

    private Logger log = Logger.getLogger(getClass().getName());

    private final static String WD_SEARCH_BUCKET = "org.deepamehta.wikidata.search_bucket";
    private final static String WD_SEARCH_ENTITY = "org.deepamehta.wikidata.search_entity";
    private final static String WD_ISO_LANGUAGE_CODE = "org.deepamehta.wikidata.language";

    private final static String DEEPAMEHTA_USERNAME_URI = "dm4.accesscontrol.username";
    private final static String DEEPAMEHTA_ADMIN_USERNAME = "admin";

    private final String WS_WIKIDATA_URI = "org.deepamehta.workspaces.wikidata";

    @Inject
    private AccessControlService acService = null;

    @Inject
    private WorkspacesService wsService = null;

    @Override
    public void run() {

        // 1) create \"Wikidata\"-Workspace (if non-existent)
        Topic wdWorkspace = null;
        try {
            wdWorkspace = wsService.getWorkspace(WS_WIKIDATA_URI);
        } catch (Exception e) {
            wdWorkspace = wsService.createWorkspace("Wikidata", WS_WIKIDATA_URI, SharingMode.PUBLIC);
            log.info("1) Created WIKIDATA Workspace ..");
        }
        if (wdWorkspace == null) throw new RuntimeException("Creating Wikidata Workspace FAILED!");
        acService.setWorkspaceOwner(wdWorkspace, DEEPAMEHTA_ADMIN_USERNAME);
        // 2) assign all types to our new workspace
        TopicType searchType = dms.getTopicType(WD_SEARCH_BUCKET);
        TopicType seachEntity = dms.getTopicType(WD_SEARCH_ENTITY);
        TopicType languageCode = dms.getTopicType(WD_ISO_LANGUAGE_CODE);
        wsService.assignTypeToWorkspace(searchType, wdWorkspace.getId());
        wsService.assignTypeToWorkspace(seachEntity, wdWorkspace.getId());
        wsService.assignTypeToWorkspace(languageCode, wdWorkspace.getId());
        log.info("2) Assigned Wikidata Search & Language Type to \"Wikidata\"-Workspace ..");
        // 3) create membership for "admin" in \"Wikidata\"-Workspace
        // acService.createMembership(DEEPAMEHTA_ADMIN_USERNAME, wdWorkspace.getId());
        // log.info("3) Created membership for " + DEEPAMEHTA_ADMIN_USERNAME + " in WIKIDATA Workspace ..");

    }

}
