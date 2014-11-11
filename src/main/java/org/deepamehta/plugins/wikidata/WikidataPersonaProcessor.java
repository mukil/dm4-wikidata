package org.deepamehta.plugins.wikidata;

import de.deepamehta.core.model.CompositeValueModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.DeepaMehtaService;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Logger;
import org.json.JSONObject;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocumentProcessor;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyDocument;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;
import org.wikidata.wdtk.datamodel.interfaces.Value;
import org.wikidata.wdtk.datamodel.interfaces.ValueSnak;
import org.wikidata.wdtk.datamodel.interfaces.ValueVisitor;
import org.wikidata.wdtk.datamodel.json.ValueJsonConverter;
import org.wikidata.wdtk.util.Timer;

/**
 * @author bob
 *
 * A simple class that processes EntityDocuments to identify personas in the wikidatawiki,
 * based on the EntityTimerProcessor of the WDTK written by Markus Kroetzsch. 
 * Thanks for sharing. Honorable mentions go to jri for telling me about the 
 * ImportPackage notations which helped me to run the WDTK within OSGi.
 */
public class WikidataPersonaProcessor implements EntityDocumentProcessor {
    
    private Logger log = Logger.getLogger(getClass().getName());
    
    private final String DM_PERSON              = "dm4.contacts.person";
    private final String DM_PERSON_NAME         = "dm4.contacts.person_name";
    private final String DM_PERSON_FIRST_NAME   = "dm4.contacts.first_name";
    private final String DM_PERSON_LAST_NAME    = "dm4.contacts.last_name";
    
    private final String DM_INSTITUTION         = "dm4.contacts.institution";
    private final String DM_INSTITUTION_NAME    = "dm4.contacts.institution_name";
    
    private final String DM_NOTES               = "dm4.contacts.notes";
        
    final Timer timer = Timer.getNamedTimer("WikidataPersonaProcessor");
    final int timeout;
    int lastSeconds = 0;
    int entityCount = 0;
    
    DeepaMehtaService dms;

    public WikidataPersonaProcessor (DeepaMehtaService dms, int timeout) {
        this.timeout = timeout;
        this.dms = dms;
        log.info("Set up WikidataPersonaProcessor to run for " + timeout + " seconds");
    }

    // globally collect some label for every item processed.. (note: in memory!)
    HashMap<String, String> itemsFirstLabel = new HashMap<String, String>();
    
    // collecting some more specific text values on each item (note: in memory!)
    HashMap<String, String> itemsDeathDate = new HashMap<String, String>();
    HashMap<String, String> itemsBirthDate = new HashMap<String, String>();
    HashMap<String, String> itemsGivenname = new HashMap<String, String>();
    HashMap<String, String> itemsSurname = new HashMap<String, String>();
    // 
    HashMap<String, String> all_persons = new HashMap<String, String>();
    HashMap<String, String> all_institutions = new HashMap<String, String>();
    HashMap<String, double[]> all_coordinates = new HashMap<String, double[]>();

