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
import com.google.api.services.calendar.model.FreeBusyRequest;
import com.google.api.services.calendar.model.FreeBusyRequestItem;
import com.google.api.services.calendar.model.FreeBusyResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.sps.util.DatastoreModule;
import com.google.sps.util.Event;

import org.apache.http.ParseException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * Class: MeetingTimeManager
 * Description:
 * This Servlet processes: List of user email addresses,
 *                         Meeting Constraints: start
 *                                              end
 *                                              length
 * It uses this information to determine a list of meeting 
 * times that will allow a majority of invitees to attend
 * that given event, based on their availability from 
 * their "Primary" google calendar.
 */
@WebServlet("/process-user-data")
public class MeetingTimeManager extends HttpServlet {

    private static final long serialVersionUID = -1058001050954484395L;

    private static final int DAYS_OF_WEEK = 7;
    private static final int HOURS_OF_DAY = 24;
    private static final String APPLICATION_NAME = "Meeting Manager";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static String[] users;
    private static int numberOfInvitees;
    private static int startConstraint;
    private static int endConstraint;
    private static int meetingLength;
    private static List<Calendar> calendars;
    private static TokenResponse tokenResponse;
    private static List<JsonObject> freeBusyResponseTimes;
    private static int[][] eventMatrix;
    private static List<Event> events;
    private static int retryCounter = 0;
    private static final String error = "{\"error\": true}";

    @Override
    public void init() {
        try {
            // Ensure the Datastore is up and running
            DatastoreModule.init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Empty doGet method, used to return a 200 OK status to requester without
    // redirect
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) {
    }

    // Helper function to strip any Strings of un-needed quotes
    public static String stripQuotes(String input) {
        return input.replaceAll("\"", "");
    }

    
    /**
         * Event Processing Algorithm:
         * 
         * 1. Determine users by email and retrieve all scheduling constraints
         * 
         * 2. Get a "Calendar Service" for each user registered in the Datastore
         * 
         * 3. Retrieve FreeBusy response, a FreeBusy Response contains a list, inside a
         * Json, that describes the times a user has an event scheduled. This portion
         * currently processes ONLY the users primary calendar
         * 
         * 4. Translate results into the 2D matrix, 'eventMatrix', for simpler
         * processing
         * 
         * 5. Scan over eventMatrix (limited to user defined time restrictions) and
         * create and Event (com.google.sps.servlets.util.Event) based on the
         * information from the eventMatrix
         * 
         * 6. Add the event to an ArrayList of Events, and repeat until eventMatrix is
         * fully processed
         * 
         * 7. Sort the ArrayList
         * 
         * 8. Using Gson library, convert ArrayList into a Stringified Json
         * 
         * 9. Return the Json string
         * 
         * 
         * NOTE: Timezones will later be determined by the User calendar default
         * timezone. Event's where less than 50% of invitees are able to attend will be
         * dropped from result.
         */

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        
        // Set response content type: Json
        res.setContentType("application/json;");

        /** Algorithm */
        try {

            // Step 1.

            /** Users get sent as a JsonArray of Strings, 
             * use Gson Library to load them into
             * a string array */
            users = new Gson().fromJson(req.getParameter("userList"), String[].class);
            numberOfInvitees = users.length;

            /* Scheduling Constraints */
            startConstraint = Integer.parseInt(req.getParameter("startRes"));
            endConstraint = Integer.parseInt(req.getParameter("endRes"));
            meetingLength = Integer.parseInt(req.getParameter("meetingLength"));

            // Step 2.
            getCalendars();

            // Step 3.
            getFreeBusyTimes();

            //Step 4.
            processFreeBusyTimes();

            //Step 5/6.
            processEventMatrix();

            //Step 7.
            Collections.sort(events);

            //Step 8.
            Gson gson = new GsonBuilder().setPrettyPrinting().create(); //Makes the Json String readable in Terminal
            String returnJson = gson.toJson(events);

            //Step 9.
            res.getWriter().println(returnJson);


        } catch (ParseException | GeneralSecurityException e) {

            e.printStackTrace();
            res.getWriter().println(error);
        }

        //Redirect to doGet to return 200 OK response
        doGet(req, res);
    }

