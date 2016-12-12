package project2;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MyFakebookOracle extends FakebookOracle {

    static String prefix = "tajik.";

    // You must use the following variable as the JDBC connection
    Connection oracleConnection = null;

    // You must refer to the following variables for the corresponding tables in your database
    String cityTableName = null;
    String userTableName = null;
    String friendsTableName = null;
    String currentCityTableName = null;
    String hometownCityTableName = null;
    String programTableName = null;
    String educationTableName = null;
    String eventTableName = null;
    String participantTableName = null;
    String albumTableName = null;
    String photoTableName = null;
    String coverPhotoTableName = null;
    String tagTableName = null;


    // DO NOT modify this constructor
    public MyFakebookOracle(String dataType, Connection c) {
        super();
        oracleConnection = c;
        // You will use the following tables in your Java code
        cityTableName = prefix + dataType + "_CITIES";
        userTableName = prefix + dataType + "_USERS";
        friendsTableName = prefix + dataType + "_FRIENDS";
        currentCityTableName = prefix + dataType + "_USER_CURRENT_CITY";
        hometownCityTableName = prefix + dataType + "_USER_HOMETOWN_CITY";
        programTableName = prefix + dataType + "_PROGRAMS";
        educationTableName = prefix + dataType + "_EDUCATION";
        eventTableName = prefix + dataType + "_USER_EVENTS";
        albumTableName = prefix + dataType + "_ALBUMS";
        photoTableName = prefix + dataType + "_PHOTOS";
        tagTableName = prefix + dataType + "_TAGS";
    }


    @Override
    // ***** Query 0 *****
    // This query is given to your for free;
    // You can use it as an example to help you write your own code
    //
    public void findMonthOfBirthInfo() {

    	
        // Scrollable result set allows us to read forward (using next())
        // and also backward.
        // This is needed here to support the user of isFirst() and isLast() methods,
        // but in many cases you will not need it.
        // To create a "normal" (unscrollable) statement, you would simply call
        // Statement stmt = oracleConnection.createStatement();
        //
        try (Statement stmt =
                     oracleConnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                             ResultSet.CONCUR_READ_ONLY)) {

            // For each month, find the number of users born that month
            // Sort them in descending order of count
            ResultSet rst = stmt.executeQuery("select count(*), month_of_birth from " +
                    userTableName +
                    " where month_of_birth is not null group by month_of_birth order by 1 desc");

            this.monthOfMostUsers = 0;
            this.monthOfLeastUsers = 0;
            this.totalUsersWithMonthOfBirth = 0;

            // Get the month with most users, and the month with least users.
            // (Notice that this only considers months for which the number of users is > 0)
            // Also, count how many total users have listed month of birth (i.e., month_of_birth not null)
            //
            while (rst.next()) {
                int count = rst.getInt(1);
                int month = rst.getInt(2);
                if (rst.isFirst())
                    this.monthOfMostUsers = month;
                if (rst.isLast())
                    this.monthOfLeastUsers = month;
                this.totalUsersWithMonthOfBirth += count;
            }

            // Get the names of users born in the "most" month
            rst = stmt.executeQuery("select user_id, first_name, last_name from " +
                    userTableName + " where month_of_birth=" + this.monthOfMostUsers);
            while (rst.next()) {
                Long uid = rst.getLong(1);
                String firstName = rst.getString(2);
                String lastName = rst.getString(3);
                this.usersInMonthOfMost.add(new UserInfo(uid, firstName, lastName));
            }

            // Get the names of users born in the "least" month
            rst = stmt.executeQuery("select first_name, last_name, user_id from " +
                    userTableName + " where month_of_birth=" + this.monthOfLeastUsers);
            while (rst.next()) {
                String firstName = rst.getString(1);
                String lastName = rst.getString(2);
                Long uid = rst.getLong(3);
                this.usersInMonthOfLeast.add(new UserInfo(uid, firstName, lastName));
            }

            // Close statement and result set
            rst.close();
            stmt.close();
        } catch (SQLException err) {
            System.err.println(err.getMessage());
        }
    }

    @Override
    // ***** Query 1 *****
    // Find information about users' names:
    // (1) The longest first name (if there is a tie, include all in result)
    // (2) The shortest first name (if there is a tie, include all in result)
    // (3) The most common first name, and the number of times it appears (if there
    //      is a tie, include all in result)
    //
    public void findNameInfo() { // Query1
        // Find the following information from your database and store the information as shown
        try (Statement stmt = oracleConnection.createStatement()) {
        	
        	ResultSet rst = stmt.executeQuery("select distinct first_name " +
                    "from " + userTableName +
                    " where length(first_name) = (select max(length(first_name)) from " + userTableName + ")");
        	while (rst.next()) {
        		this.longestFirstNames.add(rst.getString(1));
            }
        	
        	rst = stmt.executeQuery("select distinct first_name " +
                    "from " + userTableName +
                    " where length(first_name) = (select min(length(first_name)) from " + userTableName + ")");
        	while (rst.next()) {
        		this.shortestFirstNames.add(rst.getString(1));
            }
        	
        	rst = stmt.executeQuery("select distinct first_name, count(*) " +
                    "from " + userTableName +
                    " group by first_name " +
                    "having count(*) = (select max(count(*)) from " + userTableName + " group by first_name)");
        	while (rst.next()) {
        		this.mostCommonFirstNames.add(rst.getString(1));
        		this.mostCommonFirstNamesCount = rst.getInt(2);
            }
        	
        	rst.close();
            stmt.close();
        } catch (SQLException err) {
            System.err.println(err.getMessage());
        }
    	
        /*
    	this.longestFirstNames.add("JohnJacobJingleheimerSchmidt");
        this.shortestFirstNames.add("Al");
        this.shortestFirstNames.add("Jo");
        this.shortestFirstNames.add("Bo");
        this.mostCommonFirstNames.add("John");
        this.mostCommonFirstNames.add("Jane");
        this.mostCommonFirstNamesCount = 10;
        */
    }

    @Override
    // ***** Query 2 *****
    // Find the user(s) who have no friends in the network
    //
    // Be careful on this query!
    // Remember that if two users are friends, the friends table
    // only contains the pair of user ids once, subject to
    // the constraint that user1_id < user2_id
    //
    public void lonelyUsers() {
        // Find the following information from your database and store the information as shown
    	
    	try (Statement stmt = oracleConnection.createStatement()) {
        	
        	ResultSet rst = stmt.executeQuery("select user_id, first_name, last_name " +
                    "from " + userTableName +
                    " where user_id not in (select user1_id from " + friendsTableName + " union select user2_id from " + friendsTableName + ")");
        	while (rst.next()) {
        		Long uid = rst.getLong(1);
                String firstName = rst.getString(2);
                String lastName = rst.getString(3);
                this.lonelyUsers.add(new UserInfo(uid, firstName, lastName));
            }
        	
        	rst.close();
            stmt.close();
        } catch (SQLException err) {
            System.err.println(err.getMessage());
        }
    	
    	/*
        this.lonelyUsers.add(new UserInfo(10L, "Billy", "SmellsFunny"));
        this.lonelyUsers.add(new UserInfo(11L, "Jenny", "BadBreath"));
        */
    }

    @Override
    // ***** Query 3 *****
    // Find the users who do not live in their hometowns
    // (I.e., current_city != hometown_city)
    //
    public void liveAwayFromHome() throws SQLException {
    	
    	try (Statement stmt = oracleConnection.createStatement()) {
        	
        	ResultSet rst = stmt.executeQuery("select U.user_id, U.first_name, U.last_name " +
                    "from " + userTableName + " U, " + currentCityTableName + " C, " + hometownCityTableName + " H " +
                    "where U.user_id = C.user_id and U.user_id = H.user_id and C.current_city_id is not null and H.hometown_city_id is not null and C.current_city_id != H.hometown_city_id");
        	while (rst.next()) {
        		Long uid = rst.getLong(1);
                String firstName = rst.getString(2);
                String lastName = rst.getString(3);
                this.liveAwayFromHome.add(new UserInfo(uid, firstName, lastName));
            }
        	
        	rst.close();
            stmt.close();
        } catch (SQLException err) {
            System.err.println(err.getMessage());
        }
    	
    	/*
        this.liveAwayFromHome.add(new UserInfo(11L, "Heather", "Movalot"));
        */
    }

    @Override
    // **** Query 4 ****
    // Find the top-n photos based on the number of tagged users
    // If there are ties, choose the photo with the smaller numeric PhotoID first
    //
    public void findPhotosWithMostTags(int n) {
    	
    	try (Statement stmt = oracleConnection.createStatement()) {
    		
    		stmt.executeUpdate("create or replace view top_n as " +
        			"select photo_id from( " +
        			"select P.photo_id, count(*) " +
                    "from " + photoTableName + " P left join " + tagTableName + " T on P.photo_id = T.tag_photo_id " +
                    "group by P.photo_id order by 2 desc, 1 asc) " +
                    "where rownum <= " + n);
        	
        	ResultSet rst = stmt.executeQuery("select top_n.photo_id, P.album_id, A.album_name, P.photo_caption, P.photo_link, U.user_id, U.first_name, U.last_name " +
        			"from top_n, " + photoTableName + " P, " +  albumTableName + " A, " + tagTableName + " T, " + userTableName + " U " +
        			"where top_n.photo_id = P.photo_id and P.album_id = A.album_id and top_n.photo_id = T.tag_photo_id and T.tag_subject_id = U.user_id " +
        			"order by 1");
        	
        	String photoId = "";
        	TaggedPhotoInfo tp = null;
        	while (rst.next()) {
        		if (!photoId.equals(rst.getString(1))) {
        			if (tp != null) {
        				this.photosWithMostTags.add(tp);
        			}
        			photoId = rst.getString(1);
                    String albumId = rst.getString(2);
                    String albumName = rst.getString(3);
                    String photoCaption = rst.getString(4);
                    String photoLink = rst.getString(5);
                    PhotoInfo p = new PhotoInfo(photoId, albumId, albumName, photoCaption, photoLink);
                    tp = new TaggedPhotoInfo(p);
                }
        		Long uid = rst.getLong(6);
                String firstName = rst.getString(7);
                String lastName = rst.getString(8);
                tp.addTaggedUser(new UserInfo(uid, firstName, lastName));
            }
        	if (tp != null) {
				this.photosWithMostTags.add(tp);
			}
        	
        	stmt.executeUpdate("drop view top_n");
        	rst.close();
            stmt.close();
        } catch (SQLException err) {
            System.err.println(err.getMessage());
        }
    	
    	
    	/*
        String photoId = "1234567";
        String albumId = "123456789";
        String albumName = "album1";
        String photoCaption = "caption1";
        String photoLink = "http://google.com";
        PhotoInfo p = new PhotoInfo(photoId, albumId, albumName, photoCaption, photoLink);
        TaggedPhotoInfo tp = new TaggedPhotoInfo(p);
        tp.addTaggedUser(new UserInfo(12345L, "taggedUserFirstName1", "taggedUserLastName1"));
        tp.addTaggedUser(new UserInfo(12345L, "taggedUserFirstName2", "taggedUserLastName2"));
        this.photosWithMostTags.add(tp);
        */
    }

    @Override
    // **** Query 5 ****
    // Find suggested "match pairs" of users, using the following criteria:
    // (1) One of the users is female, and the other is male
    // (2) Their age difference is within "yearDiff"
    // (3) They are not friends with one another
    // (4) They should be tagged together in at least one photo
    //
    // You should return up to n "match pairs"
    // If there are more than n match pairs, you should break ties as follows:
    // (i) First choose the pairs with the largest number of shared photos
    // (ii) If there are still ties, choose the pair with the smaller user_id for the female
    // (iii) If there are still ties, choose the pair with the smaller user_id for the male
    //
    public void matchMaker(int n, int yearDiff) {
    	
    	try (Statement stmt = oracleConnection.createStatement()) {
        	
    		stmt.executeUpdate("create or replace view top_n as " +
    				"select f_id, m_id " +
    				"from (select U1.user_id as f_id, U2.user_id as m_id, count(T1.tag_photo_id) " +
                    "from " + userTableName + " U1, " + userTableName + " U2, " + tagTableName + " T1, " + tagTableName + " T2 " +
                    "where U1.gender = 'female' and U2.gender = 'male' and U1.year_of_birth - U2.year_of_birth <= " + yearDiff + " and U2.year_of_birth - U1.year_of_birth <= " + yearDiff +
                    " and not exists(select * from " + friendsTableName + " F where (F.user1_id = U1.user_id and F.user2_id = U2.user_id) or (F.user1_id = U2.user_id and F.user2_id = U1.user_id)) " +
                    "and U1.user_id = T1.tag_subject_id and U2.user_id = T2.tag_subject_id and T1.tag_photo_id = T2.tag_photo_id " +
                    "group by U1.user_id, U2.user_id " +
                    "order by 3 desc, 1 asc, 2 asc)" +
                    "where rownum <= " + n);
    		
    		ResultSet rst = stmt.executeQuery("select T.f_id, T.m_id, U1.first_name, U1.last_name, U1.year_of_birth, U2.first_name, U2.last_name, U2.year_of_birth, P.photo_id, P.album_id, A.album_name, P.photo_caption, P.photo_link " +
                    "from top_n T, " + userTableName + " U1, " + userTableName + " U2, " + tagTableName + " T1, " + tagTableName + " T2, " + photoTableName + " P, " + albumTableName + " A " +
                    "where T.f_id = U1.user_id and T.m_id = U2.user_id and T.f_id = T1.tag_subject_id and T.m_id = T2.tag_subject_id and T1.tag_photo_id = T2.tag_photo_id and T1.tag_photo_id = P.photo_id and P.album_id = A.album_id " +
                    "order by 1 asc, 2 asc");
    		
    		Long girlUserId = null;
    		Long boyUserId = null;
    		MatchPair mp = null;
    		while (rst.next()) {
        		if (girlUserId == null || (!girlUserId.equals(rst.getLong(1)) && !boyUserId.equals(rst.getLong(2)))) {
        			if (mp != null) {
        				this.bestMatches.add(mp);
        			}
        			girlUserId = rst.getLong(1);
        	        String girlFirstName = rst.getString(3);
        	        String girlLastName = rst.getString(4);
        	        int girlYear = rst.getInt(5);
        	        boyUserId = rst.getLong(2);
        	        String boyFirstName = rst.getString(6);
        	        String boyLastName = rst.getString(7);
        	        int boyYear = rst.getInt(8);
        	        mp = new MatchPair(girlUserId, girlFirstName, girlLastName,
        	                girlYear, boyUserId, boyFirstName, boyLastName, boyYear);
        		}
        		String sharedPhotoId = rst.getString(9);
                String sharedPhotoAlbumId = rst.getString(10);
                String sharedPhotoAlbumName = rst.getString(11);
                String sharedPhotoCaption = rst.getString(12);
                String sharedPhotoLink = rst.getString(13);
                mp.addSharedPhoto(new PhotoInfo(sharedPhotoId, sharedPhotoAlbumId,
                        sharedPhotoAlbumName, sharedPhotoCaption, sharedPhotoLink));
        	}
    		if (mp != null) {
				this.bestMatches.add(mp);
			}
    		
    		stmt.executeUpdate("drop view top_n");
        	rst.close();
            stmt.close();
        } catch (SQLException err) {
            System.err.println(err.getMessage());
        }
    	
    	/*
        Long girlUserId = 123L;
        String girlFirstName = "girlFirstName";
        String girlLastName = "girlLastName";
        int girlYear = 1988;
        Long boyUserId = 456L;
        String boyFirstName = "boyFirstName";
        String boyLastName = "boyLastName";
        int boyYear = 1986;
        MatchPair mp = new MatchPair(girlUserId, girlFirstName, girlLastName,
                girlYear, boyUserId, boyFirstName, boyLastName, boyYear);
        String sharedPhotoId = "12345678";
        String sharedPhotoAlbumId = "123456789";
        String sharedPhotoAlbumName = "albumName";
        String sharedPhotoCaption = "caption";
        String sharedPhotoLink = "link";
        mp.addSharedPhoto(new PhotoInfo(sharedPhotoId, sharedPhotoAlbumId,
                sharedPhotoAlbumName, sharedPhotoCaption, sharedPhotoLink));
        this.bestMatches.add(mp);
        */
    }

    // **** Query 6 ****
    // Suggest users based on mutual friends
    //
    // Find the top n pairs of users in the database who have the most
    // common friends, but are not friends themselves.
    //
    // Your output will consist of a set of pairs (user1_id, user2_id)
    // No pair should appear in the result twice; you should always order the pairs so that
    // user1_id < user2_id
    //
    // If there are ties, you should give priority to the pair with the smaller user1_id.
    // If there are still ties, give priority to the pair with the smaller user2_id.
    //
    @Override
    public void suggestFriendsByMutualFriends(int n) {
    	try (Statement stmt = oracleConnection.createStatement()) {
        	
    		stmt.executeUpdate("create or replace view friends_2 as " +
    				"select F1.user1_id, F1.user2_id from " + friendsTableName + " F1 " +
    				"union select F2.user2_id, F2.user1_id from " + friendsTableName + " F2");
    		stmt.executeUpdate("create or replace view friends_common as " +
    				"select F1.user1_id as u1_id, F2.user1_id as u2_id, F1.user2_id as u3_id " +
    				"from friends_2 F1, friends_2 F2 " +
    				"where F1.user1_id < F2.user1_id and F1.user2_id = F2.user2_id");
    		stmt.executeUpdate("create or replace view top_n2 as " +
    				"select u1_id, u2_id " +
    				"from (select u1_id, u2_id, count(u3_id) " +
    				"from friends_common " +
    				"where not exists (select * from " + friendsTableName + " F where F.user1_id = u1_id and F.user2_id = u2_id) " +
    				"group by u1_id, u2_id " +
    				"order by 3 desc, 1 asc, 2 asc) " +
    				"where rownum <= " + n);
    		
    		ResultSet rst = stmt.executeQuery("select T.u1_id, T.u2_id, F.u3_id, U1.first_name, U1.last_name, U2.first_name, U2.last_name, U3.first_name, U3.last_name " +
                    "from top_n2 T, friends_common F, " + userTableName + " U1, " + userTableName + " U2, " + userTableName + " U3 " +
                    "where T.u1_id = F.u1_id and T.u2_id = F.u2_id and T.u1_id = U1.user_id and T.u2_id = U2.user_id and F.u3_id = U3.user_id " +
    				"order by 1 asc, 2 asc");
    		
    		Long user1_id = null;
    		Long user2_id = null;
    		UsersPair p = null;
    		while (rst.next()) {
        		if (user1_id == null || (!user1_id.equals(rst.getLong(1)) && !user2_id.equals(rst.getLong(2)))) {
        			if (p != null) {
        				this.suggestedUsersPairs.add(p);
        			}
        			user1_id = rst.getLong(1);
        	        String user1FirstName = rst.getNString(4);
        	        String user1LastName = rst.getNString(5);
        	        user2_id = rst.getLong(2);
        	        String user2FirstName = rst.getNString(6);
        	        String user2LastName = rst.getNString(7);
        	        p = new UsersPair(user1_id, user1FirstName, user1LastName, user2_id, user2FirstName, user2LastName);
        		}
        		p.addSharedFriend(rst.getLong(3), rst.getNString(8), rst.getNString(9));
        	}
    		if (p != null) {
				this.suggestedUsersPairs.add(p);
			}
    		
    		stmt.executeUpdate("drop view top_n2");
    		stmt.executeUpdate("drop view friends_common");
    		stmt.executeUpdate("drop view friends_2");
        	rst.close();
            stmt.close();
        } catch (SQLException err) {
            System.err.println(err.getMessage());
        }
    	
    	/*
        Long user1_id = 123L;
        String user1FirstName = "User1FirstName";
        String user1LastName = "User1LastName";
        Long user2_id = 456L;
        String user2FirstName = "User2FirstName";
        String user2LastName = "User2LastName";
        UsersPair p = new UsersPair(user1_id, user1FirstName, user1LastName, user2_id, user2FirstName, user2LastName);

        p.addSharedFriend(567L, "sharedFriend1FirstName", "sharedFriend1LastName");
        p.addSharedFriend(678L, "sharedFriend2FirstName", "sharedFriend2LastName");
        p.addSharedFriend(789L, "sharedFriend3FirstName", "sharedFriend3LastName");
        this.suggestedUsersPairs.add(p);
        */
    }

    @Override
    // ***** Query 7 *****
    //
    // Find the name of the state with the most events, as well as the number of
    // events in that state.  If there is a tie, return the names of all of the (tied) states.
    //
    public void findEventStates() {
    	
    	try (Statement stmt = oracleConnection.createStatement()) {
        	
        	ResultSet rst = stmt.executeQuery("select distinct state_name, count(*) " +
        			"from " + eventTableName + " E left join " + cityTableName + " C on E.event_city_id = C.city_id " +
        			"where state_name is not null " +
                    "group by state_name " +
                    "having count(*) = (select max(count(*)) from " + eventTableName + " E left join " + cityTableName + " C on E.event_city_id = C.city_id where state_name is not null group by state_name)");
        	while (rst.next()) {
        		this.popularStateNames.add(rst.getString(1));
        		this.eventCount = rst.getInt(2);
            }
        	
        	rst.close();
            stmt.close();
        } catch (SQLException err) {
            System.err.println(err.getMessage());
        }
    	
    	/*
        this.eventCount = 12;
        this.popularStateNames.add("Michigan");
        this.popularStateNames.add("California");
        */
    }

    //@Override
    // ***** Query 8 *****
    // Given the ID of a user, find information about that
    // user's oldest friend and youngest friend
    //
    // If two users have exactly the same age, meaning that they were born
    // on the same day, then assume that the one with the larger user_id is older
    //
    public void findAgeInfo(Long user_id) {
    	
    	try (Statement stmt = oracleConnection.createStatement()) {
        	
        	ResultSet rst = stmt.executeQuery("select * " +
        			"from (select user_id, first_name, last_name " +
                    "from " + userTableName +
                    " where user_id in (select F1.user1_id from " + friendsTableName + " F1 where F1.user2_id = " + user_id + " union select F2.user2_id from " + friendsTableName + " F2 where F2.user1_id = " + user_id + ") " +
                    " order by year_of_birth asc, month_of_birth asc, day_of_birth asc, user_id desc) " +
                    "where rownum <= 1");
        	while (rst.next()) {
        		Long uid = rst.getLong(1);
                String firstName = rst.getString(2);
                String lastName = rst.getString(3);
                this.oldestFriend = new UserInfo(uid, firstName, lastName);
            }
        	
        	rst = stmt.executeQuery("select * " +
        			"from (select user_id, first_name, last_name " +
                    "from " + userTableName +
                    " where user_id in (select F1.user1_id from " + friendsTableName + " F1 where F1.user2_id = " + user_id + " union select F2.user2_id from " + friendsTableName + " F2 where F2.user1_id = " + user_id + ") " +
                    " order by year_of_birth desc, month_of_birth desc, day_of_birth desc, user_id asc) " +
                    "where rownum <= 1");
        	while (rst.next()) {
        		Long uid = rst.getLong(1);
                String firstName = rst.getString(2);
                String lastName = rst.getString(3);
                this.youngestFriend = new UserInfo(uid, firstName, lastName);
            }
        	
        	rst.close();
            stmt.close();
        } catch (SQLException err) {
            System.err.println(err.getMessage());
        }
    	
    	/*
        this.oldestFriend = new UserInfo(1L, "Oliver", "Oldham");
        this.youngestFriend = new UserInfo(25L, "Yolanda", "Young");
        */
    }

    @Override
    //	 ***** Query 9 *****
    //
    // Find pairs of potential siblings.
    //
    // A pair of users are potential siblings if they have the same last name and hometown, if they are friends, and
    // if they are less than 10 years apart in age.  Pairs of siblings are returned with the lower user_id user first
    // on the line.  They are ordered based on the first user_id and in the event of a tie, the second user_id.
    //
    //
    public void findPotentialSiblings() {
    	
    	try (Statement stmt = oracleConnection.createStatement()) {
        	
        	ResultSet rst = stmt.executeQuery("select U1.user_id, U1.first_name, U1.last_name, U2.user_id, U2.first_name, U2.last_name " +
                    "from " + userTableName + " U1, " + userTableName + " U2, " + hometownCityTableName + " H1, " + hometownCityTableName + " H2 " +
                    "where U1.user_id < U2.user_id and U1.last_name = U2.last_name and U1.user_id = H1.user_id and U2.user_id = H2.user_id and H1.hometown_city_id = H2.hometown_city_id " +
                    "and exists(select * from " + friendsTableName + " F where (F.user1_id = U1.user_id and F.user2_id = U2.user_id) or (F.user1_id = U2.user_id and F.user2_id = U1.user_id)) " +
                    "and U1.year_of_birth - U2.year_of_birth >= -10 and U2.year_of_birth - U1.year_of_birth >= -10 " +
                    "order by U1.user_id asc, U2.user_id asc");
        	
        	while (rst.next()) {
        		Long user1_id = rst.getLong(1);
                String user1FirstName = rst.getString(2);
                String user1LastName = rst.getString(3);
                Long user2_id = rst.getLong(4);
                String user2FirstName = rst.getString(5);
                String user2LastName = rst.getString(6);
                SiblingInfo s = new SiblingInfo(user1_id, user1FirstName, user1LastName, user2_id, user2FirstName, user2LastName);
                this.siblings.add(s);
            }
        	
        	rst.close();
            stmt.close();
        } catch (SQLException err) {
            System.err.println(err.getMessage());
        }
    	
    	/*
        Long user1_id = 123L;
        String user1FirstName = "User1FirstName";
        String user1LastName = "User1LastName";
        Long user2_id = 456L;
        String user2FirstName = "User2FirstName";
        String user2LastName = "User2LastName";
        SiblingInfo s = new SiblingInfo(user1_id, user1FirstName, user1LastName, user2_id, user2FirstName, user2LastName);
        this.siblings.add(s);
        */
    }

}
