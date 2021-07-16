
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

addCurrentUser();

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
function createAutoEvent(){
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
    console.log(selectionIndex);

    
    //Get Start Date, End Date, Event Title, Invitee List, 
}
        