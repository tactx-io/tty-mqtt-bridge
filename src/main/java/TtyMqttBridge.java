//package io.tactx.tmb;

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
import org.json.JSONObject;


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

    private String mBaseTopic;


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


    public TtyMqttBridge(CommPortIdentifier currPortId, String brokerUrlAndPort, String basetopic) {
        mCommPortIdentifier = currPortId;
        mBrokerUrl = brokerUrlAndPort;
        mBaseTopic = basetopic;
    }

    public void initialize() {

        try {
            serialPort = (SerialPort) mCommPortIdentifier.open(this.getClass().getName(),
                    TIME_OUT);
            serialPort.setSerialPortParams(DATA_RATE,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);

            input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
            output = serialPort.getOutputStream();

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

                    System.out.println("Parsing line: " + inputLine);
                    JSONObject jsonObj = new JSONObject(inputLine);

                    String topicobj = jsonObj.getString("topic-name");
                    String valuesobj = jsonObj.getJSONArray("values").toString();

                  //  if(topicobj == null){
                    //    System.out.println("not able to parse \"topic\" name");
                   // }
                    if(valuesobj == null){
                        System.out.println("not able to parse \"valuse\" name");
                    }

                    if(mClient != null){
                        if(mClient.isConnected()){
                            String topicpath = mBaseTopic + "/" + topicobj;
                            MqttMessage message = new MqttMessage(valuesobj.getBytes());
                            message.setQos(2);

                            System.out.println("Publish " + topicpath + ":" + message.toString());

                           mClient.publish(topicpath , message);
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
        String url = "tcp://"+ mBrokerUrl;
        System.out.println("Connecting to " + url);
        mClient = new MqttClient(url, "pubsub-1");
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
