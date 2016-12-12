--USERS

INSERT INTO USERS (USER_ID, FIRST_NAME, LAST_NAME)
SELECT DISTINCT USER_ID, FIRST_NAME, LAST_NAME FROM keykholt.PUBLIC_USER_INFORMATION;

CREATE TABLE T AS (
SELECT DISTINCT USER_ID, YEAR_OF_BIRTH
FROM keykholt.PUBLIC_USER_INFORMATION P
WHERE P.YEAR_OF_BIRTH IS NOT NULL
);

UPDATE USERS
SET USERS.YEAR_OF_BIRTH = (SELECT T.YEAR_OF_BIRTH
FROM T
WHERE USERS.USER_ID = T.USER_ID);

DROP TABLE T;

CREATE TABLE T AS (
SELECT DISTINCT USER_ID, MONTH_OF_BIRTH
FROM keykholt.PUBLIC_USER_INFORMATION P
WHERE P.MONTH_OF_BIRTH IS NOT NULL
);

UPDATE USERS
SET USERS.MONTH_OF_BIRTH = (SELECT T.MONTH_OF_BIRTH
FROM T
WHERE USERS.USER_ID = T.USER_ID);

DROP TABLE T;

CREATE TABLE T AS (
SELECT DISTINCT USER_ID, DAY_OF_BIRTH
FROM keykholt.PUBLIC_USER_INFORMATION P
WHERE P.DAY_OF_BIRTH IS NOT NULL
);

UPDATE USERS
SET USERS.DAY_OF_BIRTH = (SELECT T.DAY_OF_BIRTH
FROM T
WHERE USERS.USER_ID = T.USER_ID);

DROP TABLE T;

CREATE TABLE T AS (
SELECT DISTINCT USER_ID, GENDER
FROM keykholt.PUBLIC_USER_INFORMATION P
WHERE P.GENDER IS NOT NULL
);

UPDATE USERS
SET USERS.GENDER = (SELECT T.GENDER
FROM T
WHERE USERS.USER_ID = T.USER_ID);

DROP TABLE T;

--FRIENDS

INSERT INTO FRIENDS (USER1_ID, USER2_ID)
SELECT DISTINCT T.COL1, T.COL2 FROM (
SELECT P1.USER1_ID AS COL1, P1.USER2_ID AS COL2
FROM keykholt.PUBLIC_ARE_FRIENDS P1
UNION
SELECT P2.USER2_ID, P2.USER1_ID
FROM keykholt.PUBLIC_ARE_FRIENDS P2) T
WHERE T.COL1 - T.COL2 < 0;
--T.COL1 < T.COL2 NOT WORKING?

--CITIES

CREATE TABLE TC AS (
SELECT DISTINCT USER_ID, CURRENT_CITY, CURRENT_STATE, CURRENT_COUNTRY
FROM keykholt.PUBLIC_USER_INFORMATION
WHERE CURRENT_CITY IS NOT NULL AND CURRENT_STATE IS NOT NULL AND CURRENT_COUNTRY IS NOT NULL
);

CREATE TABLE TH AS (
SELECT DISTINCT USER_ID, HOMETOWN_CITY, HOMETOWN_STATE, HOMETOWN_COUNTRY
FROM keykholt.PUBLIC_USER_INFORMATION
WHERE HOMETOWN_CITY IS NOT NULL AND HOMETOWN_STATE IS NOT NULL AND HOMETOWN_COUNTRY IS NOT NULL
);

CREATE TABLE TE AS (
SELECT DISTINCT EVENT_ID, EVENT_CITY, EVENT_STATE, EVENT_COUNTRY
FROM keykholt.PUBLIC_EVENT_INFORMATION
WHERE EVENT_CITY IS NOT NULL AND EVENT_STATE IS NOT NULL AND EVENT_COUNTRY IS NOT NULL
);

CREATE SEQUENCE cid_sequence
START WITH 1
INCREMENT BY 1
CACHE 20;

CREATE TRIGGER cid_tirgger
BEFORE INSERT ON CITIES
FOR EACH ROW
BEGIN
  SELECT cid_sequence.NEXTVAL
  INTO :NEW.CITY_ID
  FROM DUAL;
END;
/

INSERT INTO CITIES (CITY_NAME, STATE_NAME, COUNTRY_NAME)
SELECT HOMETOWN_CITY, HOMETOWN_STATE, HOMETOWN_COUNTRY
FROM TH
UNION
SELECT CURRENT_CITY, CURRENT_STATE, CURRENT_COUNTRY
FROM TC
UNION
SELECT EVENT_CITY, EVENT_STATE, EVENT_COUNTRY
FROM TE;

DROP TRIGGER cid_tirgger;
DROP SEQUENCE cid_sequence;

--USER_CURRENT_CITY

INSERT INTO USER_CURRENT_CITY (USER_ID, CURRENT_CITY_ID)
SELECT TC.USER_ID, CITIES.CITY_ID FROM TC, CITIES
WHERE TC.CURRENT_CITY = CITIES.CITY_NAME AND TC.CURRENT_STATE = CITIES.STATE_NAME AND TC.CURRENT_COUNTRY = CITIES.COUNTRY_NAME;

