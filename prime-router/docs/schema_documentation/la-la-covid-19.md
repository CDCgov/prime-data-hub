
### Schema:         la/la-covid-19
#### Description:   LA COVID-19

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

**Name**: comment

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: comment_source

**Type**: CODE

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
L|Ancillary (filler) department is source of comment
O|Other system is source of comment
P|Orderer (placer) is source of comment

---

**Name**: comment_type

**Type**: CODE

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
1R|Primary Reason
2R|Secondary Reason
AI|Ancillary Instructions
DR|Duplicate/Interaction Reason
GI|General Instructions
GR|General Reason
PI|Patient Instructions
RE|Remark

---

**Name**: date_result_released

**Type**: DATETIME

**PII**: No

**Cardinality**: [0..1]

---

**Name**: device_id

**Type**: TABLE

**PII**: No

**HL7 Fields**

- [OBX-17-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBX.17.1)
- [OBX-17-9](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBX.17.9)

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 

**Table**: LIVD-SARS-CoV-2-2021-04-28

**Documentation**:

Device_id is a generated value for the OBX-17 field. It is based on the device model and the LIVD table.

---

**Name**: device_id_type

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 

**Table**: LIVD-SARS-CoV-2-2021-04-28

**Documentation**:

Device_id_type is a generated value for the OBX-17 field. It is based on the device model and the LIVD table.

---

**Name**: employed_in_healthcare

**Type**: CODE

**PII**: No

**LOINC Code**: 95418-0

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|Yes
N|No
UNK|Unknown

**Documentation**:

Is the patient employed in health care?

---

**Name**: employed_in_high_risk_setting

**Type**: CODE

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|Yes
N|No
UNK|Unknown

**Documentation**:

Is the patient employed in a high risk setting? This AOE question doesn't have an HL7 conversion per the HHS, so it is not included in HL7 messages.


---

**Name**: equipment_instance_id

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

---

**Name**: equipment_manufacture

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 

**Table**: LIVD-SARS-CoV-2-2021-04-28

**Table Column**: Manufacturer

---

**Name**: equipment_model_id

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 

**Table**: LIVD-SARS-CoV-2-2021-04-28

**Table Column**: Equipment UID

---

**Name**: equipment_model_id_type

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 

**Table**: LIVD-SARS-CoV-2-2021-04-28

**Table Column**: Equipment UID Type

---

**Name**: equipment_model_name

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 

**Table**: LIVD-SARS-CoV-2-2021-04-28

**Table Column**: Model

---

**Name**: file_created_date

**Type**: DATE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

When was this file created. This is only used for HL7 generation.

---

**Name**: filler_clia

**Type**: ID_CLIA

**PII**: No

**Cardinality**: [0..1]

---

**Name**: filler_name

**Type**: TEXT

**PII**: No

**HL7 Fields**

- [OBR-3-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.3.2)
- [ORC-3-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.3.2)

**Cardinality**: [0..1]

---

**Name**: filler_order_id

**Type**: ID

**PII**: No

**HL7 Fields**

- [OBR-3-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.3.1)
- [ORC-3-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.3.1)
- [SPM-2-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.2.2)

**Cardinality**: [0..1]

**Documentation**:

Accension number

---

**Name**: first_test

**Type**: CODE

**PII**: No

**LOINC Code**: 95417-2

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|Yes
N|No
UNK|Unknown

**Documentation**:

Is this the patient's first test for this condition?

---

**Name**: flatfile_version_no

**Type**: NUMBER

**PII**: No

**Cardinality**: [0..1]

---

**Name**: hospitalized

**Type**: CODE

**PII**: No

**LOINC Code**: 77974-4

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|Yes
N|No
UNK|Unknown

**Documentation**:

Is the patient hospitalized?

---

**Name**: icu

**Type**: CODE

**PII**: No

**LOINC Code**: 95420-6

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|Yes
N|No
UNK|Unknown

**Documentation**:

Is the patient in the ICU?

---

**Name**: illness_onset_date

**Type**: DATE

**PII**: No

**LOINC Code**: 65222-2

**Cardinality**: [0..1]

---

**Name**: message_id

**Type**: ID

**PII**: No

**Cardinality**: [1..1]

**Documentation**:

unique id to track the usage of the message

---

**Name**: message_profile_id

**Type**: EI

**PII**: No

**Default Value**: PHLabReport-NoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO

**Cardinality**: [0..1]

**Documentation**:

The message profile identifer

---

**Name**: observation_result_status

**Type**: CODE

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
C|Record coming over is a correction and thus replaces a final result
D|Deletes the OBX record
F|Final results; Can only be changed with a corrected result
I|Specimen in lab; results pending
N|Not asked; used to affirmatively document that the observation identified in the OBX was not sought when the universal service ID in OBR-4 implies that it would be sought.
O|Order detail description only (no result)
P|Preliminary results
R|Results entered -- not verified
S|Partial results
U|Results status change to final without retransmitting results already sent as ‘preliminary.’  E.g., radiology changes status from preliminary to final
W|Post original as wrong, e.g., transmitted for wrong patient
X|Results cannot be obtained for this observation

---

**Name**: order_result_status

**Type**: CODE

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
A|Some, but not all, results available
C|Corrected, final
F|Final results
I|No results available; specimen received, procedure incomplete
M|Corrected, not final
N|Procedure completed, results pending
O|Order received; specimen not yet received
P|Preliminary
R|Results stored; not yet verified
S|No results available; procedure scheduled, but not done
X|No results available; Order canceled
Y|No order on record for this test
Z|No record of this patient

---

**Name**: order_test_date

**Type**: DATE

**PII**: No

**Cardinality**: [0..1]

---

**Name**: ordered_test_code

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-2021-04-28

**Table Column**: Test Ordered LOINC Code

---

**Name**: ordered_test_encoding_version

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-2021-04-28

**Table Column**: LOINC Version ID

---

**Name**: ordered_test_name

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-2021-04-28

**Table Column**: Test Ordered LOINC Long Name

---

**Name**: ordered_test_system

**Type**: TEXT

**PII**: No

**Default Value**: LOINC

**Cardinality**: [0..1]

---

**Name**: ordered_test_system_abbr

**Type**: TEXT

**PII**: No

**Default Value**: LN

**Cardinality**: [0..1]

---

**Name**: ordering_facility_city

**Type**: CITY

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The city of the facility which the test was ordered from

---

**Name**: ordering_facility_country

**Type**: TEXT

**PII**: No

**Default Value**: USA

**Cardinality**: [0..1]

---

**Name**: ordering_facility_county

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: County

---

**Name**: ordering_facility_county_code

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: FIPS

---

**Name**: ordering_facility_email

**Type**: EMAIL

**PII**: No

**Cardinality**: [0..1]

---

**Name**: ordering_facility_name

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The name of the facility which the test was ordered from

---

