package com.google.sps.servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.gson.Gson;
import com.google.sps.util.CredentialManager;
import com.google.sps.util.DatastoreModule;
import com.google.sps.util.OAuth2Credentials;

@WebServlet("/process-user-data")
public class AutoEventProcessor extends HttpServlet {

    private static final String APPLICATION_NAME = "Meeting Manager";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
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
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {

       

        /**
         *   1. Get Users
         *   2. Get User's Calendars
         *   3. Process Calendars "Free" vs "Busy" Time
         *   4. Turn results into a json
         *   5. Return list of times: retJson = [{"Day": dayNumber, "Hour": hourNumber, "Minute": minuteNumber} ...]
         */

         //Step 1
         String[] users = getUsers(req);
        
        int startConstraint = 0;// Integer.parseInt(req.getParameter("start"));
        int endConstraint = 0;// Integer.parseInt(req.getParameter("end"));
         

         //Step 2
         List<Calendar>  calendars = getUsersCalendars(users);

         //Step 3
         int[] eventTimes = processCalendars(calendars, startConstraint, endConstraint);
         /**
          * int[] eventTimes = processCalendars(calendars, startConstraint, endConstraint)
          * processCalendars will populate a 2D array typically 7x24 to represent all the 
          * hours in the week, but will be modified by the startConstraint and endConstraint
          * , each event populates a coordinate [day][hour], by incrementing its value by 1
          * for each event per calendar that is taking up that time slot.

          * Once all calendars are processed, the 2D array will be "flattened" into a single 
          * dimmensional array, and sorted via a modified Counting Sort that populates the 
          * return array with the indexes of all the days/hours in order from "Most Available"
          * to "Least Available". A lesser available time has a higher value stored at its 
          * coordinates from the above step, the most available times will have 0's stored
          * in them.

          * Finally the returned array will be trimmed of any entry times where less
          * than 2 people are available
          */

          //Step 4
          String eventTimesJson = convertToJson(eventTimes, startConstraint, endConstraint);

          res.setContentType("application/json;");
          res.getWriter().println(eventTimesJson);

          //Allows the thing to send "OK" back
          doGet(req, res);

    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res){

    }

    private static String convertToJson(int[] eventTimes, int startConstraint, int endConstraint){
        String returnJson = "{\"result\": true}";
        return returnJson;
    }
    private static int[] processCalendars(List<Calendar> calendars, int startConstraint, int endConstraint){
        return new int[10]; 
    }
    private static List<Calendar> getUsersCalendars(String[] users){
        List<Calendar> calendars = new ArrayList<Calendar>();
        return calendars;
    }


    private static String[] getUsers(HttpServletRequest req){

        StringBuffer jb = new StringBuffer();
        String line = null;
        try {
            BufferedReader reader = req.getReader();
            while ((line = reader.readLine()) != null)
            jb.append(line);
        } catch (Exception e) { /*report an error*/ }
       System.out.println(jb.toString());
       String[] users = new Gson().fromJson(jb.toString(), String[].class);
       for(String user : users) System.out.println(user);
       return users;
    }
}