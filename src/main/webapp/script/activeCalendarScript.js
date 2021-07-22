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
 *                  1. Init a Gapi Auth Instance if there isn't one, Init a Client Instance as well. (Functions included in authScript.js)
 *                  2. Using Gapi Auth Instance, make an instance of Gapi Calendar (gapi.client.load('calendar', v3, function(){}))
 *                  3. Make a request for list of user calendars (gapi.client.calendar.calendarList.list())
 *                  4. Store List of results into a useable list: userCalendarList
 *                  
 * 
 * Step 2. Required: User calender List
 * 
 *      Steps:
 *              1. Hide all other cards, Display Active User Calendars Card 
 *              2. Get Parent Object Node
 *              3. Clear any children objects
 *              4. Create new children objects for each Calendar in "userCalendarList"
 *                      Steps:
 *                          1: Create new Element: document.createElement
 *                          2: Get element "Summary" (Name of the Calendar), and "calendarId" (the identifier for the calender, this gets sent to servlet)
 *                          3: In new element innerHTML, add "Summary" as display value, add <input type="checkbox">, add value="calendarId"
 *                          4: Set new Element class to calendarListEntry for styling purposes
 *              5. Append new Children to parent object
 */

var userCalendarList = [];

 //Main Function to start process
 async function loadUserCalendarList(){
     
    //Init auth and client instances
    await loadGapi();
    hideLogin();

    gapi.client.load("calendar", "v3", function(){

        //Build Calendar List Request
        var request = gapi.client.calendar.calendarList.list();

        //Execute the Request
        request.execute(onCalendarListSuccess(calList), onCalendarListFail(response));
    });

 }

 function onCalendarListSuccess(calList){
    userCalendarList = calList.items;
    console.log(userCalendarList);
 }

 function onCalendarListFail(response){
    alert("Error Processing Calender List Request....\nPlease Try Again...");
 }


 