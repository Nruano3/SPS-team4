package com.google.sps.servlets;

import java.io.IOException;
import java.util.Random;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Handles requests sent to the /hello URL. Try running a server and navigating to /hello! */
@WebServlet("/randMessage")
public class HelloWorldServlet extends HttpServlet {

    /**
     *
     */
    private static final long serialVersionUID = -8494463698833202272L;
    private static final Random rand = new Random();
    private static final String[] messages = {"Hello There", "General Kenobi"};
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("text/html;");
    int responeChooser = rand.nextInt(2);
    response.getWriter().println("<h1>" + messages[responeChooser]+"</h1>");
  }

}