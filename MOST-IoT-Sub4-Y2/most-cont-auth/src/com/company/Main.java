package com.company;

import algorithm.Algorithm;
import algorithm.Hash;
import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import org.knowm.xchart.QuickChart;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;

import java.sql.*;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Random;
import java.util.TooManyListenersException;

import static algorithm.Algorithm.contPhase;
import static algorithm.Algorithm.tksni;

/**
 * 2018/05/17 by Blacksmith289
 * This is just a simulate code, THERE　ARE SOME　PLACES DIFFERENT
 * the in.read() function will clear the the read byte from the buffer
 * which means in the  receive byte phase, the receive length should be -2 comparing with the original code
**/

public class Main {

    boolean entStatic = true;
    boolean canEntStatic = false;
    boolean ifSetTime= false;
    public static boolean authed = true;
    boolean sendData = false;

    int staticAuthCounter=0;

    float PERIOD = 60*1000;
    long preTime= -1;

    long startTime;

    double phase = 0;
    public static int dataCounter =0;

    void connect ( String portName ) throws Exception
    {
        CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
        if ( portIdentifier.isCurrentlyOwned() )
        {
            System.out.println("Error: Port is currently in use");
        }
        else
        {
            CommPort commPort = portIdentifier.open(this.getClass().getName(),2000);

            if ( commPort instanceof SerialPort)
            {
                SerialPort serialPort = (SerialPort) commPort;
                serialPort.setSerialPortParams(9600,SerialPort.DATABITS_8,SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);

                InputStream in = serialPort.getInputStream();
                OutputStream out = serialPort.getOutputStream();


                while(true) {

                        //Set time
                        while(!ifSetTime){

                            out.write(new byte[]{0x11,0x22});

                            Random rand = new Random();

                            int randomYear = 2018;
                            int randomMonth = rand.nextInt(12)+1;
                            int randomDay = rand.nextInt(28)+1;
                            int randomHour = rand.nextInt(23);
                            int randomMinute = rand.nextInt(59);
                            int randomSecond = rand.nextInt(59);

                            int randomYear_front = randomYear/100;
                            int randomYear_back = randomYear-randomYear_front*100;

                            System.out.println("Year: "+randomYear+" Month: "+randomMonth+" Day: "+ randomDay+
                                    " Hour: "+randomHour+" Minute: "+randomMinute+" Second: "+randomSecond);

                            //System.out.println("Year front: "+ (byte)randomYear_front+" Year back: "+(byte)randomYear_back);

                            out.write(new byte[]{(byte) randomYear_front,(byte) randomYear_back,(byte) randomMonth,(byte) randomDay,(byte) randomHour,(byte) randomMinute,(byte) randomSecond});

                            startTime = System.currentTimeMillis();

                            if(in.read()==0x00){
                                in.skip(in.available()); //Clear buffer
                                ifSetTime=true;
                                break;
                            }
                            in.skip(in.available()); //Clear buffer
                        }

                        //Authentication
                        if(preTime<0)
                            preTime = System.currentTimeMillis();

                        if(System.currentTimeMillis()-preTime > PERIOD){
                            canEntStatic = true; //Ready to enter static
                            preTime = System.currentTimeMillis(); //Reset time
                        }else{
                            System.out.print("Countdown time:");
                            System.out.println(System.currentTimeMillis()-preTime);
                        }


                        if(!sendData){
                            out.write(new byte[]{0x00, 0x33});

                            if(entStatic){

//                                diffTime = System.currentTimeMillis() - startTime;
//
//                                System.out.print("\n\n\n\n\nDifference of time: "+diffTime+"\n\n\n\n\n");


                                System.out.println("Enter Static Authentication");
                                //Static Authentication test
                                while (!(in.available() > 91)){
                                    //System.out.println(in.available());
                                };
                                byte[] data = new byte[92];
                                for (int i = 0; i < 92; i++)
                                    data[i] = (byte) in.read();


                                int[] data1 = Algorithm.phase1("666", data);


                                byte[] p2 = Algorithm.phase2("666", data1[0], data1[1], data1[2]);
                                out.write(p2);

                                entStatic = false;
                                staticAuthCounter+=1;
                                sendData = true;

                            }else{

//                                diffTime = System.currentTimeMillis() - startTime;
//
//                                System.out.print("\n\n\n\n\nDifference of time: "+diffTime+"\n\n\n\n\n");



                                //Continuous Authentication test
                                System.out.println("Enter Continuous Authentication");

                                while (!(in.available() > 69)) ;
                                byte[] data = new byte[70];
                                for (int i = 0; i < 70; i++){
                                    data[i] = (byte) in.read();
                                }

                                byte[] toString = Algorithm.contPhase("666", data, canEntStatic);

                                if(canEntStatic){
                                    entStatic = true;
                                    canEntStatic =false;
                                }

                                out.write(toString);
                                sendData=true;
                            }

                            while (in.available() < 2) ;
                            System.out.printf("%X %X %X %X\n", in.read(), in.read(),in.read(),in.read());


                            in.skip(in.available());
                            sleep(0.5);
                        }else{
                            //System.out.println("Waiting for sensor(byte): "+in.available());
                            while(in.available()<1);
                            if(in.read()==0x77){
                                out.write(new byte[]{0x44,0x33});

                                while(in.available()<25){
                                    //System.out.println("Waiting for data");
                                }
                                byte[] data = new byte[25];
                                for(int i=0;i<25;i++){
                                    data[i] = (byte) in.read();
                                }

//                                System.out.print("Received byte: ");
//                                for(int i=0;i<9;i++){
//                                    System.out.printf("%X ", data[i]);
//                                }
//                                System.out.println();

                                if(data[0]==0x44){

                                    //心跳 血氧 體溫 溫度 濕度 mp2.5
                                    byte[] heartBuffer = {data[1],data[2],data[3],data[4]};
                                    byte[] spO2Buffer = {data[5],data[6],data[7],data[8]};
                                    byte[] bodyTempBuffer = {data[9],data[10],data[11],data[12]};

                                    byte[] tempBuffer = {data[13],data[14],data[15],data[16]};
                                    byte[] humiBuffer = {data[17],data[18],data[19],data[20]};

                                    byte[] pm25Buffer = {data[21],data[22],data[23],data[24]};

                                    //float temperature = ((data[1] & 0xff) << 24) + ((data[2] & 0xff) << 16) + ((data[3] & 0xff) << 8) + (data[4] & 0xff);
                                    //float humidity = ((data[5] & 0xff) << 24) + ((data[6] & 0xff) << 16) + ((data[7] & 0xff) << 8) + (data[8] & 0xff);

                                    float heartRate = ByteBuffer.wrap(heartBuffer).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                                    int spO2 = ByteBuffer.wrap(spO2Buffer).order(ByteOrder.LITTLE_ENDIAN).getInt();

                                    if(spO2>100) spO2 = 99;

                                    float bodyTemperature = ByteBuffer.wrap(bodyTempBuffer).order(ByteOrder.LITTLE_ENDIAN).getFloat();

                                    float temperature = ByteBuffer.wrap(tempBuffer).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                                    float humidity = ByteBuffer.wrap(humiBuffer).order(ByteOrder.LITTLE_ENDIAN).getFloat();

                                    int pm25Value = ByteBuffer.wrap(pm25Buffer).order(ByteOrder.LITTLE_ENDIAN).getInt();

                                    phase += 2 * Math.PI * 2 / 20.0;

                                    //Thread.sleep(1000);
//                                    dataCounter+=1;
//                                    tempValBuffer[dataCounter] = (double)temperature;
//                                    final double[][] chartData = getSineData(tempValBuffer,dataCounter);
//
//                                    chart.updateXYSeries("C", chartData[0], chartData[1], null);
//                                    sw.repaintChart();


                                    System.out.println("=======================");
                                    System.out.print("Heart Rate: "+heartRate+" bpm");
                                    System.out.print("\t\tSp02: "+spO2+"%");
                                    System.out.println("\t\tBody Temperature: "+bodyTemperature+"C");
                                    System.out.println("=======================");
                                    System.out.print("Temperature: "+temperature+"C");
                                    System.out.println("\t\tHumidity: "+humidity+"%");
                                    System.out.println("=======================");
                                    System.out.println("PM2.5: "+pm25Value);
                                    System.out.println("=======================");

                                    System.out.println("remaining buffer: "+in.available());
                                    //in.skip(in.available());
                                    System.out.printf("%X %X\n", in.read(), in.read());

                                    if(entStatic){
                                        System.out.print("=========Ready to Send Data to Database=========");
                                        MOSTDB hrtRateDB = new MOSTDB();
                                        MOSTDB spO2DB = new MOSTDB();
                                        MOSTDB bdyTempDB = new MOSTDB();
                                        MOSTDB tempDB = new MOSTDB();
                                        MOSTDB humiDB = new MOSTDB();
                                        MOSTDB pm25DB = new MOSTDB();

                                        hrtRateDB.InsertToDatabase("01","1", ""+heartRate+"");
                                        spO2DB.InsertToDatabase("02","2", ""+spO2+"");
                                        bdyTempDB.InsertToDatabase("03","3", ""+bodyTemperature+"");
                                        tempDB.InsertToDatabase("04","4", ""+temperature+"");
                                        humiDB.InsertToDatabase("05","5", ""+humidity+"");
                                        pm25DB.InsertToDatabase("06","6", ""+pm25Value+"");
                                    }

                                    sendData=false;
                                }else{
                                    sendData=false;
                                    return;
                                }
                            }
                        }




                    //If authentication is confirmed, send notification to sensor
                    /*
                    out.write(new byte[]{0x44,0x33});
                    //Listen to the data
                    while(in.available()<13);
                    byte[] data = new byte[12];
                    for(int i =0; i<in.available();i++){
                        data[i] = (byte)in.read();
                    }

                    if(data[0]==0x44){
                        //Receive data
                        byte[] b1 = Arrays.copyOfRange(data, 1, 5);
                        byte[] b2 = Arrays.copyOfRange(data, 5, 9);
                        float f1 = ByteBuffer.wrap(b1).order(ByteOrder.nativeOrder()).getFloat();
                        float f2 = ByteBuffer.wrap(b2).order(ByteOrder.nativeOrder()).getFloat();
                        System.out.println("Humidity: "+f1);
                        System.out.println("Temperature: "+f2);

                    }else{
                        System.out.println("Unreceived data");
                        in.skip(in.available());
                    }
                    */
//                    if(staticAuthCounter>10){
//                        System.out.println("Operation Time: "+System.currentTimeMillis());
//                        while(true);
//                    }

                }// Keep looping
            }
            else
            {
                System.out.println("Error: Only serial ports are handled by this example.");
            }
        }
    }


    public static void main ( String[] args )
    {
        try
        {
            (new Main()).connect("COM7");
        }
        catch ( Exception e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
    public static void sleep(double sec)
    {
        try {
            Thread.sleep((int)(sec*1000.0));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

//    private static double[][] getSineData(double[] buffer, int counter) {
//
//        double[] xData = new double[5];
//        double[] yData = new double[5];
////        for (int i = 0; i < xData.length; i++) {
////            double radians = phase + (2 * Math.PI / xData.length * i);
////            xData[i] = radians;
////            yData[i] = data;//Math.sin(radians);
////        }
//
//        if(counter<5){
//            for(int i=0;i<5;i++){
//                xData[i]=counter;
//                yData[i]=buffer[i];
//            }
//        }else{
//            for(int i=3; i>=0; i--){
//                xData[counter-i] = counter-i;
//                yData[counter-i] = buffer[counter-i];
//            }
//        }
//
//        return new double[][] { xData, yData };
//    }

}
