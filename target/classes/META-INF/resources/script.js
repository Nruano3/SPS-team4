// Create Event Modal Handler
function launchmodal() {
    document.getElementById("eventmodal").style.display = "block";
} // Opens the modal form when the button is clicked
function closemodal() {
    document.getElementById("eventmodal").style.display = "none";
} // Hides modal form when exit button is clicked


async function getUserInfo(){
    const responseFromServer = await fetch("/UserInfo");

    const userInfoJson = await responseFromServer.json();
  //  gapi.client.setToken({access_token:'\''+ userInfoJson.access_token + '\''})
  //  gapi.auth.setToken({access_token:'\''+ userInfoJson.access_token + '\''})
    console.log(userInfoJson.userName);


}