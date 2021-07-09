/**
 *  This file is to denote all the potential properties that can be added to 
 *  an event, and to track the id for all of our programs inputs
 */

//=============================Current Form Data=============================================//
/**
 * 
 *  Event Title: id = "title-input"
 * 
 *  Event Start Date: id = "start-date-input"
 * 
 *  Event End Date: id = "end-date-input"
 * 
 *  Event Duration (Hour):  id = "hour-input"
 * 
 *  Event Duration (Minute):  id = "min-input"
 * 
 *  Event Start Time (Early): id = "early-time-input"
 * 
 *  Event Start Time (Late): id = "late-time-input"
 * 
 *  Event Color ID: id = "color-input"
 * 
 *  Event Invitees (CSV): id = "invitees-input"
 */


//=====================================All fields available to add to event===========================//
/**
 * {
  "kind": "calendar#event",
  "etag": etag,
  "id": string,         <-------- If not given, automatically assigned from calendar server
  "status": string,
  "htmlLink": string,
  "created": datetime,
  "updated": datetime,
  "summary": string,   <------ This is the name of the event
  "description": string,
  "location": string,
  "colorId": string,
  "creator": {
    "id": string,
    "email": string,
    "displayName": string,
    "self": boolean
  },
  "organizer": {
    "id": string,
    "email": string,
    "displayName": string,
    "self": boolean
  },
  "start": {        <----------Either give date (if all day event) or dateTime & timeZone (if its a timed event), but not both 
    "date": date,
    "dateTime": "2015-09-15T06:00:00+02:00",
    "timeZone": "America/Los_Angeles"
  },
  "end": {        <----------Either give date (if all day event) or dateTime & timeZone (if its a timed event), but not both
    "date": date,
    "dateTime": "2015-09-15T06:00:00+02:00",
    "timeZone": Europe/Zurich"
  },
  "endTimeUnspecified": boolean,
  "recurrence": [
    "EXDATE;VALUE=DATE:20150610",
    "RDATE;VALUE=DATE:20150609,20150611",
    "RRULE:FREQ=DAILY;UNTIL=20150628;INTERVAL=3"
  ],
  "recurringEventId": string,      <---------------Each event has a distinct id (see above), but all recurring events have the SAME secondary ID
  "originalStartTime": {
    "date": date,
    "dateTime": datetime,
    "timeZone": string
  },
  "transparency": string,
  "visibility": string,
  "iCalUID": string,                <-----------------------Similar to recurringEventId but can also be applied to non-recurring events
  "sequence": integer,
  "attendees": [                    <------------------------ List of attendees, may be used in future to determine "best meeting time"
    {
      "id": string,
      "email": string,
      "displayName": string,
      "organizer": boolean,
      "self": boolean,
      "resource": boolean,
      "optional": boolean,
      "responseStatus": string,
      "comment": string,
      "additionalGuests": integer
    }
  ],
  "attendeesOmitted": boolean,
  "extendedProperties": {
    "private": {
      (key): string
    },
    "shared": {
      (key): string
    }
  },
  "hangoutLink": string,
  "conferenceData": {
    "createRequest": {
      "requestId": string,
      "conferenceSolutionKey": {
        "type": string
      },
      "status": {
        "statusCode": string
      }
    },
    "entryPoints": [
      {
        "entryPointType": string,
        "uri": string,
        "label": string,
        "pin": string,
        "accessCode": string,
        "meetingCode": string,
        "passcode": string,
        "password": string
      }
    ],
    "conferenceSolution": {
      "key": {
        "type": string
      },
      "name": string,
      "iconUri": string
    },
    "conferenceId": string,
    "signature": string,
    "notes": string,
  },
  "gadget": {         <---------------- Deprecated
    "type": string,
    "title": string,
    "link": string,
    "iconLink": string,
    "width": integer,
    "height": integer,
    "display": string,
    "preferences": {
      (key): string
    }
  },
  "anyoneCanAddSelf": boolean,
  "guestsCanInviteOthers": boolean,
  "guestsCanModify": boolean,
  "guestsCanSeeOtherGuests": boolean,
  "privateCopy": boolean,
  "locked": boolean,
  "reminders": {
    "useDefault": boolean,
    "overrides": [
      {
        "method": string,
        "minutes": integer
      }
    ]
  },
  "source": {
    "url": string,
    "title": string
  },
  "attachments": [
    {
      "fileUrl": string,
      "title": string,
      "mimeType": string,
      "iconLink": string,
      "fileId": string
    }
  ],
  "eventType": string
}
 */