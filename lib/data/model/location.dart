class Location {
  double lat;
  double long;
  Location({this.lat = 0, this.long = 0});

  Location.parseJson(Map<String, dynamic> response) {
    lat = double.parse(response['lat'] ?? 0);
    long = double.parse(response['long'] ?? 0);
  }
}
