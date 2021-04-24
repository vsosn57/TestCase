/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.javarush.testcase.util;

import java.time.Instant;
import java.util.Base64;

/**
 *
 * @author Vladimir
 */
public class TokenUtils {
    private static final String TOKEN_DELIM = "-";
    
    public static String getTokenByLogin(String login) {
        StringBuilder sb = new StringBuilder(login);
        sb.append(TOKEN_DELIM);
        sb.append(Instant.now().toEpochMilli());
        return Base64.getUrlEncoder().encodeToString(sb.toString().getBytes());
    }
    
    public static boolean isTokenCorrect(String login, String token) {
        byte[] tokenBytes = Base64.getUrlDecoder().decode(token);
        String decodedToken = new String(tokenBytes);
        int pos = decodedToken.indexOf(TOKEN_DELIM);
        if (pos != -1) {
            String decodedLogin = decodedToken.substring(0, pos);
            return decodedLogin.equals(login);
        }
        return false;
    }
}
