// Create Event Modal Handler
function launchmodal() {
    document.getElementById("eventmodal").style.display = "block";
} // Opens the modal form when the button is clicked
function closemodal() {
    document.getElementById("eventmodal").style.display = "none";
} // Hides modal form when exit button is clicked


function createNewEvent(){
    launchmodal();
}



function addUser(){
   
    var emailField = document.getElementById('userEmailField');
    var user = emailField.value;
    emailField.value = "";

    addUserToList(user);
}

function addUserToList(user){
    //Get User List
     var userList = document.getElementById('addedUsers');
     //Create a new Div
     var userDiv = document.createElement('div');
     userDiv.className = "addedUserDiv";
     userDiv.style.display = 'flex';
     userDiv.style.justifyContent = 'space-between';
     //Add user and a remove button to the div
     userDiv.innerHTML = "<p class=\"addedUserEmail\">" + user + "</p><button class=\'userBtn\ btn' onclick=\"removeUser(event)\">Remove</button>";
    
     userList.prepend(userDiv);
     
}

function removeUser(event){
    event.preventDefault();
    var parentNode = event.target.parentNode;
    var grandParentNode = parentNode.parentNode;
    grandParentNode.removeChild(parentNode);
}


function calculateAutoEventTime(){
    console.log("Loading User List");
    var addedUserList = document.querySelectorAll('.addedUserEmail');
    var users = [];
    addedUserList.forEach(function(item){
        users.push(item.innerText);
    });
    var json = {
        userList: JSON.stringify(users)
    }
    var startRestriction = 8;
    var endRestriction = 17;
    var meetingLength = 3;
    var url = 'https://8080-cs-1084074782278-default.cs-us-west1-ijlt.cloudshell.dev/process-user-data';


    $.post(
        url,
        {userList: json.userList, startRes: startRestriction, endRes: endRestriction, meetingLength: meetingLength},
        function(response){
            console.log(response);
            alert("Ok");
            
        }).fail(function(resposne){
            
            console.log(response);
            alert("Oops, something went wrong, please try again...");
            calculateAutoEventTime();
        });

}
/**
 * 
 * @param event
 * createEvent takes all the data from the fields contained within the event modal
 * and translates them into an event json, then uses that json to create
 * an event with the Calendar API 
 */
async function createEvent(event){
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

    alert("Your event has been submitted and will be added to your calendar."); //alerts user that form is submitted
    closemodal(); //closes modal
    
}


var eventForm = document.getElementById("eventForm");

if(eventForm){
    eventForm.addEventListener("submit", createEvent, true);
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
        document.getElementById('signInButton').onclick = function() {             
            AUTH2.grantOfflineAccess().then(signInCallback);          
        }
        $('#signInButton').attr('style', 'display: grid');
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
        $('#signInButton').attr('style', 'display: none');
        $('#signOutButton').attr('style', 'display: grid');
       

        
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

    initializeCalendar();
   
    $('#userContent').attr('style', 'display: grid');
     document.getElementById('userImg').src = profile.getImageUrl();
    document.getElementById('name').innerText = user.getBasicProfile().getName();
    document.getElementById('email').innerText = user.getBasicProfile().getEmail();
    $('#signOutButton').click(function() {
        signOut();
    });
    $('#signInButton').attr('style', 'display: none');
    $('#signOutButton').attr('style', 'display: grid');
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


//==========Calender Functions based on Auth2===============================

 function appendPre(message) {
        var pre = document.getElementById('eventContent');
        var post = document.createElement("P")
        var textContent = document.createTextNode(message + '\r\n');
        post.appendChild(textContent);
        pre.appendChild(post);
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
                appendEvent('Upcomming Events: \n\n');
                if (events.length > 0) {
                    for (i = 0; i < events.length; i++) {
                    var event = events[i];
                    var when = event.start.dateTime;
                    if (!when) {
                        when = event.start.date;
                    }
                    appendEvent(event.summary + ' (' + when + ')')
                    }
                } else {
                    appendEvent('No upcoming events found.');
                }
            });
        });
}

const dayOfWeek = ['sun', 'mon', 'tue', 'wed', 'thu', 'fri', 'sat'];
const colorids = ['#000000', '#7986cb', '#33b679', '#8e24aa', '#e67c73', '#f6c026', '#f5511d', '#000000', '#616161', '#3f51b5', '#0b8043', '#d60000', '#039be5'];
const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
var currDate = new Date();

function initializeCalendar() {
    var temp = new Date();
    if(temp.getDay() == 0) {
        currDate.setDate(temp.getDate() - 6);
    }
    else {
        currDate.setDate(temp.getDate() - (temp.getDay() - 1));
    } // statement makes sure week starts on a monday
    currDate = new Date(currDate.setHours(0,0,0,0));
    cleanCalendar();
    displayDays(currDate);
    populateCalendar(currDate);
}

 /* Note on next few functions to select the date and range of dates the arithmetic for the dates seems to be wierd or I am misunderstanding something so that it is not working consistently? The adjustment of the date appears to be off by a week for some reason I can not understand, but it works.
 */

function nextWeekCalendar() {
    cleanCalendar();
    currDate.setDate(currDate.getDate()); // Increases week to next week
    displayDays(currDate);
    populateCalendar(currDate);
}

