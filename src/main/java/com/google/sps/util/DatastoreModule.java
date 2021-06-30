package com.google.sps.util;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.Key;
import com.google.gson.JsonObject;

public class DatastoreModule {

    private static Datastore dataStore;
    private static final String quotes = "\"";
    

    public static void init(){

        dataStore = DatastoreOptions.getDefaultInstance().getService();
    }



    public static boolean isUserInDatastore(String userId){
        KeyFactory keyFactory = dataStore.newKeyFactory().setKind("UserProfile");
        Key userIdKey = keyFactory.newKey(userId);
        return (dataStore.get(userIdKey) != null);
    }

    

    public static void storeUserInfo(JsonObject userInfo){
         /**
         * userInfo json contains the following:
         * 
        *  "id": "113591704021226668726",
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

        KeyFactory keyFactory = dataStore.newKeyFactory().setKind("UserProfile");

        Entity userEntity = Entity.newBuilder(keyFactory.newKey(userInfo.get("id").toString().replaceAll(quotes,"")))
                                      .set("id", userInfo.get("id").toString().replaceAll(quotes,""))
                                      .set("name", userInfo.get("name").toString().replaceAll(quotes,""))
                                      .set("given_name", userInfo.get("given_name").toString().replaceAll(quotes,""))
                                      .set("family_name", userInfo.get("family_name").toString().replaceAll(quotes,""))
                                      .set("picture", userInfo.get("picture").toString().replaceAll(quotes,""))
                                      .build();
        dataStore.add(userEntity);
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
        KeyFactory keyFactory = dataStore.newKeyFactory().setKind("UserCredentials");
        long timeStampInSeconds = System.currentTimeMillis() /1000;

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

    public static void updateData(String userId, String field, String data){
        //TODO update user information
    }

    public static void updateData(String userId, JsonObject data){
        //TODO update user information
    }
}