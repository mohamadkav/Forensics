package edu.nu.forensic.db.DBApi;

import edu.nu.forensic.db.entity.Event;
import edu.nu.forensic.db.entity.Subject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

public class PostGreSqlApi {

    private Connection c = null;

    public PostGreSqlApi() {
        Connection c = null;
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/testdb", "postgres", "1234");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName()+": "+e.getMessage());
            System.exit(0);
        }
        System.out.println("Opened database successfully");

        //create subject sheet
        try {
            System.out.println("Opened database successfully");

            Statement stmt = c.createStatement();
            String sql = "CREATE TABLE SUBJECT " +
                    "(UUID  VALCHAR PRIMARY KEY    NOT NULL," +
                    " PID        VALCHAR    NOT NULL, " +
                    " NAME       VALCHAR    NOT NULL, " +
                    " TID        VALCHAR    NOT NULL, " +
                    " PARENTUUID   VALCHAR   " +
                    "TIMESTAMP       VALCHAR  NOT NULL)";
            stmt.executeUpdate(sql);
            stmt.close();
            c.close();
        } catch ( Exception e ) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        this.c = c;
    }

    public synchronized void storeSubject(List<Subject> subjectlists) {
        try {
            Statement statement = c.createStatement();
            for (Subject subject : subjectlists) {
                String sql = "INSERT INTO SUBJECT (UUID,PID,NAME,TID,PARENTUUID,TIMESTAMP)" +
                        "VALUES (1, 'Paul', 32, 'California', 20000.00 );";
                statement.execute(sql);
            }
            statement.close();
            c.commit();
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public synchronized void storeEvent(List<Event> eventlists) {
        try {
            Statement statement = c.createStatement();
            for (Event event : eventlists) {
                String sql = "INSERT INTO SUBJECT (UUID,PID,NAME,TID,PARENTUUID,TIMESTAMP)" +
                        "VALUES (1, 'Paul', 32, 'California', 20000.00 );";
                statement.execute(sql);
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public synchronized void createEventSheet(String pid) {
        try {
            System.out.println("Opened database successfully");

            Statement stmt = c.createStatement();
            String sql = "CREATE TABLE " + pid +
                    "(UUID  VALCHAR PRIMARY KEY    NOT NULL," +
                    " PID        VALCHAR    NOT NULL, " +
                    " NAME       VALCHAR    NOT NULL, " +
                    " TID        VALCHAR    NOT NULL, " +
                    " PARENTUUID   VALCHAR   " +
                    " TIMESTAMP       VALCHAR  NOT NULL)";
            stmt.executeUpdate(sql);
            stmt.close();
            c.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
    }
}
