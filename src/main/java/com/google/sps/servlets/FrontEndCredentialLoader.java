package com.google.sps.servlets;

import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.sps.util.CredentialManager;
import com.google.sps.util.OAuth2Credentials;

@WebServlet("/getCredentials")
public class FrontEndCredentialLoader extends HttpServlet {

    
    private static final long serialVersionUID = -753371672647628426L;
    private static final String CREDENTIALS_PATH = "client_secret.json";
    private static OAuth2Credentials APP_CREDENTIALS;

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {

         APP_CREDENTIALS = CredentialManager.setCredentials(CREDENTIALS_PATH);
         String API_KEY = System.getenv("GOOGLE_API_KEY");

         StringBuilder sb = new StringBuilder().append("{\n")
                                               .append("\"apiKey\":").append("\"" + API_KEY + "\"").append(",\n")
                                               .append("\"client_id\":").append("\"" + APP_CREDENTIALS.getClient_id() + "\"")
                                               .append("\n}");
        String retJson = sb.toString();

        
        res.setContentType("application/json;");
        
        res.getWriter().println(retJson);
        

    }
}