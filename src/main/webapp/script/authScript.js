/**
 *  authScript.js contents include only functions related to Googles JS OAuth Flow
 *  for Google Sign-In capablilties.
 *  
 *  No Calendar API calls are to be made here. This file must be included with all web-pages
 * associated with the App, as this file ensures valid User Sessions and Valid Authorizations
 * are made. 
 */


var DISCOVERY_DOCS = ["https://www.googleapis.com/discovery/v1/apis/calendar/v3/rest"];
var SCOPE = 'https://www.googleapis.com/auth/calendar.events https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/calendar'; 
var auth2;

/**
 * function start()
 * description: this runs every time the page is loaded
 * It sets the session app credentials if not already set,
 * these include the API key, and Client ID, needed for 
 * API calls.
 * 
 * It then loads Google API instances for OAuth2, and Client
 * APIs. If a user is already logged in, it then displays their
 * data on the home screen. Otherwise it displays the login screen.
 */
async function start(){

    
    await setAppCredentials();
    await loadGapi();    
	if(await isValidSession()){
		loadUserData();
	}
}

//Retrive Client_id and API Key from server
function setAppCredentials(){
	fetch("/getCredentials").then(response => response.json())
							.then(function(data) {
								sessionStorage.CLIENT = data.client_id;
								sessionStorage.API_KEY = data.apiKey;
                            });
}

//Load auth2 and client instances
async function loadGapi(){
	await gapi.load('auth2',await auth2Init);
    gapi.load('client', clientInit);
}

//Init a gapi client instance
function clientInit(){
	gapi.client.init({
		client_id: sessionStorage.CLIENT,
		scope: SCOPE,
		cookiepolicy: 'single_host_origin'
	})
}

//Init a gapi auth2 instance
async function auth2Init(){
	gapi.auth2.init({
		client_id: sessionStorage.CLIENT,
        scope: SCOPE,
        cookiepolicy: 'single_host_origin'
	}).then(function(AUTH2){

        
        /**
         * The following attaches and Enables Google Sign-in to the Google Sign In Button
         */
        document.getElementById('signInButton').onclick = function() {             
            AUTH2.grantOfflineAccess().then(signInCallback);          
        }
        //This should attach the Auth instance to global variable auth2
        auth2 = AUTH2;
        $('#signInButton').attr('style', 'display: grid');
    });
	
}

/**
 * function isValidSession()
 * description:
 *      This function uses a present id token, stored in sessionStorage
 *      and checks it validity via a call to a backend 
 *      servlet that verifies the ID token using googles 
 *      ID Token Verifier library
 */
async function isValidSession(){
	//If an id token is present and valid, then 
    //its a valid session
    
	if(sessionStorage.id_token == 'null' || !sessionStorage.id_token){
		return false;
	}
	else if(sessionStorage.id_token) {
        const isVerified = await verifyUser(sessionStorage.id_token);
		return isVerified;
	}
}

/**
 * 
 * @param {String} id_token
 * Makes the call to the backend to verify the id token as a valid
 * Google ID token. 
 */
async function verifyUser(id_token){
	const res = await fetch("/Verify?id_token=" + id_token);
	const resJson = await res.json();
    
    return resJson.verified;
}



/**
 * 
 * @param {String} authResult 
 * Upon successful call back from a Google Sign-in event,
 * this function makes a post request to the backend
 * authentication servlet that trades the "code" 
 * given to this app for an access_token
 * and other user info. The result gets stored in the apps 
 * Datastore.
 */
function signInCallback(authResult) {
    
	if (authResult['code']) {
               
	    // Hide the sign-in button now that the user is authorized, for example:
        $('#signInButton').attr('style', 'display: none');
        $('#signOutButton').attr('style', 'display: grid');
       
        $.ajaxSetup({
            headers:{
                'X-Requested-With': 'XMLHttpRequest'
            }
        })
        
        $.post(
            'https://8080-cs-1084074782278-default.cs-us-west1-ijlt.cloudshell.dev/GAuthCallback',
            {code: authResult['code']},
            function(response){

                console.log(response);
                onLoginSuccess();
                
            }).fail(function(resposne){
                console.log(response);
                alert("Oops, something went wrong, please try again...");
            });
    
    } else {
        // There was an error.
        console.log("there was an error")
    }
    
}

