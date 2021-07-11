package com.google.sps.servlets;

import java.io.BufferedReader;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

@WebServlet("/process-user-data")
public class AutoEventProcessor extends HttpServlet {
    

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res){

       

        /**
         *   1. Get Users
         *   2. Get User's Calendars
         *   3. Process Calendars "Free" vs "Busy" Time
         *   4. Turn results into a json
         *   5. Return list of times: retJson = [{"Day": dayNumber, "Hour": hourNumber, "Minute": minuteNumber} ...]
         */

         String[] users = getUsers(req); 

    }


    private String[] getUsers(HttpServletRequest req){

        StringBuffer jb = new StringBuffer();
        String line = null;
        try {
            BufferedReader reader = req.getReader();
            while ((line = reader.readLine()) != null)
            jb.append(line);
        } catch (Exception e) { /*report an error*/ }
        
        return new Gson().fromJson(jb.toString(), String[].class);
    }
}