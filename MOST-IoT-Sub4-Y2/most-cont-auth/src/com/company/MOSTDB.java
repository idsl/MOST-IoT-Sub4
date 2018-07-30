package com.company;

import java.sql.*;

/**
 *
 * @author IDSL
 */
public class MOSTDB {

    String myDriver = "com.mysql.jdbc.Driver";
    String myUrl = "jdbc:mysql://140.118.109.165:33306/subproject1?verifyServerCertificate=true&useSSL=false&requireSSL=false";
    Connection conn;

    public MOSTDB() throws ClassNotFoundException, SQLException {

    }

    public void InsertToDatabase(String DeviceId, String DataType, String Value) {
        try {
            Class.forName(myDriver);
            conn = DriverManager.getConnection(myUrl, "sp1", "sp1");
            String query = "INSERT INTO `subproject1`.`device_value` (`DeviceId`,`TimeStamp`,`DataType`,`Value`,`name`,`reportid`)VALUES(?,NOW(),?, ?,0,0);";

            PreparedStatement preparedStmt = conn.prepareStatement(query);
            preparedStmt.setString(1, DeviceId);
            preparedStmt.setString(2, DataType);
            preparedStmt.setString(3, Value);

            // execute the preparedstatement
            preparedStmt.execute();

            conn.close();
        } catch (Exception e) {
            System.err.println("Got an exception!");
            System.err.println(e.getMessage());
        }

    }
}