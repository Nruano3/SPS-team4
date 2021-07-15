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
import org.apache.http.ParseException;


public class DatastoreModule {

    private static Datastore dataStore;
    private static KeyFactory keyFactory;
    private static final String CREDENTIALS_PATH = "client_secret.json";
    private static String REFRESH_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static OAuth2Credentials APP_CREDENTIALS;
    private static CloseableHttpClient HTTP_CLIENT;

    public static void init() throws IOException {
        if (dataStore == null) {
            // Init the app credentials
            APP_CREDENTIALS = CredentialManager.setCredentials(CREDENTIALS_PATH);
            // Connect to the apps Datastore
            dataStore = DatastoreOptions.getDefaultInstance().getService();
            // Create a keyFactory instance
            keyFactory = dataStore.newKeyFactory();
            // Build an HTTP connection (only need one)
            HTTP_CLIENT = HttpClientBuilder.create().build();
        }
    }

    /* Main Functionality */
    public static void storeUserInfo(JsonObject userInfo) {

        /**
         * userInfo json contains the following:
         * 
         * "id": "54135165135843153543", "name": "Ryan Anderson", "given_name": "Ryan",
         * "family_name": "Anderson", "picture":
         * "https://lh3.googleusercontent.com/a/AATXAJwV1nPAUtdTZr-7qwfxMt6f1Ih9GJZ2Y-SR4cWt=s96-c",
         * "locale": "en" Storing data in dataStore: Create key factory Create Entity
         * with all required fields Add entity to the store Note: Elements from Json
         * objects may get returned with Quotes "", scrub them of the quotes if needed
         */

        keyFactory = dataStore.newKeyFactory().setKind("UserProfile");
        if (dataStore.get(keyFactory.newKey(stripQuotes(userInfo.get("id").toString()))) == null) {
            Entity userEntity;
            if (userInfo.has("email")) {
               userEntity = Entity.newBuilder(keyFactory.newKey(stripQuotes(userInfo.get("id").toString())))
                        .set("id", stripQuotes(userInfo.get("id").toString()))
                        .set("name", stripQuotes(userInfo.get("name").toString()))
                        .set("given_name", stripQuotes(userInfo.get("given_name").toString()))
                        .set("family_name", stripQuotes(userInfo.get("family_name").toString()))
                        .set("picture", stripQuotes(userInfo.get("picture").toString()))
                        .set("locale", stripQuotes(userInfo.get("locale").toString()))
                        .set("email", stripQuotes(userInfo.get("email").toString())).build();
            } else {
                userEntity = Entity.newBuilder(keyFactory.newKey(stripQuotes(userInfo.get("id").toString())))
                        .set("id", stripQuotes(userInfo.get("id").toString()))
                        .set("name", stripQuotes(userInfo.get("name").toString()))
                        .set("given_name", stripQuotes(userInfo.get("given_name").toString()))
                        .set("family_name", stripQuotes(userInfo.get("family_name").toString()))
                        .set("picture", stripQuotes(userInfo.get("picture").toString()))
                        .set("locale", stripQuotes(userInfo.get("locale").toString())).build();
            }
            dataStore.add(userEntity);
        }

    }

    public static void storeUserCredentials(JsonObject userCredentials, String profileId) {

        /**
         * userCredential contains: { "access_token": "access_token" "refresh_token":
         * "refresh_token" "scope": "scopes" "token_type": "token_type" "expires_in":
         * "expiration in seconds" }
         */

        keyFactory = dataStore.newKeyFactory().setKind("UserCredentials");
        long timeStampInSeconds = System.currentTimeMillis() / 1000;
        if (dataStore.get(keyFactory.newKey(profileId)) == null) {
            Entity credentialEntity = Entity.newBuilder(keyFactory.newKey(profileId))
                    .set("timestamp", timeStampInSeconds).set("id", profileId)
                    .set("access_token", stripQuotes(userCredentials.get("access_token").toString()))
                    .set("refresh_token",stripQuotes(userCredentials.get("refresh_token").toString()))
                    .set("scope", stripQuotes(userCredentials.get("scope").toString()))
                    .set("token_type",stripQuotes(userCredentials.get("token_type").toString()))
                    .set("expires_in",stripQuotes(userCredentials.get("expires_in").toString()))
                    .set("id_token", stripQuotes(userCredentials.get("id_token").toString())).build();

            dataStore.add(credentialEntity);
        }

    }

    /* Auxiliary Functionality: Access Tokens */

