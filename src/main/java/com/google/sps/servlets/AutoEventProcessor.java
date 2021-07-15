package com.google.sps.servlets;


import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
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

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.Calendar.Freebusy;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.sps.util.DatastoreModule;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.google.api.services.calendar.model.FreeBusyRequest;
import com.google.api.services.calendar.model.FreeBusyRequestItem;
import com.google.api.services.calendar.model.FreeBusyResponse;

@WebServlet("/process-user-data")
public class AutoEventProcessor extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String APPLICATION_NAME = "Meeting Manager";
    final private static JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static int retryCounter = 0;

    public void init() {
        try {
            DatastoreModule.init();
            DateTimeZone.setDefault(DateTimeZone.forID("America/Los_Angeles"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String stripQuotes(String input) {
        return input.replaceAll("\"", "");
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) {
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {

        res.setContentType("application/json;");

        // Users get sent as a JsonArray of Strings, use Gson Library to load them into
        // a string array
        String[] users = new Gson().fromJson(req.getParameter("userList"), String[].class);

        /* Scheduling Constraints */
        int startConstraint = Integer.parseInt(req.getParameter("startRes"));
        int endConstraint = Integer.parseInt(req.getParameter("endRes"));
        int meetingLength = Integer.parseInt(req.getParameter("meetingLength"));
        
        try {

            List<Calendar> calendars = getCalendars(users);

            int[][] freeBusyTimes = getFreeBusyTimes(calendars);
            
            String eventTimesJson = processFreeBusyTimes(freeBusyTimes, startConstraint, endConstraint, meetingLength);

            res.getWriter().println(eventTimesJson);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error Occured");
            res.getWriter().println("{\"error\": true}");
        }

        doGet(req, res);

    }

    // ====================================Get Calendars Start ========================================//
    private static List<Calendar> getCalendars(String[] users) throws org.apache.http.ParseException, IOException, GeneralSecurityException {

        List<Calendar> calendars = new ArrayList<Calendar>();
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Credential creds;
        TokenResponse tokenResponse = new TokenResponse();

        for (String user : users) {

            String access_token = DatastoreModule.getUserAccessTokenWithEmail(user);

            if (access_token == null || access_token.equals("")) {
                System.out.println("User: " + user + "\nIs not Registered");
            } 
            else {
                tokenResponse.setAccessToken(access_token);
                creds = buildCreds(HTTP_TRANSPORT, tokenResponse);
                try {
                    Calendar service = new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, creds)
                            .setApplicationName(APPLICATION_NAME).build();

                    calendars.add(service);

                } 
                catch (Exception e) {
                    /*Authorization fails the first time
                      usually corrected by resending an auth 
                      request*/
                    if(retryCounter < 5){
                        return getCalendars(users);
                    }
                    else{
                        e.printStackTrace();
                    }
                    
                    
                }
            }
        }

        return calendars;
    }

    private static Credential buildCreds(HttpTransport transport, TokenResponse tokenResponse) {
        return new Credential(BearerToken.authorizationHeaderAccessMethod()).setFromTokenResponse(tokenResponse);
    }
    // ====================================Get Calendars End========================================//

    // ===============================GetFreeBusyTimes Start ========================================//
    private static int[][] getFreeBusyTimes(List<Calendar> calendars) throws JsonSyntaxException, IOException {

        // 2D array representing Days and Hours
        int[][] times = new int[7][24];

        ArrayList<JsonObject> freeBusyTimesList = getTimes(calendars);

        for (JsonObject timeOb : freeBusyTimesList) {

            processEventTimes(times, timeOb);

        }
        return times;
    }

    private static ArrayList<JsonObject> getTimes(List<Calendar> calendars) throws JsonSyntaxException, IOException {

        ArrayList<JsonObject> times = new ArrayList<JsonObject>();
        for (Calendar calendar : calendars) {
            JsonArray busyTimes = getBusyTimes(calendar);

            for (int i = 0; i < busyTimes.size(); i++) {
                times.add((JsonObject) busyTimes.get(i));
            }
        }

        return times;
    }

    private static JsonArray getBusyTimes(Calendar calendar) throws JsonSyntaxException, IOException {
    	JsonObject json = new Gson().fromJson(buildAndExecuteFreeBusyRequest(calendar).toString(),
    											JsonObject.class);

    	return json.getAsJsonObject("calendars").getAsJsonObject("primary").getAsJsonArray("busy");
    }

    private static FreeBusyResponse buildAndExecuteFreeBusyRequest(Calendar calendar) throws IOException {
    	//Build request
        FreeBusyRequest req = buildFreeBusyRequest();
        //turn request into query
        Freebusy.Query query = calendar.freebusy().query(req);
        //execute query/request
        FreeBusyResponse res = query.execute();

        return res;
    }

    private static FreeBusyRequest buildFreeBusyRequest(){

        //Calculate a Week in milliseconds
        int milliseconds = 1000; int seconds = 60; int minutes = 60; int hours = 24; int weekDays = 7;
        long weekInMilliseconds = weekDays * hours * minutes * seconds * milliseconds;
        //Determine range between today, and a week from today
        Date today = new Date(System.currentTimeMillis());
        Date endOfWeek = new Date(System.currentTimeMillis() + weekInMilliseconds);
        //Cast to DateTime (Google DateTime, not Joda DateTime) objects for FreeBusyRequest
        com.google.api.client.util.DateTime startTime = new com.google.api.client.util.DateTime(today, TimeZone.getDefault());
        com.google.api.client.util.DateTime endTime = new com.google.api.client.util.DateTime(endOfWeek, TimeZone.getDefault());
       
        //Build request with beggining and end dates
        FreeBusyRequest req = new FreeBusyRequest();
        req.setTimeMin(startTime);
        req.setTimeMax(endTime);
        
        //Add the primary calendar to process
        List<FreeBusyRequestItem> list = new ArrayList<FreeBusyRequestItem>();
        list.add(new FreeBusyRequestItem().setId("primary"));
        req.setItems(list);
        //return request
        return req;
    }




    private static void processEventTimes(int[][] times, JsonObject timeOb){
    	//Time Zones are Default RN, will update that later
        DateTime startDate = processDate(timeOb, "start");
   		DateTime endDate = processDate(timeOb, "end");
    	
    	int startTime = startDate.getHourOfDay();
    	int startDay = startDate.getDayOfWeek();

    	int endTime = endDate.getHourOfDay();
    	int endDay = endDate.getDayOfWeek();
           
    	while((startTime!= endTime) || (startDay != endDay)){

    		times[startDay-1][startTime] += 1;
    		//time starts at 0, mod operation AFTER incrementing
    		startTime++;
    		startTime %= 24;
    		//Day starts at 1, mod operation BEFORE incrementing
    	   	if(startTime == 0){
                   startDay %= 7;
    	   	    startDay++;
            }
    	}

    }

    private static DateTime processDate(JsonObject timeOb, String time){
    	return new DateTime(stripQuotes(timeOb.get(time).toString())).withZone(DateTimeZone.forID("America/Los_Angeles"));
    }
//===============================GetFreeBusyTimes end ========================================//

//===============================processFreeBusyTimes Start ========================================//


   
    private static String  processFreeBusyTimes(int[][] freeBusyTimes, int startConstraint, int endConstraint, int meetingLength){

       	//Helper Class to Order Events
	    class Event implements Comparable<Event>{
            public int start;
            public int end;
            public int day;
            public int val;
            public int nonAttending;
            public DateTime startDate;
            public DateTime endDate;

		    Event(int start, int end, int day, int val, int nonAttending){
			    this.start = start;
			    this.end = end;
			    this.day = day + 1;
                this.val = val;
                this.nonAttending = nonAttending;
            
                this.startDate = DateTime.now();
                this.startDate = this.startDate.hourOfDay().setCopy(this.start);
                this.startDate = this.startDate.dayOfWeek().setCopy(this.day);
                this.startDate = this.startDate.withZone(DateTimeZone.forID("America/Los_Angeles"));
            
                this.endDate = DateTime.now();
                this.endDate = this.endDate.hourOfDay().setCopy(this.end);
                this.endDate = this.endDate.dayOfWeek().setCopy(this.day);
                this.endDate = this.endDate.withZone(DateTimeZone.forID("America/Los_Angeles"));
            
		    }
        
            @Override
            public int compareTo(Event o) {
           
                int res = this.val - o.val;
                if (res < 0)
                    return -1;
                else if (res > 0)
                    return 1;
                else
                    return 0;
            }       
	    }
        
        
        List<Event> events = new ArrayList<Event>();
        
		for(int day = 0; day < 7; day++){
            //Running Sum per event, init by adding values from each time index
            //this minimizes array access later
            int sum = 0;
            for(int startTime = startConstraint; startTime < (startConstraint+meetingLength); startTime++){
                sum += freeBusyTimes[day][startTime];
            }


			for(int start = startConstraint; start < endConstraint - meetingLength; start++){

                int nonAttendees = maxNonAttending(freeBusyTimes, day, start, meetingLength);
                events.add(new Event(start, start+meetingLength, day, sum, nonAttendees));
                //Reduce running sum of non-attendees by the time index at start
                sum -= freeBusyTimes[day][start];
                /*Increase running sum of non-attendees by the time index at the end of the 
                  time block (as long as its in range) */
                if(start + meetingLength < (endConstraint - meetingLength)){
                    sum += freeBusyTimes[day][start + meetingLength];
                }
			}
		}

		Collections.sort(events);
        
        for(Event e : events){
            System.out.println(
                "Day:   " + e.day
             +"\nStart: " + e.start
             +"\nEnd:   " + e.end
             +"\nNon-attendees: " + e.nonAttending 
             +"\nStartDate: " + e.startDate
             +"\nEndDate:   " + e.endDate + "\n");
        }

        
        
        System.out.println(DateTime.now());
		return "{\"result\": true}";
    }
    

    private static int maxNonAttending(int[][] events, int day, int startHour, int range){
        List<Integer> nums = new ArrayList<Integer>();
        for(int i = startHour; i < startHour +range; i++){
            nums.add(Integer.valueOf(events[day][i]));
        }
        Collections.sort(nums);
        return nums.get(nums.size()-1);

    }


//===============================processFreeBusyTimes End ========================================//
}