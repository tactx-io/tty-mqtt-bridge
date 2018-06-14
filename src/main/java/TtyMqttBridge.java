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
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
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

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");


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

    public TtyMqttBridge(CommPortIdentifier currPortId) {
        mCommPortIdentifier = currPortId;
        mBrokerUrl = null;
        mBaseTopic = null;
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
            if(this.mBrokerUrl != null)
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

                    /* desired jason structure
                     *
                     * EXPECTED INCOMING JSON OBJECT
                     *
                     * {
                     *    "topic-name":"water-temperatures",
                     *    "payload":{
                     *       "values":[
                     *          "22.55",
                     *          "22.55",
                     *          "22.55",
                     *          "22.55",
                     *         "22.55"
                     *       ]
                     *    }
                     * }
                     *
                     * The topic name will be concatenated with the value of mBaseTopic to the full topic name
                     * The
                     *
                     * FORMT FOR THE MQTT TOPIC PAYLOAD
                     * {
                     *    "timestamp":"123578",
                     *    "payload":{
                     *       "values":[
                     *          "22.55",
                     *          "22.55",
                     *          "22.55",
                     *          "22.55",
                     *         "22.55"
                     *       ]
                     *    }
                     * }

                    */
                    // interpret incoming strin as JSON
                    JSONObject jsonObj = new JSONObject(inputLine);

                    // first parse the parameter "topic-name"
                    String topic_name = jsonObj.getString("topic-name");

                    // get the payload object
                    JSONObject payload = jsonObj.getJSONObject("payload");

                    // check if available
                    if(payload == null){
                        System.out.println("not able to parse \"payload\" name");
                        return;
                    }

                    // concatenate to full topic name
                    String full_topic_name = mBaseTopic + "/" + topic_name;

                    // create the wrapping object
                    JSONObject mqtt_json = new JSONObject();

                    // get the timestamp
                    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                    Date date = new Date();

                  //  mqtt_json.put("timestamp", timestamp);
                  //  mqtt_json.put("timestamp", new Timestamp(date.getTime()));
                    mqtt_json.put("timestamp", timestamp.getTime());
                  //  mqtt_json.put("timestamp", sdf.format(timestamp));

                    mqtt_json.put("payload", payload);


                    MqttMessage message = new MqttMessage(mqtt_json.toString().getBytes());
                    if(mClient != null){
                        if(mClient.isConnected()){
                            // instead of doing this wrap the payload into a json element
                            // including the timestamp
                            message.setRetained(true);
                            message.setQos(2);
                           mClient.publish(full_topic_name , message);
                        }
                    }
                    System.out.println("Publish " + full_topic_name + ":" + message.toString());
                }

            } catch (Exception e) {
                System.err.println("seriral in error " + e.toString());
            }
        }
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