    @Override
    public void processItemDocument(ItemDocument itemDocument) {

        countEntity();
        
        // 0) Get english label of current item
        String itemId = itemDocument.getEntityId().getId();
        String label = getFirstLabel(itemDocument);
        // .. Memorizing any items english label in instance-variable
        if (label != null && !label.isEmpty()) itemsFirstLabel.put(itemId, label);

        // 1) Iterate over statement groups 
        if (itemDocument.getStatementGroups().size() > 0) {
            
            for (StatementGroup sg : itemDocument.getStatementGroups()) {
                                
                // -- Inspect with which type of StatementGroup (Property) we deal here 
                
                boolean isInstanceOf = sg.getProperty().getId().equals(WikidataEntityMap.IS_INSTANCE_OF);
                boolean isSubclassOf = sg.getProperty().getId().equals(WikidataEntityMap.IS_SUBCLASS_OF);
                boolean isCoordinateOf = sg.getProperty().getId().equals(WikidataEntityMap.IS_COORDINATE_LOCATION);
                boolean isGivenNameOf = sg.getProperty().getId().equals(WikidataEntityMap.IS_GIVEN_NAME_OF);
                boolean isSurnameOf = sg.getProperty().getId().equals(WikidataEntityMap.IS_SURNAME_OF);
                boolean isBirthDateOf = sg.getProperty().getId().equals(WikidataEntityMap.WAS_BORN_ON);
                boolean isDeathDateOf = sg.getProperty().getId().equals(WikidataEntityMap.IS_DEAD_SINCE);
                boolean isWebsiteOf = sg.getProperty().getId().equals(WikidataEntityMap.IS_OFFICIAL_WEBSITE_OF);
                
                /** boolean isPseudonymOf = sg.getProperty().getId().equals(WikidataEntityMap.IS_PSEUDONYM_OF);
                boolean isAlmaMaterOf = sg.getProperty().getId().equals(WikidataEntityMap.IS_ALMA_MATER_OF);
                boolean isStudentOf = sg.getProperty().getId().equals(WikidataEntityMap.IS_STUDENT_OF_PERSON);
                boolean isDoctoralAdvisorOf = sg.getProperty().getId().equals(WikidataEntityMap.IS_DOCTORAL_ADVISOR_OF);
                boolean isDoctoralStudentOf = sg.getProperty().getId().equals(WikidataEntityMap.IS_DOCTORAL_STUDENT_OF); 
                boolean isMemberOf = sg.getProperty().getId().equals(WikidataEntityMap.IS_MEMBER_OF);
                boolean isCitizenOf = sg.getProperty().getId().equals(WikidataEntityMap.IS_CITIZEN_OF);
                boolean isEmployeeOf = sg.getProperty().getId().equals(WikidataEntityMap.IS_EMPLOYEE_OF);
                boolean isPartyMemberOf = sg.getProperty().getId().equals(WikidataEntityMap.IS_PARTY_MEMBER_OF);
                boolean isOfficiallyResidingAt = sg.getProperty().getId().equals(WikidataEntityMap.IS_OFFICIALLY_RESIDING_AT);
                boolean isAffiliatedWith = sg.getProperty().getId().equals(WikidataEntityMap.IS_AFFILIATED_WITH);
                boolean isOpenResearchIdOf = sg.getProperty().getId().equals(WikidataEntityMap.IS_OPEN_RESEARCH_ID_OF);
                boolean isNotableWorkOf = sg.getProperty().getId().equals(WikidataEntityMap.IS_NOTABLE_WORK_OF); **/
                /** if (isSubclassOf && classRecord == null) {
                    // logger.info("Has SubclassOf Relationship.. and NO classRecord");
                    classRecord = getClassRecord(itemDocument, itemDocument.getItemId());
                } **/
                
                // 2) Deal with the various specifics of this StatementGroup (Property)..
                
                // -- is instance | subclass of
                
                if (isInstanceOf || isSubclassOf) {
                    
                    for (Statement s : sg.getStatements()) {
                        if (s.getClaim().getMainSnak() instanceof ValueSnak) {
                            Value mainSnakValue = ((ValueSnak) s.getClaim().getMainSnak()).getValue();
                            JSONObject valueObject = mainSnakValue.accept(new ValueJsonConverter()).getJSONObject("value");
                            String mainSnakValueType = valueObject.getString("entity-type");
                            
                            // --- Statement involving other ITEMS
                            if (mainSnakValueType.equals(WikidataEntityMap.WD_TYPE_ITEM)) {
                                String referencedItemId = valueObject.getString("numeric-id");
                                
                                // 2.1 current wikidata item is direct instanceOf|subclassOf "human" or "person"
                                if (referencedItemId.equals(WikidataEntityMap.HUMAN_ITEM) 
                                    || referencedItemId.equals(WikidataEntityMap.PERSON_ITEM)) {

                                    if (!all_persons.containsKey(itemId)) { // no persona available
                                        // .. keep a reference to person items for counting (the one which has an english label)
                                        all_persons.put(itemId, label);
                                    }
                                    
                                // 2.2 current wikidata item is direct instanceOf|subclassOf "university", "company" or "organisation"
                                } else if (referencedItemId.equals(WikidataEntityMap.COMPANY_ITEM)
                                    || referencedItemId.equals(WikidataEntityMap.UNIVERSITY_ITEM)
                                    || referencedItemId.equals(WikidataEntityMap.ORGANISATION_ITEM)
                                    || referencedItemId.equals(WikidataEntityMap.COLLEGIATE_UNIVERSITY_ITEM)) { // = often subclass of "university" items
                                    
                                    // ### store item as being of some sort of "organisation"
                                    if (!all_institutions.containsKey(itemId)) { // no organisation available
                                        // .. keep a reference to institution items for counting (the one which has an english label)
                                        all_institutions.put(itemId, label);
                                    }
                                }
                            }
                        }
                    }

                } else  if (isCoordinateOf) {
                    
                    for (Statement s : sg.getStatements()) {
                        if (s.getClaim().getMainSnak() instanceof ValueSnak) {
                            Value mainSnakValue = ((ValueSnak) s.getClaim().getMainSnak()).getValue();
                            JSONObject valueObject = mainSnakValue.accept(new ValueJsonConverter()).getJSONObject("value");
                            double longitude = valueObject.getDouble("longitude");
                            double latitude = valueObject.getDouble("latitude");
                            // --- Statement involving globe-coordinates
                            if (longitude != -1 && latitude != -1) {
                                double coordinates[] = {longitude, latitude}; 
                                // String string_value = valueObject.getString("numeric-id");
                                if (!all_coordinates.containsKey(itemId)) { // no persona available
                                    // .. keep a reference to person items for counting (the one which has an english label)
                                    all_coordinates.put(itemId, coordinates);
                                }
                            }
                        }
                    }

                } else  if (isBirthDateOf) {
                    
                    for (Statement s : sg.getStatements()) {
                        if (s.getClaim().getMainSnak() instanceof ValueSnak) {
                            Value mainSnakValue = ((ValueSnak) s.getClaim().getMainSnak()).getValue();
                            JSONObject valueObject = mainSnakValue.accept(new ValueJsonConverter()).getJSONObject("value");
                            String date = valueObject.getString("time"); // ### respect calendermodel
                            // log.info("Item is dead since: " + date); // Access DataValueModel..
                        }
                    }

                } else  if (isDeathDateOf) {
                    
                    for (Statement s : sg.getStatements()) {
                        if (s.getClaim().getMainSnak() instanceof ValueSnak) {
                            Value mainSnakValue = ((ValueSnak) s.getClaim().getMainSnak()).getValue();
                            JSONObject valueObject = mainSnakValue.accept(new ValueJsonConverter()).getJSONObject("value");
                            String date = valueObject.getString("time"); // ### respect calendermodel
                            // log.info("Item is dead since: " + date);
                        }
                    }
                
                } else  if (isGivenNameOf) {
                    
                    for (Statement s : sg.getStatements()) {
                        if (s.getClaim().getMainSnak() instanceof ValueSnak) {
                            Value mainSnakValue = ((ValueSnak) s.getClaim().getMainSnak()).getValue();
                            JSONObject valueObject = mainSnakValue.accept(new ValueJsonConverter()).getJSONObject("value");
                            // ## ARE of type ITEM! log.info("Items given name is: " + valueObject);
                        }
                    }
                
                } else  if (isSurnameOf) {
                    
                    for (Statement s : sg.getStatements()) {
                        if (s.getClaim().getMainSnak() instanceof ValueSnak) {
                            Value mainSnakValue = ((ValueSnak) s.getClaim().getMainSnak()).getValue();
                            JSONObject valueObject = mainSnakValue.accept(new ValueJsonConverter()).getJSONObject("value");
                            // ## ARE of type ITEM! log.info("Items surname is : " + valueObject);
                        }
                    }
                
                } else  if (isWebsiteOf) {
                    
                    for (Statement s : sg.getStatements()) {
                        if (s.getClaim().getMainSnak() instanceof ValueSnak) {
                            Value mainSnakValue = ((ValueSnak) s.getClaim().getMainSnak()).getValue();
                            JSONObject valueObject = mainSnakValue.accept(new ValueJsonConverter()).getJSONObject("value");
                            log.info("Items officla website is at \"" + valueObject + "\"");
                        }
                    }
                }
            }
        }

        // Print a report every 10000 items:
        if (this.entityCount % 10000 == 0) {
            printProcessingStatus();
        }
    }
    
