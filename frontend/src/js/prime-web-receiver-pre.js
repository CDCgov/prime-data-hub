/**
 * Fetch all information for the display of the cards
 *
 * @returns Array of card objects for the cardGrid
 */
async function fetchCards() {
    const config = { headers: { 'Authorization': `Bearer ${window.jwt}` } };
    const baseURL = getBaseUrl();

    return window.jwt? Promise.all([
        axios.get(`${baseURL}/api/history/summary/tests`, config).then(res => res.data)
    ]) : [];
}

/**
 * Checks that the browser is on the accepted list of browsers; if not redirects
 *  to a "unsupported-browser" page
 */
function checkBrowser() {
    const browser = bowser.getParser(window.navigator.userAgent);
    const isValidBrowser = browser.satisfies({
        "edge": ">86.0",
        "chrome": ">86.0",
        "firefox": ">78.0",
        "safari": ">14"
    });

    if (!isValidBrowser) window.location.replace('unsupported-browser.html');
}

/**
 * Validates the JWT token as stored in session storage under the key "jwt";
 *  if not valid, redirects to the sign-in page; otherwise sets up the claims
 */
function checkJWT() {
    const token = window.sessionStorage.getItem("jwt");
    const claims = token ? JSON.parse(atob(token.split('.')[1])) : null;

    if (!token || !claims || moment().isAfter(moment.unix(claims.exp)))
        window.location.replace('/sign-in/?return=/daily-data/');
}

/**
 * Fetches the organization name based on the stored claim of org
 *
 * @returns Promise, eventually a String organization name
 */
async function fetchOrgName() {
    const config = { headers: { 'Authorization': `Bearer ${window.jwt}` } }
    const baseURL = getBaseUrl();

    if (!window.org || !window.jwt) return null;

    return Promise.all([
        axios.get(`${baseURL}/api/settings/organizations/${window.org.substring(2).replaceAll("_", "-")}`, config)
            .then(res => res.data)
            .then(org => org.description)
    ]);
}


/**
 *  If the user is logged in, starts an idle timer that when expires
 *      after 15 min, clears session storage and redirects to the
 *      sign-in page
 */
function idleTimer() {
    const loggedIn = window.sessionStorage.getItem("jwt");

    if (loggedIn) {
        window.sessionStorage.setItem("idle-timer", "true");
        idleTimeout(() => {
            window.sessionStorage.clear();
            window.location.replace(`/sign-in/`);
        },
            {
                element: document,
                timeout: 1000 * 60 * 15,
                loop: false
            });
    }
}

/**
 * Handles OKTA login by displaying and sending to OKTA; once successfully
 *  logged in, directs to /daily-data page
 */
function login() {
    const OKTA_clientId = '0oa6fm8j4G1xfrthd4h6';
    const redirectUri = window.location.origin;

    const config = {
        logo: '//logo.clearbit.com/cdc.gov',
        language: 'en',
        features: {
            registration: false, // Enable self-service registration flow
            rememberMe: false, // Setting to false will remove the checkbox to save username
            router: true, // Leave this set to true for the API demo
        },
        el: "#okta-login-container",
        baseUrl: `https://hhs-prime.okta.com`,
        clientId: `${OKTA_clientId}`,
        redirectUri: redirectUri,
        authParams: {
            issuer: `https://hhs-prime.okta.com/oauth2/default`
        }
    };

    new OktaSignIn(config).showSignInToGetTokens({ scopes: ['openid', 'email', 'profile'] })
        .then(function (tokens) {
            var jwt = tokens.accessToken.value;
            window.sessionStorage.setItem('jwt', jwt);
            window.location.replace(`${window.location.origin}/daily-data/`);
        });
}

/**
 * Logs out the the current user
 */
function logout() {
    window.sessionStorage.removeItem("jwt");
    window.sessionStorage.removeItem("idle-timer");
    window.sessionStorage.removeItem("oldOrg");
    window.location.replace(`${window.location.origin}`);
    const _signIn = document.getElementById("signInButton");
    if (_signIn){
        _signIn.removeAttribute( "hidden" );
    }
}

