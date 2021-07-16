
async function addCurrentUser(){

   
    if(!sessionStorage.user){
        if(!gapi.auth2) loadGapi();
        var auth2 = await gapi.auth2.getAuthInstance();
        var user = await auth2.currentUser.get();
        sessionStorage.user = await user.getBasicProfile().getEmail();
        
        addUserToList(sessionStorage.user);
    }else{

        addUserToList(sessionStorage.user);
    }
}

function back(event){
   
    $('.backBtn').attr('style', 'display: none');
    $('.forwardBtn').attr('style', 'display: inline-block');
    
    $('.autoEventInit').attr('style', 'display: flex');
    $('.autoEventFinal').attr('style', 'display: none');
    
}

function forward(event){
    $('.autoEventInit').attr('style', 'display: none');
    $('.autoEventFinal').attr('style', 'display: flex');
    $('.backBtn').attr('style', 'display: inline-block');
    $('.forwardBtn').attr('style', 'display: none');
}

function addUser(){
   
    var emailField = document.getElementById('userEmailField');
    var user = emailField.value.toLowerCase();
    emailField.value = "";

    addUserToList(user);
}

function addUserToList(user){
    if(user){
        //Get User List
        var userList = document.getElementById('addedUsers');
        //Create a new Div
        var userP = document.createElement('p');
        userP.className = "addedUserP";
        
        //Add user and a remove button to the div
        userP.innerHTML = "<p class=\"addedUserEmail\">" + user + "</p><button class=\'userBtn\ btn' onclick=\"removeUser(event)\">Remove</button>";
        
        userList.appendChild(userP);
    }
    
     
}

function removeUser(event){
    event.preventDefault();
    var parentNode = event.target.parentNode;
    var grandParentNode = parentNode.parentNode;
    grandParentNode.removeChild(parentNode);
}


function calculateAutoEventTime(){

    
    var addedUserList = document.querySelectorAll('.addedUserEmail');
    var users = [];
    addedUserList.forEach(function(item){
        users.push(item.innerText);
    });
    var json = {
        userList: JSON.stringify(users)
    }
    var startRestriction = document.getElementById('auto-start-constraint').value;
    var endRestriction = document.getElementById('auto-end-constraint').value;
    var meetingLength = document.getElementById('auto-event-length').value;
    var url = 'https://8080-cs-1084074782278-default.cs-us-west1-ijlt.cloudshell.dev/process-user-data';


    $.post(
        url,
        {userList: json.userList, startRes: startRestriction, endRes: endRestriction, meetingLength: meetingLength},
        function(response){
            console.log(response);

            populateEventTimes(response);
            
        }).fail(function(){
            
            alert("Oops, something went wrong, please try again...");
           // calculateAutoEventTime();
        });

}

var eventTimeList = [];
function populateEventTimes(response){
    
    $('.autoEventInit').attr('style', 'display: none');
    $('.autoEventFinal').attr('style', 'display: flex');
    $('.backBtn').attr('style', 'display: inline-block');
    $('.forwardBtn').attr('style', 'display: none');

    
    eventTimeList = [];
    clearTimes();
    clearTimes();
    response.forEach(element => {
        eventTimeList.push(element);    
    });

    eventTimeList.forEach(addAutoTime);
}

