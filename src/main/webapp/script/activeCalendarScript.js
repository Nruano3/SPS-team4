/**
 * This script allows a user to update their "Active" calendars
 * that will be used when processing available times. Default
 * active calendar for user is "primary".
 * 
 * To set active calendars:
 * 
 *      1. Retrieve List of User Calendars
 *      2. Display List of Calenders, with an input type attach, such as a checkbox button
 *      3. Send a Post Request to "ActiveCalendar" servlet containing each of the Calendar IDs
 * 
 * Step 1. Required: valid Gapi Auth instance, or valid access_token.
 *          Steps: 
 *                  1. Hide all other cards, Display Active User Calendars Card 
 *                  2. Init a Gapi Auth Instance if there isn't one, Init a Client Instance as well. (Functions included in authScript.js)
 *                  3. Using Gapi Auth Instance, make an instance of Gapi Calendar (gapi.client.load('calendar', v3, function(){}))
 *                  4. Make a request for list of user calendars (gapi.client.calendar.calendarList.list())
 *                  5. Store List of results into a useable list: userCalendarList
 *                  
 * 
 * Step 2. Required: User calender List
 * 
 *      Steps:
 *              1. Get Parent Object Node
 *              2. Clear any children objects
 *              3. Create new children objects for each Calendar in "userCalendarList"
 *                      Steps:
 *                          1: Create new Element: document.createElement
 *                          2: Get element "Summary" (Name of the Calendar), and "calendarId" (the identifier for the calender, this gets sent to servlet)
 *                          3: In new element innerHTML, add "Summary" as display value, add <input type="checkbox">, add value="calendarId"
 *                          4: Set new Element class to calendarListEntry for styling purposes
 *              4. Append new Children to parent object
 * 
 * 
 * Step 3. Required: List of "Selected" Calendars
 * 
 *      Steps:
 *              1. Attain the list of all selected Calendars
 *              2. Select only the calendarId from each of the selected calendars
 *              3. Stringify List of calendarIds
 *              4. Make a "Post" request to "ActiveCalendar" servlet using the stringified list of calendarIds
 *              5. Upon successful transmission, display a message saying so
 */

async function displayActiveCalendarCard(){
    $('#autoEventCard').attr('style', 'display:none');
    $('#activeCalendarCard').attr('style', 'display:grid');
    $("#setActive").attr("style", "display:block");
    $("#successActive").attr("style", "display:none");

    await loadUserCalendarList();

 }

var userCalendarList = [];

 //Main Function to start process
 async function loadUserCalendarList(){
     
    //Init auth and client instances
    await loadGapi();
    hideLogin();

    await gapi.client.load("calendar", "v3", async function(){

        //Build Calendar List Request
        var request = gapi.client.calendar.calendarList.list();

        //Execute the Request
        await request.execute(onCalendarListSuccess, onCalendarListFail);
    });    

 }

 async function onCalendarListSuccess(calList){
    
    window.userCalendarList = await calList.items;
    displayCalendarList();
 }

 function onCalendarListFail(response){
    alert("Error Processing Calender List Request....\nPlease Try Again...");
 }


 async function displayCalendarList(){
     
    var parentNode = document.getElementById('activeCalendarList');
    clearChildren(parentNode);
    var currentActiveCalendars = await getActiveCalendarsFromServer();
    window.userCalendarList.forEach(element => {
        appendNewCalendarChild(element, parentNode, currentActiveCalendars);
    });

    
 }

 async function getActiveCalendarsFromServer(){
     
    auth2 = await gapi.auth2.getAuthInstance();

    var user = await auth2.currentUser.get();

    var profile = await user.getBasicProfile();

    var userId = profile.getId();

    var responseFromServer = await fetch("/ActiveCalendars?userId="+userId);

    return await responseFromServer.json();

 }

 function clearChildren(parentNode){
     parentNode.innerHTML = "";
 }

 async function appendNewCalendarChild(calendar, parentNode, currentActiveCalendars){
    
     var newCalendarChild = await createCalendarChild(calendar);
     if(currentActiveCalendars.includes(newCalendarChild.lastChild.value)){
        if(!newCalendarChild.classList.contains('active')){
            newCalendarChild.classList.add('active');
            newCalendarChild.lastChild.checked = true;
        }
     }
    
     parentNode.prepend(newCalendarChild);
 }

 function createCalendarChild(calendar){

        var newCalendarChildNode = document.createElement('p');
        newCalendarChildNode.onclick = toggleActiveCalendarFromNode;

        var calendarSummary = calendar.summary;
        //User primary calendar has summary equal to user email address
        if(calendar.primary){
            calendarSummary = "Primary";
        }
        var calendarId = calendar.id;
        var calendarColor = calendar.backgroundColor;
        newCalendarChildNode.innerHTML = "<div class=\'text\'><div id=\'calendarIndicator\' style=\'background-color:" + calendarColor +"\'></div>"+ calendarSummary+ "</div><input type=\'checkbox\' value=\""+ calendarId + "\" onclick=\"toggleActiveCalendar(this)\">"

        newCalendarChildNode.className = "calendarListEntry";

        return newCalendarChildNode;

 }


function toggleActiveCalendar(source){

    var parentNode = source.parentNode;
    if(parentNode.classList.contains("active")) {
        parentNode.classList.remove("active");
    } else{
        parentNode.classList.add("active");
    }    
}

function toggleActiveCalendarFromNode(source){
   
    var node = source.target;
    var button = node.lastChild;
    if(button == null) {
       return; 
    } 
    else {
        if(node.classList.contains("active")){
            node.classList.remove("active");
            button.checked = false;
        }else{
            node.classList.add("active");
            button.checked = true;
        }
    }   
}


async function setActiveCalendars(){

    var activeCalendars = getActiveCalendars();

    var calendarIdList = await getCalendarIdList(activeCalendars);    

    var response = await postToServlet(calendarIdList);
}

function getActiveCalendars(){

    //Empty Return list
    var activeCalendars = [];
    //List of each "Selected" calendar
    var selectedCalendars = document.getElementsByClassName('calendarListEntry active');

     //For Each Selected calendar, push the calendar "id" into return array
    Array.prototype.forEach.call(selectedCalendars, function(element){
        activeCalendars.push(element.lastChild.value);
    });

    //Return the array
    return activeCalendars;
}

function getCalendarIdList(list){
    return JSON.stringify(list);
}

async function postToServlet(calendarIdList){

    auth2 = await gapi.auth2.getAuthInstance();

    var user = await auth2.currentUser.get();	
    var profile = await user.getBasicProfile();

    var userId = profile.getId();

    //Setup Headers
     $.ajaxSetup({
            headers:{
                'X-Requested-With': 'XMLHttpRequest'
            }
    });

    var baseUrl = window.location.protocol + "//" + window.location.hostname + '/ActiveCalendars';
    $.post(
        baseUrl,
        {userList: calendarIdList, userId: userId},
        function(response){
            onActiveCalendarPostSuccess(response);
        }
        ).fail(function(){    
            alert("Oops, something went wrong, please try again...");
        });

}

function onActiveCalendarPostSuccess(response){
    console.log(response);
        
    if(response.error){
        alert("Oops, something went wrong, please try again...");
    }else {
        loadSuccessScreen();
    }
}

function loadSuccessScreen(){
    $("#setActive").attr("style", "display:none");
    $("#successActive").attr("style", "display:block");
}