**Name**: ordering_facility_phone_number

**Type**: TELEPHONE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The phone number of the facility which the test was ordered from

---

**Name**: ordering_facility_state

**Type**: TABLE

**PII**: No

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The state of the facility which the test was ordered from

---

**Name**: ordering_facility_street

**Type**: STREET

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The address of the facility which the test was ordered from

---

**Name**: ordering_facility_street2

**Type**: STREET_OR_BLANK

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The secondary address of the facility which the test was ordered from

---

**Name**: ordering_facility_zip_code

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The zip code of the facility which the test was ordered from

---

**Name**: ordering_provider_city

**Type**: CITY

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The city of the provider

---

**Name**: ordering_provider_country

**Type**: TEXT

**PII**: No

**Default Value**: USA

**Cardinality**: [0..1]

---

**Name**: ordering_provider_county

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: County

---

**Name**: ordering_provider_county_code

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: FIPS

**Documentation**:

The FIPS code for the ordering provider

---

**Name**: ordering_provider_first_name

**Type**: PERSON_NAME

**PII**: No

**HL7 Fields**

- [OBR-16-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.16.3)
- [ORC-12-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.12.3)

**Cardinality**: [0..1]

**Documentation**:

The first name of the provider who ordered the test

---

**Name**: ordering_provider_id

**Type**: ID_NPI

**PII**: No

**HL7 Fields**

- [OBR-16-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.16.1)
- [ORC-12-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.12.1)

**Cardinality**: [0..1]

**Documentation**:

The ordering provider’s National Provider Identifier

---

**Name**: ordering_provider_id_authority

**Type**: HD

**PII**: No

**HL7 Fields**

- [OBR-16-9](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.16.9)
- [ORC-12-9](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.12.9)

**Cardinality**: [0..1]

**Documentation**:

Usually the OID for CMS

---

**Name**: ordering_provider_id_authority_type

**Type**: TEXT

**PII**: No

**HL7 Fields**

- [OBR-16-13](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.16.13)
- [ORC-12-13](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.12.13)

**Cardinality**: [0..1]

**Documentation**:

Usually NPI

---

**Name**: ordering_provider_last_name

**Type**: PERSON_NAME

**PII**: No

**HL7 Fields**

- [OBR-16-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.16.2)
- [ORC-12-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.12.2)

**Cardinality**: [0..1]

**Documentation**:

The last name of provider who ordered the test

---

**Name**: ordering_provider_middle_initial

**Type**: PERSON_NAME

**PII**: No

**HL7 Fields**

- [OBR-16-4](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.16.4)
- [ORC-12-4](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.12.4)

**Cardinality**: [0..1]

---

**Name**: ordering_provider_middle_name

**Type**: PERSON_NAME

**PII**: No

**HL7 Fields**

- [OBR-16-4](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.16.4)
- [ORC-12-4](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.12.4)

**Cardinality**: [0..1]

---

**Name**: ordering_provider_phone_number

**Type**: TELEPHONE

**PII**: Yes

**HL7 Fields**

- [OBR-17](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.17)
- [ORC-14](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.14)

**Cardinality**: [0..1]

**Documentation**:

The phone number of the provider

---

**Name**: ordering_provider_state

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The state of the provider

---

**Name**: ordering_provider_street

**Type**: STREET

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The street address of the provider

---

**Name**: ordering_provider_street2

**Type**: STREET_OR_BLANK

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The street second address of the provider

---

**Name**: ordering_provider_zip_code

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The zip code of the provider

---

**Name**: patient_age

**Type**: NUMBER

**PII**: No

**LOINC Code**: 30525-0

**Cardinality**: [0..1]

---

**Name**: patient_age_units

**Type**: CODE

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
min|minutes
h|hours
d|days
wk|weeks
mo|months
a|years

**Documentation**:

Always filled when `patient_age` is filled

---

**Name**: patient_city

**Type**: CITY

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's city

---

**Name**: patient_country

**Type**: TEXT

**PII**: No

**Default Value**: USA

**Cardinality**: [0..1]

---

**Name**: patient_county

**Type**: TABLE_OR_BLANK

**PII**: No

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: County

---

**Name**: patient_county_code

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: FIPS

**Documentation**:

The FIPS code for the patient's county

---

**Name**: patient_death_date

**Type**: DATE

**PII**: Yes

**Cardinality**: [0..1]

---

**Name**: patient_died

**Type**: CODE

**PII**: Yes

**Default Value**: N

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|Yes
N|No
UNK|Unknown

---

**Name**: patient_dob

**Type**: DATE

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's date of birth. Default format is yyyyMMdd.

Other states may choose to define their own formats.


---

**Name**: patient_drivers_license

**Type**: ID_DLN

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's drivers license number

---

**Name**: patient_email

**Type**: EMAIL

**PII**: Yes

**Cardinality**: [0..1]

---

**Name**: patient_ethnicity

**Type**: CODE

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
H|Hispanic or Latino
N|Non Hispanic or Latino
U|Unknown

**Documentation**:

The patient's ethnicity. There is a valueset defined based on the values in PID-22, but downstream
consumers are free to define their own values. Please refer to the consumer-specific schema if you have questions.


---

**Name**: patient_first_name

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's first name

---

**Name**: patient_gender

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

**Name**: patient_id

**Type**: TEXT

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The ID for the patient within one of the reporting entities for this lab result. It could be the
the patient ID from the testing lab, the oder placer, the ordering provider, or even within the PRIME system itself.


---

**Name**: patient_id_assigner

**Type**: HD

**PII**: No

**HL7 Fields**

- [PID-3-4](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/PID.3.4)
- [PID-3-6-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/PID.3.6.2)

**Cardinality**: [0..1]

**Documentation**:

The name of the assigner of the patient_id field. Typically we use the name of the ordering facility

---

**Name**: patient_id_type

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: patient_last_name

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [1..1]

**Documentation**:

The patient's last name

---

**Name**: patient_middle_initial

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [0..1]

---

**Name**: patient_middle_name

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [0..1]

---

**Name**: patient_name_type_code

**Type**: TEXT

**PII**: No

**Default Value**: L

**Cardinality**: [0..1]

---

**Name**: patient_phone_number

**Type**: TELEPHONE

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's phone number with area code

---

**Name**: patient_race

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

**Name**: patient_state

**Type**: TABLE

**PII**: No

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The patient's state

---

**Name**: patient_street

**Type**: STREET

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's street address

---

**Name**: patient_street2

**Type**: STREET_OR_BLANK

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's second address line

---

**Name**: patient_suffix

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The suffix for the patient's name, (i.e. Jr, Sr, etc)

---

**Name**: patient_tribal_citizenship