/**
 * function getAndStoreUserTokens()
 * 
 * This function ensures a valid gapi.auth2 instance, 
 * then ensures that a user is properly logged in.
 * If both of those are true, then we are able to 
 * retrieve a user access token and a user id token
 * which are used to validate API requests on the users 
 * behalf.
 */
async function getAndStoreUserTokens(){

    var user;
    //Ensure there is a Valid Google Auth Instance
    if(!gapi.auth2) loadGapi();

    //Get the auth instance
    if(!auth2){
        auth2 = await gapi.auth2.getAuthInstance();
    }

    /**
     * Ensure user is logged in, then get and return profile information
     */
    if(await auth2.isSignedIn.get()){
        //Get current user
        user = await auth2.currentUser.get();

        //Store useful information from the user
        sessionStorage.id_token = await user.getAuthResponse().id_token;
        sessionStorage.access_token = await user.getAuthResponse().access_token;
        

    }else {
        //If not signed in, attempt to "quietly" login
        var options = new gapi.auth2.SigninOptionsBuilder();
        options.setPrompt('none');       
        await auth2.signIn(options)
        onLoginSuccess();
    }

    return user;
    
}

/**
 * function onLoginSuccess()
 * 
 * Upon a successful Google Sign-In event,
 * this function ensures storage of the users access_token and 
 * id_token for later API calls, and then loads and displays all 
 * the users information to the screen. showing that 
 * they are successfully logged in.
 */
async function onLoginSuccess(){
    try{
        //Retrieve and Store user access_token and id_tokens
        getAndStoreUserTokens();            
        //Finish Sign-in process
        loadUserData(); 
        return;
        
    } catch (err) {
        alert("Please Try Logging In again");
        window.location.reload();
    }
}

/**
 * function loadUserData()
 * 
 * This function logs to the console any pertinent user Data
 * And Uses the Users profile data to display information to the 
 * screen, while hiding the login screen.
 */
async function loadUserData() {
    
    loadGapi();
    auth2 = await gapi.auth2.getAuthInstance();

    var user = await auth2.currentUser.get();	
    var profile = await user.getBasicProfile();
    

    try{
        console.log('ID: ' + profile.getId()); // Do not send to your backend! Use an ID token instead.
        console.log('Name: ' + profile.getName());
        console.log('Image URL: ' + profile.getImageUrl());
        console.log('Email: ' + profile.getEmail());
        
        
        
        //Display User Content
        $('#userContent').attr('style', 'display: grid');
        document.getElementById('userImg').src = profile.getImageUrl();
        document.getElementById('name').innerText = user.getBasicProfile().getName();
        document.getElementById('email').innerText = user.getBasicProfile().getEmail();
        sessionStorage.user = await profile.getEmail();
        await addCurrentUser();
        //Init the calendar
        initializeCalendar();

        //Apply Sign-out to signout button
        $('#signOutButton').click(function() {
            signOut();
        });
        //Hide Sign-In button, Show Sign-Out button
        $('#signInButton').attr('style', 'display: none');
        $('#signOutButton').attr('style', 'display: grid');

    }catch(err){
        resetSession();
        window.location.reload();
    }
}


//Effectively signs out user from site
async function signOut() {
    
	var auth2 = gapi.auth2.getAuthInstance();
	await auth2.signOut().then(async function () {
        auth2.disconnect(); 
        await setAppCredentials();
        await loadGapi();
        await sessionStorage.clear();     
        $('#content').attr('style', 'display: none');
        $('#userContent').attr('style', 'display: none');
        $('#signInButton').attr('style', 'display: grid');
        $('#signOutButton').attr('style', 'display: none');
    });
        

}

//Reset session storage for user data and "refresh" the page
function resetSession(){
    sessionStorage.clear();
    window.location.reload();
}