
/**
 * A page_renderer_impl that renders the "Wikidata Commons Media" page
 *
 * @see PageRenderer interface (/de.deepamehta.webclient/script/interfaces/page_renderer.js).
 */

(function() {

    dm4c.add_page_renderer("org.deepamehta.wikidata.commons_media_renderer", {

        // === Page Renderer Implementation ===

        render_page: function(topic) {

            console.log("Commons Media Item - Not yet implemented")
            // console.log(topic)

            render('<div class="field-label">Wikidata Commons Media</div>')
            render('<div class="wikidata-query-info">\"'+ topic.value + '\"</div>')

            /* var image_path = ""
            if (topic.childs.hasOwnProperty('org.deepamehta.wikidata.commons_media_path')) {
                image_path = topic.childs['org.deepamehta.wikidata.commons_media_path'].value
                render('<img src="http://'+image_path+'" title="Wikimedia Commons Item '+topic.value+'" />')
            } **/

            /** var default_topic_renderer = dm4c.get_page_renderer("dm4.webclient.topic_renderer")
                default_topic_renderer.render_page(topic)

                render('<div class="field-label">Wikidata Claims</div>')
                render('<div class="field-value">Rendering of claimed items is currently de-activated due '
                    + 'to issue <a href="https://trac.deepamehta.de/ticket/672">#672</a></div>') **/

            render_wikidata_footer()


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

                /** var requestUri = '/wikidata/property/related/claims/' + propertyTopicId
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
                }) **/
            }

            function render(content_element) {
                $('#page-content').append(content_element)
            }

            function empty_page() {
                $('#page-content').empty()
                $('#page-content').removeClass('wikidata-page')
            }

        },

        render_form: function(topic) {

            console.log("Editing \"Wikidata Commons Media\" NOT YET IMPLEMENTED")
            return function (e) {
                return topic
            }

        }
    })

})()
