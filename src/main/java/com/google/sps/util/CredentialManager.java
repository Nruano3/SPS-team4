package com.google.sps.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.sps.util.OAuth2Credentials;

public class CredentialManager {

    /**
     * 
     * @param filePath
     * 
     * 
     *  Load an OAuth2Credentials Objects with the contents of the
     *  Json specified in filePath
     * 
     * Utilizes the Json class specified in the same package
     * @throws IOException
     */
    public static OAuth2Credentials setCredentials(String filePath) throws IOException {
        
        String fileContents = getFileContents(getBufferedReader(filePath));

        JsonNode cred = Json.parse(fileContents);

        return Json.fromJson(cred, OAuth2Credentials.class);
    }

    //Returns a BufferedReader conected to the file specified by filePath
    private static BufferedReader getBufferedReader(String filePath){

        InputStream inStream = OAuth2Credentials.class.getClassLoader().getResourceAsStream(filePath);
       
        InputStreamReader iReader = new InputStreamReader(inStream);            
            
        return new BufferedReader(iReader);
        
    }

    //Returns file content as a string
    private static String getFileContents(BufferedReader reader) throws IOException {
        StringBuffer sb = new StringBuffer();
        String inputString;
        
        while( (inputString = reader.readLine()) != null ){
           sb.append(inputString);
        }

        return sb.toString();
    }
}