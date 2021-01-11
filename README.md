## Test change for PR

![Prime Data Hub](https://github.com/CDCgov/prime-data-hub/workflows/Prime%20Data%20Hub/badge.svg?branch=production)

**General disclaimer** This repository was created for use by CDC programs to collaborate on public health related projects in support of the [CDC mission](https://www.cdc.gov/about/organization/mission.htm).  GitHub is not hosted by the CDC, but is a third party website used by CDC and its partners to share information and collaborate on software. CDC use of GitHub does not imply an endorsement of any one particular service, product, or enterprise. 

## Overview

The PRIME data hub project is the part of the Pandemic Ready Interoperable Modernization Effort that works with state and local public health departments. The project is a joint effort between the CDC and USDS. Currently, we are focusing on the problem of delivering COVID-19 test data to public health departments. Later, we will work on other tools to analyze and explore this data and different types of health data.

Other PRIME repositories include

- [PRIME-Central](https://github.com/CDCgov/prime-central): a place we keep common files and documents
- [PRIME-Data-Input-Client](https://github.com/CDCgov/prime-data-input-client): The POC COVID-19 test data input application that will use the data router


**Problem Scope**

Public health departments (PHDs) rely on accurate, timely data to fulfill day to day-operations to make long-term strategic decisions. Between the time when they receive report files from reporting entities like laboratories and point of care centers, to taking action on this data, there are many barriers and challenges. Data Hub aims to provide the infrastructure and tools to address these challenges.

Our vision is to help public health systems make faster, more effective decisions. We want to both improve the speed of receiving data to action, as well as increase the quality and effectiveness of actions they can take. 

**Target users**
* Senders of data
  * Healthcare institutions that generate data (e.g. reports) that needs to be reported to public health departments
* PHDs
  * Epidemiologists who rely on health data to take regular actions
  * Senior stakeholders who make executive decisions using aggregate health data
  * IT teams who have to support epidemiologists and external stakeholders integrating with the PHD


**Measuring success**

Time to action
* Metric: Time from Report Generation to Case Action at the PHD
* This funnel will likely involve several steps, e.g.
  * Report generated to Received
  * Received to cleaned
  * Cleaned file to action
* Success in the long term could look like: A data platform and set workflow tools that PHDs use day-to-day to manage data related tasks

Quality of actions
* Metric: New actions/capabilities (e.g. outbreak forecasting)
* This is more of a forward looking, “Public Health 3.0” type of outcome. Once we understand public health planning/reporting use cases better, we should refine this further
*  Success in the long term could look like: An analytics and reporting platform that leverages the latest in software and data science to help public health departments prevent and thrive during public health challenges

**Milestone 1: Router**

The first milestone Routing project will provide the infrastructure to send data generated from the Data Input App. To achieve this, we will need to build the ability to send the data and to reformat the data to adhere to unique needs of each PHD.

*Problems we are solving*
* Data Input Team - They need a way to send the data to PHDs.
* Point of care data sender - Currently it is difficult/impossible for them to send data electronically to PHDs. This means they are sending it in time consuming and error prone ways (entering into web forms, uploading CSVs)
* PHD IT teams - Currently it is time consuming to take time to integrate with each new data sender.

Sub-Milestones to achieve an user facing experience
* Fake data
* Test data + Auth
* MVP: Real data + Auth

*Components*
Sending and transforming
* Story
  * "Once data is collected by the Data Input app, the Routing app will send it to the correct PHD in the correct format."
* Features/Tasks
  * App can figure out where to send the data the proper jurisdiction
  * App can transform the data to send the fields required by the target PHD, in the required format [TBD - is it CSV, is it ELR?]

Authentication and security
* Story
  * "The app securely connects with each PHD. Along the way, data is protected."
* Features/Tasks
  * App is able to save authentication information for each individual PHD
  * App is able to authenticate with different types of configurations
  * App has proper restrictions on what data is sent/accessible

Integration with Pilot customers
* Story
  * "For our first pilot customers, we know the requirements to connect with each jurisdiction and are integrated"
* Features/Tasks
  * What are the data fields for each PHD
  * What are the logistics/timeline for connecting with each of them

**Milestone 2: Reporting and Monitoring**

The second milestone is to make the data more useful to the users of the Hub - the senders and the PHD

*Components*
Monitoring
* Story
  * "As a human interacting with the app, I can get helpful updates about the status of data I expect to be sent"
* Features/Tasks
  * Sender is able to check receiving status for a given report
  * Sender receives weekly summary of reports sent and summarized demographics

Reporting basics
* Story
  * “As an analyst at the PHD, I can get timely, regular updates about the reports sent through the Router”
* Features/Tasks
  * PHD receives weekly summary of reports sent and summarized demographics

**Milestone 3+ Onward: Scaling**

*Components*
* Configurability
  * "As USDS/CDC works with more PHDs, we can easily add more jurisdictions to the Routing app"
* Data flexibility
  * The router is able to receive additional data types, e.g. hospital case data



## Public Domain Standard Notice
This repository constitutes a work of the United States Government and is not
subject to domestic copyright protection under 17 USC § 105. This repository is in
the public domain within the United States, and copyright and related rights in
the work worldwide are waived through the [CC0 1.0 Universal public domain dedication](https://creativecommons.org/publicdomain/zero/1.0/).
All contributions to this repository will be released under the CC0 dedication. By
submitting a pull request you are agreeing to comply with this waiver of
copyright interest.

## License Standard Notice
The repository utilizes code licensed under the terms of the Apache Software
License and therefore is licensed under ASL v2 or later.

This source code in this repository is free: you can redistribute it and/or modify it under
the terms of the Apache Software License version 2, or (at your option) any
later version.

This source code in this repository is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE. See the Apache Software License for more details.

You should have received a copy of the Apache Software License along with this
program. If not, see http://www.apache.org/licenses/LICENSE-2.0.html

The source code forked from other open source projects will inherit its license.

## Privacy Standard Notice
This repository contains only non-sensitive, publicly available data and
information. All material and community participation is covered by the
[Disclaimer](https://github.com/CDCgov/template/blob/master/DISCLAIMER.md)
and [Code of Conduct](https://github.com/CDCgov/template/blob/master/code-of-conduct.md).
For more information about CDC's privacy policy, please visit [http://www.cdc.gov/other/privacy.html](https://www.cdc.gov/other/privacy.html).

## Contributing Standard Notice
Anyone is encouraged to contribute to the repository by [forking](https://help.github.com/articles/fork-a-repo)
and submitting a pull request. (If you are new to GitHub, you might start with a
[basic tutorial](https://help.github.com/articles/set-up-git).) By contributing
to this project, you grant a world-wide, royalty-free, perpetual, irrevocable,
non-exclusive, transferable license to all users under the terms of the
[Apache Software License v2](http://www.apache.org/licenses/LICENSE-2.0.html) or
later.

All comments, messages, pull requests, and other submissions received through
CDC including this GitHub page may be subject to applicable federal law, including but not limited to the Federal Records Act, and may be archived. Learn more at [http://www.cdc.gov/other/privacy.html](http://www.cdc.gov/other/privacy.html).

## Records Management Standard Notice
This repository is not a source of government records, but is a copy to increase
collaboration and collaborative potential. All government records will be
published through the [CDC web site](http://www.cdc.gov).

## Related documents

* [Open Practices](open_practices.md)
* [Rules of Behavior](rules_of_behavior.md)
* [Thanks and Acknowledgements](thanks.md)
* [Disclaimer](DISCLAIMER.md)
* [Contribution Notice](CONTRIBUTING.md)
* [Code of Conduct](code-of-conduct.md)

## Additional Standard Notices
Please refer to [CDC's Template Repository](https://github.com/CDCgov/template)
for more information about [contributing to this repository](https://github.com/CDCgov/template/blob/master/CONTRIBUTING.md),
[public domain notices and disclaimers](https://github.com/CDCgov/template/blob/master/DISCLAIMER.md),
and [code of conduct](https://github.com/CDCgov/template/blob/master/code-of-conduct.md).
