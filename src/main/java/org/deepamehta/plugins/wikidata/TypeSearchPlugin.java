package org.deepamehta.plugins.wikidata;

import de.deepamehta.core.Topic;
import de.deepamehta.core.model.*;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.service.annotation.ConsumesService;
import de.deepamehta.core.storage.spi.DeepaMehtaTransaction;
import de.deepamehta.plugins.accesscontrol.service.AccessControlService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * A very basic plugin for searching and turning wikidata-properties into association-types.
 *
 * @author Malte Reißig (<malte@mikromedia.de>)
 * @website https://github.com/mukil/dm4-wikidata
 * @version 0.0.1-SNAPSHOT
 *
 */

@Path("/wikidata")
@Consumes("application/json")
@Produces("application/json")
public class TypeSearchPlugin extends PluginActivator {

    private Logger log = Logger.getLogger(getClass().getName());

    private final String DEEPAMEHTA_VERSION = "DeepaMehta 4.2-SNAPSHOT";
    private final String WIKIDATA_TYPE_SEARCH_VERSION = "0.0.1-SNAPSHOT";
    private final String CHARSET = "UTF-8";

    // --- DeepaMehta 4 URIs

    private final String DEFAULT_ROLE_TYPE_URI = "dm4.core.default";
    private final String CHILD_ROLE_TYPE_URI = "dm4.core.child";
    private final String PARENT_ROLE_TYPE_URI = "dm4.core.parent";
    private final String AGGREGATION_TYPE_URI = "dm4.core.aggregation";
    private final String COMPOSITION_TYPE_URI = "dm4.core.composition";
    private final String DM_WEBBROWSER_URL = "dm4.webbrowser.url";

    // --- Wikidata DeepaMehta URIs

    private final String WS_WIKIDATA_URI = "org.deepamehta.workspaces.wikidata";

    private final String WD_SEARCH_BUCKET_URI = "org.deepamehta.wikidata.search_bucket";
    private final String WD_SEARCH_QUERY_URI = "org.deepamehta.wikidata.search_query";

    private final String WD_LANGUAGE_URI = "org.deepamehta.wikidata.language";
    // private final String WD_LANGUAGE_NAME_URI = "org.deepamehta.wikidata.language_name";
    // private final String WD_LANGUAGE_ISO_CODE_URI = "org.deepamehta.wikidata.language_code_iso";
    private final String WD_LANGUAGE_DATA_URI_PREFIX = "org.deepamehta.wikidata.lang_";

    private final String WD_SEARCH_ENTITY_URI = "org.deepamehta.wikidata.search_entity";
    private final String WD_SEARCH_ENTITY_LABEL_URI = "org.deepamehta.wikidata.search_entity_label";
    private final String WD_SEARCH_ENTITY_DESCR_URI = "org.deepamehta.wikidata.search_entity_description";
    private final String WD_SEARCH_ENTITY_ALIAS_URI = "org.deepamehta.wikidata.search_entity_alias";
    private final String WD_SEARCH_ENTITIY_DATA_URI_PREFIX = "org.deepamehta.wikidata.entity_";

    // --- Wikidata Service URIs

    private final String WD_SEARCH_ENTITIES_ENDPOINT =
            "http://www.wikidata.org/w/api.php?action=wbsearchentities&format=json&limit=50";
    private final String WD_SEARCH_ENTITY_TYPE_PROPERTY = "property";
    private final String WD_SEARCH_ENTITY_TYPE_ITEM = "item";

    // --- Instance Variables

    private boolean isInitialized = false;

    private AccessControlService acService = null;



    /**
     *  This method searches a wikidata a \"Property\" by simple text-query:
     *
     *  @param {query}              name of wikidata property in search
     *  @param {language_code}      ISO 639-1 language code (must exist in DM installation)
     */

