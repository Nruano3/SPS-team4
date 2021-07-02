package com.google.sps.servlets;

import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.sps.util.CredentialManager;
import com.google.sps.util.DatastoreModule;
import com.google.sps.util.OAuth2Credentials;;

@WebServlet("/UserInfo")
public class UserInfoServlet extends HttpServlet {

    public static OAuth2Credentials APP_CREDENTIALS;
    private static final String CREDENTIALS_PATH = "client_secret.json";


    @Override
    public void init(){
        try {
            setAppCredentials();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        
        String uuid = req.getSession().getAttribute("userId").toString();
        if(uuid != null){
            
            String userInfoJson = DatastoreModule.getUserProfileData(uuid);

            res.setContentType("application/json;");
            res.getWriter().write(userInfoJson);
        }
    }

    private static void setAppCredentials() throws IOException {
        APP_CREDENTIALS = CredentialManager.setCredentials(CREDENTIALS_PATH);
    }
}