async function fetchReportFeeds(){

    var reports = await fetchReports();
    var receivingOrgSvc = reports.map( rep => rep.receivingOrgSvc )

    return Array.from( new Set( receivingOrgSvc ) );


}

/**
 *
 */
async function fetchReports( filter ) {

    const config = { headers: { 'Authorization': `Bearer ${window.jwt}` } };
    const baseURL = getBaseUrl();

    var retValue = window.jwt? await axios.get(`${baseURL}/api/history/report`, config).then(res => res.data) : [];

    return filter? retValue.filter( report => report.receivingOrgSvc === filter ) : retValue;

}

/**
 *
 */
function requestFile(reportId) {
    let baseURL = getBaseUrl();
    let config = { headers: { 'Authorization': `Bearer ${window.jwt}` } };

    return window.jwt? axios.get(`${baseURL}/api/history/report/${reportId}`, config)
        .then(res => res.data)
        .then(csv => download(csv.content, csv.filename, csv.mimetype)) : null;
}

/**
 *

function signIn() {
    const _signIn = document.getElementById("signIn");
    let _navMenu = document.getElementById("navmenu");
    if (window.sessionStorage.getItem("jwt")) {
        if (_signIn){ _signIn.style.display = "none"; console.log( 'setting signIn to none' ); }

    } else {
        if (_signIn){ _signIn.style.display = "block"; console.log( 'setting signIn to block' ); }
        if (_navMenu){ _navMenu.style.display = "none"; console.log( 'setting navMenu to none' ); }
    }
}
*/

/**
 *
 * @returns
 */
function getBaseUrl() {
    if (window.location.origin.includes("localhost"))
        return "http://localhost:7071";
    else if (window.location.origin.includes("staging"))
        return "https://staging.prime.cdc.gov";
    else
        return "https://prime.cdc.gov";
}


function changeOrg( event ){
    window.org = event.value;
    window.sessionStorage.setItem( "oldOrg", window.org );
}
/**
 *
 * @param {boolean} redirect
 */
function processJwtToken(){
    let token = window.sessionStorage.getItem("jwt");
    let claims = token ? JSON.parse(atob(token.split('.')[1])) : null;

    if (token && claims && moment().isBefore(moment.unix(claims.exp))) {

        const emailUser = document.getElementById("emailUser");
        if (emailUser) emailUser.innerHTML = claims.sub;
        const logout = document.getElementById("logout");
        if (logout) logout.innerHTML = 'Logout';

        const _signIn = document.getElementById("signInButton");
        if (_signIn) { _signIn.setAttribute( "hidden", "hidden"); }

        const _org = claims.organization.filter(c => c !== "DHPrimeAdmins");

        const oldOrg = window.sessionStorage.getItem( "oldOrg");

        window.org = oldOrg? oldOrg : (_org && _org.length > 0) ? _org[0] : null;

        window.sessionStorage.setItem( "oldOrg", window.org );
        window.orgs = _org;
        window.user = claims.sub;
        window.jwt = token;
        /*
        const _dropdown = document.getElementById("dropdown");
        if (_dropdown &&  claims.organization.includes( "DHPrimeAdmins" ) ) {
            _dropdown.innerHTML +=
                `<label class="usa-label" for="orgs">Select Org:</label>
                  <select name="orgs" id="orgs" onchange='changeOrg(this)'>
                  </select>
                `;
                const _orgsId = document.getElementById( "orgs");

                window.orgs.forEach( org => {
                    console.log( `${window.org} == ${org} ${window.org == org}` );
                    if( _orgsId ) _orgsId.innerHTML +=
                        window.org == org? `<option value="${org}" selected="selected">${org.substring(2).replaceAll("_", "-").toUpperCase()}</option>` : `<option value="${org}">${org.substring(2).replaceAll("_", "-").toUpperCase()}</option>`;
                });
        }
        */

    }
    else{
        const navmenu = document.getElementById( "navmenu" );
        if( navmenu ) navmenu.setAttribute( "hidden", "hidden" );

        const _signIn = document.getElementById( "signInButton" );
        if( _signIn ) _signIn.removeAttribute( "hidden" );
    }
}

