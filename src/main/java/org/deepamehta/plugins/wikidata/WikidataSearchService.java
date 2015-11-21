
package org.deepamehta.plugins.wikidata;

import de.deepamehta.core.RelatedAssociation;
import de.deepamehta.core.service.ResultList;
import de.deepamehta.core.Topic;
import de.deepamehta.core.Type;
import de.deepamehta.core.DeepaMehtaObject;

/**
 * A plugin to search and explore wikidata with the dm4-webclient.
 *
 * Creates topics of type "Wikidata Search Entity" and aggregates those into "Wikidata Search Bucket".
 *
 * Corresponding to the type on wikidata.org (Item, Property) the entity fetched by this plugin have their type
 * set in the child-type <code>org.deepamehta.wikidata.search_entity_type</code>, either to the String "property or
 * "item".
 *
 * <a href="https://github.com/mukil/dm4-wikidata">Source Code Repository</a>
 *
 * @author Malte Reißig (<a href="mailto:malte@mikromedia.de">Contact</a>)
 * @version 0.0.5-SNAPSHOT
 */
public interface WikidataSearchService {

    /**
     *  This method searches all wikidata entities by text and the given language code.
     *
     *  @param entity             String entity-type (can be of entity-type "item" or "property")
     *  @param query              String name of wikidata property in search
     *  @param language_code      String ISO 639-1 language code (must exist in your database)
     */
    Topic searchWikidataEntity(String query, String iso_language_code, String entityType);

    /**
     *  This method gets (or creates) a \"Wikidata Search Entity\" (in DeepaMehta 4) by its ID (wikidata).
     *  Updates values of the given topic if this wikidata-entity was already imported before.
     *
     *  @param entityId             String wikidataId (e.g. "Q42")
     *  @param iso_language_code    String ISO 639-1 language code (must exist in your database)
     *  @param doUpdate             boolean If an existing entity will get updated its *label* and *description*.
     *  @param doAliasUpdates       boolean If all aggregated alias values will be deleted and re-created (in case of
     *                              an existing wikidataEntity and an update).
     */
    Topic importWikidataEntity(String wikidataEntityId, String iso_language_code, boolean doUpdate, boolean
            doAliasUpdates);

    /**
     *  This method loads all claims (with language specific values) for a wikidata-entity into DeepaMehta 4.
     *
     *  @param id                   long Topic id of a "Wikidata Search Entity"
     *  @param iso_language_code    String ISO 639-1 language code (must exist in your database)
     */
    Topic importClaimsAndRelatedEntities(long entityTopicId, String iso_language_code);

    /**
     *  This method creates a DeepaMehta Association Type given a \"Wikidata Search Entity\" (of type=property).
     *
     *  @param id                   long Topic id of a "Wikidata Search Entity"
     */
    Topic createWikidataAssociationType(long entityTopicId);

    /**
     *  This method retrieves all associated associations of type "Wikidata Claim" for any given
     * \"Wikidata Search Entity\" (of type=property).
     *
     *  @param id                   long Topic id of a "Wikidata Search Entity"
     */
    ResultList<RelatedAssociation> getWikidataClaimsForPropertyEntity(long topicId);
    
    void assignToWikidataWorkspace(DeepaMehtaObject object);

    void assignTypeToWikidataWorkspace(Type type);

}
