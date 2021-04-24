/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.javarush.testcase;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;

/**
 *
 * @author Vladimir
 */
public class ConnectionTest {
    public static void main (String[] args)
    {
        System.out.println("nn***** MySQL JDBC Connection Testing *****");
        Connection conn = null;
        PreparedStatement pst = null;
        String  sql = "select id, login, password from users";
        String update = "update users set password = ? where id = ?";
        try
        {
            Class.forName ("com.mysql.cj.jdbc.Driver").newInstance ();
            String userName = "homestead";
            String password = "secret";
            String url = "jdbc:mysql://127.0.0.1:33060/homestead";
            conn = DriverManager.getConnection (url, userName, password);
            System.out.println ("Database Connection Established...");

            pst = conn.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                String login = rs.getString("login");
                System.out.println("Id " + id + " login = " + login);
//                PreparedStatement upd = conn.prepareStatement(update);
//                String newPassword = new StringBuilder(login).append("_").append(id).toString();
//                System.out.println("new password before " + newPassword);
//                String newPasswordCoded = Base64.getUrlEncoder().encodeToString(newPassword.getBytes());
//                upd.setString(1, newPasswordCoded);
//                upd.setInt(2, id);
//                upd.executeUpdate();
//                upd.close();
                byte[] decodedBytes = Base64.getUrlDecoder().decode(rs.getString("password"));
                System.out.println("decoded password = " + new String(decodedBytes));
            }
        }
        catch (Exception ex)
        {
            System.err.println ("Cannot connect to database server");
            ex.printStackTrace();
        }
        finally
        {
            if (pst != null) {
                System.out.println("n***** Let close sql statement *****");
                try {
                    pst.close();
                } catch (SQLException ex) {
                    System.out.println ("Error in statement close: " + ex.getMessage());
                }
            }
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
