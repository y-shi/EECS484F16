CREATE TABLE USERS (
  USER_ID NUMBER,
  FIRST_NAME VARCHAR2(100) NOT NULL,
  LAST_NAME VARCHAR2(100) NOT NULL,
  YEAR_OF_BIRTH INTEGER,
  MONTH_OF_BIRTH INTEGER,
  DAY_OF_BIRTH INTEGER,
  GENDER VARCHAR2(100),
  PRIMARY KEY(USER_ID)
);

CREATE TABLE FRIENDS (
  USER1_ID NUMBER NOT NULL,
  USER2_ID NUMBER NOT NULL,
  PRIMARY KEY(USER1_ID, USER2_ID),
  FOREIGN KEY(USER1_ID) REFERENCES USERS,
  FOREIGN KEY(USER2_ID) REFERENCES USERS
  --CHECK(USER1_ID < USER2_ID)
);

CREATE OR REPLACE TRIGGER friends_trig BEFORE INSERT OR UPDATE ON FRIENDS
FOR EACH ROW
  WHEN (NEW.USER1_ID > NEW.USER2_ID) 
DECLARE U_ID NUMBER;
BEGIN
  U_ID := :NEW.USER1_ID;
  :NEW.USER1_ID := :NEW.USER2_ID;
  :NEW.USER2_ID := U_ID;
END;
/

CREATE TABLE CITIES (
  CITY_ID INTEGER,
  CITY_NAME VARCHAR2(100),
  STATE_NAME VARCHAR2(100),
  COUNTRY_NAME VARCHAR2(100),
  PRIMARY KEY(CITY_ID)
);

CREATE TABLE USER_CURRENT_CITY (
  USER_ID NUMBER,
  CURRENT_CITY_ID INTEGER NOT NULL,
  PRIMARY KEY(USER_ID),
  FOREIGN KEY(USER_ID) REFERENCES USERS,
  FOREIGN KEY(CURRENT_CITY_ID) REFERENCES CITIES
);

CREATE TABLE USER_HOMETOWN_CITY (
  USER_ID NUMBER,
  HOMETOWN_CITY_ID INTEGER NOT NULL,
  PRIMARY KEY(USER_ID),
  FOREIGN KEY(USER_ID) REFERENCES USERS,
  FOREIGN KEY(HOMETOWN_CITY_ID) REFERENCES CITIES
);

CREATE TABLE MESSAGE (
  MESSAGE_ID INTEGER,
  SENDER_ID NUMBER,
  RECEIVER_ID NUMBER,
  MESSAGE_CONTENT VARCHAR2(2000),
  SENT_TIME TIMESTAMP,
  PRIMARY KEY(MESSAGE_ID),
  FOREIGN KEY(SENDER_ID) REFERENCES USERS,
  FOREIGN KEY(RECEIVER_ID) REFERENCES USERS
);

CREATE TABLE PROGRAMS (
  PROGRAM_ID INTEGER,
  INSTITUTION VARCHAR2(100),
  CONCENTRATION VARCHAR2(100),
  DEGREE VARCHAR2(100),
  PRIMARY KEY(PROGRAM_ID)
);

CREATE TABLE EDUCATION (
  USER_ID NUMBER NOT NULL,
  PROGRAM_ID INTEGER NOT NULL,
  PROGRAM_YEAR INTEGER,
  PRIMARY KEY(USER_ID, PROGRAM_ID),
  FOREIGN KEY(USER_ID) REFERENCES USERS,
  FOREIGN KEY(PROGRAM_ID) REFERENCES PROGRAMS
);

CREATE TABLE USER_EVENTS (
  EVENT_ID NUMBER,
  EVENT_CREATOR_ID NUMBER NOT NULL,
  EVENT_NAME VARCHAR2(100),
  EVENT_TAGLINE VARCHAR2(100),
  EVENT_DESCRIPTION VARCHAR2(100),
  EVENT_HOST VARCHAR2(100) NOT NULL,
  EVENT_TYPE VARCHAR2(100) NOT NULL,
  EVENT_SUBTYPE VARCHAR2(100) NOT NULL,
  EVENT_LOCATION VARCHAR2(100),
  EVENT_CITY_ID INTEGER,
  EVENT_START_TIME TIMESTAMP NOT NULL,
  EVENT_END_TIME TIMESTAMP NOT NULL,
  PRIMARY KEY(EVENT_ID),
  FOREIGN KEY(EVENT_CREATOR_ID) REFERENCES USERS,
  FOREIGN KEY(EVENT_CITY_ID) REFERENCES CITIES
);

CREATE TABLE PARTICIPANTS (
  EVENT_ID NUMBER,
  USER_ID NUMBER,
  CONFIRMATION VARCHAR2(100) NOT NULL,
  PRIMARY KEY(EVENT_ID, USER_ID),
  FOREIGN KEY(EVENT_ID) REFERENCES USER_EVENTS,
  FOREIGN KEY(USER_ID) REFERENCES USERS
);

CREATE TABLE ALBUMS (
  ALBUM_ID VARCHAR2(100),
  ALBUM_OWNER_ID NUMBER NOT NULL,
  ALBUM_NAME VARCHAR2(100) NOT NULL,
  ALBUM_CREATED_TIME TIMESTAMP NOT NULL,
  ALBUM_MODIFIED_TIME TIMESTAMP NOT NULL,
  ALBUM_LINK VARCHAR2(2000) NOT NULL,
  ALBUM_VISIBILITY VARCHAR2(100) NOT NULL,
  COVER_PHOTO_ID VARCHAR2(100) NOT NULL,
  PRIMARY KEY(ALBUM_ID),
  FOREIGN KEY(ALBUM_OWNER_ID) REFERENCES USERS,
  --UNIQUE(ALBUM_LINK),
  CHECK(ALBUM_VISIBILITY IN ('EVERYONE', 'FRIENDS_OF_FRIENDS', 'FRIENDS', 'MYSELF', 'CUSTOM'))
);

CREATE TABLE PHOTOS (
  PHOTO_ID VARCHAR2(100),
  ALBUM_ID VARCHAR2(100) NOT NULL,
  PHOTO_CAPTION VARCHAR2(2000),
  PHOTO_CREATED_TIME TIMESTAMP NOT NULL,
  PHOTO_MODIFIED_TIME TIMESTAMP NOT NULL,
  PHOTO_LINK VARCHAR2(2000) NOT NULL,
  PRIMARY KEY(PHOTO_ID),
  FOREIGN KEY(ALBUM_ID) REFERENCES ALBUMS
  --UNIQUE(PHOTO_LINK)
);

ALTER TABLE ALBUMS ADD CONSTRAINT foreignKey FOREIGN KEY(COVER_PHOTO_ID) REFERENCES PHOTOS;

CREATE TABLE TAGS (
  TAG_PHOTO_ID VARCHAR2(100),
  TAG_SUBJECT_ID NUMBER,
  TAG_CREATED_TIME TIMESTAMP NOT NULL,
  TAG_X NUMBER NOT NULL,
  TAG_Y NUMBER NOT NULL,
  PRIMARY KEY(TAG_PHOTO_ID, TAG_SUBJECT_ID),
  FOREIGN KEY(TAG_PHOTO_ID) REFERENCES PHOTOS,
  FOREIGN KEY(TAG_SUBJECT_ID) REFERENCES USERS
);
