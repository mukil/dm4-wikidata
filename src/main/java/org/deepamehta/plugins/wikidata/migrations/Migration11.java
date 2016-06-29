package org.deepamehta.plugins.wikidata.migrations;

import de.deepamehta.core.Topic;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.Migration;
import de.deepamehta.accesscontrol.AccessControlService;
import de.deepamehta.workspaces.WorkspacesService;

import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONException;

import java.util.logging.Logger;


/*
 * A migration to fix the "Wikidata" workspace (in DBs from 4.4).
 *
 * @author Malte Reißig (<malte@mikromedia.de>)
 * @website https://github.com/mukil/dm4-wikidata
 */
public class Migration11 extends Migration {

    private Logger log = Logger.getLogger(getClass().getName());

    private final String WS_WIKIDATA_URI = "org.deepamehta.workspaces.wikidata";

    @Inject
    private WorkspacesService wsService = null;

    @Inject
    private AccessControlService acService = null;

    @Override
    public void run() {

        // Fix As of 4.5: "No owner is assigned to workspace "Wikidata", Workspace topic looses name during 4.5
        // migration
        Topic workspace = wsService.getWorkspace(WS_WIKIDATA_URI);
        acService.setWorkspaceOwner(workspace, "admin");
        // setting label
        workspace.setSimpleValue("Wikidata");
        try {
            workspace.setChildTopics(mf.newChildTopicsModel(new JSONObject("{\"dm4.workspaces.name\": \"Wikidata\"}")));
        } catch (JSONException je) {
            throw new RuntimeException("Correcting name of Wikidata Workspace with Migration11 FAILED", je);
        }

    }

}
