
package org.deepamehta.plugins.wikidata.service;

import de.deepamehta.core.Topic;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.PluginService;


public interface WikidataSearchService extends PluginService {

    Topic searchWikidataEntity(String query, String iso_language_code, ClientState clientstate, String entityType);

    Topic getOrCreateWikidataEntity(String wikidataEntityId, String iso_language_code, ClientState clientState);

    Topic loadClaimsAndRelatedWikidataItems(long entityTopicId, String iso_language_code, ClientState clientState);

    Topic createWikidataAssociationType(long entityTopicId, ClientState clientState);

}
