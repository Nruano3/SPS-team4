package com.google.sps.servlets;

import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


import com.google.sps.util.CredentialManager;
import com.google.sps.util.OAuth2Credentials;
import com.google.api.services.calendar.CalendarScopes;

/**
 * UserLoginServlet: This servlet is designed as the entry point to the
 * Login/Authorization Flow of this app.
 * 
 * 
 * Initializing this Servlet instantiates a Datastore connection and Loads the
 * apps Credentials.
 * 
 * Upon reaching this servlet from the Webpage, the function service() checks
 * whether a user is currently logged in by checking the current Session object
 * for an active user. If no user was determined, then the Google authorization
 * URL is build and the request is executed via response.sendRedirect() call.
 * 
 * Authorization_URL is built using this apps credentials registered on the
 * cloud console, and are accessible from the resources file
 * 
 */
@WebServlet("/Login")
public class UserLoginServlet extends HttpServlet {

  
    private static final long serialVersionUID = -99178872005473038L;
    private static final String CREDENTIALS_PATH = "client_secret.json";
    private static String AUTH_URL;
    private static OAuth2Credentials APP_CREDENTIALS;
    // TESTING and PRODUCTION are used to distiguish the redirect URLs found in the
    // APP_CREDENTIALS object
    private static int TESTING = 0;
    //private static int PRODUCTION = 1;
    private static HttpSession session;

    /**
     * initialize  AppCredentials and Authorization URL
     */
    @Override
    public void init(){
        try{
            setAppCredentials();
            buildAuthorizationURL();
            
        }catch (IOException e){
            System.out.println("Error Loading Credentials");

        }
    }

    @Override 
    public void service(HttpServletRequest req, HttpServletResponse res) throws IOException {
        session = req.getSession();
        
        //check to see if a valid user is already logged in
        if(session.getAttribute("userId") != null){
            //assuming main content page is "Welcome.html"
            res.sendRedirect(req.getContextPath() + "/index.html");
            return;
        }   

        //If no user is active, send redirect to authorization page
        res.sendRedirect(AUTH_URL);
        

    }






    //============================Initialization Helper Functions==================================//
    private static void setAppCredentials() throws IOException {
        APP_CREDENTIALS = CredentialManager.setCredentials(CREDENTIALS_PATH);
    }

    private static void buildAuthorizationURL(){

        /**
         * Authorization_URL requires at minimum:
         *      client id: identifies the app
         *      response type: the type response we are expecting
         *      redirect uri: where the authorization page redirects to
         *      
         *  Optional:   
         *      scope: the permissions requested by the app (not really optional, but still works without it)
         *      State: helps verify correct traffic
         *      access type: allows app to store access code and use later with a refresh token
         *      approval prompt: determines how the approval is processed (automatically or forced)
         */
        AUTH_URL = new StringBuilder()
                .append(APP_CREDENTIALS.getAuth_uri()).append("?")
                .append("&client_id=").append(APP_CREDENTIALS.getClient_id())
                .append("&response_type=code")
                .append("&redirect_uri=").append(APP_CREDENTIALS.getRedirect_uris()[TESTING])
                .append("&scope=").append(CalendarScopes.CALENDAR_EVENTS + " " + CalendarScopes.CALENDAR + " https://www.googleapis.com/auth/userinfo.profile")
                .append("&state=adminLogin59") //used for verifying correct traffic
                .append("&access_type=offline")
                .append("&approval_prompt=force")
                .toString();

    }
    
}