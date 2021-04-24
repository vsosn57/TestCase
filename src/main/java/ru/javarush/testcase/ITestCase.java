/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.javarush.testcase;

import java.math.BigDecimal;
import java.sql.Connection;

/**
 *
 * @author Vladimir
 */
public interface ITestCase {
    public String login(Connection conn, String login, String password) throws InvalidPasswordException;
    
    public void logout(String token);
    
    public BigDecimal payment(Connection conn, String token, BigDecimal paySum) throws PaymentErrorException;
}
