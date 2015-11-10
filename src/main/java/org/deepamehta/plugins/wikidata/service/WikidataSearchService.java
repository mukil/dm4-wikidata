
package org.deepamehta.plugins.wikidata.service;

import de.deepamehta.core.RelatedAssociation;
import de.deepamehta.core.service.ResultList;
import de.deepamehta.core.Topic;
import de.deepamehta.core.DeepaMehtaObject;

/**
 * A very basic plugin to search and explore wikidata.
 * Creates \"Wikidata Search Result Entity\"-Topics.
 * Corresponding to the type of wikidata-entity found via a search these topics have either the child-type
 * ("org.deepamehta.wikidata.search_entity_type") set to the value "property or the value "item".
 *
 * @author Malte Reißig (<malte@mikromedia.de>)
 * @website https://github.com/mukil/dm4-wikidata
 * @version 0.0.4-SNAPSHOT
 */
public interface WikidataSearchService {

    /**
     *  This method searches all wikidata entities by text and the given language code.
     *
     *  @param {entity}             entity-type (can be of entity-type "item" or "property")
     *  @param {query}              name of wikidata property in search
     *  @param {language_code}      ISO 639-1 language code (must exist in DM installation)
     */
    Topic searchWikidataEntity(String query, String iso_language_code, String entityType);

    /**
     *  This method gets (or creates) a \"Wikidata Search Entity\" (in DeepaMehta 4) by its ID (wikidata).
     *  Updates values of the given topic if this wikidata-entity was already imported before.
     *
     *  @param {entityId}           wikidataId
     */
    Topic getOrCreateWikidataEntity(String wikidataEntityId, String iso_language_code);

    /**
     *  This method loads all claims (with language specific values) for a wikidata-entity into DeepaMehta 4.
     *
     *  @param {id}              \"Wikidata Search Entity\"-Topic ID
     */
    Topic loadClaimsAndRelatedWikidataItems(long entityTopicId, String iso_language_code);

    /**
     *  This method creates a DeepaMehta Association Type given a \"Wikidata Search Entity\" (of type=property).
     *
     *  @param {id}              \"Wikidata Search Entity\"-Topic ID (entity-type must be of value "property")
     */
    Topic createWikidataAssociationType(long entityTopicId);

    /**
     *  This method retrieves all associated associations of type "Wikidata Claim" for any given
     * \"Wikidata Search Entity\" (of type=property).
     *
     *  @param {id}              \"Wikidata Search Entity\"-Topic ID (entity-type must be of value "property")
     */
    ResultList<RelatedAssociation> getTopicRelatedAssociations(long topicId);
    
    void assignToWikidataWorkspace(DeepaMehtaObject object);

}
