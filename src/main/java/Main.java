//package io.tactx.tmb;

import gnu.io.CommPortIdentifier;
import org.apache.commons.cli.*;

import java.util.Enumeration;

/**
 * Created by a.poelzleithner on 26.04.2018.
 */
public class Main {
    public static void main(String[] args) {
        // first of all parse command line arguments
        boolean verbose = true;
        Options options = new Options();

        Option listport_option = new Option("l", "list", false, "list all tty ports");
        listport_option.setRequired(false);

        Option broker_url_option = new Option("b", "broker-url", true, "url to the broker");
        broker_url_option.setRequired(false);

        Option broker_port_option = new Option("p", "broker-port", true, "port of the broker");
        broker_port_option.setRequired(false);

        Option topic_option = new Option("x", "topic", true, "topic to publish under");
        topic_option.setRequired(false);

        Option clientid_option = new Option("c", "clientid", true, "client id");
        clientid_option.setRequired(false);

        Option tty_option = new Option("t", "tty", true, "local tty interface to publish the data from");
        tty_option.setRequired(false);

        options.addOption(listport_option);
        options.addOption(broker_url_option);
        options.addOption(broker_port_option);
        options.addOption(tty_option);
        options.addOption(clientid_option);
        options.addOption(topic_option);

        CommandLineParser parser = new BasicParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("tty-mqtt-bridge", options);

            System.exit(1);
            return;
        }


        // prepare the paramters
        String topic = cmd.getOptionValue("topic");
        String ttydevice = cmd.getOptionValue("tty");
        int qos = 2;
        String broker = cmd.getOptionValue("broker-url") + ":" + cmd.getOptionValue("broker-port");
        String clientId = cmd.getOptionValue("clientid");


        Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();


        if (cmd.hasOption("list")) {
            while (portEnum.hasMoreElements()) {
                CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
                System.out.println(currPortId.getName());
            }
            return;
        }


        if (verbose) {
            System.out.println("tty-mqtt-bridge");
            System.out.println("Reading data from: " + ttydevice);
            System.out.println("Publish under:     " + broker);
            System.out.println("Under the topic:   " + topic);
            System.out.println("With QOS:          " + qos);
        }

        CommPortIdentifier currPortId = TtyMqttBridge.ttyPort(ttydevice);
        if(currPortId == null){
            System.out.println("Port " + ttydevice + " does not exist");
        }
        TtyMqttBridge bridge;
        if (!cmd.hasOption("list")) {
            bridge = new TtyMqttBridge(currPortId);
        }else{
            bridge = new TtyMqttBridge(currPortId, broker, topic);
            System.out.println("Starting bridge reading from " + currPortId + ", to broker " + broker + " with topic " + topic);
        }

        bridge.initialize();
    }

}
