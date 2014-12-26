
/**
 * A page_renderer_impl that renders the "Wikidata Commons Media" page
 *
 * @see PageRenderer interface (/de.deepamehta.webclient/script/interfaces/page_renderer.js).
 */

(function() {

    dm4c.add_page_renderer("org.deepamehta.wikidata.commons_media_renderer", {

        // === Page Renderer Implementation ===

        render_page: function(topic) {

            render('<div class="field-label">Wikidata Commons Media</div>')
            render('<div class="wikidata-query-info">\"'+ topic.value + '\"</div>')

            var image_path = ""
            if (topic.childs.hasOwnProperty('org.deepamehta.wikidata.commons_media_path')) {
                image_path = topic.childs['org.deepamehta.wikidata.commons_media_path'].value
                render('<img class="commons-media" src="'+image_path+'" title="Wikimedia Commons Item: '+topic.value+'" '
                    + 'alt="Image: '+topic.value+'" />')
            }

            render_wikidata_footer(topic)


            // --- Private helpers

            function render_wikidata_footer (topic) {
                // render wikidata data license
                render('<div class="field-label attribution">Attribution</div>')
                render('<div class="field-value author">Authored by ' +
                    topic.childs['org.deepamehta.wikidata.commons_author_html'].value
                    + '</div>')
                render('<div class="field-value license">' +
                    topic.childs['org.deepamehta.wikidata.commons_license_html'].value
                    + '</div>')
                // render wikidata project link
                render('<div class="field-value project-logo">'
                    + '<a style="border: 0px;" target="_blank" href="https://www.wikidata.org">'
                    + '<img title="Search results powered by Wikidata - Visit project website" '
                    + 'src="/org.deepamehta.wikidata-search/images/Wikidata-logo-en-135px.png" /></a>'
                    + '</div>')

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
