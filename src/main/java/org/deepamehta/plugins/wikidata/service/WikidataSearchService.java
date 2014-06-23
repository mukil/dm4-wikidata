
package org.deepamehta.plugins.wikidata.service;

import de.deepamehta.core.Topic;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.PluginService;

/**
 * A very basic plugin to search and explore wikidata.
 * Creates \"Wikidata Search Result Entity\"-Topics containing the child-type "org.deepamehta.wikidata.search_entity_type"
 * with either the value "property or the value "item".
 *
 * @author Malte Reißig (<malte@mikromedia.de>)
 * @website https://github.com/mukil/dm4-wikidata
 * @version 0.0.3-SNAPSHOT
 */
public interface WikidataSearchService extends PluginService {

    /**
     *  This method searches all wikidata entities by text and the given language code.
     *
     *  @param {entity}             entity-type (either "item" or "property")
     *  @param {query}              name of wikidata property in search
     *  @param {language_code}      ISO 639-1 language code (must exist in DM installation)
     */
    Topic searchWikidataEntity(String query, String iso_language_code, ClientState clientstate, String entityType);

    /**
     *  This method gets (or creates) a \"Wikidata Search Entity\" (in DeepaMehta 4) by its ID (wikidata).
     *  ### Still needs to support update of values (if entity was already imported)
     *
     *  @param {entityId}           wikidataId
     */
    Topic getOrCreateWikidataEntity(String wikidataEntityId, String iso_language_code, ClientState clientState);

    /**
     *  This method loads all (language-specific) claims for a wikidata-entity into DeepaMehta 4.
     *
     *  @param {id}              \"Wikidata Search Entity\"-Topic ID
     */
    Topic loadClaimsAndRelatedWikidataItems(long entityTopicId, String iso_language_code, ClientState clientState);

    /**
     *  This method creates a DeepaMehta Association Type given a \"Wikidata Search Entity\" (of type=property).
     *
     *  @param {id}              \"Wikidata Search Entity\"-Topic ID
     */
    Topic createWikidataAssociationType(long entityTopicId, ClientState clientState);

}