    public static boolean isUserInDatastore(String userId) {
        keyFactory = dataStore.newKeyFactory().setKind("UserProfile");
        Key userIdKey = keyFactory.newKey(userId);
        return (dataStore.get(userIdKey) != null);
    }

    public static String getUserAccessTokenWithEmail(String email) throws ParseException, IOException {

        // Build Query for UserProfile using email as filter
        Query<Entity> query = Query.newEntityQueryBuilder().setKind("UserProfile")
                .setFilter(PropertyFilter.eq("email", email)).build();
        // Run Query
        QueryResults<Entity> profile = dataStore.run(query);

        // If there is a result, use getUserAccessTokenWithId to return result
        if (profile.hasNext()) {
            Entity profileEntity = profile.next();
            String profileId = profileEntity.getString("id");
            return getUserAccessTokenWithId(profileId.toString());
        } else {
            return null;
        }
    }

    public static String getUserAccessTokenWithId(String profileId) throws ParseException, IOException {

        // Set key factory for credentials
        keyFactory = dataStore.newKeyFactory().setKind("UserCredentials");
        // Get key based on id
        Key profileKey = keyFactory.newKey(profileId);

        // Retrieve Credential entity based on key
        Entity userCredentials = dataStore.get(profileKey);

        // Ensure credentials are valid
        validateUserCredentials(userCredentials);

        // Return the access token
        return userCredentials.getString("access_token");

    }

    public static String getUserProfileData(String profileId) {

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

    /* Helper functions */

    public static String stripQuotes(String input) {
        return input.replaceAll("\"", "");
    }

    // In one line, ensures that credentials are valid
    public static void validateUserCredentials(Entity userCredentials) throws ParseException, IOException {

        if (needsToBeRefreshed(userCredentials))
            refreshUserCredentials(userCredentials);

    }

    public static Boolean needsToBeRefreshed(Entity userCredentials) {

        // expires_in is in seconds, get current time in seconds too
        long currentTimeInSeconds = System.currentTimeMillis() / 1000;

        // Get Creds Expiration (in seconds)
        long expires_in = Long.parseLong(userCredentials.getString("expires_in"));

        // Get time stamp
        long credentialTimeStamp = userCredentials.getLong("timestamp");

        // Determine how long (in seconds) its been since storage of access_token
        long currentLifeSpanOfCredential = currentTimeInSeconds - credentialTimeStamp;

        // If determined older than "expires_in" return true
        return currentLifeSpanOfCredential >= expires_in;
    }

    public static void refreshUserCredentials(Entity userCredentials) throws ParseException, IOException {

        // Build the refresh request
        String refresh_request_url = buildRefreshRequest(stripQuotes(userCredentials.getString("refresh_token")));

        // Execute the Refresh request
        String response = execute(refresh_request_url);

        // Modify the result into something usable (ie: JsonObject)
        JsonObject responseJson = new Gson().fromJson(response, JsonObject.class);

        // Extract usable parts (separeted for readability)
        String newAccessToken = stripQuotes(responseJson.get("access_token").toString());
        String newExpiresIn = stripQuotes(responseJson.get("expires_in").toString());
        String newIdToken = stripQuotes(responseJson.get("id_token").toString());
        String newScope = stripQuotes(responseJson.get("scope").toString());
        long newTimeStampInSeconds = System.currentTimeMillis() / 1000;

        // Build a new Entity based off the Old Entity
        Entity newCreds = Entity.newBuilder(userCredentials).set("access_token", newAccessToken)
                .set("timestamp", newTimeStampInSeconds).set("expires_in", newExpiresIn).set("id_token", newIdToken)
                .set("scope", newScope).build();

        // Update the entity
        dataStore.update(newCreds);

    }

    public static String buildRefreshRequest(String refreshToken){

		StringBuilder sb = new StringBuilder().append(REFRESH_TOKEN_URL).append("?")
                                              .append("&client_id=").append(APP_CREDENTIALS.getClient_id())
                                              .append("&client_secret=").append(APP_CREDENTIALS.getClient_secret())
                                              .append("&refresh_token=").append(refreshToken)
                                              .append("&grant_type=refresh_token");
        return sb.toString();

	}

    public static String execute(String requestUrl) throws ParseException, IOException {

		//Build Post Request
		HttpPost request = new HttpPost(requestUrl);

		 //Execute the request
        HttpResponse response = HTTP_CLIENT.execute(request);
        
        //Get response entity
        HttpEntity entity = response.getEntity();

        // Stringify the entity into a Json String
        return EntityUtils.toString(entity);

	}
}
