//query1 : find users who live in the specified city. 
// Returns an array of user_ids.

function find_user(city, dbname){
    db = db.getSiblingDB(dbname)
    //implementation goes here

	var a = [];
	db.users.find({"hometown.city" : city}).forEach(function(u) {a.push(u.user_id)});
	return a;
    // returns a Javascript array. See test.js for a partial correctness check.  
    // This will be  an array of integers. The order does not matter.                                                               

}
