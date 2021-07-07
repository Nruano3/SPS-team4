var DISCOVERY_DOCS = ["https://www.googleapis.com/discovery/v1/apis/calendar/v3/rest"];


async function testStart(){

    await setAppCredentials();
    await loadGapi();    
	if(await isValidSession()){
		loadUserData();
	}

}

async function isValidSession(){
	//If an id token is present and valid, then 
    //its a valid session
    
	if(sessionStorage.id_token == 'null'){
		return false;
	}
	else if(sessionStorage.id_token) {
        const isVerified = await verifyUser(sessionStorage.id_token);
		return isVerified;
	}
}

async function verifyUser(id_token){
	const res = await fetch("/Verify?id_token=" + id_token);
	const resJson = await res.json();
    
    return resJson.verified;
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

//Init a gapi auth2 instance
async function auth2Init(){
	gapi.auth2.init({
		client_id: sessionStorage.CLIENT,
		scope: 'https://www.googleapis.com/auth/calendar.events https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email',
		cookiepolicy: 'single_host_origin'
	}).then(function(AUTH2){
        document.getElementById('signinButton1').onclick = function() {    
            AUTH2.grantOfflineAccess().then(signInCallback);          
        }
         $('#signinButton1').attr('style', 'display: block');
    });
	
}

//Init a gapi client instance
function clientInit(){
	gapi.client.init({
		client_id: sessionStorage.CLIENT,
		scope: 'https://www.googleapis.com/auth/calendar.events https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email',
		cookiepolicy: 'single_host_origin'
	})
}

//Callback function for Sign-in, finishes authenticating, sends result to backend
//and stores useful data in the front end
function signInCallback(authResult) {
    
	if (authResult['code']) {
               
	    // Hide the sign-in button now that the user is authorized, for example:
        $('#signinButton1').attr('style', 'display: none');
        $('#signOutButton').attr('style', 'display: block');
       

	    // Send the code to the server
	    $.ajax({
	        type: 'POST',
	        url: 'https://8080-cs-1084074782278-default.cs-us-west1-ijlt.cloudshell.dev/OAuth2Callback',
	        // Always include an `X-Requested-With` header in every AJAX request,
	        // to protect against CSRF attacks.
	        headers: {
	            'X-Requested-With': 'XMLHttpRequest'
	        },
	        contentType: 'application/octet-stream; charset=utf-8',
	        success: function(result) {
	        	//Get current User information
	        	const auth2 = gapi.auth2.getAuthInstance();
	        	const user = auth2.currentUser.get();

	        	//Store useful information from the user
                sessionStorage.id_token = user.getAuthResponse().id_token;
                sessionStorage.access_token = user.getAuthResponse().access_token;

                //Finish Sign-in process
                loadUserData();      
	        },
	        processData: false,
	        data: authResult['code']
	    });
    } else {
        // There was an error.
        console.log("there was an error")
    }
}

async function loadUserData() {
	if(!gapi.auth2) loadGapi();
        
    //display data
    var authInstance = await gapi.auth2.getAuthInstance();
    
    
    if(authInstance){
        displayUserData(await authInstance.currentUser.get())
    }else {
       if(!gapi.auth2) await loadGapi();
       authInstance = await gapi.auth2.getAuthInstance();
       displayUserData(await authInstance.currentUser.get())
    }
	//displayUserData(user);
}

async function displayUserData(user){

	var profile = await user.getBasicProfile();

	//Display info to Console for testing
	console.log('ID: ' + profile.getId()); // Do not send to your backend! Use an ID token instead.
    console.log('Name: ' + profile.getName());
    console.log('Image URL: ' + profile.getImageUrl());
    console.log('Email: ' + profile.getEmail()); // This is null if the 'email' scope is not present.

    //Display in document
    $('#content').attr('style', 'display: block');
    document.getElementById('name').innerText = "Signed in: " +
            user.getBasicProfile().getName();
    $('#signOutButton').click(function() {
        signOut();
    });
    $('#signinButton1').attr('style', 'display: none');
    $('#signOutButton').attr('style', 'display: block');
}

//Effectively signs out user from site
function signOut() {
	AUTH2 = gapi.auth2.getAuthInstance();
	AUTH2.signOut().then(function () {
        AUTH2.disconnect();
        console.log('User signed out.');
        $('#content').attr('style', 'display: none');
        resetSession();
    });     
}

//Reset session storage for user data and "refresh" the page
function resetSession(){
    sessionStorage.clear();
   // sessionStorage.id_token = null;
    //sessionStorage.access_token = null;
    window.location.replace("index.html");
}


// Create Event Modal Handler
function launchmodal() {
    document.getElementById("eventmodal").style.display = "block";
} // Opens the modal form when the button is clicked
function closemodal() {
    document.getElementById("eventmodal").style.display = "none";
} // Hides modal form when exit button is clicked