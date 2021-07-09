// Create Event Modal Handler
function launchmodal() {
    document.getElementById("eventmodal").style.display = "block";
} // Opens the modal form when the button is clicked
function closemodal() {
    document.getElementById("eventmodal").style.display = "none";
} // Hides modal form when exit button is clicked

async function getEventData(event){
    event.preventDefault();
   
    var endTime = new Date(document.getElementById("start-date-input").value + "T" + document.getElementById("late-time-input").value + ":00-07:00");
    var hour =  Number(document.getElementById("hour-input").value)
    var minute = Number(document.getElementById("min-input").value)
    endTime.setHours(endTime.getHours() + hour);
    endTime.setMinutes(endTime.getMinutes() + minute); 
    endTime = endTime.toISOString();

    if(!gapi.auth2) loadGapi();
    var authInstance = await gapi.auth2.getAuthInstance();  
    var user = await authInstance.currentUser.get();
    var profile = await user.getBasicProfile();

    var event = {
        'kind' : 'calendar#event',
       'summary': await document.getElementById("title-input").value,
       'colorId' : await document.getElementById("color-input").value,
        'creator': {
            'id': profile.getId(),
            'email': profile.getEmail(),
            'displayName': profile.getName(),
            "self": true
        },
        'organizer': {
            'id': profile.getId(),
            'email': profile.getEmail(),
            "displayName": await profile.getName(),
            "self": true
        },
        'start': {
            'dateTime': await document.getElementById("start-date-input").value + 'T' + await document.getElementById("early-time-input").value + ':00',
            'timeZone': 'America/Los_Angeles'
        },
        'end': {
            'dateTime': endTime,
            'timeZone': "America/Los_Angeles"
        }
    }

    gapi.client.load("calendar", "v3", function(){
        var request = gapi.client.calendar.events.insert({
            'calendarId': 'primary',
            'resource': event
        });
        request.execute(function(event){
            console.log("Event Created: " + event.htmlLink);
        })
        
    });
    
}

var eventForm = document.getElementById("eventForm");

if(eventForm){
    eventForm.addEventListener("submit", getEventData, true);
}

var DISCOVERY_DOCS = ["https://www.googleapis.com/discovery/v1/apis/calendar/v3/rest"];
var auth2;

async function start(){

    await setAppCredentials();
    await loadGapi();    
	if(await isValidSession()){
		loadUserData();
	}

}

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
		scope: 'https://www.googleapis.com/auth/calendar.events https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/calendar',
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
		scope: 'https://www.googleapis.com/auth/calendar.events https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/calendar',
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
	        url: 'https://8080-cs-1084074782278-default.cs-us-west1-ijlt.cloudshell.dev/GAuthCallback',
	        // Always include an `X-Requested-With` header in every AJAX request,
	        // to protect against CSRF attacks.
	        headers: {
	            'X-Requested-With': 'XMLHttpRequest'
	        },
	        contentType: 'application/octet-stream; charset=utf-8',
	        success: onLoginSuccess(),
            failure: function(result){
                alert("Failure Signing In, Please Try again")
                start();
            },
	        processData: false,
	        data: authResult['code']
	    });
    } else {
        // There was an error.
        console.log("there was an error")
    }
}

async function onLoginSuccess(){
    try{

    
        const auth2 = await gapi.auth2.getAuthInstance();
        if(await auth2.isSignedIn.get()){
            var user = await auth2.currentUser.get();
            
            var profile = await user.getBasicProfile();

            if(!profile){
                
                await auth2.signIn()
                user = await auth2.currentUser.get();
                profile = await user.getBasicProfile();            
            }
        
            //Store useful information from the user
            sessionStorage.id_token = await user.getAuthResponse().id_token;
            sessionStorage.access_token = await user.getAuthResponse().access_token;
            
                    
            //Finish Sign-in process
            loadUserData(); 
            return;
        }else {//=================================================================This may need to be Fixed=============================================================
            var options = new gapi.auth2.SigninOptionsBuilder();
            
            options.setPrompt('none');
            
            await auth2.signIn(options)
            onLoginSuccess();

        }
        
    } catch (err) {
        alert("Please Try Logging In again");
        window.location.reload();
    }
}

async function loadUserData() {
	if(!gapi.auth2) loadGapi();
        
    //display data
    var authInstance = await gapi.auth2.getAuthInstance();
    
    
    if(authInstance){
        displayUserData(await authInstance.currentUser.get())
    }else {
       await loadGapi();
       authInstance = await gapi.auth2.getAuthInstance();       
       displayUserData(await authInstance.currentUser.get())
    }
	//displayUserData(user);
}

async function displayUserData(user){
    if(!user){
        console.log("Not User")
        await loadGapi();
        var authInstance = await gapi.auth2.getAuthInstance(); 
        user = await authInstance.currentUser.get();
    }

    var profile = await user.getBasicProfile();
    if(!profile){
        console.log("Not Profile");
        console.log(sessionStorage.id_token);
        await loadGapi();
        var authInstance = await gapi.auth2.getAuthInstance(); 
        user = await authInstance.currentUser.get();
        profile = await user.getBasicProfile();
    }
    try{
        //Display info to Console for testing
        console.log('ID: ' + profile.getId()); // Do not send to your backend! Use an ID token instead.
        console.log('Name: ' + profile.getName());
        console.log('Image URL: ' + profile.getImageUrl());
        console.log('Email: ' + profile.getEmail()); // This is null if the 'email' scope is not present.
    } catch (err){
        
            //window.location.reload();
    }    
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
async function signOut() {
    
	var auth2 = gapi.auth2.getAuthInstance();
	await auth2.signOut().then(async function () {
        auth2.disconnect(); 
        await setAppCredentials();
        await loadGapi();
        await sessionStorage.clear();     
        $('#content').attr('style', 'display: none');
        $('#signinButton1').attr('style', 'display: none');
        $('#signOut').attr('style', 'display: block');
    });
        

}

//Reset session storage for user data and "refresh" the page
function resetSession(){
    sessionStorage.clear();
    window.location.reload();
}


//==========Calender Functions based on Auth2===============================

 function appendPre(message) {
        var pre = document.getElementById('eventContent');
        var post = document.createElement("P")
        var textContent = document.createTextNode(message + '\r\n');
        post.appendChild(textContent);
        pre.appendChild(post);
}

function httpGet()
{   
    var theUrl = 'https://www.googleapis.com/calendar/v3/calendars/anderson94@mail.fresnostate.edu/events?key=' + sessionStorage.API_KEY;
    var xmlHttp = new XMLHttpRequest();
    xmlHttp.open( "GET", theUrl, false ); // false for synchronous request
    xmlHttp.send( null );
    return xmlHttp.responseText;
}

//https://www.googleapis.com/calendar/v3/calendars/<CALENDAR_EMAIL>/events?key=
function listUpcomingEvents() {
        gapi.client.load('calendar', 'v3', function() {
            

            gapi.client.calendar.events.list({
                'calendarId': 'primary',
                'timeMin': (new Date()).toISOString(),
                'showDeleted': false,
                'singleEvents': true,
                'maxResults': 10,
                'orderBy': 'startTime'
            }).then(function(response){
                var events = response.result.items;
                appendPre('Upcomming Events: \n\n');
                if (events.length > 0) {
                    for (i = 0; i < events.length; i++) {
                    var event = events[i];
                    var when = event.start.dateTime;
                    if (!when) {
                        when = event.start.date;
                    }
                    appendPre(event.summary + ' (' + when + ')')
                    }
                } else {
                    appendPre('No upcoming events found.');
                }
            });
        });
}



