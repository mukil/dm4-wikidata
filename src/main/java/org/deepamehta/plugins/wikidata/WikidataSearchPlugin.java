
package org.deepamehta.plugins.wikidata;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.RelatedAssociation;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.*;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.service.ResultList;
import de.deepamehta.core.service.annotation.ConsumesService;
import de.deepamehta.core.storage.spi.DeepaMehtaTransaction;
import de.deepamehta.plugins.accesscontrol.service.AccessControlService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.deepamehta.plugins.wikidata.service.WikidataSearchService;
import org.wikidata.wdtk.dumpfiles.DumpProcessingController;



/**
 * A very basic plugin to search and explore wikidata.
 * Allows to turn a \"Wikidata Search Result Entity\" (of type=property) into DeepaMehta 4 AssociationTypes.
 *
 * @author Malte Reißig (<malte@mikromedia.de>)
 * @website https://github.com/mukil/dm4-wikidata
 * @version 0.0.3-SNAPSHOT
 */

@Path("/wikidata")
@Consumes("application/json")
@Produces("application/json")
public class WikidataSearchPlugin extends PluginActivator implements WikidataSearchService {

    private Logger log = Logger.getLogger(getClass().getName());

    private final String DEEPAMEHTA_VERSION = "DeepaMehta 4.4";
    private final String WIKIDATA_TYPE_SEARCH_VERSION = "0.0.4-SNAPSHOT";
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
    private final String WD_COMMONS_MEDIA_TYPE_TYPE_URI = "org.deepamehta.wikidata.commons_media_type";
    private final String WD_COMMONS_LICENSE_NAME_TYPE_URI = "org.deepamehta.wikidata.commons_media_license_name";
    private final String WD_COMMONS_LICENSE_INFO_TYPE_URI = "org.deepamehta.wikidata.commons_media_license_info";
    // private final String WD_GLOBE_COORDINATE_TYPE_URI = "org.deepamehta.wikidata.globe_coordinate";

    private final String WD_ENTITY_CLAIM_EDGE = "org.deepamehta.wikidata.claim_edge";

    // --- Wikidata Service URIs

    private final String WD_SEARCH_ENTITIES_ENDPOINT =
            "http://www.wikidata.org/w/api.php?action=wbsearchentities&format=json&limit=50";
    private final String WD_CHECK_ENTITY_CLAIMS_ENDPOINT =
            "http://www.wikidata.org/w/api.php?action=wbgetclaims&format=json"; // &ungroupedlist=0
    private final String WD_GET_ENTITY_ENDPOINT = "http://www.wikidata.org/w/api.php?action=wbgetentities"
            + "&props=info%7Csitelinks%2Furls%7Caliases%7Clabels%7Cdescriptions&dir=ascending&format=json";
    private final String WD_SEARCH_ENTITY_TYPE_PROPERTY = "property";
    private final String WD_SEARCH_ENTITY_TYPE_ITEM = "item";

    // --- Instance Variables

    private final String WIKIDATA_ENTITY_URL_PREFIX = "//www.wikidata.org/wiki/";
    private final String WIKIDATA_PROPERTY_ENTITY_URL_PREFIX = "Property:";
    private final String WIKIMEDIA_COMMONS_MEDIA_FILE_URL_PREFIX = "//commons.wikimedia.org/wiki/File:";
    
    private final int TIMEOUT_SEC = 2; // Time the global Wikidata JSON dump is processed on import

    private boolean isInitialized = false;
    private AccessControlService acService = null;



