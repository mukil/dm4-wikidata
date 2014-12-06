
/**
 * A page_renderer_impl that renders a "wikidata search" result page
 *
 * @see PageRenderer interface (/de.deepamehta.webclient/script/interfaces/page_renderer.js).
 */

(function() {

    dm4c.add_page_renderer("org.deepamehta.wikidata.search_bucket_renderer", {

        // === Page Renderer Implementation ===

        render_page: function(topic) {
            
            var search_query = topic.childs['org.deepamehta.wikidata.search_query'].value
            var search_language = topic.childs['org.deepamehta.wikidata.language'].value
            var search_entities = topic.childs['org.deepamehta.wikidata.search_entity']

            render('<div class="field-label">Wikidata Search Query</div>')
            render('<div class="wikidata-query-info">\"'+ search_query + '\" ('+ search_language +')</div>')
            render('<div class="field-label">Search Results</div>')
            render('<ol class="wikidata-search-list">')
            if (typeof search_entities === "undefined") {
                render('<div class="wikidata-query-info">Zero results</div>')
                return // fixme:
            }
            // ### used for development when ordinal nrs are part of the bucket-entity relation: console.log(topic)
            // sort
            search_entities.sort(result_order_sort)
            // render
            for (var i=0; i < search_entities.length; i++) {
                //
                var result_item = search_entities[i]
                var entity_id = result_item.id
                var entity_uri = result_item.uri
                var entity_name = result_item.childs['org.deepamehta.wikidata.search_entity_label'].value
                var entity_description = ""
                if (result_item.childs.hasOwnProperty('org.deepamehta.wikidata.search_entity_description')) {
                    entity_description = result_item.childs['org.deepamehta.wikidata.search_entity_description']
                }
                // var entity_url = result_item.childs['org.deepamehta.wikidata.search_entity_alias']
                var alias_names = ""
                if (result_item.childs.hasOwnProperty('org.deepamehta.wikidata.search_entity_alias')) {
                    var aliases = result_item.childs['org.deepamehta.wikidata.search_entity_alias']
                    for (var k=0; k < aliases.length; k++) {
                        alias_names += ' '+ aliases[k].value
                    }
                }
                var item_type = result_item.childs['org.deepamehta.wikidata.search_entity_type'].value
                var $list_item = $('<li class="wikidata-item">')
                var $item_name = ""
                var $item_icon = ""
                if (item_type === "item") {
                    // set up result-item element
                    $item_name = $('<div class="item entity-name" '
                        + 'title="Show Wikidata Item Entity: ' +entity_uri+ '" id="name-'+ entity_id +'">')
                        .append(entity_name)
                    $item_icon = $('<div class="item entity-icon" title="Reveal '
                        + 'Wikidata Item Entity: ' +entity_uri+ '"><img id="icon-'+ entity_id
                        +'" src="/org.deepamehta.wikidata-search/images/rect3228.png" /></div>')
                    // click handler 1
                    $item_name.click(function(e) {
                        dm4c.do_reveal_related_topic(e.target.id.substr(5), "show")
                    })
                    // click handler 2
                    $item_icon.click(function(e) {
                        var item_id = e.target.id
                        dm4c.do_reveal_related_topic(item_id.substr(5), "none")
                    })
                } else if (item_type === "property") {
                    $item_name = $('<div class="property entity-name" '
                        + '"title="Show Wikidata Property Entity" id="name-'+ entity_id +'">').text(entity_name)
                    $item_name.click(function(e) {dm4c.do_reveal_related_topic(e.target.id.substr(5), "show")})
                    var $item_add = $('<a class="create-type" id="'+ entity_id +'" title="Turn property into '
                        + 'new Association Type" href="#turn">').text("+")
                        $item_add.click(function(e) {
                            dm4c.restc.request("GET", "/wikidata/property/turn/" + e.target.id)
                        })
                }
                var $item_descr = $('<div class="entity-description">').text(entity_description.value)
                    if (alias_names != "") $item_descr.append('<br/>Aliases: '+ alias_names)

                $list_item.append($item_add).append($item_icon).append($item_name).append($item_descr)

                $('#page-content .wikidata-search-list').append($list_item)

            }

            render_wikidata_footer()



            // --- Private helpers

            function render(content_element) {
                $('#page-content').append(content_element)
            }

            function render_wikidata_footer () {
                // render wikidata data license
                render('<div class="field-label attribution">Attribution</div>')
                render('<div class="field-value license"><a href="https://creativecommons.org/publicdomain/zero/1.0/" '
                    + 'title="CC0 1.0 Licensed - Read more about this license (English)">'
                    + 'Public Domain Dedication, CC0 1.0 License</a></div>')
                // render wikidata project link
                render('<div class="field-value project-logo">'
                    + '<a style="border: 0px;" target="_blank" href="https://www.wikidata.org">'
                    + '<img title="Search results powered by Wikidata - Visit project website" '
                    + 'src="/org.deepamehta.wikidata-search/images/Wikidata-logo-en-135px.png" /></a>'
                    + '</div>')

            }

            function empty_page() {
                $('#page-content').empty()
                $('#page-content').removeClass('wikidata-page')
            }

            /** sorting asc by item.childs["org.deepamehta.wikidata.search_ordinal_nr"].value */
            function result_order_sort (a, b) {
                var scoreA = 0
                var scoreB = 0
                if (a.childs.hasOwnProperty("org.deepamehta.wikidata.search_ordinal_nr")) {
                    scoreA = a.childs["org.deepamehta.wikidata.search_ordinal_nr"].value
                }
                if (b.childs.hasOwnProperty("org.deepamehta.wikidata.search_ordinal_nr")) {
                    scoreB = b.childs["org.deepamehta.wikidata.search_ordinal_nr"].value
                }

                if (scoreA < scoreB) // sort string descending
                return -1
                if (scoreA > scoreB)
                return 1
                return 0 //default return value (no sorting)
            }


        },

        render_form: function(topic) {

            console.warn("Editing \"Wikidata Search Buckets\" NOT YET IMPLEMENTED")

        }
    })

})()
