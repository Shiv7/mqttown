package com.zuan.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.FileWriter;
import java.util.Calendar;
import java.util.Date;


@SpringBootApplication
public class MqttApplication {

    final static Integer[] pole2Data = new Integer[1];
    final static Integer[] pole1Data = new Integer[1];
    private static Date triggerTimePole1 = new Date();
    private static Date triggerTimePole2 = new Date();

    private static Logger logger = LoggerFactory.getLogger(MqttApplication.class);

    public static void main(String[] args) throws Exception {

        File csvFile = new File("pole1.csv");
        FileWriter fileWriter1 = new FileWriter(csvFile);
        File csvFile1 = new File("pole2.csv");
        FileWriter fileWriter2 = new FileWriter(csvFile1);
        Mqtt mqtt = new Mqtt();
        IMqttClient iMqttClient = mqtt.getInstance();
        iMqttClient.subscribeWithResponse("pole2/data", (s, mqttMessage) -> {
            try {
                pole2Data[0] = Integer.parseInt(new String(mqttMessage.getPayload()));
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.HOUR, 5);
                calendar.add(Calendar.MINUTE, 30);
                Date dateCurrent = calendar.getTime();
                String c = "Lidar 2" + "," + dateCurrent.toString() + "," + pole2Data[0] + "\n";
                fileWriter1.write(c);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        iMqttClient.subscribeWithResponse("pole1/data", (s1, mqttMessage1) -> {
            String data = new String(mqttMessage1.getPayload());
            pole1Data[0] = Integer.parseInt(data);
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.HOUR, 5);
            calendar.add(Calendar.MINUTE, 30);
            Date dateCurrent = calendar.getTime();
            String c = "Lidar 1" + "," + dateCurrent.toString() + "," + pole1Data[0] + "\n";
            fileWriter2.write(c);
        });
        SpringApplication.run(MqttApplication.class, args);
        while (true) {
            try {
                check1();
                check2();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void check2() throws MqttException {
        Mqtt mqtt = new Mqtt();
        IMqttClient iMqttClient = mqtt.getInstance();
        MqttMessage message = new MqttMessage("1".getBytes());
        message.setQos(2);
        logger.info("pole 2 data: {}", pole2Data[0]);
        if (((pole2Data[0] < 6000) && (pole2Data[0] > 500)) || (new Date().getTime() < (triggerTimePole1.getTime() + 5000))) {
            logger.info("vechile detected at pole 2: {}", pole2Data[0]);
            if ((pole2Data[0] < 6000) && (pole2Data[0] > 500)) {
                triggerTimePole1 = new Date();
            }
            if (pole1Data[0] < 3000 && pole1Data[0] > 500) {
                iMqttClient.publish("pole1/horn", message);
                logger.info("Horn signal send to pole 1 data: {}", message);
            }
        }
    }

    public static void check1() throws MqttException {
        Mqtt mqtt = new Mqtt();
        IMqttClient iMqttClient = mqtt.getInstance();
        MqttMessage message = new MqttMessage("1".getBytes());
        message.setQos(2);
        logger.info("pole 1 data: {}", pole1Data[0]);
        if (((pole1Data[0] < 3000) && (pole1Data[0] > 500)) || (new Date().getTime() < (triggerTimePole2.getTime() + 5000))) {
            logger.info("vechile detected at pole 1: {}", pole1Data[0]);
            if ((pole1Data[0] < 3000) && (pole1Data[0] > 500)) {
                triggerTimePole2 = new Date();
            }
            if (pole2Data[0] < 6000 && pole2Data[0] > 500) {
                iMqttClient.publish("pole2/horn", message);
                logger.info("Horn signal send to pole 2 data: {}", message);
            }
        }
    }

}
