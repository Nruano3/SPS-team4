/**
 * eventScript.js contains all the code relevant to creating events
 * 
 * Event Types:
 *      Auto Event
 *      Regular Event
 * 
 *  Auto Event:
 *      This is an event whose time is determined automatically by a backend servlet.
 *      Required parameters needed to build an auto event are as follows:
 *                      
 *              -   Invitee List
 *              -   Start Time Constraint
 *              -   End Time Constraint
 *              -   Meeting Length
 *              
 *      The response from the servlet will contain a list of "Event" objects, which contain the following Fields:
 * 
 *              - Start (int): Integer Value of the start hour
 *              - End (int): Integer Value of the end hour
 *              - Non-attendees (int): Integer Value denoting the maximum amount of people that will not be able to attend that event
 *              - startDate (DateTime/String): DateTime/String value of the Start of the event
 *              - endDate (DateTime/String): DateTime/String value of the End of the Event
 * 
 * 
 * 
 * Regular Event:
 *      This is an event where all parameters are specified by the user. They are free to add any invitee, and a call to 
 *      google's calendar api is made with all pertinent information to create the event.
 */


 //======================Regular Event==============================//

 
var eventForm = document.getElementById("eventForm");

if(eventForm){
    eventForm.addEventListener("submit", createEvent, false);
}

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


/**
 * 
 * @param event
 * createEvent takes all the data from the fields contained within the event modal
 * and translates them into an event json, then uses that json to create
 * an event with the Calendar API 
 */
async function createEvent(event){

    //Prevents page from reloading
    event.preventDefault();
    console.log("TEST");
    const date = new Date();
    const offset = date.getTimezoneOffset() / 60;
    
   
    var endTime = new Date(document.getElementById("start-date-input").value + "T" + document.getElementById("late-time-input").value + ":00-0" + offset.toString() + ":00");
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
            'dateTime': await document.getElementById("start-date-input").value + 'T' + await document.getElementById("early-time-input").value + ':00-0' + offset.toString() + ':00'
        },
        'end': {
            'dateTime': endTime
        }
    }

    gapi.client.load("calendar", "v3", function(){
        var request = gapi.client.calendar.events.insert({
            'calendarId': 'primary',
            'resource': event
        });
        request.execute(function(event){
            console.log("Event Created: " + event.htmlLink);
            alert("Your event has been submitted and will be added to your calendar."); //alerts user that form is submitted
        })
        
    });

    
    closemodal(); //closes modal
    
}