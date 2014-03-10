
/*
 *
 * DeepaMehta 4 Webclient Wikidata JavaScript Plugin
 * @author Malte Reißig (<malte@mikromedia.de>)
 * @website https://github.com/mukil/dm4-wikidata
 * @version: 0.0.1
 *
 */

(function ($, dm4c) {

    dm4c.add_plugin('org.deepamehta.wikidata-type-search', function () {

        var language_menu
        var search_type_menu

         // === Webclient Listeners ===

        dm4c.add_listener("init", function() {
            dm4c.toolbar.searchmode_menu.add_item({label: "Wikidata Search", value: "wikidata-search"})
        })

        dm4c.add_listener("searchmode_widget", function(searchmode) {

            if (searchmode == "wikidata-search") {

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
                    var lang_name = lang.composite['org.deepamehta.wikidata.language_name']
                    var lang_code_iso = lang.composite['org.deepamehta.wikidata.language_code_iso']

                    language_menu.add_item({
                        label: lang_name.value,
                        value: lang_code_iso.value
                    })

                }
                language_menu.select("en")
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
                    search_value = encodeURIComponent(search_value)

                if (search_value !== "" && search_value !== " ") {
                    return dm4c.restc.request("GET", "/wikidata/search/" + get_search_type_value() + "/"
                        + search_value + '/' + get_language_value())
                }
            }

            function get_language_value() {
                return language_menu.get_selection().value
            }

            function get_search_type_value() {
                return search_type_menu.get_selection().value
            }

        })
    })

}(jQuery, dm4c))
