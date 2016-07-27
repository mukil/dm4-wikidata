
# DeepaMehta 4 Wikidata Search

This plugin  provides users the functionality they need to build personal views, so called _Topic Maps_, based on wikidata.

In particular this plugin provides users the following options and commands:

*   `Wikidata Search`-Mode in the `Toolbar`, with the options
    *    to search for `Items` by text (wikidata query string)
    *    to search for `Properties` by text (wikidata query string) and
    *    to specifiy the `Language` of the wikidata-community for each search
*   `Import Claims`-command available on any `Search Result`-Topic (of type _item_)
*   `Create Association Type`-command available on any `Search Result`-Topic (of type _property_)

The motivation for this plugin was to enable users and researchers from various language-communities to (a) _query_ and _navigate_ the actual wikidata in a graphical way and (b) based on that, _create_ and _share_ personal views (so called Topic Maps) around items currently discussed in wikidata.

Currently wikidata can be queried in 44 languages. If your language is missing here, please excuse that and file an issue at http://trac.deepamehta.de.

## Requirements

DeepaMehta 4 is a platform for collaboration and knowledge management.
https://github.com/jri/deepamehta

## Download & Installation

You can find a bundle file for installation at [http://download.deepamehta.de](http://download.deepamehta.de).

Copy the downloaded `dm44-wikidata-search-0.0.x.jar` file into your DeepaMehta bundle repository and re-start your [DeepaMehta installation](https://github.com/jri/deepamehta#requirements).

## Usage

Instead of "By Text" choose the "Wikidata Search"-Search Mode in the DeepaMehta Toolbar. Select the language you want to search and enter the term you are interested in.

For any search-result entity of type "item" you can `Import claims` and after that, navigate to ''related'' wikidata-items.

For any search-result entity of type "property" ([wikidata properties](http://meta.wikimedia.org/wiki/Wikidata/Data_model#Properties) - [they are](https://www.wikidata.org/wiki/Wikidata:List_of_properties)) there is a "+" (`Turn into Associaton Type`-command) availble in the result listing.

## Research & Documentation

You can find more infos on this project in the DeepaMehta Community Trac at https://trac.deepamehta.de/wiki/WikidataSearchPlugin%20

# GNU Public License

This software is released under the terms of the GNU General Public License in Version 3.0, 2007. You can find a copy of that in the root directory of this repository or read it [here](http://www.gnu.org/licenses/gpl).

# Version History

0.4.2, Jul 27, 2016
- Adapted to and compatible with DeepaMehta 4.8
- Better support for collaboration through shared workspaces (since 4.7)
- More robust updating of already imported entities
- Bugfix: Aliases don't add up (over imports) anymore

0.4.1, ...
- Built directions into imported claims
- More robust wikimedia commons items integration

Release not further documented.

0.0.4, Dec 26, 2014
- Introduced rendering and import of "claimed" Commons Media topics
- Wikidata navigation from a property to all imported items (claimed with such) works
- Compatible with DeepaMehta 4.4
- Extracted WDTK dump file importer into new dm4-wikidata-toolkit plugin
  (due to the WDTK JDK 1.7 dependency)

0.0.3, Jul 07, 2014
- Introducing custom page renderer for "Wikidata Entities" 
  (presenting statements and all their values in alphabetical order)
- Added license rendering for data provided by wikidata
- More accurate labelling of topic command: "Import claims" instead of "Show claims"
- Simple updating mechanism for all values of involved wikidata-items (on every "Import claims"-command)
- Introduced custom rendering for all imported "Wikidata properties"
- New wikidata-service method to navigate to all associated associations (of a property-entity)
- HTTP Optimization: Just one GET request per "property" (during "Import claims")
- Compatible with DeepaMehta 4.3

Known issues:
- No qualifiers imported
- No support for quanities, commonsMedia and globe-coordinates
- No deletion of once imported claims (e.g. if claim gets deleted)
- With this simplified application-model there can be just one 
  language:value per search-entity/topic
- Search entity order of single entities may-be wrong when 
  those occure in many resultset

0.0.2, May 05, 2014
- Introduced wikidata-item entities
- Navigating wikidata-items along all their "Claims"
- Processing of claimed wikidata-text values (re-using by value) works
- Multi-language support across all queries works
- A few fixes related to URL + Language references
- Fixes search-entity order in resultsets
- Introduced some graphics + credits
- Compatible with DeepaMehta 4.2

Known issues:
- With this simplified application-model there can be just one 
  language:value/sitelink-pair per search-entity/topic
- Missing license rendering (wikidata entity page renderer) (see ###)
- Search entity order of single entities may-be wrong when 
  those occure in many resultset

0.0.1, Feb 28, 2014
- Compatible with DeepaMehta 4.2

0.0.1-SNAPSHOT, Feb 05, 2014

- Compatible with DM 4.2-SNAPSHOT (b1fe37543255df22a47d7db397e10925811e17e5)

--------------------------
Author: Malte Reißig, 2014