DROP TABLE TC;

--USER_HOMETOWN_CITY

INSERT INTO USER_HOMETOWN_CITY (USER_ID, HOMETOWN_CITY_ID)
SELECT TH.USER_ID, CITIES.CITY_ID FROM TH, CITIES
WHERE TH.HOMETOWN_CITY = CITIES.CITY_NAME AND TH.HOMETOWN_STATE = CITIES.STATE_NAME AND TH.HOMETOWN_COUNTRY = CITIES.COUNTRY_NAME;

DROP TABLE TH;

--MESSAGE

--PROGRAMS

CREATE SEQUENCE pid_sequence
START WITH 1
INCREMENT BY 1
CACHE 20;

CREATE TRIGGER pid_tirgger
BEFORE INSERT ON PROGRAMS
FOR EACH ROW
BEGIN
  SELECT pid_sequence.NEXTVAL
  INTO :NEW.PROGRAM_ID
  FROM DUAL;
END;
/

INSERT INTO PROGRAMS (INSTITUTION, CONCENTRATION, DEGREE)
SELECT DISTINCT INSTITUTION_NAME, PROGRAM_CONCENTRATION, PROGRAM_DEGREE FROM keykholt.PUBLIC_USER_INFORMATION
WHERE INSTITUTION_NAME IS NOT NULL AND PROGRAM_CONCENTRATION IS NOT NULL AND PROGRAM_DEGREE IS NOT NULL;

DROP TRIGGER pid_tirgger;
DROP SEQUENCE pid_sequence;

--EDUCATION

INSERT INTO EDUCATION (USER_ID, PROGRAM_ID, PROGRAM_YEAR)
SELECT DISTINCT PUI.USER_ID, P.PROGRAM_ID, PUI.PROGRAM_YEAR
FROM keykholt.PUBLIC_USER_INFORMATION PUI, PROGRAMS P
WHERE PUI.INSTITUTION_NAME = P. INSTITUTION AND PUI.PROGRAM_CONCENTRATION = P.CONCENTRATION AND PUI.PROGRAM_DEGREE = P.DEGREE;

--USER_EVENTS

INSERT INTO USER_EVENTS (EVENT_ID, EVENT_CREATOR_ID, EVENT_NAME, EVENT_TAGLINE, EVENT_DESCRIPTION, EVENT_HOST, EVENT_TYPE, EVENT_SUBTYPE, EVENT_LOCATION, EVENT_START_TIME, EVENT_END_TIME)
SELECT EVENT_ID, EVENT_CREATOR_ID, EVENT_NAME, EVENT_TAGLINE, EVENT_DESCRIPTION, EVENT_HOST, EVENT_TYPE, EVENT_SUBTYPE, EVENT_LOCATION, EVENT_START_TIME, EVENT_END_TIME
FROM keykholt.PUBLIC_EVENT_INFORMATION;

UPDATE USER_EVENTS
SET USER_EVENTS.EVENT_CITY_ID = (SELECT C.CITY_ID
FROM CITIES C, TE 
WHERE USER_EVENTS.EVENT_ID = TE.EVENT_ID AND TE.EVENT_CITY = C.CITY_NAME AND TE.EVENT_STATE = C.STATE_NAME AND TE.EVENT_COUNTRY = C.COUNTRY_NAME);

DROP TABLE TE;

--PARTICIPANTS

--ALBUMS

ALTER TABLE ALBUMS DROP CONSTRAINT foreignKey;

INSERT INTO ALBUMS (ALBUM_ID, ALBUM_OWNER_ID, ALBUM_NAME, ALBUM_CREATED_TIME, ALBUM_MODIFIED_TIME, ALBUM_LINK, ALBUM_VISIBILITY, COVER_PHOTO_ID)
SELECT DISTINCT ALBUM_ID, OWNER_ID, ALBUM_NAME, ALBUM_CREATED_TIME, ALBUM_MODIFIED_TIME, ALBUM_LINK, ALBUM_VISIBILITY, COVER_PHOTO_ID
FROM keykholt.PUBLIC_PHOTO_INFORMATION;

--PHOTOS

INSERT INTO PHOTOS (PHOTO_ID, ALBUM_ID, PHOTO_CAPTION, PHOTO_CREATED_TIME, PHOTO_MODIFIED_TIME, PHOTO_LINK)
SELECT DISTINCT PHOTO_ID, ALBUM_ID, PHOTO_CAPTION, PHOTO_CREATED_TIME, PHOTO_MODIFIED_TIME, PHOTO_LINK
FROM keykholt.PUBLIC_PHOTO_INFORMATION;

ALTER TABLE ALBUMS ADD CONSTRAINT foreignKey FOREIGN KEY(COVER_PHOTO_ID) REFERENCES PHOTOS;

--TAGS

INSERT INTO TAGS (TAG_PHOTO_ID, TAG_SUBJECT_ID, TAG_CREATED_TIME, TAG_X, TAG_Y)
SELECT DISTINCT PHOTO_ID, TAG_SUBJECT_ID, TAG_CREATED_TIME, TAG_X_COORDINATE, TAG_Y_COORDINATE
FROM keykholt.PUBLIC_TAG_INFORMATION;