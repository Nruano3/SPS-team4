
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
           // calculateAutoEventTime();
        });

}