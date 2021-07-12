package com.google.sps.servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.Calendar.Freebusy;
import com.google.auth.oauth2.AccessToken;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.sps.util.DatastoreModule;

import com.google.api.services.calendar.model.FreeBusyRequest;
import com.google.api.services.calendar.model.FreeBusyRequestItem;
import com.google.api.services.calendar.model.FreeBusyResponse;

@WebServlet("/process-user-data")
public class AutoEventProcessor extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String APPLICATION_NAME = "Meeting Manager";
    final private static JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    @Override
    public void init() {
        try {
            connectToDatastore();
        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    private static void connectToDatastore() throws IOException {
        DatastoreModule.init();
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {

        /**
         * 1. Get Users 2. Get User's Calendars 3. Process Calendars "Free" vs "Busy"
         * Time 4. Turn results into a json 5. Return list of times: retJson = [{"Day":
         * dayNumber, "Hour": hourNumber, "Minute": minuteNumber} ...]
         */

        // Step 1
        String[] users = getUsers(req);

        int startConstraint = 0;// Integer.parseInt(req.getParameter("start"));
        int endConstraint = 0;// Integer.parseInt(req.getParameter("end"));

        try {
            // Step 2
            List<Calendar> calendars = getUsersCalendars(users);

            // Step 3
            int[] eventTimes = processCalendars(calendars, startConstraint, endConstraint);
            /**
             * int[] eventTimes = processCalendars(calendars, startConstraint,
             * endConstraint) processCalendars will populate a 2D array typically 7x24 to
             * represent all the hours in the week, but will be modified by the
             * startConstraint and endConstraint , each event populates a coordinate
             * [day][hour], by incrementing its value by 1 for each event per calendar that
             * is taking up that time slot.
             * 
             * Once all calendars are processed, the 2D array will be "flattened" into a
             * single dimmensional array, and sorted via a modified Counting Sort that
             * populates the return array with the indexes of all the days/hours in order
             * from "Most Available" to "Least Available". A lesser available time has a
             * higher value stored at its coordinates from the above step, the most
             * available times will have 0's stored in them.
             * 
             * Finally the returned array will be trimmed of any entry times where less than
             * 2 people are available
             */

            // Step 4
            String eventTimesJson = convertToJson(eventTimes, startConstraint, endConstraint);

            res.setContentType("application/json;");
            res.getWriter().println(eventTimesJson);

            // Allows the thing to send "OK" back
            doGet(req, res);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override // Use to send "OK" response to requester
    public void doGet(HttpServletRequest req, HttpServletResponse res) {
    }

    private static String convertToJson(int[] eventTimes, int startConstraint, int endConstraint) {
        String returnJson = "{\"result\": true}";
        return returnJson;
    }

    private static int[] processCalendars(List<Calendar> calendars, int startConstraint, int endConstraint)
            throws IOException, ParseException {

        int daysOfWeek = 7;
        int hoursOfDay = 24;
        int availableHours = endConstraint - startConstraint;

        int[][] eventTimes = new int[daysOfWeek][availableHours];

        for (Calendar service : calendars) {

            JsonArray busyTimes = getFreeBusyTimes(service);

            ArrayList<JsonObject> times = new ArrayList<JsonObject>();
            for(int i = 0; i < busyTimes.size(); i++){
                times.add((JsonObject) busyTimes.get(i));
            }

            for(JsonObject ob : times){
                System.out.println(ob.get("end").toString());
            }
            //Lists all Calendars Connected to given calenar service
             /*   String pageToken = null;
                do {
                    CalendarList calendarList = service.calendarList().list()
                            .setPageToken(pageToken).execute();

                    List<CalendarListEntry> items = calendarList.getItems();

                    for (CalendarListEntry calendarListEntry : items) {
                        System.out.println(calendarListEntry.getSummary());
                    }
                    pageToken = calendarList.getNextPageToken();
                } while (pageToken != null);*/
        }
        return new int[10];
    }

    private static JsonArray getFreeBusyTimes(Calendar service) throws IOException {

        FreeBusyRequest req = buildFreeBusyRequest();

        Freebusy.Query query = service.freebusy().query(req);
           
        FreeBusyResponse res = query.execute();

        JsonObject json = new Gson().fromJson(res.toString(), JsonObject.class);

        return json.getAsJsonObject("calendars").getAsJsonObject("primary").getAsJsonArray("busy");
    }

    private static FreeBusyRequest buildFreeBusyRequest(){
        int milliseconds = 1000; int seconds = 60; int minutes = 60; int hours = 24; int weekDays = 7;
        long weekInMilliseconds = weekDays * hours * minutes * seconds * milliseconds;
        Date today = new Date(System.currentTimeMillis());
        Date endOfWeek = new Date(System.currentTimeMillis() + weekInMilliseconds);
           
        DateTime startTime = new DateTime(today, TimeZone.getDefault());
        DateTime endTime = new DateTime(endOfWeek, TimeZone.getDefault());


        FreeBusyRequest req = new FreeBusyRequest();
        req.setTimeMin(startTime);
        req.setTimeMax(endTime);
        
        List<FreeBusyRequestItem> list = new ArrayList<FreeBusyRequestItem>();
        list.add(new FreeBusyRequestItem().setId("primary"));
        req.setItems(list);

        return req;
    }

    private static List<Calendar> getUsersCalendars(String[] users) throws GeneralSecurityException, IOException {

        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Credential creds;
        TokenResponse tokenResponse = new TokenResponse();
        List<Calendar> calendars = new ArrayList<Calendar>();

        for (String userEmail : users) {

            AccessToken access_token = DatastoreModule.getUserAccessTokenWithEmail(userEmail);
            
            if (access_token != null) {
                System.out.println("User: " + userEmail + "\nAccess Token: " + access_token.getTokenValue());
                tokenResponse.setAccessToken(access_token.getTokenValue());
                
                creds = createCredentialWithAccessTokenOnly(HTTP_TRANSPORT, JSON_FACTORY, tokenResponse);
                Calendar service = new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, creds)
                        .setApplicationName(APPLICATION_NAME).build();

                calendars.add(service);
            }
            else 
                System.out.println("User: " + userEmail + "\nNot Authorized/Registered");
        }        
        return calendars;
    }

    public static Credential createCredentialWithAccessTokenOnly(
      HttpTransport transport, JsonFactory jsonFactory, TokenResponse tokenResponse) {
            return new Credential(BearerToken.authorizationHeaderAccessMethod()).setFromTokenResponse(
                        tokenResponse);
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