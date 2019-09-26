import 'package:flutter/material.dart';
import 'package:flutter_gps/channel/gps/channel_gps.dart';

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
  GpsChannel gpsChannel = GpsChannel();

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
    await gpsChannel.getCurrentLocation().then((location) {
      // Todo: Use bloc to change set state!
      setState(() {
        txt = 'Lat: ${location.lat}, long: ${location.long}' ??
            'Failed activate GPS';
      });
    });
  }
}