/**
 *
 * @returns {string} organization name; possibly null
 */
async function processOrgName(){
    let orgName = null;

    try {
        orgName = await fetchOrgName();
    } catch (error) {
        console.log('fetchOrgName() is failing');
        console.error(error);
    }

    const orgNameHtml = document.getElementById("orgName");
    if (orgNameHtml && orgName) orgNameHtml.innerHTML += orgName;

    return orgName;
}

function titleCase(str) {
    str = str.toLowerCase().split(' ');
    for (var i = 0; i < str.length; i++) {
      str[i] = str[i].charAt(0).toUpperCase() + str[i].slice(1); 
    }
    return str.join(' ');
  }

async function processReportFeeds(){
    var feeds = await fetchReportFeeds();
    const tabs = document.getElementById("tabs");  

    if( tabs ) tabs.innerHTML += `<div id="reportFeeds" class=${feeds.length>1?"tab-wrap":""}></div>`
    const reportFeeds = document.getElementById("reportFeeds");  
    if( reportFeeds )        
        if( feeds.length > 1 ){
            feeds.forEach( (feed, idx ) => {
                reportFeeds.innerHTML += `
                    <input type="radio" id="tab${idx}" name="tabGroup1" class="tab" ${idx>0? "" : "checked"}>
                    <label for="tab${idx}">${feed.replaceAll( "-", " ").toUpperCase()}</label>
                    ` 
                
            })
        }
            
            feeds.forEach( (feed,idx) => {
                reportFeeds.innerHTML += `
                    <div class=${feeds.length>1?"tab__content":""}>
                    <table class="usa-table usa-table--borderless prime-table" summary="Previous results">
                    <thead>
                      <tr>
                        <th scope="col">Report Id</th>
                        <th scope="col">Date Sent</th>
                        <th scope="col">Expires</th>
                        <th scope="col">Total tests</th>
                        <th scope="col">File</th>
                      </tr>
                    </thead>
                    <tbody id="tBody${idx?idx:''}" class="font-mono-2xs">
                    </tbody>
                  </table>
                    </div>
                `
    
            })
        
        return feeds;
}

/**
 *
 * @returns {Array<Report>} an array of the received reports; possibly empty
 */
async function processReports(feed, idx){

    let reports = [];
    try {
        reports = await fetchReports(feed);
    } catch (error) {
        console.log('fetchReports() is failing');
        console.error(error);
    }
    reports.forEach(_report => {
        const tBody = document.getElementById(`tBody${idx?idx:''}`);
        if (tBody) tBody.innerHTML +=
            `<tr>
                <th data-title="reportId" scope="row">
                    <a href="/report-details/?${_report.reportId}" class="usa-link">${_report.reportId}</a>
                </th>
                <th data-title="date" scope="row">${moment.utc(_report.sent).local().format('YYYY-MM-DD HH:mm')}</th>
                <th date-title="expires" scope="row">${moment.utc(_report.expires).local().format('YYYY-MM-DD HH:mm')}</th>
                <th data-title="Total tests" scope="row">${_report.total}</th>
                <th data-title="File" scope="row">
                    <span>
                        <a href="javascript:requestFile( \'${_report.reportId}\');" class="usa-link">
                            ${_report.fileType == "HL7_BATCH" ? "HL7(BATCH)" : _report.fileType}
                        </a>
                    </span>
                </th>
              </tr>`;
    });
    return reports;
}

/**
 *
 * @param {Array<Report>} reports array
 * @returns {Report} selected report; possibly null
 */
