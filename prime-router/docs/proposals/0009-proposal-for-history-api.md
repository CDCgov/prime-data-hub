# History API Proposal

## Context and Motivation
As the product feature set continues to grow along with the sending and receiving organizations being onboarded, more robust tracking and reporting is needed to help provide insight into how reports were processed, routed and delivered. This proposal will define end points to provide information to developers and organizations for testing, monitoring and reporting.      

## Goals
The goals of this propsal:
* Define end points for report information and details from an organization's perspective (sender/receiver)
* Provide report routing and origination details
* Provide information on failed submissions and delivery failures

## Prerequisites
* Issue [#1437](https://github.com/CDCgov/prime-reportstream/issues/1437) which adds a new JSONB column to the action table to capture the full verbose response to an /api/reports POST. This will contain error/warning messages, destinations, item specific routing which can be reported on via reportId and potentially itemId.
* Add a HttpStatus column to the action table specifically for `receive` actions to make it easier to determine pass/fail submissions. The new JSONB field will likely need to have the 'CREATED' and or 'BAD_REQUEST' prefix removed (which will be added as an http_status column in the action table)
* PR [#1549](https://github.com/CDCgov/prime-reportstream/pull/1549) which adds report routing by report index/tracking Id. This information will be used for data in the end points.
* Not a prerequisite, but useful to implement is proposal [#1285](https://github.com/CDCgov/prime-reportstream/issues/1285) which attempts to normalize messaging and summarize/aggregate to reduce the payload size. This may help fit the full response into the 2048 char `action_result` column with the full verbose response in the `action_result_json` column which will be used for end point querying.

## Database Schema Updates
* add action_response to action as JsonB column
* extract and add sending_org, sending_org_client, IP, content_length to the action table
* link the metadata to the item lineage. Store as JsonB in lineage itself? Add topic to item lineage.

## Proposed End Points
All endpoints will use the same security model as the `/settings/*` end points. Consideration should be given to check the user's role / Okta claims to ensure that they do have access to the organization to which the report is linked. Admins will be able to see all reports. Organization admins can see only their reports.

### Report Submissions for a Given Date Range by Organization
`GET /reports/{topic}/{organization}?start_dt=YYYY-MM-DD&end_dt=YYYY-MM-DD&type=received|available|sent&limit=20&offset=0`

Parameters:
* start_dt - start date (inclusive) to query against the created_at date
* end_dt - end date (exclusive) to queery against the created_at date
* type - received|available|sent: determines how to query the reports for the type of end user organization
* limit - (optional) max number of submissions to pull back
* offset - (optional) where to start the offset for pagination

This end point will use the `type` parameter to determine how to query the reports. The `topic` path element is only valid for "covid-19" but intended to be extensible to future reporting (i.e. hostpital beds, flut). Pagination is supported with the limit and offset parameters. The resultSet provided in the response will allow for the UI to create pagination navigation elements.

Example Json:
```json
{
    "result_set": {
        "count": 20,
        "offset": 0,
        "limit": 20,
        "total": 3
    },
    "reports": [
        {
            "actionId": 143,
            "reportId" : "18a8e461-b3f8-436d-b94e-4065c7b9d186",
            "createdAt": "2021-06-30 13:03:00.305657-04",
            "reportItemCount" : 23,
            "status": "received"
        },
        {
            "actionId": 158,
            "reportId" : null,
            "createdAt": "2021-06-30 13:03:00.305657-04",
            "reportItemCount" : 0,
            "status": "failed"
        }
    ]
}
```
The `status` value will be based on what happened to the report. For example: received reports will have an HttpStatus associated with the `POST` which could be a 201 (created) or 400 (failed). Status for available and sent reports will be determined and either use HttpStatus or an enumerated list of status codes mapped from the action result.  

### Action Details for a Given Id
`GET /reports/{topic}/{organization}/action/{id}`

This endpoint is intended to show the contents of the new `action_response` column added to the `action` table (JsomB column). This may be restricted to admin only roles.

### Report Items
`GET /reports/{topic}/{organization}/{reportId}/items?limit=20&offset=0`

This end point will report the status of all items in the given reportId after verifying that the authenticated user has access to the organization that sent the report. This endpoint will also use the same pagination pattern as the endpoint above. 

Exmaple Json:

```json
{
    "result_set": {
        "count": 20,
        "offset": 0,
        "limit": 20,
        "total": 2
    },
    "actionId": 143,
    "reportId": "18a8e461-b3f8-436d-b94e-4065c7b9d186",
    "createdAt": "2021-06-30 13:03:00.305657-04",
    "reportItemCount" : 23,
    "status": "received"
    "items": [
        {
            "reportIndex": 0
            "trackingId": "123456",
            "metadata": {
                "foo": "bar"
            },
            "reportIndex": 0,
            "trackingId": "123456",
            "destinationCount": 2,
            "destinations": {
                "COUNTY": [],
                "STATE": [
                    {
                        "reportId": "33a8e478-b3d2-498d-b94e-4065f7b9e134",
                        "receiverOrg": "md-doh",
                        "recieverName": "elr",
                        "format": "HL7_BATCH",
                        "sentOn": "2021-06-29 11:00:00.086375-04"
                    }
                ],
                "FEDERAL": [
                    {
                        "reportId": "44a8e478-c3f2-498d-b94d-5068f7b9e134",
                        "receiverOrg": "hhsprotect",
                        "recieverName": "elr",
                        "format": "CSV",
                        "sentOn": "2021-06-29 11:00:00.086375-04"
                    }
                ]
            },
            "reportIndex": 1,
            "trackingId": null,
            "destinationCount": 0,
            "destinations": {
                "COUNTY": [],
                "STATE": [],
                "FEDERAL": []
            }
        }
    ]
}
```
Alternatively, the jurisdiction breakdown can be removed and added as an element to each individual report. Items routing no where would have a destination count of zero and empty jurisdiction arrays (unless the structure is changed).

Alternative Json Response:
```json
{
    "result_set": {
        "count": 20,
        "offset": 0,
        "limit": 20,
        "total": 2
    },
    "reportId" : "18a8e461-b3f8-436d-b94e-4065c7b9d186",
    "createdAt": "2021-06-30 13:03:00.305657-04",
    "items": [
        {
            "reportIndex": 0,
            "trackingId": "123456",
            "destinationCount": 2,
            "destinations": [
                {
                    "reportId": "33a8e478-b3d2-498d-b94e-4065f7b9e134",
                    "receiverOrg": "md-doh",
                    "recieverName": "elr",
                    "jurisdiction": "STATE",
                    "format": "HL7_BATCH",
                    "sentOn": "2021-06-29 11:00:00.086375-04",
                },
                {
                    "reportId": "44a8e478-c3f2-498d-b94d-5068f7b9e134",
                    "receiverOrg": "hhsprotect",
                    "recieverName": "elr",
                    "jurisdiction": "FEDERAL",
                    "format": "CSV",
                    "sentOn": "2021-06-29 11:00:00.086375-04"
                }
            ],
        },
        {
            "reportIndex": 1,
            "trackingId": null,
            "destinationCount": 0,
            "destinations": []
        }
    ]
}
```

The above Json only represents the sentOn status for the report. What are the different states/status of the destination reports that we want to represent? 
* batch?
* pending?
* sent
* error/failed

### Specific Report Item Given a Report Id and Item Id (trackingId or reportIndex)
`GET /reports/submitted/{reportId}/item/index/{reportIndex}`
`GET /reports/submitted/{reportId}/item/tracking/{trackingId}`

This endpoint will return the individual response for a report item. The Json response is structured the same as for all items just an array of one so consumers can use that same Json parsing.

Exmaple Json:
```json
{
    "reportId" : "18a8e461-b3f8-436d-b94e-4065c7b9d186",
    "createdAt": "2021-06-30 13:03:00.305657-04",
    "items": [
        {
            "reportIndex": 0,
            "trackingId": "123456",
            "destinationCount": 1,
            "destinations": [
                {
                    "reportId": "33a8e478-b3d2-498d-b94e-4065f7b9e134",
                    "receiverOrg": "md-doh",
                    "recieverName": "elr",
                    "jurisdiction": "STATE",
                    "format": "HL7_BATCH",
                    "sentOn": "2021-06-29 11:00:00.086375-04"
                },
                {
                    "reportId": "44a8e478-c3f2-498d-b94d-5068f7b9e134",
                    "receiverOrg": "hhsprotect",
                    "recieverName": "elr",
                    "jurisdiction": "FEDERAL",
                    "format": "CSV",
                    "sentOn": "2021-06-29 11:00:00.086375-04"
                }
            ]
        }
    ]
}
```

### Reports Delivered for a Given date range
`GET /reports/delivered/{organization}?start=YYYY-MM-DD&end=YYYY-MM-DD&limit=20&offset=0`

Parameters:
* start - start date (inclusive) to query against the created_at date
* end - end date (exclusive) to queery against the created_at date
* limit - max number of submissions to pull back
* offset - where to start the offset for pagination

This endpoint is intended to return delivered reports to the organization. The authenticated user must be able to view the organizations reports.

Example Json:
```json
{
    "result_set": {
        "count": 20,
        "offset": 0,
        "limit": 20,
        "total": 2
    },
    "reports": [
        {
            "reportId": "33a8e478-b3d2-498d-b94e-4065f7b9e134",
            "actionId": 400
            "format": "HL7_BATCH",
            "itemCount": 10,
            "transportResult": "",
            "createdAt": "2021-06-29 08:15:00.086375-04",
            "sentOn": "2021-06-29 11:00:00.086375-04",
            "downloadedBy": "John Doe",
            "wipedAt": "2021-07-08 13:00:00.086375-04"
        },
        {
            "reportId": "3s8d7478-b3c42-49bb-b94e-4123f7b9e134",
            "actionId": 402
            "format": "CSV",
            "itemCount": 5,
            "transportResult": "",
            "createdAt": "2021-06-29 08:15:00.086375-04",
            "sentOn": "2021-06-29 11:00:00.086375-04",
            "downloadedBy": "John Doe",
            "wipedAt": "2021-07-08 13:00:00.086375-04"
        }
    ]
}
```

### Report Delivered for a Given reportId
`GET /report/delivered/{reportId}`

This end point will lookup the processed and delivered reportId providing information pertaining to the receiving organization (format, sentOn). In addition, we can use the `covid_result_metadata` to add aggregate information to the response for all the items received in the report.

Potential Metadata items to include:
* aggregate counts for test result summary i.e. Detected, Not Detected, Inconclusive
* aggregate counts for equipment model summary
* aggregate counts ordering facility / ordering provider
* aggregate counts by patient state, county, age ranges, gender, race, ethnicity
* aggregate counts by testing lab

Example Json:

```json
{
    "reportId": "33a8e478-b3d2-498d-b94e-4065f7b9e134",
    "actionId": 400
    "format": "HL7_BATCH",
    "itemCount": 10,
    "transportResult": "",
    "createdAt": "2021-06-29 08:15:00.086375-04",
    "sentOn": "2021-06-29 11:00:00.086375-04",
    "downloadedBy": "John Doe",
    "wipedAt": "2021-07-08 13:00:00.086375-04",
    "metadata": {
        "testResults": [
            {
                "result": "Detected",
                "value": "1"
            },
            {
                "result": "Not Detected",
                "value": "7"
            },
            {
                "result": "Inconclusive",
                "value": "2"
            }
        ]
    }
}
```