    @GET
    @Path("/property/search/{query}/{language_code}")
    @Produces(MediaType.APPLICATION_JSON)
    public Topic searchWikidataProperty(@PathParam("query") String query, @PathParam("language_code") String lang,
                @HeaderParam("Cookie") ClientState clientState) {

        String json_result = "";
        StringBuffer resultBody = new StringBuffer();
        URL requestUri = null;
        Topic search_bucket = null;
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            // 1) fixme: Authorize request
            requestUri = new URL(WD_SEARCH_ENTITIES_ENDPOINT + "&search="+ query +"&language="+ lang +"&type="
                    + WD_SEARCH_ENTITY_TYPE_PROPERTY); // maybe restrict results to DataType=Item
            log.fine("Requesting Wikidata " + requestUri.toString());
            // 2) initiate request
            HttpURLConnection connection = (HttpURLConnection) requestUri.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "DeepaMehta "+DEEPAMEHTA_VERSION+" - "
                    + "Wikidata Relation Type Search " + WIKIDATA_TYPE_SEARCH_VERSION);
            // 3) check the response
            int httpStatusCode = connection.getResponseCode();
            if (httpStatusCode != HttpURLConnection.HTTP_OK) {
                throw new WebApplicationException(new Throwable("Error with HTTPConnection."),
                        Status.INTERNAL_SERVER_ERROR);
            }
            // 4) read in the response
            BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream(), CHARSET));
            for (String input; (input = rd.readLine()) != null;) {
                resultBody.append(input);
            }
            rd.close();
            // 5) process response
            if (resultBody.toString().isEmpty()) {
                throw new WebApplicationException(new RuntimeException("Wikidata was silent."),
                        Status.NO_CONTENT);
            } else {
                // ..) Create Wikidata Search Bucket
                CompositeValueModel bucket_model = new CompositeValueModel();
                bucket_model.put(WD_SEARCH_QUERY_URI, query);
                bucket_model.putRef(WD_LANGUAGE_URI, WD_LANGUAGE_DATA_URI_PREFIX + lang);
                json_result = resultBody.toString();
                processWikidataEntitySearch(json_result, bucket_model);
                search_bucket = dms.createTopic(new TopicModel(WD_SEARCH_BUCKET_URI, bucket_model), clientState);
                log.info("Wikidata Search Bucket for "+ query +" in ("+ lang +") was CREATED");
            }
            tx.success();
        } catch (MalformedURLException e) {
            log.warning("Wikidata Plugin: MalformedURLException ..." + e.getMessage());
            throw new RuntimeException("Could not find wikidata endpoint.", e);
        } catch (IOException ioe) {
            log.warning("Wikidata Plugin: IOException ..." + ioe.getMessage());
            throw new WebApplicationException(new Throwable("... dunnow"), Status.BAD_REQUEST);
        } finally {
            tx.finish();
            return search_bucket;
        }
    }

    /**
     *  This method creates a DeepaMehta Association Type from a wikidata \"Property\"-Entity.
     *
     *  @param {query}      name of wikidata property in search
     */

    @GET
    @Path("/property/turn/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Topic createWikidataAssociationType(@PathParam("id") long id,
                @HeaderParam("Cookie") ClientState clientState) {

        Topic association_type = null;
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            Topic property_entity = dms.getTopic(id, true);
            // 1) Create new Association Type model
            String property_name = property_entity.getSimpleValue().toString();
            AssociationTypeModel assoc_type_model = new AssociationTypeModel("org.deepamehta.wikidata.assoctype_"
                    + property_entity.getUri().replaceAll(WD_SEARCH_ENTITIY_DATA_URI_PREFIX, ""),
                    property_name, "dm4.core.text");
            association_type = dms.createAssociationType(assoc_type_model, clientState);
            // 2) Assign to "Wikidata" Workspace
            assignToWikidataWorkspace(association_type);
            // 3) fixme: assign serch-result entity to new assoc-type (to keep track)
            log.info("Turned wikidata property \""+ property_entity.getSimpleValue() +"\" into DM Association Type!");
            tx.success();
        } catch (Error e) {
            log.warning("OH: The Wikidata Plugin experienced an unforeseen error! "+ e.getMessage());
        } finally {
            tx.finish();
            return association_type;
        }
    }



    /** --- Initialize the migrated soundsets ACL-Entries.  --- */

    public void init() {
        isInitialized = true;
        configureIfReady();
    }

    private void configureIfReady() {
        if (isInitialized) {
            checkACLsOfMigration();
        }
    }

    /** --- Implementing PluginService Interfaces to consume AccessControlService --- */

    @Override
    @ConsumesService({
        "de.deepamehta.plugins.accesscontrol.service.AccessControlService"
    })
    public void serviceArrived(PluginService service) {
        if (service instanceof AccessControlService) {
            acService = (AccessControlService) service;
        }
    }

    @Override
    @ConsumesService({
        "de.deepamehta.plugins.accesscontrol.service.AccessControlService"
    })
    public void serviceGone(PluginService service) {
        if (service == acService) {
            acService = null;
        }
    }

    /** --- Code running once, after plugin initialization. --- */

    private void checkACLsOfMigration() {
        // todo:
    }

    private void processWikidataEntitySearch(String json_result, CompositeValueModel search_bucket) {
        try {
            JSONObject response = new JSONObject(json_result);
            JSONArray result = response.getJSONArray("search");
            if (result.length() > 0) {
                for (int i=0; i < result.length(); i++) {
                    JSONObject entity_response = result.getJSONObject(i);
                    // Check if entity already exists
                    String id = entity_response.getString("id");
                    Topic existing_entity = dms.getTopic("uri",
                            new SimpleValue(WD_SEARCH_ENTITIY_DATA_URI_PREFIX + id), false);
                    TopicModel entity_model = null;
                    if (existing_entity == null) {
                        // Create new entity
                        String name = entity_response.getString("label");
                        String url = entity_response.getString("url");
                        // setup new wikidata entity
                        CompositeValueModel entity_composite = new CompositeValueModel();
                        entity_composite.put(WD_SEARCH_ENTITY_LABEL_URI, name);
                        if (entity_response.has("description")) {
                            String description = entity_response.getString("description");
                            entity_composite.put(WD_SEARCH_ENTITY_DESCR_URI, description);
                        }
                        entity_composite.put(DM_WEBBROWSER_URL, url);
                        if (entity_response.has("aliases")) {
                            JSONArray aliases = entity_response.getJSONArray("aliases");
                            for (int a=0; a < aliases.length(); a++) {
                                String alias = aliases.getString(a);
                                entity_composite.add(WD_SEARCH_ENTITY_ALIAS_URI,
                                    new TopicModel(WD_SEARCH_ENTITY_ALIAS_URI, new SimpleValue(alias)));
                            }
                        }
                        // fixme: search-result does not include language information
                        entity_model = new TopicModel(WD_SEARCH_ENTITIY_DATA_URI_PREFIX + id,
                                WD_SEARCH_ENTITY_URI, entity_composite);
                        // create new wikidata entity
                        // reference new entity in wikidata search bucket
                        search_bucket.add(WD_SEARCH_ENTITY_URI, entity_model);
                    } else {
                        // reference new entity in wikidata search bucket by URI
                        search_bucket.addRef(WD_SEARCH_ENTITY_URI, WD_SEARCH_ENTITIY_DATA_URI_PREFIX + id);
                    }
                }
            }
        } catch (JSONException ex) {
            log.warning("Wikidata Plugin: JSONException during processing a wikidata entity search response. "
                    + ex.getMessage());
        }
    }

    private void assignToWikidataWorkspace(Topic topic) {
        // fixme: remove assignment of type to other (selected via clientState) workspace
        Topic defaultWorkspace = dms.getTopic("uri", new SimpleValue(WS_WIKIDATA_URI), false);
        dms.createAssociation(new AssociationModel("dm4.core.aggregation",
            new TopicRoleModel(topic.getId(), "dm4.core.parent"),
            new TopicRoleModel(defaultWorkspace.getId(), "dm4.core.child")
        ), null);
    }

}
