package com.focusassistant.backend.util;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBUtil {

    private static final String URL =
            "jdbc:mysql://localhost:3306/focus_assistant";

    private static final String USER = "root";
    private static final String PASSWORD = "Rishu@2903";

    public static Connection getConnection() {
        try {
            Connection con =
                    DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("MySQL Connected Successfully");
            return con;
        } catch (Exception e) {
            System.out.println("Database Connection Failed");
            e.printStackTrace();
            return null;
        }
    }
}
