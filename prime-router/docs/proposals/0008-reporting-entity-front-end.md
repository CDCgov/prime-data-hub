### Proposal 0008 - Reporting Entity Front-end
## High Level Overview

We propose to build a CSV upload feature for submitters of COVID-19 test results as part of the existing ReportStream
front end. We propose to use the concept of permissions in Okta to differentiate entities who are receivers (public
health departments), senders (submitting entities), and entities who are both senders and receivers (i.e. Public health
departments that might need to send records to a different state). We propose to use the existing ReportStream codebase,
leveraging the existing tech stack including USDWS, Javascript, Eleventy, etc. We propose that based on a user’s
permissions in Okta, it will determine which screens in the front end the user can see and access. In the case of the
proposed CSV upload feature, a user with a permission of “sender” would see the CSV upload page on the ReportStream
front end. The initial use-case for the CSV upload feature will be to meet an immediate need for a constrained list of
senders who are uploading data into an unsecured Louisiana Department of Health web portal. The first user will be
Greenlight Urgent Care who is able to systematically generate a CSV file from their Experity Electronic Medical Record
System but has no way to get that data to ReportStream today.

## Background

As technical assistance teams are scaling up and out across the nation, the ReportStream’s web portal would be a good
place for the assistance teams to send smaller, less tech-savvy submitting entities that need to get their data to the
Public Health Department in a quick-and-(semi)-painless manner. One way to achieve this is to utilize ReportStream’s
built-in features such as:

  1. Authentication
  2. ReportStream API integration 

to create a form that accepts comma separated value (.CSV) files that can be
sent to the ReportSteam API for the submitting entity’s respective Public Health Department to consume. This paper
outlines a proposal on how this CSV Upload Submission feature would work and would be implemented.

## Goals

Create a new page on the ReportStream front-end which houses a simple web portal form that accepts CSV files, formatted
in a constrained set of data formats for which ReportStream has sender schemas configured.

This will involve segmenting users in the current authentication service (Okta) to allow for certain actions to be performed when given
permissions to. For instance, if a submitting entity is also a State Public Health Department, they will have
permissions to both see Daily Data (which is currently what they can see now), plus, they can have the permission to
also Upload CSVs in their desired format. However, a submitting entity such as a hospital or school does not need the
Daily Data screen, and so their permissions will solely be the Upload CSV feature.

## Proposal

We propose that to create a form in the ReportStream web portal that accepts CSV files for submitting entities to pass
their data to their respective State Health Department.

## Initial Implementation Plan

This will involve 3 steps:
1. **Authentication/Authorization:** The current authentication service will have to be set up to send roles or permissions that
pertain to the user/organization, and the web portal client, as well as the API, will need to consume these permissions
and behave accordingly.

    1. The permission that comes through can be called something like `sender`.

    2. If it is necessary, also create a `receiver`
permission to prevent organizations that do not receive reports from getting any reports. (This may not be necessary, as
they would simply get a blank screen when they enter ReportStream.)

    3. We will need to add a data element to the user scope returned from the authentication service that informs what format
the sender is using to send data to ReportStream, i.e: `experity-emr-covid-19`. 

2. **Creating the upload form in the web
portal:** Creating the form is the easy part! Segmenting the users and having the client’s logic behave correctly may be
involved with how ReportStream behaves today, but it is not expected to be too difficult. 

    1. **Navigation:**
     
        1. If the user/organization has the `sender` permission, present a new link in the navigation to proceed to the CSV Upload feature web portal page
     
        2. If we have both of the `sender` and `receiver`
    permissions, we can make the experience a little easier by putting the user straight to the “sender” page if they do not
    have `receiver` permission
       
        3. **Sample user navigation flow and permission plan:** Users are already segmented by "permissions" in Okta in ReportStream by the "DH" group that they are a part of. We can add a "Sender" group and add users to that group to enable "sending" capabilities. This image is a proposed workflow as to how the navigation will work with these different senarios:
           ![Sample navigation flow](https://imgur.com/RDvfqwd.png)

    2. **Create the upload form** in HTML/CSS/JavaScript to send to the ReportStream API

        1. Use sender and receiver data elements from the current authentication service to send the CSV file to ReportStream’s API
    

3. **ReportStream Front-end**
    1. **React**: Make new React form component underneath `pages/sender/tools/upload`
    2. Form component
    3. Success component
    4. Error component
    5. Warning component
    6. Write tests

4. **ReportStream Settings**
    1. Add `sender` organizations to the database settings list

        1. These need to match what is used in the current authentication service - same naming conventions, etc. 

        2. Create “schema”s for ReportStream to consume to create the report
for Public Health Departments

5. **ReportStream API**
   
    1. /sender/reports New endpoint for uploading report in specified format that handles Okta claims tokens
    2. **Future** 
       
       1. /sender/history  endpoint for a history of sent reports by entity  
       1. capture user information of who uploaded the report  

## First Flight for MVP

For our “first flight”, we plan to use a single submitting entity in the state of Louisiana as our trial user.

Our first submitting entity will be Greenlight Urgent Care (GUC) of Louisiana.

The process for getting them started will be:
1. Send a direct link from Okta to their main point of contact to get them an account with the correct permissions and
receiver information to access the CSV upload functionality 
   
2. Create a schema based on the canned COVID-19 test result
report output from the Experity Electronic Medical Record platform, which GUC uses. Test, rinse, repeat until correct 
   
3. Do a test run of a small, sans-PHI/PII dataset on ReportStream’s staging server 
   
4. Work out/tweak code/kinks as necessary
   
5. Deploy CSV upload feature to production for use by Greenlight Urgent Care
