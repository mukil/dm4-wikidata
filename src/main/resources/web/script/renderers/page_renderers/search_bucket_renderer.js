
/**
 * A page_renderer_impl that renders a "wikidata search" result page
 *
 * @see PageRenderer interface (/de.deepamehta.webclient/script/interfaces/page_renderer.js).
 */

(function() {

    dm4c.add_page_renderer("org.deepamehta.wikidata.search_bucket_renderer", {

        // === Page Renderer Implementation ===

        render_page: function(topic) {

            var search_query = topic.composite['org.deepamehta.wikidata.search_query'].value
            var search_language = topic.composite['org.deepamehta.wikidata.language'].value
            var search_entities = topic.composite['org.deepamehta.wikidata.search_entity']

            render('<div class="field-label">Wikidata Search Query</div>')
            render('<div class="wikidata-query-info">\"'+ search_query + '\" ('+ search_language +')</div>')
            render('<div class="field-label">Search Results</div>')
            render('<ol class="wikidata-search-list">')
            if (typeof search_entities === "undefined") {
                render('<div class="wikidata-query-info">Zero results</div>')
                return // fixme:
            }
            for (var i=0; i < search_entities.length; i++) {
                //
                var result_item = search_entities[i]
                var entity_id = result_item.id
                var entity_name = result_item.composite['org.deepamehta.wikidata.search_entity_label'].value
                var entity_description = ""
                if (result_item.composite.hasOwnProperty('org.deepamehta.wikidata.search_entity_description')) {
                    entity_description = result_item.composite['org.deepamehta.wikidata.search_entity_description']
                }
                // var entity_url = result_item.composite['org.deepamehta.wikidata.search_entity_alias']
                var alias_names = ""
                if (result_item.composite.hasOwnProperty('org.deepamehta.wikidata.search_entity_alias')) {
                    var aliases = result_item.composite['org.deepamehta.wikidata.search_entity_alias']
                    for (var k=0; k < aliases.length; k++) {
                        alias_names += ' '+ aliases[k].value
                    }
                }
                var item_type = result_item.composite['org.deepamehta.wikidata.search_entity_type'].value
                var $list_item = $('<li class="wikidata-item">')
                var $item_name = ""
                if (item_type === "item") {
                    $item_name = $('<div class="item entity-name" '
                        + 'title="Reveal search entity" id="name-'+ entity_id +'">').text(entity_name)
                    $item_name.click(function(e) { dm4c.do_reveal_related_topic(e.target.id.substr(5), "show") })
                    /** var $item_add = $('<a class="create-type" id="'+ entity_id +'" title="Create Wikidata Item"'
                        + ' href="#turn">').text("+")
                        $item_add.click(function(e) {
                            dm4c.restc.request("GET", "/wikidata/item/turn/" + e.target.id)
                        }) **/
                } else if (item_type === "property") {
                    $item_name = $('<div class="property entity-name" '
                        + '"title="Reveal search entity" id="name-'+ entity_id +'">').text(entity_name)
                    $item_name.click(function(e) { dm4c.do_reveal_related_topic(e.target.id.substr(5), "show") })
                    var $item_add = $('<a class="create-type" id="'+ entity_id +'" title="Turn property into '
                        + 'new Association Type" href="#turn">').text("+")
                        $item_add.click(function(e) {
                            dm4c.restc.request("GET", "/wikidata/property/turn/" + e.target.id)
                        })

                }
                var $item_descr = $('<div class="entity-description">').text(entity_description.value)
                    if (alias_names != "") $item_descr.append('<br/><b>Aliases</b>: '+ alias_names)

                $list_item.append($item_add).append($item_name).append($item_descr)

                $('#page-content .wikidata-search-list').append($list_item)
            }

            render('<br/><br/><a style="border: 0px;" target="_blank" href="https://www.wikidata.org">'
                + '<img title="Search results powered by www.wikidata.org" '
                + 'src="/org.deepamehta.wikidata-search/images/Wikidata-logo-en-135px.png" /></a>')

            function render(content_element) {
                $('#page-content').append(content_element)
            }

            function empty_page() {
                $('#page-content').empty()
                $('#page-content').removeClass('wikidata-page')
            }

        },

        render_form: function(topic) {

            console.warn("Editing \"Wikidata Search Buckets\" NOT YET IMPLEMENTED")

        }
    })

})()
