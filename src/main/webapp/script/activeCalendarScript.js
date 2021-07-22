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
 */

async function displayActiveCalendarCard(){
    $('#autoEventCard').attr('style', 'display:none');
    $('#activeCalendarCard').attr('style', 'display:grid');

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

    //await clearChildren(parentNode);
    console.log("Cleared Children")
    window.userCalendarList.forEach(element => {
        appendNewCalendarChild(element, parentNode);
    });
    
 }

 function clearChildren(parentNode){
     parentNode.innerHTML = "";
 }

 function appendNewCalendarChild(calendar, parentNode){
    console.log(calendar);
     var newCalendarChild = createCalendarChild(calendar);

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
    console.log(source);

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
    console.log(button);

    if(node.classList.contains("active")){
        node.classList.remove("active");
        button.checked = false;
    }else{
        node.classList.add("active");
        button.checked = true;
    }
}