function lastWeekCalendar() {
    cleanCalendar();
    currDate.setDate(currDate.getDate() - 14); // Decreases week to previous week
    displayDays(currDate);
    populateCalendar(currDate);
}

function populateCalendar(calDate) {
        gapi.client.load('calendar', 'v3', function() {
            

            gapi.client.calendar.events.list({
                'calendarId': 'primary',
                'timeMin': (calDate).toISOString(),
                'timeMax': (new Date(calDate.setDate(calDate.getDate() + 7))).toISOString(),
                'showDeleted': false,
                'singleEvents': true,
                'orderBy': 'startTime'
            }).then(function(response){
                var events = response.result.items;
                displayMonth(events);
                if (events.length > 0) {
                    for (i = 0; i < events.length; i++) {
                    var event = events[i];
                    var when = event.start.dateTime;
                    if (!when) {
                        when = new Date(event.start.date);
                    }
                    var day = (new Date(when)).getDay();
                    var cid = event.colorId;
                    if (isNaN(cid)) {
                        cid = 12;
                    }
                    appendEvent(event.summary, event.start, event.end, dayOfWeek[day], colorids[cid]);
                    }
                }
            });
        });
}

function appendEvent(message, start, end, weekday, color) {
        var pre = document.getElementById('event' + weekday);
        var div = document.createElement("div");
        div.style.backgroundColor = color;
        div.style.border = "1px solid black";
        div.style.borderRadius = "15px";
        div.style.padding = "6px";
        div.style.color = "white";
        div.style.textAlign = "center";
        var textContent = document.createTextNode(message);
        div.appendChild(textContent);
        getDateTimeStrings(start, end, div);
        pre.appendChild(div);
}

function getDateTimeStrings(start, end, elem) {
    var startDateTime = new Date(start.dateTime);
    var endDateTime = new Date(end.dateTime);
    if(!startDateTime.getMonth() || !endDateTime.getMonth()) {
        var startMonth = months[(new Date(start.date)).getMonth()];
        var startDay = (new Date(start.date)).getDate();
        var endMonth = months[(new Date(end.date)).getMonth()];
        var endDay = (new Date(end.date)).getDate();
        elem.appendChild(document.createElement("br"));
        elem.appendChild(document.createTextNode("Starts: " + startMonth + " " + startDay));
        elem.appendChild(document.createElement("br"));
        elem.appendChild(document.createTextNode("Ends: " + endMonth + " " + endDay));
    }
    else {
        var startTime = getStringTime(startDateTime);
        var endTime = getStringTime(endDateTime);
        elem.appendChild(document.createElement("br"));
        elem.appendChild(document.createTextNode("Starts: " + months[startDateTime.getMonth()] + " " + startDateTime.getDate()  +startTime));
        elem.appendChild(document.createElement("br"));
        elem.appendChild(document.createTextNode("Ends: " + months[endDateTime.getMonth()] + " " + endDateTime.getDate() + endTime));
    }
}

function getStringTime(timeValue) {
    var hours = timeValue.getHours();
    var mins = timeValue.getMinutes();
    if(mins == 0) {
        mins = "00";
    }
    if(hours == 0) {
        return(" 12:" + mins + " AM");
    }
    else if(hours < 12) {
        return(" " + hours + ":" + mins + " AM");
    }
    else if(hours == 12) {
        return(" " + hours + ":" + mins + " PM")
    }
    else if(hours < 24) {
        return(" " + (hours-12) + ":" + mins + " PM");
    }
    else {
        return(" All day");
    }
}

function displayDays() {
    var date = new Date(currDate);
    for(i=0; i<dayOfWeek.length; i++) {
        var dayweek = date.getDay();
        var daymonth = date.getDate();
        var dayelem = document.getElementById(dayOfWeek[dayweek]);
        var div = document.createElement("div");
        var today = new Date();
        if((date.getDate() == today.getDate()) && (date.getMonth() == today.getMonth()) && (date.getFullYear() == today.getFullYear())) {
            div.style.borderRadius = "50%";
            div.style.border = "1px solid black";
            div.style.width = "45px";
            div.style.backgroundColor = "lightblue";
            div.style.margin = "0 auto";
        }
        div.appendChild(document.createTextNode(dayOfWeek[dayweek].toUpperCase()))
        div.appendChild(document.createElement("br"));
        div.appendChild(document.createTextNode(daymonth));
        dayelem.appendChild(div);
        date.setDate(date.getDate() + 1);
    }
}

function displayMonth(events) {
    var month = document.getElementById('month');
    var monMonth = (new Date(events[0].start.dateTime)).getMonth();
    var sunMonth = (new Date(events[events.length-1].start.dateTime)).getMonth();
    if(monMonth == sunMonth) {
        month.appendChild(document.createTextNode(months[monMonth]));
    }
    else {
        month.appendChild(document.createTextNode(months[monMonth] + '/' + months[sunMonth]));
    }
}

function cleanCalendar() {
    var ele = document.getElementById('month');
    ele.innerHTML = '';
    for(i=0; i<dayOfWeek.length; i++) {
        var ele = document.getElementById(dayOfWeek[i]);
        while(ele.hasChildNodes()) {
            ele.removeChild(ele.firstChild);
        }
    }
    for(i=0; i<dayOfWeek.length; i++) {
        var ele = document.getElementById('event' + dayOfWeek[i]);
        while(ele.hasChildNodes()) {
            ele.removeChild(ele.firstChild);
        }
    }
}


