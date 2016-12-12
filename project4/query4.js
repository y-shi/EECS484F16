
// query 4: find user pairs such that, one is male, second is female,
// their year difference is less than year_diff, and they live in same
// city and they are not friends with each other. Store each user_id
// pair as arrays and return an array of all the pairs. The order of
// pairs does not matter. Your answer will look something like the following:
// [
//      [userid1, userid2],
//      [userid1, userid3],
//      [userid4, userid2],
//      ...
//  ]
// In the above, userid1 and userid4 are males. userid2 and userid3 are females.
// Besides that, the above constraints are satisifed.
// userid is the field from the userinfo table. Do not use the _id field in that table.

  
function suggest_friends(year_diff, dbname) {
    db = db.getSiblingDB(dbname)
    //implementation goes here

	var ret = [];
	db.users.find({gender: "male"}).forEach(function(u1) {
		db.users.find({"gender": "female", "hometown.city": u1.hometown.city, "YOB": {$gt: u1.YOB - year_diff, $lt: u1.YOB + year_diff}}).forEach(function(u2) {
			if (!db.flat_users.find({"user_id": Math.min(u1.user_id, u2.user_id), "friends": Math.max(u1.user_id, u2.user_id)}).hasNext()) ret.push([u1.user_id, u2.user_id]);
		});
	});
	return ret;
  
    // Return an array of arrays.
}
