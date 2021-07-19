## Background

The PRIME ReportStream uses Metabase for business analytics. Metabase is a business intelligence tool that allows for the exploration, investigation, and visualization of data. With this internal tool, our team is able to generate charts and dashboards from our stored data. Even though this instrument provides quality charts, it also has its shortcomings:

### Limitations 

- Query complexity 
    - Limited to simple queries only
    
- Data Source 
    - Only works well with a single SQL data source
    - Does not allow for joining of data
    - Unable to upload CSV file format
    
- Formatting
    - Can be difficult to customize charts
    

As our data input becomes larger and keeping scalability in mind, these limitations will become problematic. It will be more time-consuming to write complex queries and to customize charts than it is now.

## Goals

Our goal is to meet the demands of scalability while maintaining the ability to analyze data by dicing and visualization of charts and reports. Other key factors to consider are:

### Key Factors

- Open Source
- Works with Azure
- Has more visualization options (chart types)
- Can connect to multiple data sources
- Able to run complex queries (joins)
- Low learning curve
- Accepts CSV format
- capable of running locally (for testing)
- Highly customizable


## Proposal

### Migration to Apache Superset

Superset will provide the same abilities as metabase plus added features. Our team will be able to choose from a wider selection of charts. Chart customization will have more options, and it won't be as complicated to perform. Furthermore, Superset has the ability to run analytic workloads against most popular databases (Postgres). Having this option will allow our team to create charts from complex queries.

Another great addon is having state of the art interactive dashboards and able to work with Azure. Interactive dashboards is the feature that sets Superset apart from Metabase. With this, our team will be able to create interactive dashboards using data stored on the cloud. 


### Economic Impact

Superset is open source, free to use, and has a low learning curve. As a result, the economic impact is low as there is no additional technical debt acquired. 

