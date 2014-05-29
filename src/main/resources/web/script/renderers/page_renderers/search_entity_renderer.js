
/**
 * A page_renderer_impl that renders the "Search Claim" page
 *
 * @see PageRenderer interface (/de.deepamehta.webclient/script/interfaces/page_renderer.js).
 */

(function() {

    dm4c.add_page_renderer("org.deepamehta.wikidata.search_entity_renderer", {

        // === Page Renderer Implementation ===

        render_page: function(topic) {

            var search_query = topic.value
            // var search_language = topic.composite['org.deepamehta.wikidata.language'].value
            // var search_entities = topic.composite['org.deepamehta.wikidata.search_entity']
            // ### render data-license http://creativecommons.org/publicdomain/zero/1.0/

            console.log(topic)

            render('<div class="field-label">Wikidata Search Entity</div>')
            render('<div class="wikidata-query-info">\"'+ search_query + '\"</div>')

            function render(content_element) {
                $('#page-content').append(content_element)
            }

            function empty_page() {
                $('#page-content').empty()
                $('#page-content').removeClass('wikidata-page')
            }

        },

        render_form: function(topic) {

            console.log("Editing \"Wikidata Search Entities\" NOT YET IMPLEMENTED")
            return function (e) {
                return topic
            }

        }
    })

})()
