
package org.deepamehta.plugins.wikidata;

import de.deepamehta.core.*;
import de.deepamehta.core.model.*;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.ResultList;
import de.deepamehta.core.service.Transactional;
import de.deepamehta.core.service.event.PostCreateAssociationListener;
import de.deepamehta.core.service.event.PostCreateTopicListener;
import de.deepamehta.core.storage.spi.DeepaMehtaTransaction;
import de.deepamehta.plugins.accesscontrol.AccessControlService;
import de.deepamehta.plugins.workspaces.WorkspacesService;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 * A basic plugin service to search and import wikidata entities into DeepaMehta 4.
 *
 * <a href="https://github.com/mukil/dm4-wikidata">Source Code Repository</a>
 *
 * @author Malte Reißig (<a href="mailto:malte@mikromedia.de">Contact</a>)
 * @version 0.0.5-SNAPSHOT
 */

@Path("/wikidata")
@Consumes("application/json")
@Produces("application/json")
public class WikidataSearchPlugin extends PluginActivator implements WikidataSearchService,
                                                    PostCreateTopicListener, PostCreateAssociationListener {

    private Logger log = Logger.getLogger(getClass().getName());

    private final String DEEPAMEHTA_VERSION = "DeepaMehta 4.7";
    private final String WIKIDATA_TYPE_SEARCH_VERSION = "0.0.5-SNAPSHOT";
    private final String CHARSET = "UTF-8";


    // --- DeepaMehta 4 URIs

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
    private final String WD_SEARCH_ENTITY_TYPE_URI = "org.deepamehta.wikidata.search_entity_type";
    private final String WD_SEARCH_ENTITY_ORDINAL_NR = "org.deepamehta.wikidata.search_ordinal_nr";
    private final String WD_SEARCH_ENTITY_DESCR_URI = "org.deepamehta.wikidata.search_entity_description";
    private final String WD_SEARCH_ENTITY_ALIAS_URI = "org.deepamehta.wikidata.search_entity_alias";
    private final String WD_SEARCH_ENTITIY_DATA_URI_PREFIX = "org.deepamehta.wikidata.entity_";
    private final String WD_TEXT_TYPE_URI = "org.deepamehta.wikidata.text";
    private final String WD_COMMONS_MEDIA_TYPE_URI = "org.deepamehta.wikidata.commons_media";
    private final String WD_COMMONS_MEDIA_NAME_TYPE_URI = "org.deepamehta.wikidata.commons_media_name";
    private final String WD_COMMONS_MEDIA_PATH_TYPE_URI = "org.deepamehta.wikidata.commons_media_path";
    // private final String WD_COMMONS_MEDIA_TYPE_TYPE_URI = "org.deepamehta.wikidata.commons_media_type";
    private final String WD_COMMONS_MEDIA_DESCR_TYPE_URI = "org.deepamehta.wikidata.commons_media_descr";
    private final String WD_COMMONS_AUTHOR_HTML_URI = "org.deepamehta.wikidata.commons_author_html";
    private final String WD_COMMONS_LICENSE_HTML_URI = "org.deepamehta.wikidata.commons_license_html";
    // private final String WD_GLOBE_COORDINATE_TYPE_URI = "org.deepamehta.wikidata.globe_coordinate";
    private final String WD_ENTITY_CLAIM_EDGE = "org.deepamehta.wikidata.claim_edge";



    // --- Wikidata Service URIs

    private final String WD_SEARCH_ENTITIES_ENDPOINT =
            "https://www.wikidata.org/w/api.php?action=wbsearchentities&format=json&limit=50";
    private final String WD_CHECK_ENTITY_CLAIMS_ENDPOINT =
            "https://www.wikidata.org/w/api.php?action=wbgetclaims&format=json"; // &ungroupedlist=0
    private final String WD_GET_ENTITY_ENDPOINT = "https://www.wikidata.org/w/api.php?action=wbgetentities"
            + "&props=info%7Caliases%7Clabels%7Cdescriptions&format=json"; // sitelinks%2Furls%7C

    private final String WIKIDATA_ENTITY_URL_PREFIX = "//www.wikidata.org/wiki/";
    private final String LANG_EN = "en";
    
    @Inject
    private AccessControlService acService = null;

    @Inject
    private WorkspacesService wsService = null;

    Topic wikidataWorkspace = null;

    // --- Public REST API Endpoints

    /**
     * Turns a "Wikidata Search Entity" (of type="property") into a first-classe DeepaMehta 4 Association Type.
     */
    @GET
    @Path("/property/turn/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    @Transactional
    public Topic createWikidataAssociationType(@PathParam("id") long id) {
        AssociationType association_type = null;
        try {
            Topic property_entity = dms.getTopic(id);
            // 1) Create new Association Type model
            String property_name = property_entity.getSimpleValue().toString();
            AssociationTypeModel assoc_type_model = new AssociationTypeModel("org.deepamehta.wikidata.assoctype_"
                    + property_entity.getUri().replaceAll(WD_SEARCH_ENTITIY_DATA_URI_PREFIX, ""),
                    property_name, "dm4.core.text");
            association_type = dms.createAssociationType(assoc_type_model);
            // 2) Assign to "Wikidata" Workspace
            assignTypeToWikidataWorkspace(association_type);
            // 3) Associated search-result-entity to new assoc-type (to keep track)
            dms.createAssociation(new AssociationModel("dm4.core.association",
                    new TopicRoleModel(property_entity.getUri(), "dm4.core.default"),
                    new TopicRoleModel(association_type.getUri(), "dm4.core.default")
            ));
            log.info("Turned wikidata property \""+ property_entity.getUri() +"\" into DM Association Type!");
        } catch (Error e) {
            throw new RuntimeException("Creating Association Type FAILED", e);
        } finally {
            return association_type;
        }
    }

    @GET
    @Path("/search/{entity}/{query}/{language_code}")
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    @Transactional
    public Topic searchWikidataEntity(@PathParam("query") String query, @PathParam("language_code") String lang,
                                      @PathParam("entity") String type) {
        String json_result = "";
        StringBuffer resultBody = new StringBuffer();
        URL requestUri = null;
        Topic search_bucket = null;
        // sanity check (set en as default-language if nothing was provided by un-initialized language widget)
        if (lang == null || lang.equals("undefined")) {
            log.warning("Wikidata Language Search Option was not provided, now requesting data in EN");
            lang = LANG_EN;
        }
        // start search operation
        try {
            // 1) fixme: Authorize request
            requestUri = new URL(WD_SEARCH_ENTITIES_ENDPOINT + "&search="+ query +"&language="+ lang +"&type=" + type);
            log.info("Wikidata Search Entities Request: " + requestUri.toString());
            // 2) initiate request
            HttpURLConnection connection = (HttpURLConnection) requestUri.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "DeepaMehta "+DEEPAMEHTA_VERSION+" - "
                    + "Wikidata Search " + WIKIDATA_TYPE_SEARCH_VERSION);
            // 3) check the response
            int httpStatusCode = connection.getResponseCode();
            if (httpStatusCode != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("Error with HTTPConnection, HTTP Status: " + httpStatusCode);
            }
            // 4) read in the response
            BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream(), CHARSET));
            for (String input; (input = rd.readLine()) != null;) {
                resultBody.append(input);
            }
            rd.close();
            // 5) process response
            if (resultBody.toString().isEmpty()) {
                throw new RuntimeException("Wikidata was silent, HTTP Response: No content!");
            } else {
                log.fine("Wikidata Search Request Response: " + resultBody.toString());
                // ..) Create Wikidata Search Bucket
                ChildTopicsModel bucket_model = new ChildTopicsModel();
                bucket_model.put(WD_SEARCH_QUERY_URI, query);
                bucket_model.putRef(WD_LANGUAGE_URI, WD_LANGUAGE_DATA_URI_PREFIX + lang);
                json_result = resultBody.toString();
                buildWikidataSearchBucketModel(json_result, bucket_model, type, lang);
                search_bucket = dms.createTopic(new TopicModel(WD_SEARCH_BUCKET_URI, bucket_model));
                // workaround: addRef does not (yet) fetchComposite, so fetchComposite=true
                search_bucket = dms.getTopic(search_bucket.getId());
                log.info("Wikidata Search Bucket for "+ query +" in ("+ lang +") was CREATED");
            }
            search_bucket.loadChildTopics(); // load all child topics
        } catch (MalformedURLException e) {
            log.warning("Wikidata Plugin: MalformedURLException ..." + e.getMessage());
            throw new RuntimeException("Could not find wikidata endpoint - " + requestUri.toString(), e);
        } catch (IOException ioe) {
            log.warning("Wikidata Plugin: IOException ..." + ioe.getMessage());
            throw new WebApplicationException(new Throwable(ioe), Status.BAD_REQUEST);
        }
        return search_bucket;
    }

    @GET
    @Path("/{entityId}/{language_code}/{doUpdate}/{doAliasUpdates}")
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    @Transactional
    public Topic importWikidataEntity(@PathParam("entityId") String entityId,
                                      @PathParam("language_code") String language_code,
                                      @PathParam("doUpdate") boolean doUpdate,
                                      @PathParam("doAliasUpdates") boolean doAliasUpdates) {
        String json_result = "";
        StringBuffer resultBody = new StringBuffer();
        URL requestUri = null;
        Topic entity = null;
        // sanity check (set en as default-language if nothing was provided by un-initialized language widget)
        if (language_code == null || language_code.equals("undefined")) {
            log.warning("Wikidata Language Search Option was not provided, now requesting data in EN");
            language_code = LANG_EN;
        }
        try {
            // 1) fixme: Authorize request
            // &sites=dewiki&&languages=de
            requestUri = new URL(WD_GET_ENTITY_ENDPOINT + "&ids="+ entityId + "&languages=" + language_code);
            log.fine("Requesting Wikidata Entity Details: " + requestUri.toString());
            // 2) initiate request
            HttpURLConnection connection = (HttpURLConnection) requestUri.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "DeepaMehta "+DEEPAMEHTA_VERSION+" - "
                    + "Wikidata Search " + WIKIDATA_TYPE_SEARCH_VERSION);
            // 3) check the response
            int httpStatusCode = connection.getResponseCode();
            if (httpStatusCode != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("Error with HTTPConnection, HTTP Status: " + httpStatusCode);
            }
            // 4) read in the response
            BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream(), CHARSET));
            for (String input; (input = rd.readLine()) != null;) {
                resultBody.append(input);
            }
            rd.close();
            // 5) process response
            if (resultBody.toString().isEmpty()) {
                throw new RuntimeException("Wikidata was silent, HTTP Response: No content!");
            } else {
                // 6) Create or Update Wikidata Search Entity
                json_result = resultBody.toString();
                log.fine("Wikidata Entity Request Response: " + json_result);
                JSONObject response = new JSONObject(json_result);
                JSONObject entities = response.getJSONObject("entities");
                JSONObject response_entity = entities.getJSONObject(entityId);
                // 0) Check if we need to CREATE or UPDATE our search result entity item
                Topic existingEntity = dms.getTopic("uri",
                            new SimpleValue(WD_SEARCH_ENTITIY_DATA_URI_PREFIX + entityId));
                if (existingEntity == null) {
                    entity = createWikidataSearchEntity(response_entity, language_code);
                    entity.loadChildTopics();
                } else if (doUpdate) {
                    // Updates label, description (and maybe aliases) of related entities in this language.
                    entity = updateWikidataSearchEntity(existingEntity, response_entity, language_code, doAliasUpdates);
                    entity.loadChildTopics();
                } else {
                    entity = existingEntity;
                }
            }
        } catch (MalformedURLException e) {
            log.warning("Wikidata Plugin: MalformedURLException ..." + e.getMessage());
            throw new RuntimeException("Could not find wikidata endpoint  - " + requestUri.toString(), e);
        } catch (IOException ioe) {
            throw new WebApplicationException(new Throwable(ioe), Status.BAD_REQUEST);
        } catch (JSONException je) {
            throw new WebApplicationException(new Throwable(je), Status.INTERNAL_SERVER_ERROR);
        }
        return entity;
    }

    /**
     * Service endpoint for the "Import topics" command available on all "Wikidata Search Result"
     * topics.
     **/
    @GET
    @Path("/check/claims/{id}/{language_code}")
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    @Transactional
    public Topic importClaimsAndRelatedEntities(@PathParam("id") long topicId,
                                                @PathParam("language_code") String language_option) {
        String json_result = "";
        StringBuffer resultBody = new StringBuffer();
        URL requestUri = null;
        Topic wikidataItem = dms.getTopic(topicId);
        // 0) sanity check (set en as default-language if nothing was provided by un-initialized language widget)
        if (language_option == null || language_option.equals("undefined")) {
            log.warning("Wikidata Language Search Option was not provided, now requesting data in EN.");
            language_option = LANG_EN;
        }
        String wikidataId = wikidataItem.getUri().replaceAll(WD_SEARCH_ENTITIY_DATA_URI_PREFIX, "");
        // 1) update wikidate item in question, too
        importWikidataEntity(wikidataId, language_option, true, true);
        // 2) update all claims related to this item
        try {
            // 1) ### Authorize request
            // 2) ### be explicit and add "&rank=normal" to wbgetclaims-call, ### add "&props=references" somewhen
            requestUri = new URL(WD_CHECK_ENTITY_CLAIMS_ENDPOINT + "&entity=" + wikidataId);
            log.fine("Requesting Wikidata Entity Claims: " + requestUri.toString());
            // 2) initiate request
            HttpURLConnection connection = (HttpURLConnection) requestUri.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "DeepaMehta "+DEEPAMEHTA_VERSION+" - "
                    + "Wikidata Search " + WIKIDATA_TYPE_SEARCH_VERSION);
            // 3) check the response
            int httpStatusCode = connection.getResponseCode();
            if (httpStatusCode != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("Error with HTTPConnection, HTTP Status: " + httpStatusCode);
            }
            // 4) read in the response
            BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream(), CHARSET));
            for (String input; (input = rd.readLine()) != null;) {
                resultBody.append(input);
            }
            rd.close();
            // 5) process response
            if (resultBody.toString().isEmpty()) {
                throw new RuntimeException("Wikidata was silent, HTTP Response: No content!");
            } else {
                json_result = resultBody.toString();
                log.fine("Wikidata Claim Request Response: " + json_result);
                createWikidataClaims(json_result, wikidataItem, language_option);
                log.info("Wikidata Claim Response is FINE");
            }
            wikidataItem.loadChildTopics(); // load all child topics
        } catch (MalformedURLException e) {
            log.warning("Wikidata Plugin: MalformedURLException ..." + e.getMessage());
            throw new RuntimeException("Could not find wikidata endpoint - " + requestUri.toString(), e);
        } catch (IOException ioe) {
            throw new WebApplicationException(new Throwable(ioe), Status.BAD_REQUEST);
        }
        return wikidataItem;
    }

    @GET
    @Path("/property/related/claims/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public ResultList<RelatedAssociation> getWikidataClaimsForPropertyEntity(@PathParam("id") long wdSearchEntity) {
        Topic wikidataSearchEntity = dms.getTopic(wdSearchEntity);
        ResultList<RelatedAssociation> associations = wikidataSearchEntity.getRelatedAssociations("dm4.core.aggregation",
                "dm4.core.child", "dm4.core.parent", "org.deepamehta.wikidata.claim_edge");
        return associations.loadChildTopics();
    }



    // ---  Wikidata Search (Application Specific) Private Methods

    /**
     * Creates a topic of type *Wikdiata Search Entity* based on the wikidata base API response.
     *
     * @param entity_response   JSONObject  As returned by the HTTP wikidata base API.
     * @param lang              String      ISO language code.
     * @return
     */
    private Topic createWikidataSearchEntity(JSONObject entity_response, String lang) {
        Topic entity = null;
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            String id = entity_response.getString("id");
            // Create new search entity composite
            ChildTopicsModel entity_composite = buildWikidataSearchEntityModel(entity_response, lang);
            TopicModel entity_model = new TopicModel(WD_SEARCH_ENTITIY_DATA_URI_PREFIX + id,
                    WD_SEARCH_ENTITY_URI, entity_composite);
            entity = dms.createTopic(entity_model);
            log.info("Wikidata Search Entity Created (" +
                entity_composite.getString(WD_SEARCH_ENTITY_TYPE_URI)+ "): \"" +
                entity.getSimpleValue() +"\" "+entity.getId()+" - FINE!");
            tx.success();
            tx.finish();
            return entity;
        } catch (Exception ex) {
            tx.failure();
            throw new RuntimeException(ex);
        }
    }

    /**
     * Updates the given Wikidata Search Entity Topic about the values in the given JSON Object (response body).
     *
     * @param entity            Topic       Topic of type **Wikidata Search Entity**.
     * @param entity_response   JSONObject  HTTP Entity Response Body from the wikdiata base API.
     * @param lang              String      ISO language code
     * @param doAliasUpdates   boolean     If set to true,**org.deepamehta.wikidata.search_entity_alias** entities
     *                          will not be deleted and re-created.
     * @return
     */
    private Topic updateWikidataSearchEntity(Topic entity, JSONObject entity_response, String lang, boolean doAliasUpdates) {
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            if (doAliasUpdates) {
                entity.loadChildTopics();
                // what if entity is of type property?
                if (entity.getChildTopics().has(WD_SEARCH_ENTITY_ALIAS_URI)) {
                    List<RelatedTopic> aliasChilds = entity.getChildTopics().getTopics(WD_SEARCH_ENTITY_ALIAS_URI);
                    Iterator<RelatedTopic> iterator = aliasChilds.iterator();
                    while(iterator.hasNext()) {
                        RelatedTopic aliasChild = iterator.next();
                        log.info("REMOVING Wikdiata Item Alias " + aliasChild.getSimpleValue()
                                + " from parent=\"" + aliasChild.getRelatedTopic(null, "dm4.core.child", "dm4.core.parent",
                                null).getSimpleValue() + "\"");
                        aliasChild.delete();
                    }
                }
            }
            // Update existing search entity topic
            ChildTopicsModel entity_composite = buildWikidataSearchEntityModel(entity_response, lang);
            TopicModel entity_model = new TopicModel(entity.getId(), entity_composite);
            dms.updateTopic(entity_model);
            log.fine("Wikidata Search Entity Updated (" +
                entity_composite.getString(WD_SEARCH_ENTITY_TYPE_URI)+ "): \"" + entity.getSimpleValue() +"\" - FINE!");
            tx.success();
            tx.finish();
            return entity;
        } catch (Exception ex) {
            tx.failure();
            throw new RuntimeException(ex);
        }
    }

    /**
     *  Processes a wikidata search entities response as given by the wikidata base API and modifies the given
     *  ChildTopicsModel accordingly.
     **/
    private void buildWikidataSearchBucketModel(String json_result, ChildTopicsModel search_bucket,
                                                String type, String lang) {
        try {
            JSONObject response = new JSONObject(json_result);
            JSONArray result = response.getJSONArray("search");
            if (result.length() > 0) {
                for (int i = 0; i < result.length(); i++) {
                    JSONObject entity_response = result.getJSONObject(i);
                    // Check if entity already exists
                    String id = entity_response.getString("id");
                    // the following throws "401 Unauthorized" if the topic exists but in another workspace (Private)
                    Topic existing_entity = dms.getTopic("uri",
                            new SimpleValue(WD_SEARCH_ENTITIY_DATA_URI_PREFIX + id));
                    if (existing_entity == null) {
                        // ### need new way to persist URI in results
                        // Create new search entity composite
                        String name = entity_response.getString("label");
                        String url = entity_response.getString("url");
                        //
                        ChildTopicsModel entity_composite = new ChildTopicsModel();
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
                        // set entity place in resultset
                        entity_composite.put(WD_SEARCH_ENTITY_ORDINAL_NR, i);
                        // set entity-type
                        entity_composite.put(WD_SEARCH_ENTITY_TYPE_URI, type);
                        // set language-value on entity-result
                        entity_composite.putRef(WD_LANGUAGE_URI, WD_LANGUAGE_DATA_URI_PREFIX + lang);
                        TopicModel entity_model = new TopicModel(WD_SEARCH_ENTITIY_DATA_URI_PREFIX + id,
                                WD_SEARCH_ENTITY_URI, entity_composite);
                        // create and reference  entity in wikidata search bucket
                        search_bucket.add(WD_SEARCH_ENTITY_URI, entity_model);
                    } else {
                        // reference existing entity in wikidata search bucket by URI
                        search_bucket.addRef(WD_SEARCH_ENTITY_URI, WD_SEARCH_ENTITIY_DATA_URI_PREFIX + id);
                    }
                }
            }
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Builds the model (ChildTopicsModel) for one **Wikidata Search Entity** based the response body of the wikidata
     * base API.
     * */
    private ChildTopicsModel buildWikidataSearchEntityModel(JSONObject entity_response, String lang) {
        ChildTopicsModel entity_composite = null;
        try {
            String id = entity_response.getString("id");
            String type = entity_response.getString("type");
            entity_composite = new ChildTopicsModel();
            // main label
            if (entity_response.has("labels")) {
                JSONObject labels = entity_response.getJSONObject("labels");
                JSONObject languaged_label = null;
                if (labels.has(lang)) {
                    languaged_label = labels.getJSONObject(lang);
                    String label = languaged_label.getString("value");
                    entity_composite.put(WD_SEARCH_ENTITY_LABEL_URI, label);
                } else {
                    log.warning("No label found for language \"" + lang + "\" and id " + id);
                }
            }
            // main description
            if (entity_response.has("descriptions")) {
                JSONObject descriptions = entity_response.getJSONObject("descriptions");
                JSONObject languaged_descr = null;
                if (descriptions.has(lang)) {
                    languaged_descr = descriptions.getJSONObject(lang);
                    String description = languaged_descr.getString("value");
                    entity_composite.put(WD_SEARCH_ENTITY_DESCR_URI, description);
                } else {
                    log.warning("No description found for language \"" + lang + "\" and id " + id);
                }
            }
            // aliases
            if (entity_response.has("aliases")) {
                JSONObject aliases = entity_response.getJSONObject("aliases");
                JSONArray languaged_aliases = null;
                if (aliases.has(lang)) {
                    languaged_aliases = aliases.getJSONArray(lang);
                    for (int a=0; a < languaged_aliases.length(); a++) {
                        JSONObject alias_object = languaged_aliases.getJSONObject(a);
                        String alias = alias_object.getString("value");
                        entity_composite.add(WD_SEARCH_ENTITY_ALIAS_URI,
                            new TopicModel(WD_SEARCH_ENTITY_ALIAS_URI, new SimpleValue(alias)));
                    }
                }
            }
            // set wikidata url
            if (type.equals("property")) {
                entity_composite.put(DM_WEBBROWSER_URL, WIKIDATA_ENTITY_URL_PREFIX + "Property:" + id);
            } else {
                entity_composite.put(DM_WEBBROWSER_URL, WIKIDATA_ENTITY_URL_PREFIX + id);
            }
            // set language-value on entity-result
            entity_composite.putRef(WD_LANGUAGE_URI, WD_LANGUAGE_DATA_URI_PREFIX + lang);
            // ### sitelinks
            /** if (entity_response.has("sitelinks")) {
                JSONObject sitelinks = entity_response.getJSONObject("sitelinks");
                if (sitelinks.has(lang + "wiki")) {
                    JSONObject sitelink = sitelinks.getJSONObject(lang + "wiki");
                    entity_composite.put(DM_WEBBROWSER_URL, sitelink.getString("url"));
                } else {
                    log.warning("There is no sitelink for this item in this language/wiki: " + lang + "wiki");
                }
            } **/
            entity_composite.put(WD_SEARCH_ENTITY_TYPE_URI, type);
            return entity_composite;
        } catch (JSONException jex) {
            throw new RuntimeException(jex);
        }
    }

    /**
     * Handles the response body of a HTTP Request to the CLAIM endpoint of wikidata base API.
     *
     * Fix 1: Process qualifierSnaks on each claim (extend migration for that).
     * Fix 2: Process all references for each claim (simply as URLs?).
     */
    private void createWikidataClaims(String json_result, Topic wikidataItem, String language_code) {
        try {
            JSONObject response = new JSONObject(json_result);
            // the following assumes that our API response is not an error
            JSONObject result = response.getJSONObject("claims");
            // Delete all claims going out from this item (me)
            deleteWikidataClaims(wikidataItem);
            wikidataItem = dms.getTopic(wikidataItem.getId());
            // Then re-create all claims going out from this item (this is our "UPDATE")
            Iterator properties = result.keys();
            log.info("Wikidata Plugin is processing all properties part of related " + result.length() + " CLAIMS");
            Topic propertyEntity = null;
            while (properties.hasNext()) {
                String property_id = properties.next().toString();
                // 1) Load related property-entity
                propertyEntity = importWikidataEntity(property_id, language_code, false, false);
                // HashMap<String, List<Topic>> all_entities = new HashMap<String, List<Topic>>();
                JSONArray property_listing = result.getJSONArray(property_id);
                // ### process all claims properly (delete and then create)
                for (int i=0; i < property_listing.length(); i++) {
                    // 2) fetch related wikidata entity
                    Topic referencedItemEntity = null;
                    JSONObject entity_response = property_listing.getJSONObject(i);
                    JSONObject mainsnak = entity_response.getJSONObject("mainsnak");
                    String claim_guid = entity_response.getString("id");
                    // 3) build up item as part of the claim (if so)
                    String itemId = "";
                    String snakDataType = mainsnak.getString("datatype");
                    // log.info("SNakDataType=" + snakDataType + "MainSnak" + mainsnak.toString());
                    JSONObject snakDataValue = mainsnak.getJSONObject("datavalue");
                    // ..) depending on the various (claimed/realted) value-types
                    if (snakDataType.equals("wikibase-item")) {
                        // log.info("Wikibase Item claimed via \"" + propertyEntity.getSimpleValue() + "\"");
                        JSONObject snakDataValueValue = snakDataValue.getJSONObject("value");
                        long numericId = snakDataValueValue.getLong("numeric-id");
                        itemId = "Q" + numericId; // is this always of entity-type "item"? responses looks like.
                        referencedItemEntity = importWikidataEntity(itemId, language_code, false, false);
                    } else if (snakDataType.equals("commonsMedia")) {
                        // do relate wikidata.commons_media
                        if (snakDataValue.has("value")) {
                            String fileName = snakDataValue.getString("value");
                            referencedItemEntity = getWikimediaCommonsMediaTopic(fileName);
                        }
                        /**  **/
                        // ### make use of WIKIMEDIA_COMMONS_MEDIA_FILE_URL_PREFIX and implement page-renderer
                    } else if (snakDataType.equals("globe-coordinate")) {
                        // TODO: do relate wikidata.globe_coordinate
                        // log.fine("Globe Coordinate claimed via \"" + propertyEntity.getSimpleValue()
                               // + "\" ("+language_code+") DEBUG:");
                        // log.fine("  " + snakDataValue.toString());
                    } else if (snakDataType.equals("url")) {
                        if (snakDataValue.has("value")) {
                            // TODO: ### getOrCreateWebResource()
                            String value = snakDataValue.getString("value");
                            log.warning("### SKIPPING URL => " + value);
                        }
                    } else if (snakDataType.equals("string")) {
                        if (snakDataValue.has("value")) {
                            String value = snakDataValue.getString("value");
                            referencedItemEntity = getWikidataTextTopic(value, language_code);
                        } else {
                            log.warning("Could not access wikidata-text value - json-response EMPTY!");
                        }
                    } else if (snakDataType.equals("quantity")) {
                        // TODO: Fix datatype
                        if (snakDataValue.has("value")) {
                            JSONObject value = snakDataValue.getJSONObject("value");
                            if (value.has("amount")) {
                                String amount = value.getString("amount");
                                referencedItemEntity = getWikidataTextTopic(amount, language_code);
                            } else {
                               log.warning("Could not access wikidata-text value - AMOUNT EMPTY!");
                            }
                        } else {
                            log.warning("Could not access wikidata-text value - NO VALUE SET!");
                        }
                    } else {
                        log.warning("Value claimed as " + propertyEntity.getSimpleValue() + " is not of any known type"
                                + " wikibase-item but \"" + snakDataType +"\" ("+snakDataValue+")");
                        // e.g. snakDataType.equals("quantity")
                    }
                    // store topic reference to (new or already existing) wikidata-entity/ resp. -value topic
                    if (referencedItemEntity != null) {
                        createWikidataClaim(claim_guid, wikidataItem, referencedItemEntity,
                            propertyEntity);
                    } else {
                        log.fine("SKIPPED creating claim of type \""+snakDataType+"\" value for "
                                + "\""+propertyEntity.getSimpleValue()+"\" on \"" + wikidataItem.getSimpleValue()+"\"");
                    }
                }
            }
        } catch (JSONException ex) {
            log.warning("JSONException during processing a wikidata claim. " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    /**
     *  Delets all association of type *org.deepamehta.wikidata.claim_edge* related to the given <code>Wikidata Search
     *  Entity</code>.
     **/
    private void deleteWikidataClaims(Topic wikidataItem) {
        List<Association> all_claims = wikidataItem.getAssociations();
        ArrayList<Association> claims_to_be_deleted = new ArrayList<Association>();
        for (Association claim : all_claims) {
            if (claim.getTypeUri().equals(WD_ENTITY_CLAIM_EDGE)) {
                if (claim.getRole1().getModel().getRoleTypeUri().equals("dm4.core.default")
                    && claim.getRole2().getModel().getRoleTypeUri().equals("dm4.core.default")) {
                    // just delete _all_ old, un-directed associations invloving me (to re-import them with direction)
                    claims_to_be_deleted.add(claim);
                }
                // every "claim" where i am the "parent" is to be deleted and re-created
                if ((claim.getRole2().getModel().getRoleTypeUri().equals("dm4.core.parent")
                    && claim.getRole2().getPlayerId() == wikidataItem.getId())
                    || (claim.getRole1().getModel().getRoleTypeUri().equals("dm4.core.parent") &&
                        claim.getRole1().getPlayerId() == wikidataItem.getId())) {
                    if (!(claim.getRole2().getPlayerId() == wikidataItem.getId() && // ### cannot remove association to one-self
                          claim.getRole1().getPlayerId() == wikidataItem.getId())) {
                        claims_to_be_deleted.add(claim);
                    } else {
                        log.severe("IDENTIFIED self-referential association EXISTS IN DB - Data Inconsistency!");
                    }
                }
            }
        }
        log.info("> " + claims_to_be_deleted.size() + " claims to be DELETED");
        for (Association edge : claims_to_be_deleted) {
            log.fine("> Associaton \""+edge.getSimpleValue()+"\" is to be deleted (" + edge.getUri() + ")\n"
                + " from 1: \""+edge.getRole1().getPlayer().getSimpleValue()+"\" ==>\n"
                + " to 2: \""+edge.getRole2().getPlayer().getSimpleValue() + "\"");
            edge.delete();
        }
    }

    /**
     * The "from" Topic plays the role of a "parent" and the "to" topic plays role of a "child", analogous to the
     * relation between wikidata items in the semantics of a *Claim*.
     *
     * @param   claim_guid
     *      String  GUID of the Wikidata Claim.
     * @param   from
     *      Topic   Wikidata Search Entity topic of type "item" playing the role of a *dm4.core.parent* in the claim.
     * @param   to
     *      Topic   Wikidata Search Entity topic of type "item" playing the role of a *dm4.core.child* in the claim.
     * @param   property
     *      Topic   Wikidata Search Entity topic of type "property" playing the role of a "Wikidata Search Entity" in
     *      the claim.
     */
    private Association createWikidataClaim(String claim_guid, Topic from, Topic to, Topic property) {
        Association claim = null;
        DeepaMehtaTransaction dx = dms.beginTx();
        try {
            if (!associationExists(WD_ENTITY_CLAIM_EDGE, from, to)
                && (to.getId() != from.getId())) { // ### dm4 does not allow self-referential associations
                // 1) Create \"Wikidata Claim\"-Edge with GUID
                claim = dms.createAssociation(new AssociationModel(WD_ENTITY_CLAIM_EDGE,
                    new TopicRoleModel(from.getId(), "dm4.core.parent"),
                    new TopicRoleModel(to.getId(), "dm4.core.child")));
                claim.setUri(claim_guid);
                // 2) Assign wikidata property (=Wikidata Search Entity) to this claim-edge
                claim.setChildTopics(new ChildTopicsModel().putRef(WD_SEARCH_ENTITY_URI,
                        property.getUri()));
                // ### problems with missing aggregated childs for composite assocTypes to be investigated ..
                dms.updateAssociation(claim.getModel());
                claim.loadChildTopics();
            }
            dx.success();
            dx.finish();
            return claim;
        } catch (Exception e) {
            log.severe("FAILED to create a \"Claim\" between \""+from.getSimpleValue()+"\" - \""+to.getSimpleValue());
            dx.failure();
            throw new RuntimeException(e);
        }
    }

    /**
     * Note: ...
     * @param value
     * @param lang
     * @return
     */
    private Topic getWikidataTextTopic(String value, String lang) {
        Topic textValue = null;
        // 1) query for text-value
        try {
            textValue = dms.getTopic(WD_TEXT_TYPE_URI, new SimpleValue(value));
        } catch (Exception ex) {
            // log.info("Could not find a wikidata-text value topic for \"" + value + ex.getMessage() + "\"");
        }
        // 2) re-use  or create
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            if (textValue == null) {
                textValue = dms.createTopic(new TopicModel(WD_TEXT_TYPE_URI, new SimpleValue(value)));
                log.info("CREATED \"Wikidata Text\" - \"" + value +"\" (" + lang + ") - OK!");
            } /** else {
                log.info("FETCHED \"Wikidata Text\" - \"" + textValue.getSimpleValue() +"\" "
                        + "(" + lang + ") - Re-using it!");
            } **/
            tx.success();
            tx.finish();
            return textValue;
        } catch (Exception ex) {
            tx.failure();
            log.warning("FAILURE during creating a wikidata value topic: " + ex.getLocalizedMessage());
            throw new RuntimeException(ex);
        }
    }

    /**
     * Note: ...
     * @param fileName
     * @return
     */
    private Topic getWikimediaCommonsMediaTopic(String fileName) {
        Topic mediaTopic = dms.getTopic(WD_COMMONS_MEDIA_NAME_TYPE_URI, new SimpleValue(fileName));
        if (mediaTopic == null) { // create new media topic
            DeepaMehtaTransaction dx = dms.beginTx();
            ChildTopicsModel mediaCompositeModel = new ChildTopicsModel()
                .put(WD_COMMONS_MEDIA_NAME_TYPE_URI, fileName);
            // load item with filename via markus's commons file media API
            loadWikimediaCommonsMediaItem(mediaCompositeModel, fileName);
            TopicModel mediaTopicModel = new TopicModel(WD_COMMONS_MEDIA_TYPE_URI, mediaCompositeModel);
            try {
                mediaTopic = dms.createTopic(mediaTopicModel).loadChildTopics();
                log.info("Created new Wikimedia Commons Media Topic \"" + mediaTopic.getSimpleValue().toString());
                dx.success();
                dx.finish();
            } catch (RuntimeException re) {
                log.log(Level.SEVERE, "Could not create Wikidata Commons Media Topic", re);
                dx.failure();
            }
        } else {
            mediaTopic = mediaTopic.getRelatedTopic("dm4.core.composition", 
                "dm4.core.child", "dm4.core.parent", WD_COMMONS_MEDIA_TYPE_URI);
        }
        // reference existing media topic ### here is no update mechanism yet
        return mediaTopic;
    }

    /**
     * ...
     * @param model
     * @param fileName
     */
    private void loadWikimediaCommonsMediaItem(ChildTopicsModel model, String fileName) {
        // 1) fetch data by name from http://tools.wmflabs.org/magnus-toolserver/commonsapi.php?image=
        URL requestUri;
        StringBuffer resultBody = new StringBuffer();
        String xml_result = "";
        try {
            requestUri = new URL("http://tools.wmflabs.org/magnus-toolserver/commonsapi.php?image=" 
                    + URLEncoder.encode(fileName, CHARSET));
            log.fine("Requesting Wikimedia Commons Item Details: " + requestUri.toString());
            // 2) initiate request
            HttpURLConnection connection = (HttpURLConnection) requestUri.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "DeepaMehta "+DEEPAMEHTA_VERSION+" - "
                    + "Wikidata Search " + WIKIDATA_TYPE_SEARCH_VERSION);
            // 3) check the response
            int httpStatusCode = connection.getResponseCode();
            if (httpStatusCode != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("Error with HTTPConnection, HTTP Status: " + httpStatusCode);
            }
            // 4) read in the response
            BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream(), CHARSET));
            for (String input; (input = rd.readLine()) != null;) {
                resultBody.append(input);
            }
            rd.close();
            // 5) process response
            if (resultBody.toString().isEmpty()) {
                throw new RuntimeException("Wikidata was silent, HTTP Response: No content!");
            } else {
                DocumentBuilder builder;
                Document document;
                xml_result = resultBody.toString();
                builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                document = builder.parse(new InputSource(new ByteArrayInputStream(xml_result.getBytes("utf-8"))));
                NodeList responses = document.getElementsByTagName("response");
                // Node defaultLanguageDescr = responses.item(1).getFirstChild();
                Node fileElement = responses.item(0).getFirstChild();
                // 
                Node resourceUrls = fileElement.getChildNodes().item(2);
                NodeList resourceElements = resourceUrls.getChildNodes(); // file and description as childs
                Node filePath = resourceElements.item(0); // file at 0 
                Node authorUrl = fileElement.getChildNodes().item(10); // authorUrl HTML at 10
                Node permission = fileElement.getChildNodes().item(12); // permission HTML at 12
                // 
                String authorText = (authorUrl != null) ? authorUrl.getTextContent() : "No author information available.";
                String permissionText = (permission != null) ? permission.getTextContent() : "No license information available.";
                model.put(WD_COMMONS_MEDIA_PATH_TYPE_URI, filePath.getTextContent());
                // model.put(WD_COMMONS_MEDIA_DESCR_TYPE_URI, defaultLanguageDescr.getTextContent());
                model.put(WD_COMMONS_AUTHOR_HTML_URI, authorText);
                model.put(WD_COMMONS_LICENSE_HTML_URI, permissionText);
            }
        } catch (MalformedURLException e) {
            log.log(Level.SEVERE, "Wikidata Plugin: MalformedURLException ...", e);
        } catch (ParserConfigurationException e) {
            log.log(Level.SEVERE, "Wikidata Plugin: ParserConfigurationException ...", e);
        } catch (IOException ioe) {
            log.log(Level.SEVERE, "Wikidata Plugin: IOException ...", ioe);
        } catch (SAXException ex) {
            log.log(Level.SEVERE, null, ex);
        } catch (DOMException e) {
            log.log(Level.SEVERE, null , e);
        }
    }



    // --- DeepaMehta 4 Plugin Related Private Methods

    @Override
    public void assignToWikidataWorkspace(DeepaMehtaObject object) {
        if (object == null) return;
        if (wikidataWorkspace == null) wikidataWorkspace = wsService.getWorkspace(WS_WIKIDATA_URI);
        wsService.assignToWorkspace(object, wikidataWorkspace.getId());
    }

    @Override
    public void assignTypeToWikidataWorkspace(Type type) {
        if (type == null) return;
        if (wikidataWorkspace == null) wikidataWorkspace = wsService.getWorkspace(WS_WIKIDATA_URI);
        wsService.assignTypeToWorkspace(type, wikidataWorkspace.getId());
    }

    private boolean associationExists(String edge_type, Topic item, Topic user) {
        List<Association> results = dms.getAssociations(item.getId(), user.getId(), edge_type);
        return (results.size() > 0) ? true : false;
    }

    @Override
    public void postCreateTopic(Topic topic) {
        if (   topic.getTypeUri().equals("org.deepamehta.wikidata.search_entity")
            || topic.getTypeUri().equals("org.deepamehta.wikidata.commons_media")
            || topic.getTypeUri().equals("org.deepamehta.wikidata.globe_coordinate")
            || topic.getTypeUri().equals("org.deepamehta.wikidata.language")
            || topic.getTypeUri().equals("org.deepamehta.wikidata.language_code_iso")
            || topic.getTypeUri().equals("org.deepamehta.wikidata.language_name")) {
            assignToWikidataWorkspace(topic);
        }

    }

    public void postCreateAssociation(Association association) {
        if (association.getTypeUri().equals("org.deepamehta.wikidata.claim_edge")) {
            assignToWikidataWorkspace(association);
        }
    }
}
