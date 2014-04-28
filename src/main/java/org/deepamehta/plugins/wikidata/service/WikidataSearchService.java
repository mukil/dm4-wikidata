
package org.deepamehta.plugins.wikidata.service;

import de.deepamehta.core.Topic;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.PluginService;


public interface WikidataSearchService extends PluginService {

    Topic getOrCreateWikidataEntity(String wikidataEntityId, ClientState clientState);

    Topic searchWikidataEntity(String query, String iso_language_code, ClientState clientstate, String entityType);

    Topic checkClaims(long entityTopicId, ClientState clientState);

    Topic createWikidataAssociationType(long entityTopicId, ClientState clientState);

}
