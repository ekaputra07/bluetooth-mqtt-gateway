# bluetooth-mqtt-gateway

This app is a modified version of [SimpleBluetoothTerminal](https://github.com/kai-morich/SimpleBluetoothTerminal), where I added the ability for the app to publish the Bluetooth message to an MQTT broker. This is part of a project that I'm working on where I need to receive data from Arduino and push them to an MQTT broker, and the rest of the system will handle that data.

Here's where it sits in my system:

```mermaid
flowchart LR
    1a[Sensor 1] --> 2[Arduino];
    1b[Sensor 2] --> 2[Arduino];
    1c[Sensor 3] --> 2[Arduino];
    2[Arduino] -. bluetooth .-> 3[Android Phone w/ bluetooth-mqtt-gateway];
    3[Android Phone w/ bluetooth-mqtt-gateway] <-. internet .-> 4[MQTT Broker];
    4[MQTT Broker] <-. internet .-> 5[Subsciber 1];
    4[MQTT Broker] <-. internet .-> 6[Subsciber 2];
```
