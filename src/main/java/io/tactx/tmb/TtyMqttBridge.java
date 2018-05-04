package io.tactx.tmb;

/**
 * Created by andy from tactx on 25.04.2018.
 */


//import org.apache.commons.cli.*;


import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import org.apache.commons.cli.*;
import gnu.io.CommPortIdentifier;
import org.eclipse.paho.client.mqttv3.*;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Enumeration;


public class TtyMqttBridge implements SerialPortEventListener {

    private static final int TIME_OUT = 2000;
    private static final int DATA_RATE = 9600;

    BufferedReader input;
    OutputStream output;

    private MqttClient mClient;


    CommPortIdentifier mCommPortIdentifier;
    String mBrokerUrl  = "iot.eclipse.org";

    SerialPort serialPort;


    public static CommPortIdentifier ttyPort(String tty_name) {
        Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();
        CommPortIdentifier currPortId = null;
        while (portEnum.hasMoreElements()) {

            currPortId = (CommPortIdentifier) portEnum.nextElement();
            String portname = currPortId.getName();
            if (portname.equals(tty_name)) {
                return currPortId;
            }
        }
        return null;
    }


    public TtyMqttBridge(CommPortIdentifier currPortId, String brokerUrlAndPort) {
        mCommPortIdentifier = currPortId;
        mBrokerUrl = brokerUrlAndPort;
    }

    public void initialize() {

        try {
            serialPort = (SerialPort) mCommPortIdentifier.open(this.getClass().getName(),
                    TIME_OUT);
            serialPort.setSerialPortParams(DATA_RATE,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);

            // open the streams
            input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
            output = serialPort.getOutputStream();


            // output.write("measure 1000".getBytes());
            serialPort.addEventListener(this);
            serialPort.notifyOnDataAvailable(true);
            this.connect();
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    public synchronized void serialEvent(SerialPortEvent oEvent) {
        if (oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
            try {
                String inputLine = null;
                if (input.ready()) {
                    inputLine = input.readLine();


                    if(mClient != null){
                        if(mClient.isConnected()){
                            MqttMessage message = new MqttMessage(inputLine.getBytes());
                            message.setQos(2);

                            // Send the message to the server, control is not returned until
                            // it has been delivered to the server meeting the specified
                            // quality of service.
                            //
                            //
                            System.out.println("Publish " + inputLine);

                            mClient.publish("kiska/amessage", message);
                        }
                    }
                }

            } catch (Exception e) {
                System.err.println("seriral in error " + e.toString());
            }
        }
        // Ignore all the other eventTypes, but you should consider the other ones.
    }

    public synchronized void close() {
        if (serialPort != null) {
            serialPort.removeEventListener();
            serialPort.close();
        }
    }



    public void connect() throws MqttException {
        System.out.println("Connecting to " + mBrokerUrl);
        mClient = new MqttClient("tcp://iot.eclipse.org:1883", "pubsub-1");
        mClient.setCallback(mCallback);
        mClient.connect();
    }


    MqttCallback mCallback = new MqttCallback() {
        public void connectionLost(Throwable t) {
            try {
                connect();
            } catch (MqttException e) {
                System.out.println("Connection error " + e.getMessage() + " --- " + e.toString());
            }
        }

        public void messageArrived(String topic, MqttMessage message) throws Exception {
            System.out.println("topic - " + topic + ": " + new String(message.getPayload()));
        }

        public void deliveryComplete(IMqttDeliveryToken token) {
            System.out.println("message sent");
        }

    };

}