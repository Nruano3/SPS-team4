package com.google.sps.servlets;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/test")
public class testServlet extends HttpServlet {

    @Override 
    public void doGet(HttpServletRequest request, HttpServletResponse response){
        String id_token = request.getParameter("id_token").toString();
        System.out.println("Printing id_tokenP:" + id_token);


    }
}