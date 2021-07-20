
### Schema:         waters/waters-covid-19
#### Description:   WATERS OTC,POC COVID-19 flat file

---

**Name**: abnormal_flag

**Type**: CODE

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
A|Abnormal (applies to non-numeric results)
>|Above absolute high-off instrument scale
H|Above high normal
HH|Above upper panic limits
AC|Anti-complementary substances present
<|Below absolute low-off instrument scale
L|Below low normal
LL|Below lower panic limits
B|Better--use when direction not relevant
TOX|Cytotoxic substance present
DET|Detected
IND|Indeterminate
I|Intermediate. Indicates for microbiology susceptibilities only.
MS|Moderately susceptible. Indicates for microbiology susceptibilities only.
NEG|Negative
null|No range defined, or normal ranges don't apply
NR|Non-reactive
N|Normal (applies to non-numeric results)
ND|Not Detected
POS|Positive
QCF|Quality Control Failure
RR|Reactive
R|Resistant. Indicates for microbiology susceptibilities only.
D|Significant change down
U|Significant change up
S|Susceptible. Indicates for microbiology susceptibilities only.
AA|Very abnormal (applies to non-numeric units, analogous to panic limits for numeric units)
VS|Very susceptible. Indicates for microbiology susceptibilities only.
WR|Weakly reactive
W|Worse--use when direction not relevant

**Documentation**:

This field is generated based on the normalcy status of the result. A = abnormal; N = normal

---

**Name**: PatStID

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: healthcareEmployee

**Type**: CODE

**PII**: No

**Format**: $display

**LOINC Code**: 95418-0

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|YES
N|NO
UNK|UNK

**Documentation**:

Override the base hl70136 valueset with a custom one, to handle slightly different syntax

---

**Name**: serialNumber

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

---

**Name**: deviceName

**Type**: TABLE

**PII**: No

