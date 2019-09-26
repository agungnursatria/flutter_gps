import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() => runApp(MyApp());

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: MainPage(),
    );
  }
}

class MainPage extends StatefulWidget {
  @override
  _MainPageState createState() => _MainPageState();
}

class _MainPageState extends State<MainPage> {
  String txt = 'Activate GPS';
  var mc = MethodChannel('gps');

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
        child: RaisedButton(
          child: Text(txt),
          onPressed: activateGps,
        ),
      ),
    );
  }

  void activateGps() async {
    try {
      await mc.invokeMethod('turn on gps').then((result) {
        Map map = json.decode(result);
        var lat = map['lat'];
        var long = map['long'];
        setState(() {
          txt = 'Lat: $lat, long: $long' ?? 'Failed activate GPS';
        });
      });
    } catch (e) {
      setState(() {
        txt = e;
      });
    }
  }
}
