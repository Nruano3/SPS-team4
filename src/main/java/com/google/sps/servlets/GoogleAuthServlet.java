package com.google.sps.servlets;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.gson.JsonObject;
import com.google.sps.util.CredentialManager;
import com.google.sps.util.DatastoreModule;
import com.google.sps.util.OAuth2Credentials;

@WebServlet("/GAuthCallback")
public class GoogleAuthServlet extends HttpServlet{

    
    private static final long serialVersionUID = 1L;
    private static final String CREDENTIALS_PATH = "client_secret.json";
    private static OAuth2Credentials APP_CREDENTIALS;


    @Override 
    public void init(){
        try {
            setAppCredentials();
            connectToDatastore();
        } catch (IOException e) {
            
            e.printStackTrace();
        }
    }

    private static void connectToDatastore() {
        DatastoreModule.init();
    }
    private static void setAppCredentials() throws IOException {
        APP_CREDENTIALS = CredentialManager.setCredentials(CREDENTIALS_PATH);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws FileNotFoundException, IOException {

        
        InputStream is = request.getInputStream(); 
        
        StringBuilder sb = new StringBuilder();

        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        String inputLine = "";
        while ((inputLine = in.readLine()) != null) {
            sb.append(inputLine);
        }
        String authCode = sb.toString();
        if (request.getHeader("X-Requested-With") == null) {
        // Without the `X-Requested-With` header, this request could be forged. Aborts.
            System.out.println("Error");
             response.sendRedirect(request.getContextPath() + "/index.html");

        }
        
         APP_CREDENTIALS = CredentialManager.setCredentials(CREDENTIALS_PATH);
        // Set path to the Web application client_secret_*.json file you downloaded from the
        // Google API Console: https://console.developers.google.com/apis/credentials
        // You can also find your Web application client ID and client secret from the
        // console and specify them directly when you create the GoogleAuthorizationCodeTokenRequest
        // object.
     

       
        // Exchange auth code for access token
        GoogleTokenResponse tokenResponse =
                new GoogleAuthorizationCodeTokenRequest(
                    new NetHttpTransport(),
                    JacksonFactory.getDefaultInstance(),
                    "https://oauth2.googleapis.com/token",
                    APP_CREDENTIALS.getClient_id(),
                   APP_CREDENTIALS.getClient_secret(),
                    authCode,
                    "postmessage")  // Specify the same redirect URI that you use with your web
                                    // app. If you don't have a web version of your app, you can
                                    // specify an empty string.
                    .execute();
     

        GoogleIdToken idToken = tokenResponse.parseIdToken();
        GoogleIdToken.Payload payload = idToken.getPayload();
        String userId = payload.getSubject();  // Use this value as a key to identify a user.

        storeUserCredentials(tokenResponse,  userId);
        storeUserInfo(payload, userId);      
    }

    private void storeUserCredentials(GoogleTokenResponse tokenResponse,  String userId){

        JsonObject userCredentials = new JsonObject();
        userCredentials.addProperty("id", userId);
        userCredentials.addProperty("access_token", tokenResponse.getAccessToken());
        userCredentials.addProperty("refresh_token", tokenResponse.getRefreshToken());
        userCredentials.addProperty("scope", tokenResponse.getScope());
        userCredentials.addProperty("id_token", tokenResponse.getIdToken());
        userCredentials.addProperty("token_type", tokenResponse.getTokenType());
        userCredentials.addProperty("expires_in", tokenResponse.getExpiresInSeconds());

        DatastoreModule.storeUserCredentials(userCredentials, userId);



        /**
         *  .set("timestamp", timeStampInSeconds)
                .set("id", profileId)
                .set("access_token", userCredentials.get("access_token").toString().replaceAll(quotes,""))
                .set("refresh_token", userCredentials.get("refresh_token").toString().replaceAll(quotes,""))
                .set("scope", userCredentials.get("scope").toString().replaceAll(quotes,""))
                .set("token_type", userCredentials.get("token_type").toString().replaceAll(quotes,""))
                .set("expires_in", userCredentials.get("expires_in").toString().replaceAll(quotes,""))
                .set("id_token", userCredentials.get("id_token").toString().replaceAll(quotes,""))
                .build();
         */

    }
    
    private void storeUserInfo(GoogleIdToken.Payload payload, String userId){
        
        JsonObject userInfo = new JsonObject();
        userInfo.addProperty("id", userId);
        userInfo.addProperty("name", (String) payload.get("name"));
        userInfo.addProperty("given_name", (String) payload.get("given_name"));
        userInfo.addProperty("family_name", (String) payload.get("family_name"));
        userInfo.addProperty("picture", (String) payload.get("picture"));
        userInfo.addProperty("locale", (String) payload.get("locale"));
        userInfo.addProperty("email", payload.getEmail());

        DatastoreModule.storeUserInfo(userInfo);
        /**
         * Entity userEntity = Entity.newBuilder(keyFactory.newKey(userInfo.get("id").toString().replaceAll(quotes,"")))
                                      .set("id", userInfo.get("id").toString().replaceAll(quotes,""))
                                      .set("name", userInfo.get("name").toString().replaceAll(quotes,""))
                                      .set("given_name", userInfo.get("given_name").toString().replaceAll(quotes,""))
                                      .set("family_name", userInfo.get("family_name").toString().replaceAll(quotes,""))
                                      .set("picture", userInfo.get("picture").toString().replaceAll(quotes,""))
                                      .build();
         */
    }
}