async function processReport( reports ){
    let report = null;
    if (reports && reports.length > 0) {
        if (window.location.search == "") report = reports[0];
        else report = reports.find(report => report.reportId == window.location.search.substring(1));
    }
    if (report != null) {
        const details = document.getElementById("details");
        if (details) details.innerHTML +=
            `<div class="tablet:grid-col-6">
                            <h4 class="text-base-darker text-normal margin-bottom-0">Report type</h4>
                            <p class="text-bold margin-top-0">${report.type}</p>
                            <h4 class="text-base-darker text-normal margin-bottom-0">Report sent</h4>
                            <p class="text-bold margin-top-0">${moment.utc(report.sent).local().format('dddd, MMM DD, YYYY  HH:mm')}</p>
                    </div>
                    <div class="tablet:grid-col-6">
                            <h4 class="text-base-darker text-normal margin-bottom-0">Total tests reported</h4>
                            <p class="text-bold margin-top-0">${report.total}</p>
                            <h4 class="text-base-darker text-normal margin-bottom-0">Download expires</h4>
                            <p class="text-bold margin-top-0">${moment.utc(report.expires).local().format('dddd, MMM DD, YYYY  HH:mm')}</p>
                    </div>`;
        const facilities = document.getElementById( "tBodyFac");
        if( facilities ){
            report.facilities.forEach( reportFacility => {
                facilities.innerHTML += 
                    `
                    <tr>
                        <td>${reportFacility.facility}</td>
                        <td>${reportFacility.CLIA}</td>
                        <td>${reportFacility.total}</td>
                    </tr>
                    `;
            });
        }
       
        const noFac = document.getElementById( 'nofacilities' );
        const facTable = document.getElementById( 'facilitiestable');


        if( report.facilities.length ){
            if( noFac ) noFac.setAttribute( "hidden", "hidden" );    
        }
        else{
            if( facTable ) facTable.setAttribute( "hidden", "hidden" );
        }

        const reportId = document.getElementById("report.id");
        if (reportId) reportId.innerHTML = report.reportId;
        const download = document.getElementById("download");
        if (download) download.innerHTML +=
            `<a id="report.fileType"
                class="usa-button usa-button--outline float-right"
                href="javascript:requestFile( \'${report.reportId}\');"</a>`;
        const reportFileType = document.getElementById("report.fileType");
        if (reportFileType) reportFileType.innerHTML = (report.fileType == "HL7" || report.fileType == "HL7_BATCH") ? "HL7" : "CSV";
    }
    return report;
}

/**
 *
 */
async function processCharts(){
    let cards = [];
    try{
        cards = await fetchCards();
    } catch( error ){
        console.log( 'fetchCards() is failing' );
        console.error( error );
    }
    cards.forEach(card => {
        const cards = document.getElementById("cards");
        if (cards) cards.innerHTML +=
        `<div class="tablet:grid-col-6">
            <div class="usa-card__container">
            <div class="usa-card__body">
                <h4 class="text-base margin-bottom-0">${card.title}</h4>
                <h3 class="text-bold margin-top-0">${card.subtitle}</h3>
                <h4 class="text-base margin-bottom-0">Last 24 hours</h4>
                <p class="text-bold margin-top-0">${card.daily} &nbsp; &nbsp; &nbsp; <span class="text-heavy ${card.positive ? "text-green" : "text-red"}">
                    ${card.positive ? "&#8599;" : "&#8600;"} ${card.change.toFixed(2)}
                </span></p>
                <h4 class="text-base margin-bottom-0">Last 7 days (average)</h4>
                <p class="text-bold margin-top-0">${card.last}</p>
                <canvas id="${card.id}" width="200" height="40"></canvas>
            </div>
            </div>
        </div>`;
    });
    var ctx = 'summary-tests';
    var options = {
        plugins: {
            legend: {
                display: false,
            }
        },
        scales: {
            y: {
                ticks: {
                    beginAtZero: false
                },
                display: false,
            },
            x: {
                display: false,
            }
        }
    };

    var labels = [];
    for (var i = 7; i >= 0; i--) {
        labels.push(moment().subtract(i, 'days').format("YYYY-MM-DD"))
    }

    const myLineChartHtml = document.getElementById(ctx);
    if (myLineChartHtml) {
        new Chart(ctx, {
            type: 'line',
            data: {

                labels: labels,
                datasets: [{
                    data: cards[0].data,
                    borderColor: "#4682B4",
                    backgroundColor: "#B0C4DE",
                    fill: 'origin',
                    borderJoinStyle: "round"
                }]
            },
            options: options
        });
    }

}