    private String getFirstLabel(ItemDocument itemDocument) {
        MonolingualTextValue value = null;
        if (itemDocument.getLabels().size() > 1) {
            if (itemDocument.getLabels().containsKey(WikidataEntityMap.LANG_EN)) {
                value = itemDocument.getLabels().get(WikidataEntityMap.LANG_EN);
            } else { // if no english label avaiable, take first label available
                for (String key : itemDocument.getLabels().keySet()) {
                    value = itemDocument.getLabels().get(key);
                    break;
                }
            }
        }
        if (value == null) {
            log.warning("could not find ANY label for item, default_label is NULL!");
            return null;
        }
        return value.getText();
    }
    
    private void createPersonTopic(String firstName, String lastName, String itemId) {
        CompositeValueModel personComposite = new CompositeValueModel();
        personComposite.put(DM_PERSON_NAME, new CompositeValueModel()
            .put(DM_PERSON_FIRST_NAME, firstName)
            .put(DM_PERSON_LAST_NAME, lastName)
        );
        // personComposite.put(DM_NOTES, new SimpleValue("<p><a href=\"http://www.wikidata.org./entity/"+itemId+"\"></a></p>"));
        TopicModel personModel = new TopicModel(
            WikidataEntityMap.WD_ENTITY_BASE_URI + itemId, DM_PERSON, personComposite);
        dms.createTopic(personModel, null);
        // log.info("> Created person Topic: " + firstName + " " + lastName);
    }

