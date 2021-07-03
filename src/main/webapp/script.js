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

function onSignIn(googleUser) {
  var profile = googleUser.getBasicProfile();
  console.log('ID: ' + profile.getId()); // Do not send to your backend! Use an ID token instead.
  console.log('Name: ' + profile.getName());
  console.log('Image URL: ' + profile.getImageUrl());
  console.log('Email: ' + profile.getEmail()); // This is null if the 'email' scope is not present.
}