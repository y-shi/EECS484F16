import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;



//json.simple 1.1
// import org.json.simple.JSONObject;
// import org.json.simple.JSONArray;

// Alternate implementation of JSON modules.
import org.json.JSONObject;
import org.json.JSONArray;

public class GetData{
	
    static String prefix = "tajik.";
	
    // You must use the following variable as the JDBC connection
    Connection oracleConnection = null;
	
    // You must refer to the following variables for the corresponding 
    // tables in your database

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

    // This is the data structure to store all users' information
    // DO NOT change the name
    JSONArray users_info = new JSONArray();		// declare a new JSONArray

	
    // DO NOT modify this constructor
    public GetData(String u, Connection c) {
	super();
	String dataType = u;
	oracleConnection = c;
	// You will use the following tables in your Java code
	cityTableName = prefix+dataType+"_CITIES";
	userTableName = prefix+dataType+"_USERS";
	friendsTableName = prefix+dataType+"_FRIENDS";
	currentCityTableName = prefix+dataType+"_USER_CURRENT_CITY";
	hometownCityTableName = prefix+dataType+"_USER_HOMETOWN_CITY";
	programTableName = prefix+dataType+"_PROGRAMS";
	educationTableName = prefix+dataType+"_EDUCATION";
	eventTableName = prefix+dataType+"_USER_EVENTS";
	albumTableName = prefix+dataType+"_ALBUMS";
	photoTableName = prefix+dataType+"_PHOTOS";
	tagTableName = prefix+dataType+"_TAGS";
    }
	
	
	
	
    //implement this function

    @SuppressWarnings("unchecked")
    public JSONArray toJSON() throws SQLException{ 
		
	// Your implementation goes here....		

    JSONArray users_info = new JSONArray();
    HashMap<Long, JSONObject> map = new HashMap<Long, JSONObject>();
    HashMap<Long, JSONArray> mapFriends = new HashMap<Long, JSONArray>();
    
    try (Statement stmt = oracleConnection.createStatement()) {
    	
    	ResultSet rst = stmt.executeQuery("select user_id, first_name, last_name, gender, year_of_birth, month_of_birth, day_of_birth " +
    			"from " + userTableName);
    	while (rst.next()) {
    		JSONObject o = new JSONObject();
    		map.put(rst.getLong(1), o);
    		o.put("user_id", rst.getLong(1));
    		o.put("first_name", rst.getString(2));
    		o.put("last_name", rst.getString(3));
    		o.put("gender", rst.getString(4));
    		o.put("YOB", rst.getInt(5));
    		o.put("MOB", rst.getInt(6));
    		o.put("DOB", rst.getInt(7));
        }
    	
    	rst = stmt.executeQuery("select H.user_id, C.city_name, C.state_name, C.country_name " +
    			"from " + hometownCityTableName + " H, " + cityTableName + " C " + 
    			"where H.hometown_city_id = C.city_id");
    	while (rst.next()) {
    		if (!map.containsKey(rst.getLong(1))) {
    			continue;
    		}
    		JSONObject o = new JSONObject();
    		map.get(rst.getLong(1)).put("hometown", o);
    		o.put("city", rst.getString(2));
    		o.put("state", rst.getString(3));
    		o.put("country", rst.getString(4));
        }
    	
    	rst = stmt.executeQuery("select user1_id, user2_id " +
    			"from " + friendsTableName);
    	while (rst.next()) {
    		Long u1ID = Math.min(rst.getLong(1), rst.getLong(2));
    		Long u2ID = Math.max(rst.getLong(1), rst.getLong(2));
    		if (!mapFriends.containsKey(u1ID)) {
    			mapFriends.put(u1ID, new JSONArray());
    		}
    		mapFriends.get(u1ID).put(u2ID);
        }
    	
    	rst.close();
        stmt.close();
    } catch (SQLException err) {
        System.err.println(err.getMessage());
    }
    
    for (Long l : map.keySet()) {
    	JSONObject o = map.get(l);
    	users_info.put(o);
    	if (mapFriends.containsKey(l)) {
    		o.put("friends", mapFriends.get(l));
    	}
    	else {
    		o.put("friends", new JSONArray());
    	}
    }
    
	/*	
	// This is an example usage of JSONArray and JSONObject
	// The array contains a list of objects
	// All user information should be stored in the JSONArray object: users_info
	// You will need to DELETE this stuff. This is just an example.

	// A JSONObject is an unordered collection of name/value pairs. Add a few name/value pairs.
	JSONObject test = new JSONObject();	// declare a new JSONObject
	// A JSONArray consists of multiple JSONObjects. 
	JSONArray users_info = new JSONArray();

	test.put("user_id", "testid");		// populate the JSONObject
	test.put("first_name", "testname");

	JSONObject test2 = new JSONObject();
	test2.put("user_id", "test2id");
	test2.put("first_name", "test2name");

	// users_info.add(test);			// add the JSONObject to JSONArray	
	// users_info.add(test2);			// add the JSONObject to JSONArray	

	// Use put method if using the alternate JSON modules.
	users_info.put(test);		// add the JSONObject to JSONArray     
	users_info.put(test2);		// add the JSONObject to JSONArray	
	*/
    return users_info;
	}

    // This outputs to a file "output.json"
    public void writeJSON(JSONArray users_info) {
	// DO NOT MODIFY this function
	try {
	    FileWriter file = new FileWriter(System.getProperty("user.dir")+"/output.json");
	    file.write(users_info.toString());
	    file.flush();
	    file.close();

	} catch (IOException e) {
	    e.printStackTrace();
	}
		
    }
}

