
/**
 * A page_renderer_impl that renders the "Wikidata Search Entity" page
 *
 * @see PageRenderer interface (/de.deepamehta.webclient/script/interfaces/page_renderer.js).
 */

(function() {

    dm4c.add_page_renderer("org.deepamehta.wikidata.search_entity_renderer", {

        // === Page Renderer Implementation ===

        render_page: function(topic) {

            var search_query = topic.value
            // var search_language = topic.childs['org.deepamehta.wikidata.language'].value
            // var search_entities = topic.childs['org.deepamehta.wikidata.search_entity']

            var wikidata_type = topic.childs["org.deepamehta.wikidata.search_entity_type"].value

            if (wikidata_type === "item") {                                           // --  Wikidata Item Renderer ---

                render('<div class="field-label">Wikidata Item</div>')
                render('<div class="wikidata-query-info">\"'+ search_query + '\"</div>')

                var entity_description = ""
                if (topic.childs.hasOwnProperty('org.deepamehta.wikidata.search_entity_description')) {
                    entity_description = topic.childs['org.deepamehta.wikidata.search_entity_description'].value
                }
                render('<div class="field-label">Wikidata Item Description</div>')
                render('<div class="field-value">'+entity_description+'</div>')
                // var entity_url = result_item.childs['org.deepamehta.wikidata.search_entity_alias']
                var alias_names = ""
                if (topic.childs.hasOwnProperty('org.deepamehta.wikidata.search_entity_alias')) {
                    var aliases = topic.childs['org.deepamehta.wikidata.search_entity_alias']
                    for (var k=0; k < aliases.length; k++) {
                        if (k > 0 && k < aliases.length) alias_names += ', '
                        alias_names += aliases[k].value
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
                var related_topics = dm4c.restc.get_topic_related_topics(topic.id).items // ### assoc has no child topics
                // and populate list to group topics by their related assoc (wikidata property)-type
                for (var index in related_topics) {
                    var related_topic = related_topics[index]
                    if (related_topic.assoc.type_uri === "org.deepamehta.wikidata.claim_edge") {
                        put_property_in_list_of_related_properties(related_topic.assoc, related_properties)
                    } else {
                        otherwise_related_topics.push(related_topic)
                    }
                }
                // sort claim-edge-types alphabetically
                related_properties.sort(alphabetic_claim_edge_sort)
                // populate, sort and then render grouped list of realted-items
                for (var key in related_properties) {
                    var wikidata_relation_type_listing = related_properties[key] // getting listing per claim-edge-type
                    if (wikidata_relation_type_listing.length > 0) {
                        var property_id = wikidata_relation_type_listing[0].id
                        var property_label = wikidata_relation_type_listing[0].value
                        render('<div title="Reveal this wikidata property" '
                            + 'class="field-label wikidata-relation-type" id="'+property_id+'">'
                            + property_label + '</div>')
                        $('div#' + property_id).click(function (e){
                            var assoc = dm4c.restc.get_association_by_id(e.target.id, true)
                            var topic_id = assoc.childs['org.deepamehta.wikidata.search_entity'].id
                            dm4c.do_reveal_related_topic(topic_id, "show")
                        })
                        // create listing per wikidata-property type
                        var $container = $('<div class="field-value">')
                        var $list = $('<ul class="related-values">')
                        // render list items
                        var to_be_sorted_list_of_items = []
                        for (var item_of in wikidata_relation_type_listing) {
                            var wikidata_claim_edge = wikidata_relation_type_listing[item_of]
                            var correct_end = wikidata_claim_edge.role_1.topic_id
                            if (correct_end === topic.id) correct_end = wikidata_claim_edge.role_2.topic_id
                            var related_wikidata_entity = get_topic_player_by_id(correct_end)
                            to_be_sorted_list_of_items.push(related_wikidata_entity)
                        }
                    }
                    // sort related wikidata items (that list which is per claim-edge-type) ..
                    to_be_sorted_list_of_items.sort(entity_alphabetic_sort)
                    // render complete and sorted list of related-items (per claim-edge-type)
                    for (var el in to_be_sorted_list_of_items) {
                        var related_wikidata_element = to_be_sorted_list_of_items[el]
                        var $list_item = $('<li class="wikidata-item">')
                        var $item_name = ""
                        var $item_icon = ""
                        var name = ""
                        try {
                            name = related_wikidata_element
                                .childs['org.deepamehta.wikidata.search_entity_label'].value
                        } catch (name_e) {
                            name = related_wikidata_element.value
                        }
                        // set up result-item element
                        $item_name = $('<div class="item entity-name" '
                            + 'title="Show Wikidata Item Entity: ' +related_wikidata_element.uri
                            + '" id="name-'+ related_wikidata_element.id +'">').append(name)
                        $item_icon = $('<div class="item entity-icon" title="Reveal '
                            + 'Wikidata Item Entity: ' +related_wikidata_element.uri+ '">'
                            + '<img id="icon-'+ related_wikidata_element.id
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
                        $list_item.append($item_icon).append($item_name)
                        $list.append($list_item)
                    }
                    $container.append($list)
                    render($container)
                }
                // render custom related topics of two types:
                for (var rel_topic_idx in otherwise_related_topics) {
                    var otherwise_related_topic = otherwise_related_topics[rel_topic_idx]
                    // insert dom-elements
                    if (otherwise_related_topic.type_uri === "dm4.webbrowser.url") {
                        render('<div class="topic url box" id="'+otherwise_related_topic.id+'">'
                            + '<div class="field-label">Wikidata Item URL</div>'
                            + otherwise_related_topic.value+ '</div>')
                        //  || otherwise_related_topic.type_uri === "org.deepamehta.wikidata.language"
                    }
                    // append js handler
                    $('#' + otherwise_related_topic.id).click(function (e) {
                        dm4c.do_reveal_related_topic(e.target.id, "show")
                    })
                }
                // render all otherwise related topics
                // render('<div class="field-label wikidata-relation-type">DeepaMehta 4 - What`s related?</div>')
                dm4c.render.topic_associations(topic.id)
                render_wikidata_footer()

            } else {                                                              // --  Wikidata Property Renderer ---
                load_related_claims(topic.id, function (data) {
                    var default_topic_renderer = dm4c.get_page_renderer("dm4.webclient.topic_renderer")
                        default_topic_renderer.render_page(topic)
                    render('<div class="field-label">Wikidata Claims</div>')
                    var $container_p = $('<div class="field-value">')
                    var related_claims = data.items
                    var $claim_listing = $('<ul class="claim-listing">')
                    for (var claim_idx in related_claims) {
                        var claim = related_claims[claim_idx]
                        var player_one = dm4c.fetch_topic(claim.role_1.topic_id)
                        var player_two = dm4c.fetch_topic(claim.role_2.topic_id)
                        //
                        var $list_item = $('<li class="wikidata-item">')
                        var $property_name = $('<div class="property entity-name" '
                            + 'title="Show Wikidata Claim: ' +claim.uri
                            + '" id="name-'+ claim.id +'">').append(player_one.value+":"+player_two.value)
                        var $property_icon = $('<div class="property entity-icon" title="Reveal '
                            + 'Wikidata Claim: ' +claim.uri+ '">'
                            + '<img id="icon-'+ claim.id
                            +'" src="/org.deepamehta.wikidata-search/images/rect_p3228.png" /></div>')
                        // click handler 1, ### to reveal both players, then draw and select association
                        $property_name.click(function(e) {
                            // fetch assoc-composite .. (again)
                            var assoc = dm4c.restc.get_association_by_id(e.target.id.substr(5), true)
                            // load both player (again) ... (because fetchRelated is not yet implemented)
                            var player_one = dm4c.fetch_topic(assoc.role_1.topic_id)
                            var player_two = dm4c.fetch_topic(assoc.role_2.topic_id)
                            //
                            dm4c.show_topic(player_one)
                            dm4c.show_topic(player_two)
                            dm4c.show_association(assoc)
                            dm4c.do_select_association(e.target.id.substr(5))
                        })
                        // click handler 2, ### to reveal both players, then draw and select association
                        $property_icon.click(function(e) {
                            var item_id = e.target.id
                            dm4c.show_association(item_id.substr(5))
                        })
                        $list_item.append($property_icon.append($property_name))
                        $claim_listing.append($list_item)
                    }
                    $container_p.append($claim_listing)
                    render($container_p)
                    render_wikidata_footer()

                })

            }



            // --- Private helpers

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

            function load_related_claims (propertyTopicId, handler) {

                var requestUri = '/wikidata/property/related/claims/' + propertyTopicId
                var response_data_type = response_data_type || "json"
                //
                $.ajax({
                    type: "GET", url: requestUri,
                    dataType: response_data_type, processData: false,
                    async: true,
                    success: function(data, text_status, jq_xhr) {
                        handler(data)
                    },
                    error: function(jq_xhr, text_status, error_thrown) {
                        $('#page-content').html('<div class="field-label wikidata-search started">'
                            + 'An error occured: ' +error_thrown+ ' </div>')
                        throw "RESTClientError: GET request failed (" + text_status + ": " + error_thrown + ")"
                    },
                    complete: function(jq_xhr, text_status) {
                        var status = text_status
                    }
                })
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

            function alphabetic_claim_edge_sort (a, b) {
                var scoreA = a[0].value.toLowerCase()
                var scoreB = b[0].value.toLowerCase()
                if (scoreA < scoreB)
                    return -1
                if (scoreA > scoreB)
                    return 1
                return 0 //default return value (no sorting)
            }

            function entity_alphabetic_sort (a, b) {
                var scoreA = ""
                try {
                    scoreA = a.childs['org.deepamehta.wikidata.search_entity_label'].value
                } catch (name_e) {
                    scoreA = a.value
                }
                var scoreB = ""
                try {
                    scoreB = b.childs['org.deepamehta.wikidata.search_entity_label'].value
                } catch (name_e) {
                    scoreB = b.value
                }
                if (scoreA < scoreB)
                    return -1
                if (scoreA > scoreB)
                    return 1
                return 0 //default return value (no sorting)
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
