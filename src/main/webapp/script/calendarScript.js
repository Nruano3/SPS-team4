
const dayOfWeek = ['sun', 'mon', 'tue', 'wed', 'thu', 'fri', 'sat'];
const colorids = ['#000000', '#7986cb', '#33b679', '#8e24aa', '#e67c73', '#f6c026', '#f5511d', '#000000', '#616161', '#3f51b5', '#0b8043', '#d60000', '#039be5'];
const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
var currDate = new Date();

function initializeCalendar() {
    var temp = new Date();
    if (temp.getDay() == 0) {
        currDate.setDate(temp.getDate() - 6);
    }
    else {
        currDate.setDate(temp.getDate() - (temp.getDay() - 1));
    } // statement makes sure week starts on a monday
    currDate = new Date(currDate.setHours(0, 0, 0, 0));
    displayDays(currDate);
    populateCalendar(currDate);
}



/* Note on next few functions to select the date and range of dates the arithmetic for the dates seems to be wierd or I am misunderstanding something so that it is not working consistently? The adjustment of the date appears to be off by a week for some reason I can not understand, but it works.
*/

function nextWeekCalendar() {
    cleanCalendar();
    currDate.setDate(currDate.getDate()); // Increases week to next week
    displayDays(currDate);
    populateCalendar(currDate);
}

function lastWeekCalendar() {
    cleanCalendar();
    currDate.setDate(currDate.getDate() - 14); // Decreases week to previous week
    displayDays(currDate);
    populateCalendar(currDate);
}

function populateCalendar(calDate) {
    gapi.client.load('calendar', 'v3', function () {

        gapi.client.calendar.events.list({
            'calendarId': 'primary',
            'timeMin': (calDate).toISOString(),
            'timeMax': (new Date(calDate.setDate(calDate.getDate() + 7))).toISOString(),
            'showDeleted': false,
            'singleEvents': true,
            'orderBy': 'startTime'
        }).then(function (response) {
            var events = response.result.items;
            
            displayMonth(events);
            if (events.length > 0) {
                for (i = 0; i < events.length; i++) {
                    var event = events[i];
                    var when = event.start.dateTime;
                    if (!when) {
                        when = new Date(event.start.date);
                    }
                    var day = (new Date(when)).getDay();
                    var cid = event.colorId;
                    if (isNaN(cid)) {
                        cid = 12;
                    }
                    appendEvent(event.summary, event.start, event.end, dayOfWeek[day], colorids[cid]);
                }
            }
        });
    });
}

function appendEvent(message, start, end, weekday, color) {
    var pre = document.getElementById('event' + weekday);
    var div = document.createElement("div");
    div.style.backgroundColor = color;
    div.style.border = "1px solid black";
    div.style.borderRadius = "15px";
    div.style.padding = "6px";
    div.style.color = "white";
    div.style.textAlign = "center";
    var textContent = document.createTextNode(message);
    div.appendChild(textContent);
    getDateTimeStrings(start, end, div);
    pre.appendChild(div);
}

function getDateTimeStrings(start, end, elem) {
    var startDateTime = new Date(start.dateTime);
    var endDateTime = new Date(end.dateTime);
    if (!startDateTime.getMonth() || !endDateTime.getMonth()) {
        var startMonth = months[(new Date(start.date)).getMonth()];
        var startDay = (new Date(start.date)).getDate();
        var endMonth = months[(new Date(end.date)).getMonth()];
        var endDay = (new Date(end.date)).getDate();
        elem.appendChild(document.createElement("br"));
        elem.appendChild(document.createTextNode("Starts: " + startMonth + " " + startDay));
        elem.appendChild(document.createElement("br"));
        elem.appendChild(document.createTextNode("Ends: " + endMonth + " " + endDay));
    }
    else {
        var startTime = getStringTime(startDateTime);
        var endTime = getStringTime(endDateTime);
        elem.appendChild(document.createElement("br"));
        elem.appendChild(document.createTextNode("Starts: " + months[startDateTime.getMonth()] + " " + startDateTime.getDate() + startTime));
        elem.appendChild(document.createElement("br"));
        elem.appendChild(document.createTextNode("Ends: " + months[endDateTime.getMonth()] + " " + endDateTime.getDate() + endTime));
    }
}

function getStringTime(timeValue) {
    var hours = timeValue.getHours();
    var mins = timeValue.getMinutes();
    if (mins == 0) {
        mins = "00";
    }
    if (hours == 0) {
        return (" 12:" + mins + " AM");
    }
    else if (hours < 12) {
        return (" " + hours + ":" + mins + " AM");
    }
    else if (hours == 12) {
        return (" " + hours + ":" + mins + " PM")
    }
    else if (hours < 24) {
        return (" " + (hours - 12) + ":" + mins + " PM");
    }
    else {
        return (" All day");
    }
}

function displayDays() {
    var date = new Date(currDate);
    for (i = 0; i < dayOfWeek.length; i++) {
        var dayweek = date.getDay();
        var daymonth = date.getDate();
        var dayelem = document.getElementById(dayOfWeek[dayweek]);
        var div = document.createElement("div");
        var today = new Date();
        if ((date.getDate() == today.getDate()) && (date.getMonth() == today.getMonth()) && (date.getFullYear() == today.getFullYear())) {
            div.style.borderRadius = "50%";
            div.style.border = "1px solid black";
            div.style.width = "45px";
            div.style.backgroundColor = "lightblue";
            div.style.margin = "0 auto";
        }
        div.appendChild(document.createTextNode(dayOfWeek[dayweek].toUpperCase()))
        div.appendChild(document.createElement("br"));
        div.appendChild(document.createTextNode(daymonth));
        dayelem.appendChild(div);
        date.setDate(date.getDate() + 1);
    }
}

function displayMonth(events) {
    var month = document.getElementById('month');
    var monMonth = (new Date(events[0].start.dateTime)).getMonth();
    var sunMonth = (new Date(events[events.length - 1].start.dateTime)).getMonth();
    if (monMonth == sunMonth) {
        month.appendChild(document.createTextNode(months[monMonth]));
    }
    else {
        month.appendChild(document.createTextNode(months[monMonth] + '/' + months[sunMonth]));
    }
}

function cleanCalendar() {
    var ele = document.getElementById('month');
    ele.innerHTML = '';
    for (i = 0; i < dayOfWeek.length; i++) {
        var ele = document.getElementById(dayOfWeek[i]);
        while (ele.hasChildNodes()) {
            ele.removeChild(ele.firstChild);
        }
    }
    for (i = 0; i < dayOfWeek.length; i++) {
        var ele = document.getElementById('event' + dayOfWeek[i]);
        while (ele.hasChildNodes()) {
            ele.removeChild(ele.firstChild);
        }
    }
}