    private void createInstitutionTopic(String name, String itemId) {
        CompositeValueModel institutionComposite = new CompositeValueModel();
        institutionComposite.put(DM_INSTITUTION_NAME, name);
        TopicModel institutionModel = new TopicModel(
            WikidataEntityMap.WD_ENTITY_BASE_URI + itemId, DM_INSTITUTION, institutionComposite);
        // ### set GeoCoordinate Facet via values in all_coordinates
        dms.createTopic(institutionModel, null);
    }

    @Override
    public void processPropertyDocument(PropertyDocument propertyDocument) {
        // count and check time 
        countEntity();
    }

    /**
     * Prints a report about the statistics gathered so far.
     */
    private void printProcessingStatus() {
        log.info("Processed " + this.entityCount + " items.");
        log.info("Identified " + this.all_persons.size() + " human beings "
            + "and " + this.all_institutions.size() + " institutions.");
    }

    /**
    * Counts one entity. Every once in a while, the current time is checked so
    * as to print an intermediate report roughly every ten seconds.
    */
    private void countEntity() {
       if (!this.timer.isRunning()) {
           startTimer();
       }
       this.entityCount++;
       if (this.entityCount % 100 == 0) {
           timer.stop();
           int seconds = (int) (timer.getTotalWallTime() / 1000000000);
           if (seconds >= this.lastSeconds + 10) {
               this.lastSeconds = seconds;
               printProcessingStatus();
               if (this.timeout > 0 && seconds > this.timeout) {
                   log.info("Timeout. Aborting processing.");
                   throw new TimeoutException();
               }
           }
           timer.start();
       }
    }

    /**
    * Stops the processing and prints the final time.
    */
    public void stop() {
        
        printProcessingStatus();
        
        log.info("Start importing topics ...");
        
        for (String itemId : all_persons.keySet()) { // this might work but only after having read in the complete dump
            String fullName = itemsFirstLabel.get(itemId);
            if (fullName != null) {
                String firstName = fullName.split(" ")[0]; // ### import full name
                String lastName = fullName.split(" ")[fullName.split(" ").length-1];
                createPersonTopic(firstName, lastName, itemId);   
            } else {
                log.warning("Person Topic ("+itemId+") NOT created because value of Fullname ends up being NULL!");
            }
        }
        
        for (String itemId : all_institutions.keySet()) { // this might work but only after having read in the complete dump
            String instName = itemsFirstLabel.get(itemId);
            if (instName != null) createInstitutionTopic(instName, itemId);
        }

        log.info("Finished processing.");
        this.timer.stop();
        this.lastSeconds = (int) (timer.getTotalWallTime() / 1000000000);
        printProcessingStatus();
    }

    private void startTimer() {
        log.info("Starting processing ("+timeout+" sec) wikidata JSON dump.");
        this.timer.start();
    }

    public class TimeoutException extends RuntimeException {

        private static final long serialVersionUID = -1083533602730765194L;

    }

}