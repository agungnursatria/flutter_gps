import 'dart:convert';

import 'package:flutter/services.dart';
import 'package:flutter_gps/data/model/location.dart';

class GpsChannel {
  MethodChannel channel = const MethodChannel('com.payfazz.Fazzcard/gps');

  Future<Location> getCurrentLocation() async {
    return await channel
        .invokeMethod('get_current_location')
        .then((stringResponse) {
      Map map = json.decode(stringResponse);
      return Location.parseJson(map);
    }).catchError((e) {
      print(e);
    });
  }
}
