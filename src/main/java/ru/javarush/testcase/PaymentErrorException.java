/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.javarush.testcase;

/**
 *
 * @author Vladimir
 */
public class PaymentErrorException extends Exception {
    public PaymentErrorException(String message) {
        super(message);
    }
}
