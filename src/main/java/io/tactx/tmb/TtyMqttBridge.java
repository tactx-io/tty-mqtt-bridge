package io.tactx.tmb;

/**
 * Created by andy from tactx on 25.04.2018.
 */


//import org.apache.commons.cli.*;

import org.apache.commons.cli.*;


public class TtyMqttBridge {
    public static void main(String[] args) {
        // parse command line arguments
        boolean verbose = true;
        Options options = new Options();

        Option broker_url_option = new Option("b", "broker-url", true, "url to the broker");
        broker_url_option.setRequired(true);

        Option broker_port_option = new Option("p", "broker-port", true, "port of the broker");
        broker_port_option.setRequired(true);

        Option topic_option = new Option("x", "topic", true, "topic to publish under");
        topic_option.setRequired(true);

        Option clientid_option = new Option("c", "clientid", true, "client id");
        clientid_option.setRequired(true);

        Option tty_option = new Option("t", "tty", true, "local tty interface to publish the data from");
        tty_option.setRequired(true);


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

        if(verbose){
            System.out.println("tty-mqtt-bridge");
            System.out.println("using mqtt broker " + cmd.getOptionValue("broker-url") + " on port " + cmd.getOptionValue("broker-port"));
            System.out.println("Connecting...");
            System.out.println("using tty" + cmd.getOptionValue("tty"));
            System.out.println("Connecting...");
        }

        String topic        = cmd.getOptionValue("topic");
        int qos             = 2;
        String broker       = cmd.getOptionValue("broker-url") + ":" + cmd.getOptionValue("broker-port");
        String clientId     = cmd.getOptionValue("clientid");
    }
}
