// query 7: For each city, find the average friend count per user in that city using
// MapReduce. Using the same terminology as in query6, we are asking
// you to write the mapper, reducer and finalizer to find the average
// friend count for each city.


var city_average_friendcount_mapper = function() {
  // implement the Map function of average friend count
	emit(this.hometown.city, {uCount: 1, fCount: this.friends.length});
};

var city_average_friendcount_reducer = function(key, values) {
  // implement the reduce function of average friend count
	var v = {uCount: 0, fCount: 0}
	for (i = 0; i < values.length; i++) {
		v.uCount += values[i].uCount;
		v.fCount += values[i].fCount;
	}
	return v;
};

var city_average_friendcount_finalizer = function(key, reduceVal) {
  // We've implemented a simple forwarding finalize function. This implementation 
  // is naive: it just forwards the reduceVal to the output collection.
  // Feel free to change it if needed. However, please keep this unchanged:
  // the var ret should be the average friend count per user of each city.
  return 1.0 * reduceVal.fCount / reduceVal.uCount;
}
