/**
 * 
 */
 (async () => {
    // checkJWT - no redirect
    processJwtToken();

    // start the idle timer
    idleTimer();

    // orgName
    let orgName = await processOrgName();

    // reports
    let reports = await processReports();

    // report
    let report = await processReport( reports );

    // charts
    /* processCharts(); */
})().catch(err => {
    console.error(err);
});
