package com.google.sps.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.sps.util.DatastoreModule;

@WebServlet("/ActiveCalendars")
public class ActiveCalendarsServlet extends HttpServlet {

    
    private static final long serialVersionUID = 1L;

    @Override
    public void init() {
        try {
            // Ensure the Datastore is up and running
            DatastoreModule.init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {

        res.setContentType("application/json;");
        String[] activeCalendars = DatastoreModule.getActiveCalendarList(req.getParameter("userId"));
        String retJson = new Gson().toJson(activeCalendars);
        
        res.getWriter().println(retJson);
    }


    @Override 
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {

         
        String[] userCalendarIds = new Gson().fromJson(req.getParameter("userList"), String[].class);
        String userId = req.getParameter("userId");

        DatastoreModule.storeActiveCalendars(userId, userCalendarIds);

        doGet(req, res);
    }
}