package edu.nu.forensic.db.DBApi;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class SQLForETWFormat {
    Connection c = null;
    Statement stmt = null;

    public SQLForETWFormat(){
        Connection c = null;
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/testdb", "postgres", "123456");
            stmt = c.createStatement();
            CreateSubjectSheet();
            createFileTable();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName()+": "+e.getMessage());
            System.exit(0);
        }
        System.out.println("Opened database successfully");
        this.c = c;
    }


}
