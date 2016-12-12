//find the oldest friend for each user who has a friend. For simplicity, use only year of birth to determine age, if there is a tie, use the one with smallest user_id
//return a javascript object : key is the user_id and the value is the oldest_friend id
//You may find query 2 and query 3 helpful. You can create selections if you want. Do not modify users collection.
//
//You should return something like this:(order does not matter)
//{user1:userx1, user2:userx2, user3:userx3,...}

function oldest_friend(dbname){
  db = db.getSiblingDB(dbname)

  //implementation goes here
	var o = {};
	db.friends.drop();
	db.runCommand({create: "friends"}, {size: 2 * db.flat_users.find().count()});
	db.flat_users.find().forEach(function(d) {
		db.runCommand({insert: "friends", documents: [ {"u1": d.user_id, "u2": d.friends }, {"u1": d.friends, "u2": d.user_id } ] } );
	});
	var yob = {};
	db.users.find().forEach(function(d) {yob[d.user_id] = d.YOB;});
	db.friends.aggregate([
		{$group: {_id: "$u1", f: {$addToSet: "$u2"}}}
	]).forEach(function(d) {
		var k = d._id;
		var y = yob[d.f[0]];
		var v = d.f[0];
		for (i = 1; i < d.f.length; i++) {
			if (Math.min(y, yob[d.f[i]]) != y) {	//consider year as number
				y = yob[d.f[i]];
				v = d.f[i];
			}
			else if (Math.max(y, yob[d.f[i]]) == y) {
				v = Math.min(v, d.f[i]);
			}
		}
		o[k] = v;
	});
	return o;

  //return an javascript object described above

}
