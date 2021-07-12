package com.google.sps.util;

import java.io.IOException;


import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.cloud.datastore.Key;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.google.sps.util.OAuth2Credentials;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;


public class DatastoreModule {

    private static Datastore dataStore;
    private static final String quotes = "\"";
     private static KeyFactory keyFactory;
     private static final String CREDENTIALS_PATH = "client_secret.json";
    private static OAuth2Credentials APP_CREDENTIALS;
    private static String REFRESH_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static CloseableHttpClient HTTP_CLIENT;

   public static void init() throws IOException {
        if (dataStore == null) {
             setAppCredentials();
            dataStore = DatastoreOptions.getDefaultInstance().getService();
            keyFactory = dataStore.newKeyFactory();
            HTTP_CLIENT = HttpClientBuilder.create().build();
        }
    }

    private static void setAppCredentials() throws IOException {
        APP_CREDENTIALS = CredentialManager.setCredentials(CREDENTIALS_PATH);
    }

    public static boolean isUserInDatastore(String userId){
        keyFactory = dataStore.newKeyFactory().setKind("UserProfile");
        Key userIdKey = keyFactory.newKey(userId);
        return (dataStore.get(userIdKey) != null);
    }

    

    public static void storeUserInfo(JsonObject userInfo){
         /**
         * userInfo json contains the following:
         * 
        *  "id": "54135165135843153543",
           "name": "Ryan Anderson",
           "given_name": "Ryan",
           "family_name": "Anderson",
           "picture": "https://lh3.googleusercontent.com/a/AATXAJwV1nPAUtdTZr-7qwfxMt6f1Ih9GJZ2Y-SR4cWt=s96-c",
            "locale": "en"
            Storing data in dataStore:
                Create key factory
                Create Entity with all required fields
                Add entity to the store
            Note: Elements from Json objects may get returned with Quotes "", scrub them of the quotes if needed
        */

        keyFactory = dataStore.newKeyFactory().setKind("UserProfile");
        if(dataStore.get(keyFactory.newKey(userInfo.get("id").toString().replaceAll(quotes,""))) == null){
            Entity userEntity;
            if(userInfo.has("email")){
                userEntity = Entity.newBuilder(keyFactory.newKey(userInfo.get("id").toString().replaceAll(quotes,"")))
                                        .set("id", userInfo.get("id").toString().replaceAll(quotes,""))
                                        .set("name", userInfo.get("name").toString().replaceAll(quotes,""))
                                        .set("given_name", userInfo.get("given_name").toString().replaceAll(quotes,""))
                                        .set("family_name", userInfo.get("family_name").toString().replaceAll(quotes,""))
                                        .set("picture", userInfo.get("picture").toString().replaceAll(quotes,""))
                                        .set("locale", userInfo.get("locale").toString().replaceAll("\"", ""))
                                        .set("email", userInfo.get("email").toString().replaceAll("\"", ""))
                                        .build();
            }
            else{
                userEntity = Entity.newBuilder(keyFactory.newKey(userInfo.get("id").toString().replaceAll(quotes,"")))
                                        .set("id", userInfo.get("id").toString().replaceAll(quotes,""))
                                        .set("name", userInfo.get("name").toString().replaceAll(quotes,""))
                                        .set("given_name", userInfo.get("given_name").toString().replaceAll(quotes,""))
                                        .set("family_name", userInfo.get("family_name").toString().replaceAll(quotes,""))
                                        .set("picture", userInfo.get("picture").toString().replaceAll(quotes,""))
                                        .set("locale", userInfo.get("locale").toString().replaceAll("\"", ""))
                                        .build();
            }
            dataStore.add(userEntity);
        }
        
        return;

    }

    public static void storeUserCredentials(JsonObject userCredentials, String profileId){

        /**
         * userCredential contains:
         * {
         *  "access_token": "access_token"
         *  "refresh_token": "refresh_token"
         *  "scope": "scopes"
         *  "token_type": "token_type"
         *  "expires_in": "expiration in seconds"
         * }
         */
        
        keyFactory = dataStore.newKeyFactory().setKind("UserCredentials");
        long timeStampInSeconds = System.currentTimeMillis() /1000;
        if(dataStore.get(keyFactory.newKey(profileId)) == null){
            Entity credentialEntity = Entity.newBuilder(keyFactory.newKey(profileId))
                .set("timestamp", timeStampInSeconds)
                .set("id", profileId)
                .set("access_token", userCredentials.get("access_token").toString().replaceAll(quotes,""))
                .set("refresh_token", userCredentials.get("refresh_token").toString().replaceAll(quotes,""))
                .set("scope", userCredentials.get("scope").toString().replaceAll(quotes,""))
                .set("token_type", userCredentials.get("token_type").toString().replaceAll(quotes,""))
                .set("expires_in", userCredentials.get("expires_in").toString().replaceAll(quotes,""))
                .set("id_token", userCredentials.get("id_token").toString().replaceAll(quotes,""))
                .build();

            dataStore.add(credentialEntity);
        }
        
    }

