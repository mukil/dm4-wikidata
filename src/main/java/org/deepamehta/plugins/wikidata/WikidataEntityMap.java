package org.deepamehta.plugins.wikidata;

/**
 *
 * @author bob
 */
public class WikidataEntityMap {
    
    public final static String WD_TYPE_ITEM                 = "item";
    public final static String WD_TYPE_GLOBE_COORDINATES    = "globe-coordinates";
    public final static String WD_TYPE_PROPERTY             = "property";
    public final static String WD_ENTITY_BASE_URI           = "org.wikidata.entity.";
    
    
    
    // --- Wikidata Language Codes
    
    public final static String LANG_EN                      = "en";
    
    
    
    // --- General Wikidata Items
    
    public final static String HUMAN_ITEM                   = "Q5";
    public final static String UNIVERSITY_ITEM              = "Q3918";
    public final static String ORGANISATION_ITEM            = "Q43229";
    public final static String PERSON_ITEM                  = "Q215627";
    public final static String COMPANY_ITEM                 = "Q783794";
    public final static String COLLEGIATE_UNIVERSITY_ITEM   = "Q3354859";
    public final static String CITY_ITEM                    = "Q515";
    public final static String METROPOLIS_ITEM              = "Q200250";
    public final static String CAPITAL_CITY_ITEM            = "Q36";
    public final static String SOVEREIGN_STATE_ITEM         = "Q3624078";
    public final static String COUNTRY_ITEM                 = "Q6256";
    
    
    
    // --- Generic Wikidata Properties
    
    public final static String IS_INSTANCE_OF               = "P31";
    public final static String IS_SUBCLASS_OF               = "P279";
    public final static String STARTED_AT                   = "P580";
    public final static String ENDED_AT                     = "P582";    
    
    
    
    // --- Persona Related Properties

    public final static String IS_SISTER_OF                 = "P9";
    public final static String IS_FATHER_OF                 = "P22";
    public final static String IS_MOTHER_OF                 = "P25";
    public final static String IS_SPOUSE_OF                 = "P26";
    public final static String IS_CITIZEN_OF                = "P27";
    public final static String IS_CHILD_OF                  = "P40";
    public final static String IS_ALMA_MATER_OF             = "P69";
    public final static String IS_PARTY_MEMBER_OF           = "P102";
    public final static String IS_EMPLOYEE_OF               = "P108";
    public final static String IS_FOUNDER_OF                = "P112";
    public final static String IS_DOCTORAL_ADVISOR_OF       = "P184";
    public final static String IS_DOCTORAL_STUDENT_OF       = "P185";
    public final static String IS_OFFICIALLY_RESIDING_AT    = "P263";
    public final static String IS_MEMBER_OF                 = "P463";
    public final static String IS_OPEN_RESEARCH_ID_OF       = "P496";
    public final static String CAUSE_OF_DEATH               = "P509";
    public final static String WAS_BORN_ON                  = "P569";
    public final static String IS_DEAD_SINCE                = "P570";
    public final static String IS_COORDINATE_LOCATION       = "P625";
    public final static String IS_PARTICIPANT_OF            = "P710";
    public final static String IS_SURNAME_OF                = "P734";
    public final static String IS_GIVEN_NAME_OF             = "P735";
    public final static String IS_PSEUDONYM_OF              = "P742";
    public final static String IS_NOTABLE_WORK_OF           = "P800";
    public final static String IS_OFFICIAL_WEBSITE_OF       = "P856";
    public final static String IS_STUDENT_OF_PERSON         = "P1066"; // doublecheck
    public final static String IS_AFFILIATED_WITH           = "P1416";
    public final static String IS_HEAD_OF_GOVERNMENT_OF     = "P6";
    public final static String IS_POPULATION_OF             = "P1082";
    
}
