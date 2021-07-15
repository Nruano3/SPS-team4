package com.google.sps.servlets;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

import com.google.sps.util.CredentialManager;

import com.google.sps.util.OAuth2Credentials;

@WebServlet("/Verify")
public class IDVerifyer extends HttpServlet {
 
    private static final long serialVersionUID = -1902833343487881018L;
    private static final String CREDENTIALS_PATH = "client_secret.json";
    private static OAuth2Credentials APP_CREDENTIALS;

    @Override
    public void init() {
        try {
            APP_CREDENTIALS = CredentialManager.setCredentials(CREDENTIALS_PATH);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
              
        String id_token = req.getParameter("id_token");
        if (id_token.equals("undefined")){
            res.setContentType("application/json;");  
            res.getWriter().println("{\"verified\": false }");
            return;
        }
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(),
                JacksonFactory.getDefaultInstance())
                        .setAudience(Collections.singletonList(APP_CREDENTIALS.getClient_id()))
                        .build();
       
        try {
           GoogleIdToken idToken = verifier.verify(id_token);
           String value = "{\"verified\": ";
            if(idToken != null){
                value += "true";
            }else{
               value += "false";
            }
            
            value += "}";
            
            res.setContentType("application/json;");           
            res.getWriter().println(value);
        } catch (GeneralSecurityException e) {
            
            e.printStackTrace();
        }
       
    }
}