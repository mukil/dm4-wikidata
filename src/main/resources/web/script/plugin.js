
/*
 *
 * DeepaMehta 4 Webclient Wikidata JavaScript Plugin
 * @author Malte Reißig (<malte@mikromedia.de>)
 * @website https://github.com/mukil/dm4-wikidata
 * @version 0.0.3-SNAPSHOT
 *
 */

(function ($, dm4c) {

    dm4c.add_plugin('org.deepamehta.wikidata-type-search', function () {

        var language_menu
        var search_type_menu

        // === Webclient Listeners ===

        dm4c.add_listener('topic_commands', function (topic) {
            // check if user is authenticated
            if (!dm4c.has_create_permission('org.deepamehta.wikidata.search_bucket')) {
                return
            }
            var commands = []
            if (topic.type_uri === 'org.deepamehta.wikidata.search_entity') {
                // if type is item
                var entity_type = topic.childs['org.deepamehta.wikidata.search_entity_type'].value
                if (entity_type === "item") {
                    commands.push({is_separator: true, context: 'context-menu'})
                    commands.push({
                        label: 'Import claims',
                        handler: importClaimedItems,
                        context: ['context-menu', 'detail-panel-show']
                    })
                }
            }
            return commands
        })

        dm4c.add_listener("init", function() {
            dm4c.toolbar.searchmode_menu.add_item({label: "Wikidata Search", value: "wikidata-search"})
        })

        dm4c.add_listener("init_2", function() {
            // console.log("Trying to select the wikidata search mode by default for new users of the WTUI.")
            // ### failed attempt: this does not initiate searchmode widget properly
            // ### dont know how to do it differently in the webclient
            // ## dm4c.toolbar.searchmode_menu.close()
            // ## dm4c.toolbar.searchmode_menu.remove_item("by-text")
            // ## dm4c.toolbar.searchmode_menu.remove_item("by-type")
            // dm4c.toolbar.searchmode_menu.select("wikidata-search")
        })

        dm4c.add_listener("searchmode_widget", function(searchmode) {

            if (searchmode == "wikidata-search") {

                /** check if user is authenticated
                if (!dm4c.has_create_permission('org.deepamehta.wikidata.search_bucket')) {
                    return
                } **/

                // enable search button
                dm4c.toolbar.search_button.button("enable")

                // load language options                                                // do sort, do fetch-composites
                var languages = dm4c.restc.get_topics('org.deepamehta.wikidata.language', true, true).items

                // create wikidata search menu
                var $container = $('<div class="wikidata-search">')
                var $input_field= $('<input type="text" class="wikidata-type-search">')
                // create search type menu
                search_type_menu = dm4c.ui.menu()
                search_type_menu.add_item({label: "Item", value: "item"})
                search_type_menu.add_item({label: "Property", value: "property"})
                // create language selector
                language_menu = dm4c.ui.menu()
                for (var i=0; i < languages.length; i++) {

                    var lang = languages[i]
                    var lang_name = lang.childs['org.deepamehta.wikidata.language_name']
                    var lang_code_iso = lang.childs['org.deepamehta.wikidata.language_code_iso']

                    language_menu.add_item({
                        label: lang_name.value,
                        value: lang_code_iso.value
                    })

                }
                language_menu.select("en")
                // ### $('.workspace-widget').append('<span>Language</span>')
                // ### $('.workspace-widget').append(language_menu.dom) //
                // append elements to DOM
                $container.append(search_type_menu.dom).append(language_menu.dom).append($input_field)

                // register ENTER handler
                dm4c.on_return_key($input_field, function() {
                    dm4c.do_search("wikidata-search")
                })

                return $container
            }

        })

        dm4c.add_listener("search", function(searchmode) {

            if (searchmode == "wikidata-search") {

                var search_value = $('input.wikidata-type-search').val()
                    search_value = search_value.replace(" ", "+")
                    search_value = search_value.replace(" ", "+")
                    search_value = encodeURIComponent(search_value)
                    showSpinningWheel()
                if (search_value !== "" && search_value !== " ") {
                    return dm4c.restc.request("GET", "/wikidata/search/" + get_search_type_value() + "/"
                        + search_value + '/' + get_language_value()) // this request blocks the Browser/UI .. 
                }
            }

            function get_language_value() {
                return language_menu.get_selection().value
            }

            function get_search_type_value() {
                return search_type_menu.get_selection().value
            }

        })

        function showSpinningWheel () {
            $('#page-content').html('<img src="/org.deepamehta.wikidata-search/images/ajax-loader.gif" '
                + ' class="wikidata-loading" />')
        }

        function importClaimedItems () {

            var requestUri = '/wikidata/check/claims/' + dm4c.selected_object.id  + '/' + get_language_value()

            var response_data_type = response_data_type || "json"
            //
            $.ajax({
                type: "GET", url: requestUri,
                dataType: response_data_type, processData: false,
                async: true,
                success: function(data, text_status, jq_xhr) {
                    dm4c.do_select_topic(data.id, true)
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

            $('#page-content').html('<div class="field-label wikidata-search started">'
                + 'Asking https://www.wikidata.org in '+get_language_name()+' </div>')

            $('#page-content').append('<div class="field-item wikidata-search-spinner">'
                + '<img src="/org.deepamehta.wikidata-search/images/ajax-loader.gif" '
                    + 'title="Processing the wikidata search response"></div>')
            // Notes:
            // - page_renderer() selbst übernehmen, und simple und multi-renderer trotzdem ausführen (direkt aufrufen)
            //   see webclient simple title_renderer:
            //   "dm4c.get_simple_renderer("dm4.webclient.text_renderer").render_form(page_model, parent_element)"
            // - see webclient migration to get name of all default renderer
            // - defualt_text_renderer (assoc_def, wenn aggregation dann rendert dieser die combobox plus eingabefeld)

            function get_language_value() {
                var lang_value = undefined
                if (typeof language_menu !== "undefined") {
                    lang_value = language_menu.get_selection().value
                    return lang_value
                }
                console.warn("Please initiate the \"Wikidata search\"-Mode widget before making any requests - "
                    + " FALLBACK: Now requesting data in EN")
                return "en"

            }

            function get_language_name() {
                if (typeof language_menu !== "undefined") {
                    return language_menu.get_selection().label
                }
                return "English"
            }

        }

    })

}(jQuery, dm4c))