    /**
     * 
     * @throws ParseException
     * @throws IOException
     * @throws GeneralSecurityException
     * 
     * Adds A freshly created Google Calendar Service to static "calendars" ArrayList object
     * for each registered user
     */
    private static void getCalendars() throws ParseException, IOException, GeneralSecurityException {
        // Init the ArrayList of Calendars
        calendars = new ArrayList<Calendar>();

        // Create a single HttpTransport object for Building Calendar Services
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        // Access token for building backend Calendar "service"
        String access_token;

        for (String userEmail : users) {
            // Retrieve User Access Token (This method ensures access tokens are valid, but
            // Google's auth servers may lag a bit causing an exeception below)
            access_token = DatastoreModule.getUserAccessTokenWithEmail(userEmail);

            // If access_token does not exist, this means user is not register with app
            if (access_token == null || access_token.equals("")) {
                System.out.println("User: " + userEmail + " is not Registered");
            } else {

                try {

                    // get Calendar service, NOTE: Building Calendar Services sometimes fails, may
                    // need to be retried
                    // due to "User not Authorized" exception when googles Auth servers don't update
                    // in time
                    Calendar service = getCalendarService(access_token, HTTP_TRANSPORT);

                    // Add calendar to calendar list
                    calendars.add(service);

                } catch (Exception e) {
                    // Allowed Retrys per run is set to number of invitees to prevent any inifinite
                    // loops
                    if (retryCounter < numberOfInvitees) {
                        retryCounter++;
                        getCalendars();
                    } else {
                        e.printStackTrace();
                    }
                } finally {
                    // reset retry counter upon success
                    retryCounter = 0;
                }

            }

        }
    }

