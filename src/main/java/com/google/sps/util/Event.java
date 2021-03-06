package com.google.sps.util;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

//Helper Class to Order Events
public class Event implements Comparable<Event> {
    public int start;
    public int end;
    public int day;
    public int val;
    public int nonAttending;
    public DateTime startDate;
    public DateTime endDate;

    public Event(int start, int end, int day, int val, int nonAttending) {
        this.start = start;
        this.end = end;
        this.day = day + 1;
        this.val = val;
        this.nonAttending = nonAttending;

        DateTime startDate = new DateTime();
        startDate = startDate.minuteOfHour().setCopy(0);
        startDate = startDate.hourOfDay().setCopy(this.start);
        startDate = startDate.dayOfWeek().setCopy(this.day);
        this.startDate = startDate.withZone(DateTimeZone.getDefault());

        DateTime endDate = new DateTime();
        endDate = endDate.minuteOfHour().setCopy(0);
        endDate = endDate.hourOfDay().setCopy(this.end);
        endDate = endDate.dayOfWeek().setCopy(this.day);
        this.endDate = endDate.withZone(DateTimeZone.getDefault());

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