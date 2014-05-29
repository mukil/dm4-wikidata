
# DeepaMehta 4 Wikidata Search Module

This DeepaMehta 4 module enables you to search, explore and map wikidata items (the "shared structures of meaning" - Janet Murray, New Media Reader, 2003 - of [wikidata-communities](http://www.wikidata.org/wiki) from all around the globe) based on their relations.

In particular this plugin enables you to build on wikidata in your personal information work through:

* enabling you to turn any search-result entity of type "property" ([wikidata properties](http://meta.wikimedia.org/wiki/Wikidata/Data_model#Properties) - [see list of examples here](https://www.wikidata.org/wiki/Wikidata:List_of_properties)) into a DeepaMehta *Assocation Type*
* enabling you to "Import claims" of any "Wikidata Item" (roughly anything that has at least one wikipedia-page?) and thus navigate along related wikidata items

[DeepaMehta 4](http://www.deepamehta.de) is a free software platform for associative and personal information work and a situation-centered user interface based on *free placement* in *stable views*.

## Research & Documentation

Find more infos on this project in the DeepaMehta Community Trac at https://trac.deepamehta.de/wiki/WikidataSearchPlugin%20

## Download & Installation

You can find a bundle file for installation at [http://download.deepamehta.de](http://download.deepamehta.de).

Copy the downloaded `dm42-wikidata-search-0.0.2.jar` file into your DeepaMehta bundle repository and re-start your [DeepaMehta installation](https://github.com/jri/deepamehta#requirements).

## Usage

Instead of "By Text" choose the "Wikidata Search"-Search Mode in the DeepaMehta Toolbar. Select the language you want to search and enter the term you are interested in.

Any search-result entity of type "property" ([wikidata properties](http://meta.wikimedia.org/wiki/Wikidata/Data_model#Properties) - [they are](https://www.wikidata.org/wiki/Wikidata:List_of_properties)) can be turned into an ''Association Type'' if you click the "+" command (visible in the result listing).

For any search-result entity of type "item" you can `Import claims` and after that, navigate to ''related'' items.


# GNU Public License

This software is released under the terms of the GNU General Public License in Version 3.0, 2007. You can find a copy of that in the root directory of this repository or read it [here](http://www.gnu.org/licenses/gpl).


# Version HIstory

0.0.3-SNAPSHOT, UPCOMING
- Proper command label: "Import claims"
- Introducing custom page renderer for "Wikidata Entities"

Known issues:
- No update-logic part of import (e.g. if remote-values changed)
- With this simplified application-model there can be just one 
  language:value/sitelink-pair per search-entity/topic
- Missing license rendering (wikidata entity page renderer) (see ###)
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