    @GET
    @Path("/search/{entity}/{query}/{language_code}")
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public Topic searchWikidataEntity(@PathParam("query") String query, @PathParam("language_code") String lang,
                @HeaderParam("Cookie") ClientState clientState, @PathParam("entity") String type) {

        String json_result = "";
        StringBuffer resultBody = new StringBuffer();
        URL requestUri = null;
        Topic search_bucket = null;
        // sanity check (set en as default-language if nothing was provided by un-initialized language widget)
        if (lang == null || lang.equals("undefined")) {
            log.warning("Wikidata Language Search Option was not provided, now requesting data in EN");
            lang = WikidataEntityMap.LANG_EN;
        }
        // start search operation
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            // 1) fixme: Authorize request
            requestUri = new URL(WD_SEARCH_ENTITIES_ENDPOINT + "&search="+ query +"&language="+ lang +"&type=" + type);
            log.fine("Wikidata Search Entities Request: " + requestUri.toString());
            // 2) initiate request
            HttpURLConnection connection = (HttpURLConnection) requestUri.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "DeepaMehta "+DEEPAMEHTA_VERSION+" - "
                    + "Wikidata Search " + WIKIDATA_TYPE_SEARCH_VERSION);
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
                log.fine("Wikidata Search Request Response: " + json_result);
                processWikidataEntitySearch(json_result, bucket_model, type, lang);
                search_bucket = dms.createTopic(new TopicModel(WD_SEARCH_BUCKET_URI, bucket_model), clientState);
                // workaround: addRef does not (yet) fetchComposite, so fetchComposite=true
                search_bucket = dms.getTopic(search_bucket.getId(), true);
                log.info("Wikidata Search Bucket for "+ query +" in ("+ lang +") was CREATED");
            }
            tx.success();
        } catch (MalformedURLException e) {
            log.warning("Wikidata Plugin: MalformedURLException ..." + e.getMessage());
            tx.failure();
            throw new RuntimeException("Could not find wikidata endpoint.", e);
        } catch (IOException ioe) {
            tx.failure();
            throw new WebApplicationException(new Throwable(ioe), Status.BAD_REQUEST);
        } catch (Exception e) {
            tx.failure();
            throw new WebApplicationException(new Throwable(e), Status.INTERNAL_SERVER_ERROR);
        } finally {
            tx.finish();
            return search_bucket;
        }
    }

    @GET
    @Path("/{entityId}/{language_code}")
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public Topic getOrCreateWikidataEntity(@PathParam("entityId") String entityId,
        @PathParam("language_code") String language_code, @HeaderParam("Cookie") ClientState clientState) {
        String json_result = "";
        StringBuffer resultBody = new StringBuffer();
        URL requestUri = null;
        Topic entity = null;
        // sanity check (set en as default-language if nothing was provided by un-initialized language widget)
        if (language_code == null || language_code.equals("undefined")) {
            log.warning("Wikidata Language Search Option was not provided, now requesting data in EN");
            language_code = WikidataEntityMap.LANG_EN;
        }
        try {
            // 1) fixme: Authorize request
            // &sites=dewiki&&languages=de
            requestUri = new URL(WD_GET_ENTITY_ENDPOINT + "&ids="+ entityId + "&languages=" + language_code);
            log.fine("Requesting Wikidata Entity Details " + requestUri.toString());
            // 2) initiate request
            HttpURLConnection connection = (HttpURLConnection) requestUri.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "DeepaMehta "+DEEPAMEHTA_VERSION+" - "
                    + "Wikidata Search " + WIKIDATA_TYPE_SEARCH_VERSION);
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
                // 6) Create or Update Wikidata Search Entity
                json_result = resultBody.toString();
                log.fine("Wikidata Entity Request Response: " + json_result);
                JSONObject response = new JSONObject(json_result);
                JSONObject entities = response.getJSONObject("entities");
                JSONObject response_entity = entities.getJSONObject(entityId);
                // 0) Check if we need to CREATE or UPDATE our search result entity item
                Topic existingEntity = dms.getTopic("uri",
                            new SimpleValue(WD_SEARCH_ENTITIY_DATA_URI_PREFIX + entityId), true);
                if (existingEntity == null) {
                    entity = createWikidataSearchEntity(response_entity, language_code, clientState);
                } else {
                    // Updates labels, descriptions, aliases, url and (query) language
                    entity = updateWikidataEntity(existingEntity, response_entity, language_code, clientState);
                }
            }
        } catch (MalformedURLException e) {
            log.warning("Wikidata Plugin: MalformedURLException ..." + e.getMessage());
            throw new RuntimeException("Could not find wikidata endpoint.", e);
        } catch (IOException ioe) {
            throw new WebApplicationException(new Throwable(ioe), Status.BAD_REQUEST);
        } catch (JSONException je) {
            throw new WebApplicationException(new Throwable(je), Status.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            throw new WebApplicationException(new Throwable(e), Status.INTERNAL_SERVER_ERROR);
        } finally {
            return entity;
        }
    }

    @GET
    @Path("/check/claims/{id}/{language_code}")
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public Topic loadClaimsAndRelatedWikidataItems(@PathParam("id") long topicId,
            @PathParam("language_code") String language_option, @HeaderParam("Cookie") ClientState clientState) {

        String json_result = "";
        StringBuffer resultBody = new StringBuffer();
        URL requestUri = null;
        Topic wikidataItem = dms.getTopic(topicId, true);
        // 0) sanity check (set en as default-language if nothing was provided by un-initialized language widget)
        if (language_option == null || language_option.equals("undefined")) {
            log.warning("Wikidata Language Search Option was not provided, now requesting data in EN.");
            language_option = WikidataEntityMap.LANG_EN;
        }
        String wikidataId = wikidataItem.getUri().replaceAll(WD_SEARCH_ENTITIY_DATA_URI_PREFIX, "");
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            // 1) ### Authorize request
            // 2) ### be explicit and add "&rank=normal" to wbgetclaims-call
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
                json_result = resultBody.toString();
                log.fine("Wikidata Claim Request Response: " + json_result);
                processWikidataClaims(json_result, wikidataItem, language_option, clientState);
                log.info("Wikidata Claim Response is FINE");
            }
            tx.success();
        } catch (MalformedURLException e) {
            log.warning("Wikidata Plugin: MalformedURLException ..." + e.getMessage());
            tx.failure();
            throw new RuntimeException("Could not find wikidata endpoint.", e);
        } catch (IOException ioe) {
            tx.failure();
            throw new WebApplicationException(new Throwable(ioe), Status.BAD_REQUEST);
        } catch (Exception e) {
            tx.failure();
            throw new WebApplicationException(new Throwable(e), Status.INTERNAL_SERVER_ERROR);
        } finally {
            tx.finish();
            return wikidataItem;
        }
    }

    @GET
    @Path("/property/turn/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public Topic createWikidataAssociationType(@PathParam("id") long id,
            @HeaderParam("Cookie") ClientState clientState) {

        AssociationType association_type = null;
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
            // 3) Associated search-result-entity to new assoc-type (to keep track)
            dms.createAssociation(new AssociationModel("dm4.core.association",
                    new TopicRoleModel(property_entity.getUri(), "dm4.core.default"),
                    new TopicRoleModel(association_type.getUri(), "dm4.core.default")
                    ), clientState);
            log.info("Turned wikidata property \""+ property_entity.getUri() +"\" into DM Association Type!");
            tx.success();
        } catch (Error e) {
            tx.failure();
            log.warning("OH: The Wikidata Plugin experienced an unforeseen error! "+ e.getMessage());
        } finally {
            tx.finish();
            return association_type;
        }
    }

    @GET
    @Path("/property/related/claims/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public List<RelatedAssociation> getTopicRelatedAssociations (@PathParam("id") long topicId) {
        Topic topic = dms.getTopic(topicId, false);
        List<RelatedAssociation> associations = topic.getRelatedAssociations("dm4.core.aggregation",
                "dm4.core.child", "dm4.core.parent", "org.deepamehta.wikidata.claim_edge", false, false);
        return associations;
    }
    
    @GET
    @Path("/import/humans")
    @Produces(MediaType.TEXT_PLAIN)
    public String processPersonsFromWikidataDump() {
        importWikidataPersonas();
        return "OK";
    }
    
    @Override
    public void init() {
        // Delete all "Person"-Topics
        /** for (RelatedTopic person : dms.getTopics("dm4.contacts.person", false, 0)){
            dms.deleteTopic(person.getId());
        }
        // Delete all "Institution"-Topics
        for (RelatedTopic institution : dms.getTopics("dm4.contacts.institution", false, 0)){
            dms.deleteTopic(institution.getId());
        }
        // Start importing "Person"-Topcis from WD
        importWikidataPersonas(); **/
    }

    

    // --
    // ---  Wikidata Search (Application Specific) Private Methods
    // --

    private void processWikidataEntitySearch(String json_result, CompositeValueModel search_bucket,
            String type, String lang) {
        try {
            JSONObject response = new JSONObject(json_result);
            JSONArray result = response.getJSONArray("search");
            if (result.length() > 0) {
                for (int i = 0; i < result.length(); i++) {
                    JSONObject entity_response = result.getJSONObject(i);
                    // Check if entity already exists
                    String id = entity_response.getString("id");
                    Topic existing_entity = dms.getTopic("uri",
                            new SimpleValue(WD_SEARCH_ENTITIY_DATA_URI_PREFIX + id), false);
                    if (existing_entity == null) {
                        // Create new search entity composite
                        String name = entity_response.getString("label");
                        String url = entity_response.getString("url");
                        //
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
                        // set enity place in resultset
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
            log.warning("Wikidata Plugin: JSONException during processing a wikidata entity search response. "
                    + ex.getMessage());
        }
    }

    private Topic createWikidataSearchEntity(JSONObject entity_response, String lang, ClientState clientState) {
        Topic entity = null;
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            String id = entity_response.getString("id");
            // Create new search entity composite
            CompositeValueModel entity_composite = buildWikidataEntityModel(entity_response, lang);
            TopicModel entity_model = new TopicModel(WD_SEARCH_ENTITIY_DATA_URI_PREFIX + id,
                    WD_SEARCH_ENTITY_URI, entity_composite);
            entity = dms.createTopic(entity_model, clientState);
            log.fine("Wikidata Search Entity Created (" +
                entity_composite.getString(WD_SEARCH_ENTITY_TYPE_URI)+ "): \"" + entity.getSimpleValue() +"\" - FINE!");
            tx.success();
        } catch (Exception ex) {
            log.warning("FAILED to create a \"Wikidata Search Entity\" caused by " + ex.getMessage());
            tx.failure();
        } finally {
            tx.finish();
            return entity;
        }
    }

    private Topic updateWikidataEntity(Topic entity, JSONObject entity_response, String lang, ClientState clientState) {
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            // Update existing search entity topic
            CompositeValueModel entity_composite = buildWikidataEntityModel(entity_response, lang);
            TopicModel entity_model = new TopicModel(entity.getId(), entity_composite);
            dms.updateTopic(entity_model, clientState);
            log.fine("Wikidata Search Entity Updated (" +
                entity_composite.getString(WD_SEARCH_ENTITY_TYPE_URI)+ "): \"" + entity.getSimpleValue() +"\" - FINE!");
            tx.success();
        } catch (Exception ex) {
            log.warning("FAILED to UPDATE \"Wikidata Search Entity\" caused by " + ex.getMessage());
            tx.failure();
        } finally {
            tx.finish();
            return entity;
        }
    }

    private CompositeValueModel buildWikidataEntityModel(JSONObject entity_response, String lang) {
        CompositeValueModel entity_composite = new CompositeValueModel();
        try {
            String id = entity_response.getString("id");
            String type = entity_response.getString("type");
            entity_composite = new CompositeValueModel();
            // main label
            if (entity_response.has("labels")) {
                JSONObject labels = entity_response.getJSONObject("labels");
                JSONObject languaged_label = labels.getJSONObject(lang);
                String label = languaged_label.getString("value");
                entity_composite.put(WD_SEARCH_ENTITY_LABEL_URI, label);
            }
            // main description
            if (entity_response.has("descriptions")) {
                JSONObject descriptions = entity_response.getJSONObject("descriptions");
                JSONObject languaged_descr = descriptions.getJSONObject(lang);
                String description = languaged_descr.getString("value");
                entity_composite.put(WD_SEARCH_ENTITY_DESCR_URI, description);
            }
            // aliases
            if (entity_response.has("aliases")) {
                JSONObject aliases = entity_response.getJSONObject("aliases");
                JSONArray languaged_aliases = aliases.getJSONArray(lang);
                for (int a=0; a < languaged_aliases.length(); a++) {
                    JSONObject alias_object = languaged_aliases.getJSONObject(a);
                    String alias = alias_object.getString("value");
                    entity_composite.add(WD_SEARCH_ENTITY_ALIAS_URI,
                        new TopicModel(WD_SEARCH_ENTITY_ALIAS_URI, new SimpleValue(alias)));
                }
            }
            // set wikidata url
            if (type.equals(WD_SEARCH_ENTITY_TYPE_PROPERTY)) {
                entity_composite.put(DM_WEBBROWSER_URL, WIKIDATA_ENTITY_URL_PREFIX
                        + WIKIDATA_PROPERTY_ENTITY_URL_PREFIX + id);
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
        } catch (JSONException jex) {
            log.warning("JSONException during build up of the search-entities composite model");
            return null;
        } finally {
            return entity_composite;
        }
    }


    private void processWikidataClaims(String json_result, Topic wikidataItem, String language_code,
            ClientState clientState) {
        try {
            JSONObject response = new JSONObject(json_result);
            JSONObject result = response.getJSONObject("claims");
            // ### Needs to identify if claims (already imported in DM4) are not yet part of the current wikidata-data
            Iterator properties = result.keys();
            log.info("Wikidata Plugin is processing all properties part of related CLAIMS");
            Topic propertyEntity = null;
            while (properties.hasNext()) {
                String property_id = properties.next().toString();
                // 1) Load related property-entity
                propertyEntity = getOrCreateWikidataEntity(property_id, language_code, clientState);
                // HashMap<String, List<Topic>> all_entities = new HashMap<String, List<Topic>>();
                JSONArray property_listing = result.getJSONArray(property_id);
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
                        referencedItemEntity = getOrCreateWikidataEntity(itemId, language_code, clientState);
                    } else if (snakDataType.equals("commonsMedia")) {
                        // do relate wikidata.commons_media
                        if (snakDataValue.has("value")) {
                            String fileName = snakDataValue.getString("value");
                            referencedItemEntity = getOrCreateWikimediaCommonsMediaTopic(fileName, clientState);
                            log.info(" --- FINE! --- Related Wikimedia Commons File to Wikidata Item!");
                        }
                        /**  **/
                        // ### make use of WIKIMEDIA_COMMONS_MEDIA_FILE_URL_PREFIX and implement page-renderer
                    } else if (snakDataType.equals("globe-coordinate")) {
                        // do relate wikidata.globe_coordinate
                        log.fine("Globe Coordinate claimed via \"" + propertyEntity.getSimpleValue()
                                + "\" ("+language_code+") DEBUG:");
                        log.fine("  " + snakDataValue.toString());
                    } else if (snakDataType.equals("url")) {
                        if (snakDataValue.has("value")) {
                            // ### getOrCreateWebResource()
                            String value = snakDataValue.getString("value");
                            log.warning("### SKIPPING URL => " + value);
                        }
                    } else if (snakDataType.equals("string")) {
                        if (snakDataValue.has("value")) {
                            String value = snakDataValue.getString("value");
                            referencedItemEntity = getOrCreateWikidataText(value, language_code, clientState);
                        } else {
                            log.warning("Could not access wikidata-text value - json-response EMPTY!");
                        }
                    } else {
                        log.warning("Value claimed as " + propertyEntity.getSimpleValue() + " is not of any known type"
                                + " wikibase-item but \"" + snakDataType +"\" ("+snakDataValue+")");
                        // e.g. snakDataType.equals("quantity")
                    }
                    // store topic reference to (new or already existing) wikidata-entity/ resp. -value topic
                    if (referencedItemEntity != null) {
                        createWikidataClaimEdge(claim_guid, wikidataItem, referencedItemEntity,
                            propertyEntity, clientState);
                    } else {
                        log.warning("SKIPPED creating claim of type \""+snakDataType+"\" value for "
                                + "\""+propertyEntity.getSimpleValue()+"\"");
                    }
                }
                /** Iterator entity_iterator = all_entities.keySet().iterator();
                StringBuffer requesting_ids = new StringBuffer();
                while (entity_iterator.hasNext()) {
                    String entity_id = entity_iterator.next().toString();
                    requesting_ids.append(entity_id + "|");
                }
                log.info("Requesting ALL ITEMS for " +property_id+ ": " + requesting_ids.toString());
                omitting this solution bcause: "*": "Too many values supplied for parameter 'ids': the limit is 50" **/
            }
        } catch (JSONException ex) {
            log.warning("JSONException during processing a wikidata claim. " + ex.getMessage());
        }
    }

    private Association createWikidataClaimEdge (String claim_guid, Topic one, Topic two,
            Topic property, ClientState clientState) {
        DeepaMehtaTransaction tx = dms.beginTx();
        Association claim = null;
        try {
            if (!associationExists(WD_ENTITY_CLAIM_EDGE, one, two)) {
                // 1) Create \"Wikidata Claim\"-Edge with GUID
                claim = dms.createAssociation(new AssociationModel(WD_ENTITY_CLAIM_EDGE,
                    new TopicRoleModel(one.getId(), "dm4.core.default"),
                    new TopicRoleModel(two.getId(), "dm4.core.default")), clientState);
                claim.setUri(claim_guid);
                log.info("Created \"Wikidata Claim\" with GUID: " + claim.getUri() +" for \"" + two.getSimpleValue() +
                                " (property: " + property.getSimpleValue() +
                                "\") for \"" + one.getSimpleValue() + "\" - FINE");
                // 2) Assign wikidata property (=Wikidata Search Entity) to this claim-edge
                claim.setCompositeValue(new CompositeValueModel().putRef(WD_SEARCH_ENTITY_URI,
                        property.getUri()), clientState, null);
            }
            tx.success();
        } catch (Exception e) {
            log.severe("FAILED to create a \"Claim\" between \""+one.getSimpleValue()+"\" - \""+two.getSimpleValue());
            tx.failure();
            throw new RuntimeException(e);
        } finally {
            tx.finish();
            return claim;
        }
    }

    private Topic getOrCreateWikidataText(String value, String lang, ClientState clientState) {
        Topic textValue = null;
        // 1) query for text-value
        try {
            textValue = dms.getTopic(WD_TEXT_TYPE_URI, new SimpleValue(value), false);
        } catch (Exception ex) {
            log.info("Could not find a wikidata-text value topic for \"" + value + ex.getMessage() + "\"");
        }
        // 2) re-use  or create
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            if (textValue == null) {
                textValue = dms.createTopic(new TopicModel(WD_TEXT_TYPE_URI, new SimpleValue(value)), clientState);
                log.info("CREATED \"Wikidata Text\" - \"" + value +"\" (" + lang + ") - OK!");
            } /** else {
                log.info("FETCHED \"Wikidata Text\" - \"" + textValue.getSimpleValue() +"\" "
                        + "(" + lang + ") - Re-using it!");
            } **/
            tx.success();
        } catch (Exception ex) {
            log.warning("FAILURE during creating a wikidata value topic: " + ex.getLocalizedMessage());
            tx.failure();
        } finally {
            tx.finish();
            return textValue;
        }
    }



    // --
    // --- DeepaMehta 4 Plugin Related Private Methods
    // --

    private boolean associationExists(String edge_type, Topic item, Topic user) {
        List<Association> results = dms.getAssociations(item.getId(), user.getId(), edge_type);
        return (results.size() > 0) ? true : false;
    }

    private void assignToWikidataWorkspace(Topic topic) {
        // fixme: remove assignment of type to other (selected via clientState) workspace
        Topic defaultWorkspace = dms.getTopic("uri", new SimpleValue(WS_WIKIDATA_URI), false);
        dms.createAssociation(new AssociationModel("dm4.core.aggregation",
            new TopicRoleModel(topic.getId(), "dm4.core.parent"),
            new TopicRoleModel(defaultWorkspace.getId(), "dm4.core.child")
        ), null);
    }

    /** --- Implementing PluginService Interfaces to consume AccessControlService --- */

    @Override
    @ConsumesService("de.deepamehta.plugins.accesscontrol.service.AccessControlService")
    public void serviceArrived(PluginService service) {
        if (service instanceof AccessControlService) {
            acService = (AccessControlService) service;
        }
    }

    @Override
    @ConsumesService("de.deepamehta.plugins.accesscontrol.service.AccessControlService")
    public void serviceGone(PluginService service) {
        if (service == acService) {
            acService = null;
        }
    }

    private Topic getOrCreateWikimediaCommonsMediaTopic(String fileName, ClientState clientState) {
        Topic mediaTopic = dms.getTopic(WD_COMMONS_MEDIA_NAME_TYPE_URI, new SimpleValue(fileName), false);
        if (mediaTopic == null) { // create new media topic
            CompositeValueModel mediaCompositeModel = new CompositeValueModel().
                put(WD_COMMONS_MEDIA_NAME_TYPE_URI, fileName);
            // ### enrichAboutWikimediaCommonsMetaData(mediaCompositeModel, fileName);
            TopicModel mediaTopicModel = new TopicModel(WD_COMMONS_MEDIA_TYPE_URI, mediaCompositeModel);
            mediaTopic = dms.createTopic(mediaTopicModel, clientState);
            log.info("Created new Wikimedia Commons Media Topic \"" + mediaTopic.getSimpleValue().toString());
        } else {
            mediaTopic = mediaTopic.getRelatedTopic("dm4.core.composition", 
                "dm4.core.child", "dm4.core.parent", WD_COMMONS_MEDIA_TYPE_URI, true, false);
        }
        // reference existing media topic ### here is no update mechanism yet
        return mediaTopic;
    }

    private void enrichAboutWikimediaCommonsMetaData(CompositeValueModel model, String fileName) {
        // 1) fetch data by name from http://tools.wmflabs.org/magnus-toolserver/commonsapi.php?image=
        // 2) mediaCompositeModel.put(WD_COMMONS_MEDIA_PATH_TYPE_URI, filePath);
    }
    
    
    
    // --
    // --- Methods to process and import topics based on a complete (daily) wikidatawiki (json) dump.
    // --
        
    private void importWikidataPersonas() {
        WikidataPersonaProcessor wikidataPersonaProcessor = new WikidataPersonaProcessor(dms, TIMEOUT_SEC);
        processEntitiesFromWikidataDump(wikidataPersonaProcessor);
    }
    
    /**
     * Processes all entities in a Wikidata dump using the given entity
     * processor. By default, the most recent JSON dump will be used. In offline
     * mode, only the most recent previously downloaded file is considered.
     *
     * @param entityProcessor the object to use for processing entities
     * in this dump
     */
    private void processEntitiesFromWikidataDump(WikidataPersonaProcessor entityProcessor) {
        // Importer settings
        boolean OFFLINE_MODE = false;
        boolean ONLY_CURRENT_REVISIONS = false;
        // Controller object for processing dumps:
        DumpProcessingController dumpProcessingController = new DumpProcessingController("wikidatawiki");
        dumpProcessingController.setOfflineMode(OFFLINE_MODE);
        try {
            // Use another download directory:
            String path = findDumpDirectoryPath();
            log.info("Processing complete most recent wikidata (json) dump from " + path);
            dumpProcessingController.setDownloadDirectory(path);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        // Register two entity processors, the one given for persons, the other as a timer
        dumpProcessingController.registerEntityDocumentProcessor(entityProcessor, null, ONLY_CURRENT_REVISIONS);
        try {
           dumpProcessingController.processMostRecentJsonDump();
        } catch (Exception e) {
            log.severe("Exception of type " + e.getClass() + " caught silent!");
            // The timer caused a time out. Continue and finish normally.
        }
        entityProcessor.stop();
        ResultList<RelatedTopic> personas = dms.getTopics("dm4.contacts.person", false, 0);
        ResultList<RelatedTopic> institutions = dms.getTopics("dm4.contacts.institution", false, 0);
        if (personas != null) {
            log.info("Imported " + personas.getSize() + " personas and " 
                + institutions.getSize() + " institutions from wikidata.org");
        }
    }
    
    private String findDumpDirectoryPath() {
        // ### use Sysetm.getenv() for the best OS independent solution
        // see http://docs.oracle.com/javase/6/docs/api/java/lang/System.html
        String filerepo = System.getProperty("dm4.filerepo.path");
        if (filerepo != null && !filerepo.isEmpty()) {
            return filerepo + "/";
        }
        String userhome = System.getProperty("user.home");
        if (userhome != null) {
            return userhome + "/";
        }
        return "";
    }
    
}