**Type**: CODE

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
338|Village of Afognak
339|Agdaagux Tribe of King Cove
340|Native Village of Akhiok
341|Akiachak Native Community
342|Akiak Native Community
343|Native Village of Akutan
344|Village of Alakanuk
345|Alatna Village
346|Native Village of Aleknagik
347|Algaaciq Native Village (St. Mary's)
348|Allakaket Village
349|Native Village of Ambler
350|Village of Anaktuvuk Pass
351|Yupiit of Andreafski
352|Angoon Community Association
353|Village of Aniak
354|Anvik Village
355|Arctic Village (See Native Village of Venetie Trib
356|Asa carsarmiut Tribe (formerly Native Village of M
357|Native Village of Atka
358|Village of Atmautluak
359|Atqasuk Village (Atkasook)
360|Native Village of Barrow Inupiat Traditional Gover
361|Beaver Village
362|Native Village of Belkofski
363|Village of Bill Moore's Slough
364|Birch Creek Tribe
365|Native Village of Brevig Mission
366|Native Village of Buckland
367|Native Village of Cantwell
368|Native Village of Chanega (aka Chenega)
369|Chalkyitsik Village
370|Village of Chefornak
371|Chevak Native Village
372|Chickaloon Native Village
373|Native Village of Chignik
374|Native Village of Chignik Lagoon
375|Chignik Lake Village
376|Chilkat Indian Village (Klukwan)
377|Chilkoot Indian Association (Haines)
378|Chinik Eskimo Community (Golovin)
379|Native Village of Chistochina
380|Native Village of Chitina
381|Native Village of Chuathbaluk (Russian Mission, Ku
382|Chuloonawick Native Village
383|Circle Native Community
384|Village of Clark's Point
385|Native Village of Council
386|Craig Community Association
387|Village of Crooked Creek
388|Curyung Tribal Council (formerly Native Village of
389|Native Village of Deering
390|Native Village of Diomede (aka Inalik)
391|Village of Dot Lake
392|Douglas Indian Association
393|Native Village of Eagle
394|Native Village of Eek
395|Egegik Village
396|Eklutna Native Village
397|Native Village of Ekuk
398|Ekwok Village
399|Native Village of Elim
400|Emmonak Village
401|Evansville Village (aka Bettles Field)
402|Native Village of Eyak (Cordova)
403|Native Village of False Pass
404|Native Village of Fort Yukon
405|Native Village of Gakona
406|Galena Village (aka Louden Village)
407|Native Village of Gambell
408|Native Village of Georgetown
409|Native Village of Goodnews Bay
410|Organized Village of Grayling (aka Holikachuk)
411|Gulkana Village
412|Native Village of Hamilton
413|Healy Lake Village
414|Holy Cross Village
415|Hoonah Indian Association
416|Native Village of Hooper Bay
417|Hughes Village
418|Huslia Village
419|Hydaburg Cooperative Association
420|Igiugig Village
421|Village of Iliamna
422|Inupiat Community of the Arctic Slope
423|Iqurmuit Traditional Council (formerly Native Vill
424|Ivanoff Bay Village
425|Kaguyak Village
426|Organized Village of Kake
427|Kaktovik Village (aka Barter Island)
428|Village of Kalskag
429|Village of Kaltag
430|Native Village of Kanatak
431|Native Village of Karluk
432|Organized Village of Kasaan
433|Native Village of Kasigluk
434|Kenaitze Indian Tribe
435|Ketchikan Indian Corporation
436|Native Village of Kiana
437|King Island Native Community
438|King Salmon Tribe
439|Native Village of Kipnuk
440|Native Village of Kivalina
441|Klawock Cooperative Association
442|Native Village of Kluti Kaah (aka Copper Center)
443|Knik Tribe
444|Native Village of Kobuk
445|Kokhanok Village
446|Native Village of Kongiganak
447|Village of Kotlik
448|Native Village of Kotzebue
449|Native Village of Koyuk
450|Koyukuk Native Village
451|Organized Village of Kwethluk
452|Native Village of Kwigillingok
453|Native Village of Kwinhagak (aka Quinhagak)
454|Native Village of Larsen Bay
455|Levelock Village
456|Lesnoi Village (aka Woody Island)
457|Lime Village
458|Village of Lower Kalskag
459|Manley Hot Springs Village
460|Manokotak Village
461|Native Village of Marshall (aka Fortuna Ledge)
462|Native Village of Mary's Igloo
463|McGrath Native Village
464|Native Village of Mekoryuk
465|Mentasta Traditional Council
466|Metlakatla Indian Community, Annette Island Reserv
467|Native Village of Minto
468|Naknek Native Village
469|Native Village of Nanwalek (aka English Bay)
470|Native Village of Napaimute
471|Native Village of Napakiak
472|Native Village of Napaskiak
473|Native Village of Nelson Lagoon
474|Nenana Native Association
475|New Koliganek Village Council (formerly Koliganek
476|New Stuyahok Village
477|Newhalen Village
478|Newtok Village
479|Native Village of Nightmute
480|Nikolai Village
481|Native Village of Nikolski
482|Ninilchik Village
483|Native Village of Noatak
484|Nome Eskimo Community
485|Nondalton Village
486|Noorvik Native Community
487|Northway Village
488|Native Village of Nuiqsut (aka Nooiksut)
489|Nulato Village
490|Nunakauyarmiut Tribe (formerly Native Village of T
491|Native Village of Nunapitchuk
492|Village of Ohogamiut
493|Village of Old Harbor
494|Orutsararmuit Native Village (aka Bethel)
495|Oscarville Traditional Village
496|Native Village of Ouzinkie
497|Native Village of Paimiut
498|Pauloff Harbor Village
499|Pedro Bay Village
500|Native Village of Perryville
501|Petersburg Indian Association
502|Native Village of Pilot Point
503|Pilot Station Traditional Village
504|Native Village of Pitka's Point
505|Platinum Traditional Village
506|Native Village of Point Hope
507|Native Village of Point Lay
508|Native Village of Port Graham
509|Native Village of Port Heiden
510|Native Village of Port Lions
511|Portage Creek Village (aka Ohgsenakale)
512|Pribilof Islands Aleut Communities of St. Paul & S
513|Qagan Tayagungin Tribe of Sand Point Village
514|Qawalangin Tribe of Unalaska
515|Rampart Village
516|Village of Red Devil
517|Native Village of Ruby
518|Saint George Island(See Pribilof Islands Aleut Com
519|Native Village of Saint Michael
520|Saint Paul Island (See Pribilof Islands Aleut Comm
521|Village of Salamatoff
522|Native Village of Savoonga
523|Organized Village of Saxman
524|Native Village of Scammon Bay
525|Native Village of Selawik
526|Seldovia Village Tribe
527|Shageluk Native Village
528|Native Village of Shaktoolik
529|Native Village of Sheldon's Point
530|Native Village of Shishmaref
531|Shoonaq Tribe of Kodiak
532|Native Village of Shungnak
533|Sitka Tribe of Alaska
534|Skagway Village
535|Village of Sleetmute
536|Village of Solomon
537|South Naknek Village
538|Stebbins Community Association
539|Native Village of Stevens
540|Village of Stony River
541|Takotna Village
542|Native Village of Tanacross
543|Native Village of Tanana
544|Native Village of Tatitlek
545|Native Village of Tazlina
546|Telida Village
547|Native Village of Teller
548|Native Village of Tetlin
549|Central Council of the Tlingit and Haida Indian Tb
550|Traditional Village of Togiak
551|Tuluksak Native Community
552|Native Village of Tuntutuliak
553|Native Village of Tununak
554|Twin Hills Village
555|Native Village of Tyonek
556|Ugashik Village
557|Umkumiute Native Village
558|Native Village of Unalakleet
559|Native Village of Unga
560|Village of Venetie (See Native Village of Venetie
561|Native Village of Venetie Tribal Government (Arcti
562|Village of Wainwright
563|Native Village of Wales
564|Native Village of White Mountain
565|Wrangell Cooperative Association
566|Yakutat Tlingit Tribe
1|Absentee-Shawnee Tribe of Indians of Oklahoma
10|Assiniboine and Sioux Tribes of the Fort Peck Indi
100|Havasupai Tribe of the Havasupai Reservation, Ariz
101|Ho-Chunk Nation of Wisconsin (formerly known as th
102|Hoh Indian Tribe of the Hoh Indian Reservation, Wa
103|Hoopa Valley Tribe, California
104|Hopi Tribe of Arizona
105|Hopland Band of Pomo Indians of the Hopland Ranche
106|Houlton Band of Maliseet Indians of Maine
107|Hualapai Indian Tribe of the Hualapai Indian Reser
108|Huron Potawatomi, Inc., Michigan
109|Inaja Band of Diegueno Mission Indians of the Inaj
11|Augustine Band of Cahuilla Mission Indians of the
110|Ione Band of Miwok Indians of California
111|Iowa Tribe of Kansas and Nebraska
112|Iowa Tribe of Oklahoma
113|Jackson Rancheria of Me-Wuk Indians of California
114|Jamestown S'Klallam Tribe of Washington
115|Jamul Indian Village of California
116|Jena Band of Choctaw Indians, Louisiana
117|Jicarilla Apache Tribe of the Jicarilla Apache Ind
118|Kaibab Band of Paiute Indians of the Kaibab Indian
119|Kalispel Indian Community of the Kalispel Reservat
12|Bad River Band of the Lake Superior Tribe of Chipp
120|Karuk Tribe of California
121|Kashia Band of Pomo Indians of the Stewarts Point
122|Kaw Nation, Oklahoma
123|Keweenaw Bay Indian Community of L'Anse and Ontona
124|Kialegee Tribal Town, Oklahoma
125|Kickapoo Tribe of Indians of the Kickapoo Reservat
126|Kickapoo Tribe of Oklahoma
127|Kickapoo Traditional Tribe of Texas
128|Kiowa Indian Tribe of Oklahoma
129|Klamath Indian Tribe of Oregon
13|Bay Mills Indian Community of the Sault Ste. Marie
130|Kootenai Tribe of Idaho
131|La Jolla Band of Luiseno Mission Indians of the La
132|La Posta Band of Diegueno Mission Indians of the L
133|Lac Courte Oreilles Band of Lake Superior Chippewa
134|Lac du Flambeau Band of Lake Superior Chippewa Ind
135|Lac Vieux Desert Band of Lake Superior Chippewa In
136|Las Vegas Tribe of Paiute Indians of the Las Vegas
137|Little River Band of Ottawa Indians of Michigan
138|Little Traverse Bay Bands of Odawa Indians of Mich
139|Lower Lake Rancheria, California
14|Bear River Band of the Rohnerville Rancheria, Cali
140|Los Coyotes Band of Cahuilla Mission Indians of th
141|Lovelock Paiute Tribe of the Lovelock Indian Colon
142|Lower Brule Sioux Tribe of the Lower Brule Reserva
143|Lower Elwha Tribal Community of the Lower Elwha Re
144|Lower Sioux Indian Community of Minnesota Mdewakan
145|Lummi Tribe of the Lummi Reservation, Washington
146|Lytton Rancheria of California
147|Makah Indian Tribe of the Makah Indian Reservation
148|Manchester Band of Pomo Indians of the Manchester-
149|Manzanita Band of Diegueno Mission Indians of the
15|Berry Creek Rancheria of Maidu Indians of Californ
150|Mashantucket Pequot Tribe of Connecticut
151|Match-e-be-nash-she-wish Band of Pottawatomi India
152|Mechoopda Indian Tribe of Chico Rancheria, Califor
153|Menominee Indian Tribe of Wisconsin
154|Mesa Grande Band of Diegueno Mission Indians of th
155|Mescalero Apache Tribe of the Mescalero Reservatio
156|Miami Tribe of Oklahoma
157|Miccosukee Tribe of Indians of Florida
158|Middletown Rancheria of Pomo Indians of California
159|Minnesota Chippewa Tribe, Minnesota (Six component
16|Big Lagoon Rancheria, California
160|Bois Forte Band (Nett Lake); Fond du Lac Band; Gra
161|Mississippi Band of Choctaw Indians, Mississippi
162|Moapa Band of Paiute Indians of the Moapa River In
163|Modoc Tribe of Oklahoma
164|Mohegan Indian Tribe of Connecticut
165|Mooretown Rancheria of Maidu Indians of California
166|Morongo Band of Cahuilla Mission Indians of the Mo
167|Muckleshoot Indian Tribe of the Muckleshoot Reserv
168|Muscogee (Creek) Nation, Oklahoma
169|Narragansett Indian Tribe of Rhode Island
17|Big Pine Band of Owens Valley Paiute Shoshone Indi
170|Navajo Nation, Arizona, New Mexico & Utah
171|Nez Perce Tribe of Idaho
172|Nisqually Indian Tribe of the Nisqually Reservatio
173|Nooksack Indian Tribe of Washington
174|Northern Cheyenne Tribe of the Northern Cheyenne I
175|Northfork Rancheria of Mono Indians of California
176|Northwestern Band of Shoshoni Nation of Utah (Wash
177|Oglala Sioux Tribe of the Pine Ridge Reservation,
178|Omaha Tribe of Nebraska
179|Oneida Nation of New York
18|Big Sandy Rancheria of Mono Indians of California
180|Oneida Tribe of Wisconsin
181|Onondaga Nation of New York
182|Osage Tribe, Oklahoma
183|Ottawa Tribe of Oklahoma
184|Otoe-Missouria Tribe of Indians, Oklahoma
185|Paiute Indian Tribe of Utah
186|Paiute-Shoshone Indians of the Bishop Community of
187|Paiute-Shoshone Tribe of the Fallon Reservation an
188|Paiute-Shoshone Indians of the Lone Pine Community
189|Pala Band of Luiseno Mission Indians of the Pala R
19|Big Valley Band of Pomo Indians of the Big Valley
190|Pascua Yaqui Tribe of Arizona
191|Paskenta Band of Nomlaki Indians of California
192|Passamaquoddy Tribe of Maine
193|Pauma Band of Luiseno Mission Indians of the Pauma
194|Pawnee Nation of Oklahoma
195|Pechanga Band of Luiseno Mission Indians of the Pe
196|Penobscot Tribe of Maine
197|Peoria Tribe of Indians of Oklahoma
198|Picayune Rancheria of Chukchansi Indians of Califo
199|Pinoleville Rancheria of Pomo Indians of Californi
2|Agua Caliente Band of Cahuilla Indians of the Agua
20|Blackfeet Tribe of the Blackfeet Indian Reservatio
200|Pit River Tribe, California (includes Big Bend, Lo
201|Poarch Band of Creek Indians of Alabama
202|Pokagon Band of Potawatomi Indians of Michigan
203|Ponca Tribe of Indians of Oklahoma
204|Ponca Tribe of Nebraska
205|Port Gamble Indian Community of the Port Gamble Re
206|Potter Valley Rancheria of Pomo Indians of Califor
207|Prairie Band of Potawatomi Indians, Kansas
208|Prairie Island Indian Community of Minnesota Mdewa
209|Pueblo of Acoma, New Mexico
21|Blue Lake Rancheria, California
210|Pueblo of Cochiti, New Mexico
211|Pueblo of Jemez, New Mexico
212|Pueblo of Isleta, New Mexico
213|Pueblo of Laguna, New Mexico
214|Pueblo of Nambe, New Mexico
215|Pueblo of Picuris, New Mexico
216|Pueblo of Pojoaque, New Mexico
217|Pueblo of San Felipe, New Mexico
218|Pueblo of San Juan, New Mexico
219|Pueblo of San Ildefonso, New Mexico
22|Bridgeport Paiute Indian Colony of California
220|Pueblo of Sandia, New Mexico
221|Pueblo of Santa Ana, New Mexico
222|Pueblo of Santa Clara, New Mexico
223|Pueblo of Santo Domingo, New Mexico
224|Pueblo of Taos, New Mexico
225|Pueblo of Tesuque, New Mexico
226|Pueblo of Zia, New Mexico
227|Puyallup Tribe of the Puyallup Reservation, Washin
228|Pyramid Lake Paiute Tribe of the Pyramid Lake Rese
229|Quapaw Tribe of Indians, Oklahoma
23|Buena Vista Rancheria of Me-Wuk Indians of Califor
230|Quartz Valley Indian Community of the Quartz Valle
231|Quechan Tribe of the Fort Yuma Indian Reservation,
232|Quileute Tribe of the Quileute Reservation, Washin
233|Quinault Tribe of the Quinault Reservation, Washin
234|Ramona Band or Village of Cahuilla Mission Indians
235|Red Cliff Band of Lake Superior Chippewa Indians o
236|Red Lake Band of Chippewa Indians of the Red Lake
237|Redding Rancheria, California
238|Redwood Valley Rancheria of Pomo Indians of Califo
239|Reno-Sparks Indian Colony, Nevada
24|Burns Paiute Tribe of the Burns Paiute Indian Colo
240|Resighini Rancheria, California (formerly known as
241|Rincon Band of Luiseno Mission Indians of the Rinc
242|Robinson Rancheria of Pomo Indians of California
243|Rosebud Sioux Tribe of the Rosebud Indian Reservat
244|Round Valley Indian Tribes of the Round Valley Res
245|Rumsey Indian Rancheria of Wintun Indians of Calif
246|Sac and Fox Tribe of the Mississippi in Iowa
247|Sac and Fox Nation of Missouri in Kansas and Nebra
248|Sac and Fox Nation, Oklahoma
249|Saginaw Chippewa Indian Tribe of Michigan, Isabell
25|Cabazon Band of Cahuilla Mission Indians of the Ca
250|Salt River Pima-Maricopa Indian Community of the S
251|Samish Indian Tribe, Washington
252|San Carlos Apache Tribe of the San Carlos Reservat
253|San Juan Southern Paiute Tribe of Arizona
254|San Manual Band of Serrano Mission Indians of the
255|San Pasqual Band of Diegueno Mission Indians of Ca
256|Santa Rosa Indian Community of the Santa Rosa Ranc
257|Santa Rosa Band of Cahuilla Mission Indians of the
258|Santa Ynez Band of Chumash Mission Indians of the
259|Santa Ysabel Band of Diegueno Mission Indians of t
26|Cachil DeHe Band of Wintun Indians of the Colusa I
260|Santee Sioux Tribe of the Santee Reservation of Ne
261|Sauk-Suiattle Indian Tribe of Washington
262|Sault Ste. Marie Tribe of Chippewa Indians of Mich
263|Scotts Valley Band of Pomo Indians of California
264|Seminole Nation of Oklahoma
265|Seminole Tribe of Florida, Dania, Big Cypress, Bri
266|Seneca Nation of New York
267|Seneca-Cayuga Tribe of Oklahoma
268|Shakopee Mdewakanton Sioux Community of Minnesota
269|Shawnee Tribe, Oklahoma
27|Caddo Indian Tribe of Oklahoma
270|Sherwood Valley Rancheria of Pomo Indians of Calif
271|Shingle Springs Band of Miwok Indians, Shingle Spr
272|Shoalwater Bay Tribe of the Shoalwater Bay Indian
273|Shoshone Tribe of the Wind River Reservation, Wyom
274|Shoshone-Bannock Tribes of the Fort Hall Reservati
275|Shoshone-Paiute Tribes of the Duck Valley Reservat
276|Sisseton-Wahpeton Sioux Tribe of the Lake Traverse
277|Skokomish Indian Tribe of the Skokomish Reservatio
278|Skull Valley Band of Goshute Indians of Utah
279|Smith River Rancheria, California
28|Cahuilla Band of Mission Indians of the Cahuilla R
280|Snoqualmie Tribe, Washington
281|Soboba Band of Luiseno Indians, California (former
282|Sokaogon Chippewa Community of the Mole Lake Band
283|Southern Ute Indian Tribe of the Southern Ute Rese
284|Spirit Lake Tribe, North Dakota (formerly known as
285|Spokane Tribe of the Spokane Reservation, Washingt
286|Squaxin Island Tribe of the Squaxin Island Reserva
287|St. Croix Chippewa Indians of Wisconsin, St. Croix
288|St. Regis Band of Mohawk Indians of New York
289|Standing Rock Sioux Tribe of North & South Dakota
29|Cahto Indian Tribe of the Laytonville Rancheria, C
290|Stockbridge-Munsee Community of Mohican Indians of
291|Stillaguamish Tribe of Washington
292|Summit Lake Paiute Tribe of Nevada
293|Suquamish Indian Tribe of the Port Madison Reserva
294|Susanville Indian Rancheria, California
295|Swinomish Indians of the Swinomish Reservation, Wa
296|Sycuan Band of Diegueno Mission Indians of Califor
297|Table Bluff Reservation - Wiyot Tribe, California
298|Table Mountain Rancheria of California
299|Te-Moak Tribe of Western Shoshone Indians of Nevad
3|Ak Chin Indian Community of the Maricopa (Ak Chin)
30|California Valley Miwok Tribe, California (formerl
300|Thlopthlocco Tribal Town, Oklahoma
301|Three Affiliated Tribes of the Fort Berthold Reser
302|Tohono O'odham Nation of Arizona
303|Tonawanda Band of Seneca Indians of New York
304|Tonkawa Tribe of Indians of Oklahoma
305|Tonto Apache Tribe of Arizona
306|Torres-Martinez Band of Cahuilla Mission Indians o
307|Tule River Indian Tribe of the Tule River Reservat
308|Tulalip Tribes of the Tulalip Reservation, Washing
309|Tunica-Biloxi Indian Tribe of Louisiana
31|Campo Band of Diegueno Mission Indians of the Camp
310|Tuolumne Band of Me-Wuk Indians of the Tuolumne Ra
311|Turtle Mountain Band of Chippewa Indians of North
312|Tuscarora Nation of New York
313|Twenty-Nine Palms Band of Mission Indians of Calif
314|United Auburn Indian Community of the Auburn Ranch
315|United Keetoowah Band of Cherokee Indians of Oklah
316|Upper Lake Band of Pomo Indians of Upper Lake Ranc
317|Upper Sioux Indian Community of the Upper Sioux Re
318|Upper Skagit Indian Tribe of Washington
319|Ute Indian Tribe of the Uintah & Ouray Reservation
32|Capitan Grande Band of Diegueno Mission Indians of
320|Ute Mountain Tribe of the Ute Mountain Reservation
321|Utu Utu Gwaitu Paiute Tribe of the Benton Paiute R
322|Walker River Paiute Tribe of the Walker River Rese
323|Wampanoag Tribe of Gay Head (Aquinnah) of Massachu
324|Washoe Tribe of Nevada & California (Carson Colony
325|White Mountain Apache Tribe of the Fort Apache Res
326|Wichita and Affiliated Tribes (Wichita, Keechi, Wa
327|Winnebago Tribe of Nebraska
328|Winnemucca Indian Colony of Nevada
329|Wyandotte Tribe of Oklahoma
33|Barona Group of Capitan Grande Band of Mission Ind
330|Yankton Sioux Tribe of South Dakota
331|Yavapai-Apache Nation of the Camp Verde Indian Res
332|Yavapai-Prescott Tribe of the Yavapai Reservation,
333|Yerington Paiute Tribe of the Yerington Colony & C
334|Yomba Shoshone Tribe of the Yomba Reservation, Nev
335|Ysleta Del Sur Pueblo of Texas
336|Yurok Tribe of the Yurok Reservation, California
337|Zuni Tribe of the Zuni Reservation, New Mexico
34|Viejas (Baron Long) Group of Capitan Grande Band o
35|Catawba Indian Nation (aka Catawba Tribe of South
36|Cayuga Nation of New York
37|Cedarville Rancheria, California
38|Chemehuevi Indian Tribe of the Chemehuevi Reservat
39|Cher-Ae Heights Indian Community of the Trinidad R
4|Alabama-Coushatta Tribes of Texas
40|Cherokee Nation, Oklahoma
41|Cheyenne-Arapaho Tribes of Oklahoma
42|Cheyenne River Sioux Tribe of the Cheyenne River
43|Chickasaw Nation, Oklahoma
44|Chicken Ranch Rancheria of Me-Wuk Indians of Calif
45|Chippewa-Cree Indians of the Rocky Boy's Reservati
46|Chitimacha Tribe of Louisiana
47|Choctaw Nation of Oklahoma
48|Citizen Potawatomi Nation, Oklahoma
49|Cloverdale Rancheria of Pomo Indians of California
5|Alabama-Quassarte Tribal Town, Oklahoma
50|Cocopah Tribe of Arizona
51|Coeur D'Alene Tribe of the Coeur D'Alene Reservati
52|Cold Springs Rancheria of Mono Indians of Californ
53|Colorado River Indian Tribes of the Colorado River
54|Comanche Indian Tribe, Oklahoma
55|Confederated Salish & Kootenai Tribes of the Flath
56|Confederated Tribes of the Chehalis Reservation, W
57|Confederated Tribes of the Colville Reservation, W
58|Confederated Tribes of the Coos, Lower Umpqua and
59|Confederated Tribes of the Goshute Reservation, Ne
6|Alturas Indian Rancheria, California
60|Confederated Tribes of the Grand Ronde Community o
61|Confederated Tribes of the Siletz Reservation, Ore
62|Confederated Tribes of the Umatilla Reservation, O
63|Confederated Tribes of the Warm Springs Reservatio
64|Confederated Tribes and Bands of the Yakama Indian
65|Coquille Tribe of Oregon
66|Cortina Indian Rancheria of Wintun Indians of Cali
67|Coushatta Tribe of Louisiana
68|Cow Creek Band of Umpqua Indians of Oregon
69|Coyote Valley Band of Pomo Indians of California
7|Apache Tribe of Oklahoma
70|Crow Tribe of Montana
71|Crow Creek Sioux Tribe of the Crow Creek Reservati
72|Cuyapaipe Community of Diegueno Mission Indians of
73|Death Valley Timbi-Sha Shoshone Band of California
74|Delaware Nation, Oklahoma (formerly Delaware Tribe
75|Delaware Tribe of Indians, Oklahoma
76|Dry Creek Rancheria of Pomo Indians of California
77|Duckwater Shoshone Tribe of the Duckwater Reservat
78|Eastern Band of Cherokee Indians of North Carolina
79|Eastern Shawnee Tribe of Oklahoma
8|Arapahoe Tribe of the Wind River Reservation, Wyom
80|Elem Indian Colony of Pomo Indians of the Sulphur
81|Elk Valley Rancheria, California
82|Ely Shoshone Tribe of Nevada
83|Enterprise Rancheria of Maidu Indians of Californi
84|Flandreau Santee Sioux Tribe of South Dakota
85|Forest County Potawatomi Community of Wisconsin Po
86|Fort Belknap Indian Community of the Fort Belknap
87|Fort Bidwell Indian Community of the Fort Bidwell
88|Fort Independence Indian Community of Paiute India
89|Fort McDermitt Paiute and Shoshone Tribes of the F
9|Aroostook Band of Micmac Indians of Maine
90|Fort McDowell Yavapai Nation, Arizona (formerly th
91|Fort Mojave Indian Tribe of Arizona, California
92|Fort Sill Apache Tribe of Oklahoma
93|Gila River Indian Community of the Gila River Indi
94|Grand Traverse Band of Ottawa & Chippewa Indians o
95|Graton Rancheria, California
96|Greenville Rancheria of Maidu Indians of Californi
97|Grindstone Indian Rancheria of Wintun-Wailaki Indi
98|Guidiville Rancheria of California
99|Hannahville Indian Community of Wisconsin Potawato

**Documentation**:

If the patient is a citizen of a tribal entity, we can track which entity here

---

**Name**: patient_zip_code

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The patient's zip code

---

**Name**: placer_clia

**Type**: ID_CLIA

**PII**: No

**HL7 Fields**

- [OBR-2-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.2.3)
- [ORC-2-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.2.3)
- [ORC-4-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.4.3)

**Cardinality**: [0..1]

**Documentation**:

The CLIA of the order placer

---

**Name**: placer_name

**Type**: TEXT

**PII**: No

**HL7 Fields**

- [OBR-2-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.2.2)
- [ORC-2-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.2.2)
- [ORC-4-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.4.2)

**Cardinality**: [0..1]

**Documentation**:

The name of the placer of the lab order

---

**Name**: placer_order_group_id

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

---

**Name**: placer_order_id

**Type**: ID

**PII**: No

**HL7 Fields**

- [OBR-2-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.2.1)
- [ORC-2-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.2.1)

**Cardinality**: [0..1]

**Documentation**:

The ID number of the lab order from the placer

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

**Name**: previous_message_id

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

pointer/link to the unique id of a previously submitted result.  Usually blank. Or, if an item modifies/corrects a prior item, this field holds the message_id of the prior item.

---

**Name**: prime_patient_id

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

---

**Name**: prime_patient_id_assigner

**Type**: HD

**PII**: No

**Cardinality**: [0..1]

---

**Name**: processing_mode_code

**Type**: CODE

**PII**: No

**Default Value**: P

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
D|Debugging
P|Production
T|Training

**Documentation**:

P, D, or T for Production, Debugging, or Training

---

**Name**: reason_for_study

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: receiving_application

**Type**: HD

**PII**: No

**Default Value**: LA-ELR

**Cardinality**: [0..1]

**Documentation**:

The receiving application for the message (specified by the receiver)

---

**Name**: receiving_facility

**Type**: HD

**PII**: No

**Default Value**: LADOH

**Cardinality**: [0..1]

**Documentation**:

The receiving facility for the message (specified by the receiver)

---

**Name**: reference_range

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The reference range of the lab result, such as “Negative” or “Normal”. For IgG, IgM and CT results that provide a value you MUST fill out this filed.

---

**Name**: reporting_facility

**Type**: HD

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The reporting facility for the message, as specified by the receiver. This is typically used if PRIME is the
aggregator


---

**Name**: reporting_facility_clia

**Type**: ID_CLIA

**PII**: No

**HL7 Fields**

- [PID-3-4-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/PID.3.4.2)
- [PID-3-6-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/PID.3.6.2)
- [SPM-2-1-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.2.1.3)
- [SPM-2-2-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.2.2.3)

**Cardinality**: [0..1]

**Documentation**:

The reporting facility's CLIA

---

**Name**: reporting_facility_name

**Type**: TEXT

**PII**: No

**Default Value**: CDC Prime - Atlanta

**HL7 Fields**

- [MSH-4-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/MSH.4.1)
- [PID-3-4-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/PID.3.4.1)

**Cardinality**: [0..1]

**Documentation**:

The reporting facility's name

---

**Name**: resident_congregate_setting

**Type**: CODE

**PII**: No

**LOINC Code**: 95421-4

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|Yes
N|No
UNK|Unknown

**Documentation**:

Does the patient reside in a congregate care setting?

---

**Name**: result_format

**Type**: TEXT

**PII**: No

**Default Value**: CWE

**Cardinality**: [0..1]

---

**Name**: sender_id

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

ID name of org that is sending this data to ReportStream.  Suitable for provenance or chain of custody tracking.  Not to be confused with sending_application, in which ReportStream acts as the 'sender' to the downstream jurisdiction.

---

**Name**: sending_application

**Type**: HD

**PII**: No

**Default Value**: CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO

**Cardinality**: [0..1]

**Documentation**:

The name and OID for the application sending information to the receivers


---

**Name**: site_of_care

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The type of facility providing care (Hospital, Nursing Home, etc.).

---

**Name**: specimen_collection_date_time

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

**Name**: specimen_collection_method

**Type**: CODE

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
ANP|Plates, Anaerobic
BAP|Plates, Blood Agar
BCAE|Blood Culture, Aerobic Bottle
BCAN|Blood Culture, Anaerobic Bottle
BCPD|Blood Culture, Pediatric Bottle
BIO|Biopsy
CAP|Capillary Specimen
CATH|Catheterized
CVP|Line, CVP
EPLA|Environmental, Plate
ESWA|Environmental, Swab
FNA|Aspiration, Fine Needle
KOFFP|Plate, Cough
LNA|Line, Arterial
LNV|Line, Venous
MARTL|Martin-Lewis Agar
ML11|Mod. Martin-Lewis Agar
MLP|Plate, Martin-Lewis
NYP|Plate, New York City
PACE|Pace, Gen-Probe
PIN|Pinworm Prep
PNA|Aterial puncture
PRIME|Pump Prime
PUMP|Pump Specimen
QC5|Quality Control For Micro
SCLP|Scalp, Fetal Vein
SCRAPS|Scrapings
SHA|Shaving
SWA|Swab
SWD|Swab, Dacron tipped
TMAN|Transport Media, Anaerobic
TMCH|Transport Media, Chalamydia
TMM4|Transport Media, M4
TMMY|Transport Media, Mycoplasma
TMOT|Transport Media
TMP|Plate, Thayer-Martin
TMPV|Transport Media, PVA
TMSC|Transport Media, Stool Culture
TMUP|Transport Media, Ureaplasma
TMVI|Transport Media, Viral
VENIP|Venipuncture
WOOD|Swab, Wooden Shaft

---

**Name**: specimen_collection_site

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]


**Reference URL**:
[https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.10](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.10) 

---

**Name**: specimen_description

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]


**Reference URL**:
[https://hl7-definition.caristix.com/v2/HL7v2.8/Fields/SPM.14](https://hl7-definition.caristix.com/v2/HL7v2.8/Fields/SPM.14) 

---

**Name**: specimen_id

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

**Name**: specimen_role

**Type**: CODE

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
B|Blind sample
E|Electronic QC
F|Filer
G|Group
L|Pool
O|Operator proficiency
P|Patient
Q|Control specimen
R|Replicate
V|Verifying collaborator

---

**Name**: specimen_source_site_code

**Type**: CODE

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
119297000|Blood specimen (specimen)
71836000|Nasopharyngeal structure (body structure)
45206002|Nasal structure (body structure)
53342003|Internal nose structure (body structure)

**Documentation**:

Refers back to the specimen source site, which is then encoded into the SPM-8 segment

---

**Name**: specimen_type

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

**Name**: symptomatic_for_disease

**Type**: CODE

**PII**: No

**LOINC Code**: 95419-8

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|Yes
N|No
UNK|Unknown

**Documentation**:

Is the patient symptomatic?

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

**Name**: test_kit_name_id

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 

**Table**: LIVD-SARS-CoV-2-2021-04-28

**Table Column**: Testkit Name ID

**Documentation**:

Follows guidence for OBX-17 as defined in the HL7 Confluence page

---

**Name**: test_kit_name_id_cwe_version

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-2021-04-28

**Table Column**: LOINC Version ID

**Documentation**:

Follows guidance for OBX-17-7 where the version of the CWE field is passed along

---

**Name**: test_kit_name_id_type

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 

**Table**: LIVD-SARS-CoV-2-2021-04-28

**Table Column**: Testkit Name ID Type

---

**Name**: test_method_description

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

A text field that allows the lab to provide more information aboout the test method

---

**Name**: test_performed_code

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-2021-04-28

**Table Column**: Test Performed LOINC Code

**Documentation**:

The LOINC code of the test performed. This is a standardized coded value describing the test

---

**Name**: test_performed_name

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-2021-04-28

**Table Column**: Test Performed LOINC Long Name

**Documentation**:

The LOINC description of the test performed as related to the LOINC code.

---

**Name**: test_performed_system

**Type**: TEXT

**PII**: No

**Default Value**: LOINC

**Cardinality**: [0..1]

---

**Name**: test_performed_system_abbr

**Type**: TEXT

**PII**: No

**Default Value**: LN

**Cardinality**: [0..1]

---

**Name**: test_performed_system_version

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-2021-04-28

**Table Column**: LOINC Version ID

---

**Name**: test_result

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

**Name**: test_result_date

**Type**: DATETIME

**PII**: No

**Cardinality**: [0..1]

---

**Name**: test_result_report_date

**Type**: DATETIME

**PII**: No

**Cardinality**: [0..1]

---

**Name**: test_result_status

**Type**: CODE

**PII**: No

**HL7 Fields**

- [OBR-25-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.25.1)
- [OBX-11-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBX.11.1)

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
A|Some, but not all, results available
C|Corrected, final
F|Final results
I|No results available; specimen received, procedure incomplete
M|Corrected, not final
N|Procedure completed, results pending
O|Order received; specimen not yet received
P|Preliminary
R|Results stored; not yet verified
S|No results available; procedure scheduled, but not done
X|No results available; Order canceled
Y|No order on record for this test
Z|No record of this patient

**Documentation**:

The test result status, which is different from the test result itself. Per the valueset, this indicates if
the test result is in some intermediate status, is a correction, or is the final result.


---

**Name**: test_result_sub_id

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

---

**Name**: test_result_units

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The units the test result is measured in.

---

**Name**: testing_lab_accession_number

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The accession number of the specimen collected

---

**Name**: testing_lab_city

**Type**: CITY

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The city of the testing lab

---

**Name**: testing_lab_clia

**Type**: ID_CLIA

**PII**: No

**HL7 Fields**

- [OBR-2-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.2.3)
- [OBR-3-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.3.3)
- [OBX-15-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBX.15.1)
- [OBX-23-10](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBX.23.10)
- [ORC-2-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.2.3)
- [ORC-3-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.3.3)

**Cardinality**: [1..1]

**Documentation**:

CLIA Number from the laboratory that sends the message to DOH

An example of the ID is 03D2159846


---

**Name**: testing_lab_country

**Type**: TEXT

**PII**: No

**Default Value**: USA

**Cardinality**: [0..1]

**Documentation**:

The country for the testing lab. Currently defaults to USA

---

**Name**: testing_lab_county

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: County

**Documentation**:

The text value for the testing lab county. This is used to do the lookup in the FIPS dataset.

---

**Name**: testing_lab_county_code

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: FIPS

**Documentation**:

The county code for the testing lab from the FIPS dataset. This is the standard code used in ELR reporting.


---

**Name**: testing_lab_id

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

Typically this will be the same as the `testing_lab_clia`, but potentially could not be.

---

**Name**: testing_lab_id_assigner

**Type**: HD

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

This is the assigner of the CLIA for the testing lab. If the testing lab has a CLIA, this field will be filled in.

---

**Name**: testing_lab_name

**Type**: TEXT

**PII**: No

**HL7 Fields**

- [OBR-2-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.2.2)
- [OBR-3-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.3.2)
- [OBX-23-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBX.23.1)
- [ORC-2-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.2.2)
- [ORC-3-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.3.2)
- [PID-3-4-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/PID.3.4.1)

**Cardinality**: [0..1]

**Documentation**:

The name of the laboratory which performed the test, can be the same as the sending facility name

---

**Name**: testing_lab_phone_number

**Type**: TELEPHONE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The phone number of the testing lab

---

**Name**: testing_lab_specimen_id

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The specimen-id from the testing lab

---

**Name**: testing_lab_specimen_received_datetime

**Type**: DATETIME

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The received date time for the specimen. This field is very important to many states for their HL7,
but for most of our senders, the received date time is the same as the collected date time. Unfortunately,
setting them to the same time breaks many validation rules. Most ELR systems apparently look for them to
be offset, so this field takes the `specimen_collection_date_time` field and offsets it by a small amount.


---

**Name**: testing_lab_state

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The state for the testing lab

---

**Name**: testing_lab_street

**Type**: STREET

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The street address for the testing lab

---

**Name**: testing_lab_street2

**Type**: STREET_OR_BLANK

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

Street 2 field for the testing lab

---

**Name**: testing_lab_zip_code

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The postal code for the testing lab

---

**Name**: value_type

**Type**: CODE

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
AD|Address
CE|Coded Entry
CF|Coded Element With Formatted Values
CK|Composite ID With Check Digit
CN|Composite ID And Name
CP|Composite Price
CX|Extended Composite ID With Check Digit
DT|Date
ED|Encapsulated Data
FT|Formatted Text (Display)
MO|Money
NM|Numeric
PN|Person Name
RP|Reference Pointer
SN|Structured Numeric
ST|String Data
TM|Time
TN|Telephone Number
TS|Time Stamp (Date & Time)
TX|Text Data (Display)
XAD|Extended Address
XCN|Extended Composite Name and Number For Persons
XON|Extended Composite Name and Number For Organizations
XPN|Extended Person Name
XTN|Extended Telecommunications Number

---