    /**
     * 
     * @param access_token
     * @param HTTP_TRANSPORT
     * @return com.google.api.services.calendar.Calendar
     * @throws GeneralSecurityException
     * @throws IOException
     * 
     * Creates a fresh Google Calendar "service" Object, for user defined by the passed
     * access token and returns it
     */
    private static Calendar getCalendarService(String access_token, HttpTransport HTTP_TRANSPORT)
            throws GeneralSecurityException, IOException {

        /**
         * To create a Calendar service, a call to Googles Calendar.Builder method is
         * made using each of the: HttpTransport Object JsonFactory Object Credential
         * Object
         * 
         * Credential Objects are built using an HttpTransport Object and a
         * TokenResponse object
         */

        // Init global tokenResponse and set access token
        tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken(access_token);

        // Build Credentials
        Credential cred = buildCreds(HTTP_TRANSPORT);

        // Build and return calendar service
        return new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, cred).setApplicationName(APPLICATION_NAME).build();

    }

    /**
     * Builds Credential object for Calendar Service Builder
     */
    private static Credential buildCreds(HttpTransport transport) {
        return new Credential(BearerToken.authorizationHeaderAccessMethod()).setFromTokenResponse(tokenResponse);
    }

    /**
     * 
     * @throws JsonSyntaxException
     * @throws IOException
     * 
     * Populates the Global freeBusyResponseTimes ArrayList with 
     * parsed Google Calendar FreeBusyResponse values
     */
    private static void getFreeBusyTimes() throws JsonSyntaxException, IOException {

        // Init the Free/Busy Array List
        freeBusyResponseTimes = new ArrayList<JsonObject>();

        // Holder for FreeBusy response, it returns a Json, which contains a JsonArray
        JsonArray times;

        // For each calendar service, parse the free busy times, and add them to the
        // freeBusyResponseTimes arraylist
        for (Calendar service : calendars) {

            times = parseFreeBusyTimes(service);

            for (int i = 0; i < times.size(); i++) {
                freeBusyResponseTimes.add((JsonObject) times.get(i));
            }

        }

        //Free up space incase garbage collection fails
        times = null;
    }

    /**
     * 
     * @param service
     * @return com.google.gson.JsonArray
     * @throws JsonSyntaxException
     * @throws IOException
     * 
     * Returns a Parsed Google Calendar FreeBusyResponse as a JsonArray. Picks out ONLY
     * the Array labeled "busy" in the response
     */
    private static JsonArray parseFreeBusyTimes(Calendar service) throws JsonSyntaxException, IOException {

        //Build and Execute a Free Busy request
        JsonObject json = new Gson().fromJson(buildAndExecuteFreeBusyRequest(service).toString(),
    											JsonObject.class);
        //Return the desired portion of the result
        return json.getAsJsonObject("calendars").getAsJsonObject("primary").getAsJsonArray("busy");
        
    }

    /**
     * 
     * @param service
     * @return com.google.api.services.calendar.model.FreeBusyResponse
     * @throws IOException
     * 
     * Builds and Executes a Google Calendar FreeBusyRequest. Returns the response.
     */
    private static FreeBusyResponse buildAndExecuteFreeBusyRequest(Calendar service) throws IOException {
    	//Build request
        FreeBusyRequest req = buildFreeBusyRequest();
        //turn request into query
        Freebusy.Query query = service.freebusy().query(req);
        //execute query/request
        FreeBusyResponse res = query.execute();

        return res;
    }

    /**
     * 
     * @return om.google.api.services.calendar.model.FreeBusyRequest
     * 
     * Builds and returns a Google Calendar FreeBusyRequest for 
     * a single Calendar Week starting from time of Request, currently only 
     * requests for a user's "Primary" calendar
     */
    private static FreeBusyRequest buildFreeBusyRequest(){

        //Calculate a Week in milliseconds NOTE: these could be global, but for readability they are calculated here
        int milliseconds = 1000; int seconds = 60; int minutes = 60; int hours = 24; int weekDays = 7;
        long weekInMilliseconds = weekDays * hours * minutes * seconds * milliseconds;

        //Determine range between today, and a week from today
        Date today = new Date(System.currentTimeMillis());
        Date endOfWeek = new Date(System.currentTimeMillis() + weekInMilliseconds);
        
        //Cast to DateTime (Google DateTime, not Joda DateTime) objects for FreeBusyRequest
        com.google.api.client.util.DateTime startTime = new com.google.api.client.util.DateTime(today, TimeZone.getDefault());
        com.google.api.client.util.DateTime endTime = new com.google.api.client.util.DateTime(endOfWeek, TimeZone.getDefault());
       
        //Build request with beginning and end dates
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

    /**
     * Proccess all the "Busy" times in the FreeBusy response parsed into the 
     * global freeBusyResponseTimes ArrayList
     */
    private static void processFreeBusyTimes(){

        //Init the eventMatrix to resemble a "Week" with 24 hour "Days"
        eventMatrix = new int[DAYS_OF_WEEK][HOURS_OF_DAY];

        for(JsonObject timeDate : freeBusyResponseTimes){
            //For each timeDate object, process and populate eventMatrix accordingly
            processTimeDateObject(timeDate);
        }
    }

    /**
     * 
     * @param timeDate
     * 
     * Proccess the TimeDate object passed in, by extracting
     * the Start day and time, as well as the End day and time
     * and populates the Global eventMatrix
     */
    private static void processTimeDateObject(JsonObject timeDate){

        //Retrieve Start and EndDates as a Joda DateTime object
        DateTime startDate = processDate(timeDate, "start");
   		DateTime endDate = processDate(timeDate, "end");
        
        //Retrieve start Day and Hour
    	int startTime = startDate.getHourOfDay();
    	int startDay = startDate.getDayOfWeek();

        //Retrieve end Day and Hour
    	int endTime = endDate.getHourOfDay();
    	int endDay = endDate.getDayOfWeek();
           
        //populate eventMatrix with values inidicating "Invitee Unavailable" at time slots
        populateEventMatrix(startDay, startTime, endDay, endTime);
    	
    }

    /**
     * 
     * @param timeOb
     * @param time
     * @return org.joda.time.DateTime
     * 
     * Proccesses a DateTime JsonObject by parsing the Date string in the field specified by "time"
     * and creating a new Joda DateTime object
     */
    private static DateTime processDate(JsonObject timeOb, String time){
        //Time Zones are Default RN, will update that later
    	return new DateTime(stripQuotes(timeOb.get(time).toString())).withZone(DateTimeZone.forID("America/Los_Angeles"));
    }

    /**
     * 
     * @param startDay
     * @param startTime
     * @param endDay
     * @param endTime
     * 
     * Given a pair of Start Coordinates and End Coordinates, populates the eventMatrix
     * by incremeting each index in the specified range by 1. This value indicates 
     * an invitee that is unable to attend an event at this time slot
     */
    private static void populateEventMatrix(int startDay, int startTime, int endDay, int endTime){

        while((startTime!= endTime) || (startDay != endDay)){
            //Increment event index by 1, this value indicates at least 1 person is "unavailable"
            //during that time slot
            eventMatrix[startDay-1][startTime] += 1;
            

    		//Joda time starts at 0, mod operation AFTER incrementing
    		startTime++;
            startTime %= 24;

    		//startTime == 0 indicates 12 am, increment to next day
    	   	if(startTime == 0){
                //Joda Day starts at 1, mod operation BEFORE incrementing
                startDay %= 7;
    	   	    startDay++;
            }
    	}
    }


    /**
     * Proccess the global eventMatrix, by looking at each possible 
     * Event range, specified by user input, and creating an event with all
     * possible information, such as Start Day, Start Time, End Time,
     * Amount of Non-attendees, Largest amount of Non-attendees 
     * for any given time slot during the event. Then populates
     * the global events ArrayList with each event created this way,
     * as long as at least 50% can fully attend.
     */
    private static void processEventMatrix(){

        //Init Event ArrayList
        events = new ArrayList<Event>();

        //Modify endConstraint to Contain the "Absolute" End time of event (saves calculation later)
        endConstraint -= meetingLength;
        //Calculate end of the first available meeting within constraints (saves repetative calculation)
        int endOfFirstMeetingTime = startConstraint + meetingLength;
        for(int day = 0; day < DAYS_OF_WEEK; day++){
            
            int sum = 0;
            //Running Sum per event, init by adding values from each time index
            //this minimizes array access later
            for(int eventTime = startConstraint; eventTime < endOfFirstMeetingTime; eventTime++){
                sum += eventMatrix[day][eventTime];
            }

            int newMeetingEndTime;

			for(int start = startConstraint; start < endConstraint; start++){

                //Determine largest amount of invitees unavailable during any given time block during time frame
                int nonAttendees = maxNonAttending(day, start);

                //calculate the end time in hours 
                newMeetingEndTime = start + meetingLength;

                //Create and add event to Event ArrayList only if MORE than 50% can attend
                if(nonAttendees <= (numberOfInvitees / (2.0))) {
                    events.add(new Event(start, newMeetingEndTime, day, sum, nonAttendees));
                }

                //Reduce running sum of non-attendees by the time index at start
                sum -= eventMatrix[day][start];

                /*Increase running sum of non-attendees by the time index at the end of the 
                  time block (as long as its in range) */
                if(start + meetingLength < (endConstraint - meetingLength)){
                    sum += eventMatrix[day][newMeetingEndTime + 1];
                }
			}
		}
    }

    //Calculates the Maximum number of Non-available invitees per given set of time blocks
    private static int maxNonAttending(int day, int startHour){

        //Init a new ArrayList (allows the Collections.sort() function to work properly)
        List<Integer> nums = new ArrayList<Integer>();
        //Populate it with values from event matrix
        for(int i = startHour; i < startHour + meetingLength; i++){
            nums.add(Integer.valueOf(eventMatrix[day][i]));
        }

        //Sort List
        Collections.sort(nums);

        //Get largest number
        int largestNumber = nums.get(nums.size()-1);

        //Free up memory incase garbage collection fails
        nums = null;

        //Return largest number
        return largestNumber;
    }
}