    public static void updateData(String userId, String field, String data){
        //TODO update user information
    }

    public static void updateData(String userId, JsonObject data){
        //TODO update user information
    }

    public static String getUserAccessTokenWithEmail(String email) throws ClientProtocolException, IOException {

       String access_token = "";
       Query<Entity> query = Query.newEntityQueryBuilder()
                                .setKind("UserProfile")
                                .setFilter(PropertyFilter.eq("email", email))
                                .build();
        QueryResults<Entity> profile = dataStore.run(query);

        if(profile.hasNext()){
            Entity profileEntity = profile.next();
            String profileId = profileEntity.getString("id");
            return getUserAccessTokenWithId(profileId.toString());
        }
        return access_token;

    }

    public static String getUserAccessTokenWithId(String userId) throws ClientProtocolException, IOException {
        String access_token = "";
        keyFactory = dataStore.newKeyFactory().setKind("UserCredentials");
        Key profileKey = keyFactory.newKey(userId);

        Entity userCredentials = dataStore.get(profileKey);
        if(needsToBeRefreshed(userCredentials)){
           
            refreshCredentials(userCredentials);
        }
        
        access_token = userCredentials.getString("access_token");
        return access_token;
    }



    public static Boolean needsToBeRefreshed(Entity userCredentials) {

        // Get Creds Expiration (in seconds)
        long expires_in = Long.parseLong(userCredentials.getString("expires_in"));
        // Get time stamp
        long credentialTimeStamp = userCredentials.getLong("timestamp");
        // If difference between current time and the time stamp is greater than
        // expiration time, return true
        
        return ((System.currentTimeMillis() / 1000) - credentialTimeStamp) > expires_in;
    }

    public static void refreshCredentials(Entity userCredentials) throws ClientProtocolException, IOException {
        /**
         *  client_id=8819981768.apps.googleusercontent.com&
            client_secret={client_secret}&
            refresh_token={Refresh_token}
            grant_type=refresh_token
            https://accounts.google.com/o/oauth2/token refresh_token={Refresh_token} client_id=XXXX client_secret=XXXX grant_type=refresh_token
         */

        //Build Request for new token based on the refresh token url
        StringBuilder sb = new StringBuilder().append(REFRESH_TOKEN_URL).append("?")
                                              .append("&client_id=").append(APP_CREDENTIALS.getClient_id())
                                              .append("&client_secret=").append(APP_CREDENTIALS.getClient_secret())
                                              .append("&refresh_token=").append(userCredentials.getString("refresh_token").replaceAll("\"",""))
                                              .append("&grant_type=refresh_token");
        String token_req_url = sb.toString();
        
        //Build Request
        HttpPost request = new HttpPost(token_req_url);

        //Execute the request
        HttpResponse response = HTTP_CLIENT.execute(request);
        
        //Get response entity
        HttpEntity entity = response.getEntity();

        // Stringify the entity into a Json String
        String body = EntityUtils.toString(entity);
        
        // turn Stringified JSON into JSON object
        JsonObject returnJson = new Gson().fromJson(body, JsonObject.class);
        
        //Extract Access Token
        String newAccess_Token = returnJson.get("access_token").toString().replaceAll("\"", "");
        String newExpires_In = returnJson.get("expires_in").toString().replaceAll("\"", "");
        String newId_Token = returnJson.get("id_token").toString().replaceAll("\"", "");
        String newScope = returnJson.get("scope").toString().replaceAll("\"", "");
        

        //Update the datastore entity        
        long timeStampInSeconds = System.currentTimeMillis() /1000;
        Entity newCreds = Entity.newBuilder(userCredentials).set("access_token", newAccess_Token)
                                                            .set("timestamp", timeStampInSeconds)
                                                            .set("expires_in", newExpires_In)
                                                            .set("id_token",newId_Token )
                                                            .set("scope", newScope)
                                                            .build();

        //Store the update entity
        dataStore.update(newCreds);
    }

    public static String getUserProfileData(String profileId){

        
        KeyFactory keyFactory = dataStore.newKeyFactory().setKind("UserProfile");
        Entity userProfile = dataStore.get(keyFactory.newKey(profileId));
        keyFactory = dataStore.newKeyFactory().setKind("UserCredentials");
        Entity userCredentials = dataStore.get(keyFactory.newKey(profileId));
       
        UserInfo userInfo = new UserInfo();

        userInfo.setFirstName(userProfile.getString("given_name"));
        userInfo.setLastName(userProfile.getString("family_name"));
        userInfo.setUserName(userProfile.getString("name"));
        userInfo.setProfileId(userProfile.getString("id"));
        userInfo.setAccess_token(userCredentials.getString("access_token"));

        String returnJson = new Gson().toJson(userInfo);
       
        return returnJson;
    }
}