//Using Joda Days (de facto Java DateTime standard library) 
const jodaDayOfWeek = ['', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

function addAutoTime(element, index){
    var start = element.start % 12;
    var startTag = 'am';
    if (start == 0) start = 12;
    if(element.start > 12){
        startTag = 'pm';
    }
    var end = element.end % 12;
    var endTag = 'am';
    if (end == 0) end = 12;
    if(element.end > 12){
        endTag = 'pm';
    }
    
    

    var dayOfWeek = jodaDayOfWeek[element.day];

    var innerp = "<div class=\"text\"><img src=\"clock.ico\" style=\"margin: 10px; height: 20px; display: inline-block; vertical-align: middle\">" + 
                 "<p style=\"display: inline-block;\">" + "<strong>" + dayOfWeek + "</strong>  "+ start + ":00" + startTag + "<span style='font-size: 15px;'>" + 
                 " - </span>" + end + ":00" + endTag + "</p></div><button class=\'userBtn\ btn' onclick=\"setActive(event)\">Select</button>";

    var autoEventTimeContainer = document.getElementById('auto-times');

    var newTime = document.createElement('P');
    newTime.className = "time";
    newTime.id = "autoTime" + index;
    newTime.innerHTML = innerp;

    autoEventTimeContainer.appendChild(newTime);

}

function clearTimes(){
    var times = document.getElementById('auto-times');
    var childNodes = times.childNodes;
    childNodes.forEach(node => {
        console.log(node);
        node.parentNode.removeChild(node);
    })    
}
function setActive(event){
    event.preventDefault();
    var parentNode = event.target.parentNode;
    var grandParentNode = parentNode.parentNode;
    var childNodes = grandParentNode.childNodes;
    childNodes.forEach(element => {
        element.className = "time";
    })
    parentNode.className += "active"
    console.log(parentNode);
}
async function createAutoEvent(){
    //Get list of autoTime elements, find which one is active

    var activeTimeSelection = document.getElementsByClassName('timeactive');
    console.log(activeTimeSelection);
    if(activeTimeSelection.length == 0){
        if(!document.getElementById('selection-error')){
            console.log("error");
            var header = document.getElementById('auto-event-time');
            var errorMsg = document.createElement('p');
            errorMsg.id = "selection-error";
            errorMsg.style.color = 'red';
            errorMsg.innerText = 'Please Select a Time';
            header.appendChild(errorMsg);
            return;
        }
    }

    var activeSelection = activeTimeSelection[0];
    var selectionIndex = activeSelection.id.match(/(\d+)/)[0];

    var eventJson = eventTimeList[selectionIndex];

    //Adjust hour for timezone
    var startTime = parseDate(eventJson.startDate);
    var endTime = parseDate(eventJson.endDate);
    
    /**
     * Adjust the date up a week if weekday of 
     * event comes before today (ie today is thurs, event is tues)
     */
    const today = new Date(Date.now());
    if(today.getDay() > startTime.getDay()){
        startTime.setDate(startTime.getDate()+7);
        endTime.setDate(endTime.getDate()+7);
    }

    

    var addedUserList = document.querySelectorAll('.addedUserEmail');
    var inviteeList = [];
    addedUserList.forEach(function(item){
       
        inviteeList.push({'email': item.innerText})
        
    });

    if(!gapi.auth2) loadGapi();
    var authInstance = await gapi.auth2.getAuthInstance();  
    var user = await authInstance.currentUser.get();
    var profile = await user.getBasicProfile();

    var title = document.getElementById('auto-event-title').value;
    if(!title){
        title = "Meeting Manager Event: " + profile.getName();
    }
    var colorId = document.getElementById('auto-event-color-input').value;
    if(isNaN(colorId)) colorId = 7;
    //Get Start Date, End Date, Event Title, Invitee List,
    var event = {
        'kind' : 'calendar#event',
        'summary': title,
        'colorId' : colorId,
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
        'start' : {
            'dateTime': startTime.toISOString(),
            'timeZone': eventJson.startDate.iChronology.iBase.iParam.iID
        },
        'end': {
            'dateTime': endTime.toISOString(),
            'timeZone': eventJson.endDate.iChronology.iBase.iParam.iID
        },
        'attendees': inviteeList
    }

    console.log(event);
    

    gapi.client.load("calendar", "v3", function(){
        var request = gapi.client.calendar.events.insert({
            'calendarId': 'primary',
            'resource': event
        });
        request.execute(function(event){
            console.log("Event Created: " + event.htmlLink);
            alert("Your event has been submitted and will be added to your calendar."); //alerts user that form is submitted
           
        }), function(event){
            console.log("error");
        }        
    });
}

/**
 * 
 * @param {Date} date
 * 
 * Adjusts the Hour of the event to take into accout the TimeZone offset 
 */
function parseDate(date){
    var temp = new Date(date.iMillis);
    var off = new Date();
    var offset = off.getTimezoneOffset() / 60;
    temp.setHours(temp.getHours() + offset);
    console.log("Temp: " + temp);
    return temp;
}