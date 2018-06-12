# tty-mqtt-bridge

## Brief
This project is part of a sensor network for reading environmental data.
The purpose of this project in particular is to read data from a serial port and forward it to a MQTT broker.

## Function
The program first opens the desired tty port and then connects to the MQTT broker. Both, serial port settigns and broker settings can be passed as commandline arguments when the programm is started.
Once up and running the program does not interpret the data coming in, yet the actual payload must be wrapped into a JSON format in order to know which topic to publish under.

The program is intended to be used with an Arduino based sensor controller.


## Installation 
This project requires the native libraries for rxtx installed on the target machine
Widows: http://rxtx.qbang.org/wiki/index.php/Installation_for_Windows
Raspbian: Todo
## Run
In order publish on a remote broker use 
```
java -jar -Djava.library.path=/usr/lib/jni tty-mqtt-bridge-1.0-SNAPSHOT.jar  -b iot.eclipse.org -p 1883 -t /dev/ttyACM0 -x <basetopic>/ -c mqttandi
```
If your system is configured with a local broker (on the same machine) use to following command
``
java -jar -Djava.library.path=/usr/lib/jni tty-mqtt-bridge-1.0-SNAPSHOT.jar  -b 127.0.0.1 -p 1883 -t /dev/ttyACM0 -x <basetopic>/ -c mqttandi
``






https://stackoverflow.com/questions/1051640/correct-way-to-add-external-jars-lib-jar-to-an-intellij-idea-project