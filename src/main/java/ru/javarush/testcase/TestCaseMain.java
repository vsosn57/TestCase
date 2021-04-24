/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.javarush.testcase;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author Vladimir
 */
public class TestCaseMain {
    private static final String DATABASE_USER     = "homestead";
    private static final String DATABASE_PASSWORD = "secret";
    private static final String DATABASE_URL      =
            "jdbc:mysql://127.0.0.1:33060/homestead";
    
    private static final String GET_ALL_USERS = "select id, login from users";
    
    private static final String GET_USER_1 = "select login from users where id = 1";
    
    private static Connection getDatabaseConnection() throws Exception {
        Class.forName ("com.mysql.cj.jdbc.Driver").newInstance();
        return DriverManager.getConnection (DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD);
    }
    
    private static String getFakePassword(int id, String login) {
        return new StringBuilder(login).append("_").append(id).toString();
    }
    
    private static void processInvalidPassword(Connection conn) throws Exception
    {
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = conn.prepareStatement(GET_USER_1);
            rs = pst.executeQuery();
            while(rs.next()) {
                String login = rs.getString("login");
                ITestCase testCase = new TestCaseImpl();
                for (int i = 1; i <= 7; i++) {
                    try {
                        testCase.login(conn, login, getFakePassword(1+i, login));
                    }
                    catch(InvalidPasswordException ex) {
                        System.out.println("Login error: " + ex.getMessage());
                    }
                }
            }
        }
        finally {
            if (rs != null) {
                try {
                    rs.close();
                }
                catch(SQLException se){}
            }
            if (pst != null) {
                try {
                    pst.close();
                }
                catch(SQLException se){}
            }
        }
    }
    
    private static void processPaymentTooBig(Connection conn) throws Exception
    {
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = conn.prepareStatement(GET_USER_1);
            rs = pst.executeQuery();
            while(rs.next()) {
                String login = rs.getString("login");
                ITestCase testCase = new TestCaseImpl();
                String token = testCase.login(conn, login, getFakePassword(1, login));
                System.out.println("Received token = " + token);
                BigDecimal newBalance = testCase.payment(conn, token, new BigDecimal("100"));
                System.out.println("Balance after payment = " + newBalance.toString());
                testCase.logout(token);
            }
        }
        finally {
            if (rs != null) {
                try {
                    rs.close();
                }
                catch(SQLException se){}
            }
            if (pst != null) {
                try {
                    pst.close();
                }
                catch(SQLException se){}
            }
        }
    }
    
    private static void processPayment4AllUsers(Connection conn) throws Exception
    {
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = conn.prepareStatement(GET_ALL_USERS);
            rs = pst.executeQuery();
            while(rs.next()) {
                int    id    = rs.getInt("id");
                String login = rs.getString("login");
                ITestCase testCase = new TestCaseImpl();
                String token = testCase.login(conn, login, getFakePassword(id, login));
                System.out.println("Received token = " + token);
                BigDecimal newBalance = testCase.payment(conn, token, new BigDecimal("1.1"));
                System.out.println("Balance after payment = " + newBalance.toString());
                testCase.logout(token);
            }
        }
        finally {
            if (rs != null) {
                try {
                    rs.close();
                }
                catch(SQLException se){}
            }
            if (pst != null) {
                try {
                    pst.close();
                }
                catch(SQLException se){}
            }
        }
    }
    
    
    public static void main (String[] args)
    {
        Connection conn = null;
        try
        {
            conn = getDatabaseConnection();
            //processPayment4AllUsers(conn);
            //processPaymentTooBig(conn);
            processInvalidPassword(conn);
        }
        catch (Exception ex)
        {
            System.err.println ("Database error: " + ex.getMessage());
            ex.printStackTrace();
        }
        finally
        {
            if (conn != null)
            {   
                try
                {
                    System.out.println("n***** Let terminate the Connection *****");
                    conn.close ();
                    System.out.println ("Database connection terminated... ");
                }
                catch (Exception ex)
                {
                    System.out.println ("Error in connection termination!");
                }
            }
        }
    }
}