**Cardinality**: [1..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 

**Table**: LIVD-SARS-CoV-2-2021-04-28

**Table Column**: Model

---

**Name**: firstTest

**Type**: CODE

**PII**: No

**Format**: $display

**LOINC Code**: 95417-2

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|YES
N|NO
UNK|UNK

**Documentation**:

Override the base hl70136 valueset with a custom one, to handle slightly different syntax

---

**Name**: hospitalized

**Type**: CODE

**PII**: No

**Format**: $display

**LOINC Code**: 77974-4

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|YES
N|NO
UNK|UNK

**Documentation**:

Override the base hl70136 valueset with a custom one, to handle slightly different syntax

---

**Name**: symptomsIcu

**Type**: CODE

**PII**: No

**Format**: $display

**LOINC Code**: 95420-6

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|YES
N|NO
UNK|UNK

**Documentation**:

Override the base hl70136 valueset with a custom one, to handle slightly different syntax

---

**Name**: testId

**Type**: ID

**PII**: No

**Cardinality**: [1..1]

**Documentation**:

unique id to track the usage of the message

---

**Name**: testOrderedDate

**Type**: DATE

**PII**: No

**Cardinality**: [0..1]

---

**Name**: testOrdered

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-2021-04-28

**Table Column**: Test Ordered LOINC Code

---

**Name**: testName

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-2021-04-28

**Table Column**: Test Ordered LOINC Long Name

---

**Name**: ordering_facility_county

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: County

---

**Name**: ordering_facility_state

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The state of the facility which the test was ordered from

---

**Name**: orderingProviderCity

**Type**: CITY

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The city of the provider

---

**Name**: orderingProviderFname

**Type**: PERSON_NAME

**PII**: No

**HL7 Fields**

- [OBR-16-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.16.3)
- [ORC-12-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.12.3)

**Cardinality**: [0..1]

**Documentation**:

The first name of the provider who ordered the test

---

**Name**: orderingProviderNpi

**Type**: ID_NPI

**PII**: No

**HL7 Fields**

- [OBR-16-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.16.1)
- [ORC-12-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.12.1)

**Cardinality**: [0..1]

**Documentation**:

The ordering provider’s National Provider Identifier

---

**Name**: orderingProviderLname

**Type**: PERSON_NAME

**PII**: No

**HL7 Fields**

- [OBR-16-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.16.2)
- [ORC-12-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.12.2)

**Cardinality**: [0..1]

**Documentation**:

The last name of provider who ordered the test

---

**Name**: orderingProviderPhone

**Type**: TELEPHONE

**PII**: Yes

**HL7 Fields**

- [OBR-17](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.17)
- [ORC-14](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.14)

**Cardinality**: [0..1]

**Documentation**:

The phone number of the provider

---

**Name**: orderingProviderState

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The state of the provider

---

**Name**: orderingProviderAddress

**Type**: STREET

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The street address of the provider

---

**Name**: orderingProviderAddress2

**Type**: STREET_OR_BLANK

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The street second address of the provider

---

**Name**: orderingProviderZip

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The zip code of the provider

---

**Name**: patientAge

**Type**: NUMBER

**PII**: No

**LOINC Code**: 30525-0

**Cardinality**: [0..1]

---

**Name**: patientCity_pii

**Type**: CITY

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's city

---

**Name**: patientCounty

**Type**: TABLE_OR_BLANK

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: County

---

**Name**: patientDob_pii

**Type**: DATE

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's date of birth. Default format is yyyyMMdd.

Other states may choose to define their own formats.


---

**Name**: patientEmail_pii

**Type**: EMAIL

**PII**: Yes

**Cardinality**: [0..1]

---

**Name**: patientEthnicity

**Type**: CODE

**PII**: No

**Format**: $alt

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
H|Hispanic or Latino
N|Non Hispanic or Latino
U|Unknown
H|Hispanic or Latino
N|Non Hispanic or Latino
U|Unknown
U|Unknown

**Alt Value Sets**

Code | Display
---- | -------
H|2135-2
N|2186-5
U|UNK
U|ASKU

**Documentation**:

The patient's ethnicity. There is a valueset defined based on the values in PID-22, but downstream
consumers are free to define their own values. Please refer to the consumer-specific schema if you have questions.


---

**Name**: patientNameFirst_pii

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's first name

---

**Name**: patientSex

**Type**: CODE

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
M|Male
F|Female
O|Other
A|Ambiguous
U|Unknown
N|Not applicable

**Documentation**:

The patient's gender. There is a valueset defined based on the values in PID-8-1, but downstream consumers are free to define their own accepted values. Please refer to the consumer-specific schema if you have questions.


---

**Name**: patientUniqueId_pii

**Type**: TEXT

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The ID for the patient within one of the reporting entities for this lab result. It could be the
the patient ID from the testing lab, the oder placer, the ordering provider, or even within the PRIME system itself.


---

**Name**: patientUniqueId

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: patientNameLast_pii

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's last name

---

**Name**: patientNameMiddle_pii

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [0..1]

---

**Name**: patientPhone_pii

**Type**: TELEPHONE

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's phone number with area code

---

**Name**: patientRace

**Type**: CODE

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
1002-5|American Indian or Alaska Native
2028-9|Asian
2054-5|Black or African American
2076-8|Native Hawaiian or Other Pacific Islander
2106-3|White
2131-1|Other
UNK|Unknown
ASKU|Asked, but unknown

**Documentation**:

The patient's race. There is a common valueset defined for race values, but some states may choose to define different code/value combinations.


---

**Name**: healthcareEmployeeType

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: patientState

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The patient's state

---

**Name**: patientHomeAddress_pii

**Type**: STREET

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's street address

---

**Name**: patientHomeAddress2_pii

**Type**: STREET_OR_BLANK

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's second address line

---

**Name**: PatZip

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The patient's zip code

---

**Name**: pregnant

**Type**: CODE

**PII**: No

**LOINC Code**: 82810-3

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
77386006|Pregnant
60001007|Not Pregnant
261665006|Unknown

**Documentation**:

Is the patient pregnant?

---

**Name**: previousTestDate

**Type**: DATE

**PII**: No

**Cardinality**: [0..1]

---

**Name**: previousTestResult

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
260373001|Detected
260415000|Not detected
720735008|Presumptive positive
10828004|Positive
42425007|Equivocal
260385009|Negative
895231008|Not detected in pooled specimen
462371000124108|Detected in pooled specimen
419984006|Inconclusive
125154007|Specimen unsatisfactory for evaluation
455371000124106|Invalid result
840539006|Disease caused by sever acute respiratory syndrome coronavirus 2 (disorder)
840544004|Suspected disease caused by severe acute respiratory coronavirus 2 (situation)
840546002|Exposure to severe acute respiratory syndrome coronavirus 2 (event)
840533007|Severe acute respiratory syndrome coronavirus 2 (organism)
840536004|Antigen of severe acute respiratory syndrome coronavirus 2 (substance)
840535000|Antibody to severe acute respiratory syndrome coronavirus 2 (substance)
840534001|Severe acute respiratory syndrome coronavirus 2 vaccination (procedure)
373121007|Test not done

---

**Name**: previousTestType

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: reportingFacility

**Type**: HD

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The reporting facility for the message, as specified by the receiver. This is typically used if PRIME is the
aggregator


---

**Name**: reportingFacilityCLIA

**Type**: ID_CLIA

**PII**: No

**HL7 Fields**

- [MSH-4-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/MSH.4.2)
- [PID-3-4-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/PID.3.4.2)
- [PID-3-6-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/PID.3.6.2)
- [SPM-2-1-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.2.1.3)
- [SPM-2-2-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.2.2.3)

**Cardinality**: [0..1]

**Documentation**:

The reporting facility's CLIA

---

**Name**: congregateResident

**Type**: CODE

**PII**: No

**Format**: $display

**LOINC Code**: 95421-4

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|YES
N|NO
UNK|UNK

**Documentation**:

Override the base hl70136 valueset with a custom one, to handle slightly different syntax

---

**Name**: SubmitterUID

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

ID name of org that is sending this data to ReportStream.  Suitable for provenance or chain of custody tracking.  Not to be confused with sending_application, in which ReportStream acts as the 'sender' to the downstream jurisdiction.

---

**Name**: congregateResidentType

**Type**: CODE

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
22232009|Hospital
32074000|Long Term Care Hospital
224929004|Secure Hospital
42665001|Nursing Home
30629002|Retirement Home
74056004|Orphanage
722173008|Prison-based care site
20078004|Substance Abuse Treatment Center
257573002|Boarding House
224683003|Military Accommodation
284546000|Hospice
257628001|Hostel
310207003|Sheltered Housing
57656006|Penal Institution
32911000|Homeless

**Documentation**:

The type of facility providing care (Hospital, Nursing Home, etc.).

---

**Name**: specimenCollectedDate

**Type**: DATETIME

**PII**: No

**HL7 Fields**

- [OBR-7](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.7)
- [OBR-8](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.8)
- [OBX-14](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBX.14)
- [SPM-17-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.17.1)

**Cardinality**: [0..1]

**Documentation**:

The date which the specimen was collected. The default format is yyyyMMddHHmmsszz


---

**Name**: specimenId

**Type**: EI

**PII**: No

**HL7 Fields**

- [SPM-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.2)

**Cardinality**: [0..1]


**Reference URL**:
[https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.2) 

**Documentation**:

A unique code for this specimen

---

**Name**: specimenSource

**Type**: CODE

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
445297001|Swab of internal nose
258500001|Nasopharyngeal swab
871810001|Mid-turbinate nasal swab
697989009|Anterior nares swab
258411007|Nasopharyngeal aspirate
429931000124105|Nasal aspirate
258529004|Throat swab
119334006|Sputum specimen
119342007|Saliva specimen
258607008|Bronchoalveolar lavage fluid sample
119364003|Serum specimen
119361006|Plasma specimen
440500007|Dried blood spot specimen
258580003|Whole blood sample
122555007|Venous blood specimen

**Documentation**:

The specimen source, such as Blood or Serum

---

**Name**: symptomatic

**Type**: CODE

**PII**: No

**Format**: $display

**LOINC Code**: 95419-8

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|YES
N|NO
UNK|UNK

**Documentation**:

Override the base hl70136 valueset with a custom one, to handle slightly different syntax

---

**Name**: symptomsList

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: test_authorized_for_home

**Type**: TABLE

**PII**: No

**Default Value**: N

**Cardinality**: [0..1]


**Reference URL**:
[https://www.fda.gov/news-events/fda-newsroom/press-announcements](https://www.fda.gov/news-events/fda-newsroom/press-announcements) 

**Table**: LIVD-Supplemental-2021-06-07

**Table Column**: is_home

**Documentation**:

Is the test authorized for home use by the FDA (Y, N, UNK)

---

**Name**: test_authorized_for_otc

**Type**: TABLE

**PII**: No

**Default Value**: N

**Cardinality**: [0..1]


**Reference URL**:
[https://www.fda.gov/news-events/fda-newsroom/press-announcements](https://www.fda.gov/news-events/fda-newsroom/press-announcements) 

**Table**: LIVD-Supplemental-2021-06-07

**Table Column**: is_otc

**Documentation**:

Is the test authorized for over-the-counter purchase by the FDA (Y, N, UNK)

---

**Name**: test_authorized_for_unproctored

**Type**: TABLE

**PII**: No

**Default Value**: N

**Cardinality**: [0..1]


**Reference URL**:
[https://www.fda.gov/news-events/fda-newsroom/press-announcements](https://www.fda.gov/news-events/fda-newsroom/press-announcements) 

**Table**: LIVD-Supplemental-2021-06-07

**Table Column**: is_unproctored

**Documentation**:

Is the test authorized for unproctored administration by the FDA (Y, N, UNK)

---

**Name**: deviceIdentifier

**Type**: TABLE

**PII**: No

**Cardinality**: [1..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 

**Table**: LIVD-SARS-CoV-2-2021-04-28

**Table Column**: Testkit Name ID

**Documentation**:

Follows guidence for OBX-17 as defined in the HL7 Confluence page

---

**Name**: testResult

**Type**: CODE

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
260373001|Detected
260415000|Not detected
720735008|Presumptive positive
10828004|Positive
42425007|Equivocal
260385009|Negative
895231008|Not detected in pooled specimen
462371000124108|Detected in pooled specimen
419984006|Inconclusive
125154007|Specimen unsatisfactory for evaluation
455371000124106|Invalid result
840539006|Disease caused by sever acute respiratory syndrome coronavirus 2 (disorder)
840544004|Suspected disease caused by severe acute respiratory coronavirus 2 (situation)
840546002|Exposure to severe acute respiratory syndrome coronavirus 2 (event)
840533007|Severe acute respiratory syndrome coronavirus 2 (organism)
840536004|Antigen of severe acute respiratory syndrome coronavirus 2 (substance)
840535000|Antibody to severe acute respiratory syndrome coronavirus 2 (substance)
840534001|Severe acute respiratory syndrome coronavirus 2 vaccination (procedure)
373121007|Test not done

**Documentation**:

The result of the test performed. For IgG, IgM and CT results that give a numeric value put that here.

---

**Name**: testResultDate

**Type**: DATETIME

**PII**: No

**Cardinality**: [0..1]

---

**Name**: testReportDate

**Type**: DATETIME

**PII**: No

**Cardinality**: [0..1]

---

**Name**: performingFacility

**Type**: ID_CLIA

**PII**: No

**HL7 Fields**

- [OBR-2-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.2.3)
- [OBR-3-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.3.3)
- [OBX-15-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBX.15.1)
- [OBX-23-10](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBX.23.10)
- [ORC-2-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.2.3)
- [ORC-3-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.3.3)

**Cardinality**: [0..1]

**Documentation**:

CLIA Number from the laboratory that sends the message to DOH

An example of the ID is 03D2159846


---

**Name**: performingFacilityZip

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The postal code for the testing lab

---

**Name**: TXNTIMESTAMP

**Type**: DATETIME

**PII**: No

**Format**: yyyyMMddhhmmss

**Cardinality**: [0..1]

---

**Name**: TxInitiator

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---
