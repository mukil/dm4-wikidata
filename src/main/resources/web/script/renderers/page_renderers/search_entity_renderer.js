
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

            var wikidata_type = topic.composite["org.deepamehta.wikidata.search_entity_type"].value

            if (wikidata_type === "item") {                                           // --  Wikidata Item Renderer ---

                render('<div class="field-label">Wikidata Item</div>')
                render('<div class="wikidata-query-info">\"'+ search_query + '\"</div>')

                var entity_description = ""
                if (topic.composite.hasOwnProperty('org.deepamehta.wikidata.search_entity_description')) {
                    entity_description = topic.composite['org.deepamehta.wikidata.search_entity_description'].value
                }
                render('<div class="field-label">Wikidata Item Description</div>')
                render('<div class="field-value">'+entity_description+'</div>')
                // var entity_url = result_item.composite['org.deepamehta.wikidata.search_entity_alias']
                var alias_names = ""
                if (topic.composite.hasOwnProperty('org.deepamehta.wikidata.search_entity_alias')) {
                    var aliases = topic.composite['org.deepamehta.wikidata.search_entity_alias']
                    for (var k=0; k < aliases.length; k++) {
                        alias_names += ', '+ aliases[k].value
                    }
                }
                render('<div class="field-label">Wikidata Item Aliases</div>')
                render('<div class="field-value">'+alias_names+'</div>')
                //
                render('<div class="field-label">Wikidata Claims </div>')
                //
                var related_properties = []
                var otherwise_related_topics = []
                // fetch all related topics (with their relating assoc)
                var related_topics = dm4c.restc.get_topic_related_topics(topic.id).items
                // and populate list to group topics by their related assoc (wikidata property)-type
                for (var index in related_topics) {
                    var related_topic = related_topics[index]
                    if (related_topic.assoc.type_uri === "org.deepamehta.wikidata.claim_edge") {
                        put_property_in_list_of_related_properties(related_topic.assoc, related_properties)
                    } else {
                        otherwise_related_topics.push(related_topic)
                    }
                }
                // render groupd list ..
                for (var key in related_properties) {
                    var wikidata_relation_type_listing = related_properties[key]
                    if (wikidata_relation_type_listing.length > 0) {
                        var property_label = wikidata_relation_type_listing[0].value
                        render('<div class="field-label wikidata-relation-type">'+property_label+'</div>')
                        // create listing per wikidata-property type
                        var $container = $('<div class="field-value">')
                        var $list = $('<ul class="related-values">')
                        // render list items
                        for (var item_of in wikidata_relation_type_listing) {
                            var wikidata_claim_edge = wikidata_relation_type_listing[item_of]
                            var correct_end = wikidata_claim_edge.role_1.topic_id
                            if (correct_end === topic.id) correct_end = wikidata_claim_edge.role_2.topic_id
                            var related_wikidata_entity = get_topic_player_by_id(correct_end)
                            var $list_item = $('<li class="wikidata-item">')
                            var $item_name = ""
                            var $item_icon = ""
                            var name = ""
                            try {
                                name = related_wikidata_entity
                                    .composite['org.deepamehta.wikidata.search_entity_label'].value
                            } catch (name_e) {
                                name = related_wikidata_entity.value
                            }
                            // set up result-item element
                            $item_name = $('<div class="item entity-name" '
                                + 'title="Show Wikidata Item Entity: ' +related_wikidata_entity.uri
                                + '" id="name-'+ related_wikidata_entity.id +'">').append(name)
                            $item_icon = $('<div class="item entity-icon" title="Reveal '
                                + 'Wikidata Item Entity: ' +related_wikidata_entity.uri+ '">'
                                + '<img id="icon-'+ related_wikidata_entity.id
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
                            //
                            //
                            $list_item.append($item_icon).append($item_name)
                            $list.append($list_item)
                        }
                        $container.append($list)
                        render($container)
                    }
                }
                // console.log(otherwise_related_topics)
                // ### check wikidata data license
                render('<div class="field-label">Wikidata License</div>')
                render('<div class="field-value"><a href="http://creativecommons.org" '
                    + 'title="CC0 1.0 Licensed (License Text in English)>'
                    + 'Creative Commons CC0 License (CC0 1.0)</a></div>')
                render('<p>&nbsp;</p>')
                // for (var rel_topic_idx in otherwise_related_topics) {
                    // console.log(otherwise_related_topics)
                   // ### render custom related topics of type:
                   // "dm4.webbrowser.url" and "org.deepamehta.wikidata.language",
                   //
                // }
                // render all otherwise related topics
                render('<div class="field-label wikidata-relation-type">DeepaMehta 4 - What`s related?</div>')
                dm4c.render.topic_associations(topic.id)

            } else {                                                              // --  Wikidata Property Renderer ---

                var default_topic_renderer = dm4c.get_page_renderer("dm4.webclient.topic_renderer")
                default_topic_renderer.render_page(topic)
                /** ### navigating involved wikidata-claims needs
                 * new method: dm4c.rest.get_topic_related_assocs(topic.id)
                 * var wikidata_claims = dm4c.restc.get_topic_related_topics(topic.id,
                    { "assoc_type_uri": "dm4.core.aggregation" }
                )
                console.log(wikidata_claims) **/

            }

            function get_topic_player_by_id(topicId) {
                for (var item in related_topics) {
                    if (related_topics[item].id === topicId) return related_topics[item]
                }
                return undefined
            }

            function render(content_element) {
                $('#page-content').append(content_element)
            }

            function empty_page() {
                $('#page-content').empty()
                $('#page-content').removeClass('wikidata-page')
            }

            function put_property_in_list_of_related_properties (assoc, list) {
                var contains = false
                // a) either add to an existing list
                for (var i in list) {
                    var property_assocs = list[i] // references an array of property_associations
                    if (property_assocs.length > 0) {
                        if (property_assocs[0].value === assoc.value) {
                            property_assocs.push(assoc)
                            contains = true
                        }
                    }
                }
                // b) or start a new list with this assoc as its first item
                if (!contains) {
                    list.push([assoc]) // the initial push ..
                }
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
