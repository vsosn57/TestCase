/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.javarush.testcase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import ru.javarush.testcase.util.TokenUtils;

/**
 *
 * @author Vladimir
 */
public class TestCaseImpl implements ITestCase {
    private static final String CHECK_LOGIN =
            "select count(*) from users where login = ? and password = ?";
    private static final String GET_ACC_BALANCE =
    "select ac.balance, ac.owner from account ac, users u where ac.owner = u.id and u.login = ?";
    
    private static final String ADD_PAYMENT =
    "insert into payment (payer, paysum, created_at) values(?, ?, ?)";
    
    private static final String UPDATE_BALANCE =
    "update account set balance = ? where owner = ?";
    private static final int MAX_INCORRECT_ENTRY_COUNT = 5;
    
    private static final int INACTIVITY_HOURS = 24;
    
    private static final String TRY_AGAIN_LATER =
            "Вход в систему временно заблокирован. Попробуйте позже"; 
    private static final String INVALID_LOGIN_OR_PASSWORD =
            "Неверное имя пользователя и / или пароль";
    private static final String INVALID_TOKEN =
            "Невозможно определить владельца токена";
    private static final String INSUFFICIENT_FUND = "Недосточно средств на счете";
    private static final String ACCOUNT_NOT_FOUND = "Счет не найден";
    
    private HashMap<String, IncorrectEntryInfo> incorrectEntries =
            new HashMap<String, IncorrectEntryInfo>();
    
    private HashMap<String,String> successEntries =
            new HashMap<String,String>();
    
    
    private boolean isCorrectLogin(Connection conn, String login, String password) {
        PreparedStatement pst = null;
        boolean res = false;
        try {
            pst = conn.prepareStatement(CHECK_LOGIN);
            pst.setString(1, login);
            String base64Password =
                    Base64.getUrlEncoder().encodeToString(password.getBytes());
            pst.setString(2, base64Password);
            ResultSet rs = pst.executeQuery();
            
            if (rs.next()) {
                res = rs.getInt(1) > 0;
            }
        }
        catch(SQLException se) {
            se.printStackTrace(System.err);
        }
        finally {
            if (pst != null) {
                try {
                    pst.close();
                }
                catch(SQLException se){}
            }
        }
        return res;
    }

    @Override
    public String login(Connection conn, String login, String password)
            throws InvalidPasswordException {
        // Проверяем не было ли некорректного входа с этим login
        IncorrectEntryInfo incorrectEntry = incorrectEntries.get(login);
        if(incorrectEntry != null) {
            // Некорректные входы были - проверяем их количество
            int nEntries = incorrectEntry.getCountEntries();
            if (nEntries == MAX_INCORRECT_ENTRY_COUNT) {
                // Было предельно допустимое количество некорректных входов
                Instant lastEntryTime = incorrectEntry.getLastEntryTime();
                if (lastEntryTime.plus(INACTIVITY_HOURS, ChronoUnit.HOURS).isAfter(Instant.now())) {
                    // с момента последнего некорректного входа прошло менее 24 часов
                    // выдаем сообщение "попробуйте позднее"
                    throw new InvalidPasswordException(TRY_AGAIN_LATER);
                }
                else {
                    // с момента последнего некорректного входа прошло 24 или более часов
                    // - удаляем запись о некорректном входе с этого login
                    incorrectEntries.remove(login);
                }
            }
        }
        if (isCorrectLogin(conn, login, password)) {
            // введен корректный пароль - генерируем токен
            // и сохраняем связь логин <=> токен
            if (successEntries.containsKey(login)) {
                return successEntries.get(login);
            }
            String token = TokenUtils.getTokenByLogin(login);
            successEntries.put(login, token);
            if (incorrectEntries.containsKey(login)) {
                incorrectEntries.remove(login);
            }
            return token;
        }
        // пароль неверный
        if (incorrectEntry != null) {
            // были некорректные входы с этого login ранее - кооректируем информацию
            incorrectEntry.setCountEntries(incorrectEntry.getCountEntries()+1);
            incorrectEntry.setLastEntryTime(Instant.now());
        }
        else {
            incorrectEntry = new IncorrectEntryInfo();
        }
        incorrectEntries.put(login, incorrectEntry);
        throw new InvalidPasswordException(INVALID_LOGIN_OR_PASSWORD);
    }
    
    private String getLoginByToken(String token) {
        String login = "";
        if (!successEntries.isEmpty()) {
            Iterator<Entry<String, String>> it = successEntries.entrySet().iterator();
            while(it.hasNext()) {
               Entry<String, String> entry = it.next();
               if (entry.getValue().equals(token)) {
                   login = entry.getKey();
               }
            }
        }
        return login;
    }

    @Override
    public void logout(String token) {
       String login = getLoginByToken(token);
       if (!login.isEmpty()) {
           successEntries.remove(login);
           if (incorrectEntries.containsKey(login)) {
               incorrectEntries.remove(login);
           }
       }
    }

    @Override
    public synchronized BigDecimal payment(Connection conn, String token, BigDecimal paySum) throws PaymentErrorException {
        String login = getLoginByToken(token);
        if (login.isEmpty()) {
            throw new PaymentErrorException(INVALID_TOKEN);
        }
        PreparedStatement findBalance = null;
        PreparedStatement savePayment = null;
        PreparedStatement updateBalance = null;
        ResultSet rs                    = null;
        BigDecimal newBalance           = null;
        BigDecimal balance              = null;
        try {
            conn.setAutoCommit(false);
            findBalance = conn.prepareStatement(GET_ACC_BALANCE);
            findBalance.setString(1, login);
            rs = findBalance.executeQuery();
            while (rs.next()) {
                balance = rs.getBigDecimal("balance").setScale(2, BigDecimal.ROUND_HALF_UP);
                if (balance.compareTo(paySum.setScale(2, BigDecimal.ROUND_HALF_UP)) < 0) {
                    throw new PaymentErrorException(INSUFFICIENT_FUND);
                }
                int accountOwner = rs.getInt("owner");
                updateBalance = conn.prepareStatement(UPDATE_BALANCE);
                newBalance = balance.subtract(paySum.setScale(2, BigDecimal.ROUND_HALF_UP));
                updateBalance.setBigDecimal(1, newBalance);
                updateBalance.setInt(2, accountOwner);
                updateBalance.executeUpdate();
                savePayment = conn.prepareStatement(ADD_PAYMENT);
                savePayment.setInt(1, accountOwner);
                savePayment.setBigDecimal(2, paySum.setScale(2, BigDecimal.ROUND_HALF_UP));
                savePayment.setDate(3, new java.sql.Date(new java.util.Date().getTime()));
                savePayment.executeUpdate();
            }
            conn.commit();
            return newBalance != null ? newBalance : balance;
        }
        catch(SQLException se) {
            try {
                conn.rollback();
            } catch (SQLException ex) {}
        }
        finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch(SQLException s) {}
            }
            if (findBalance != null) {
                try {
                    findBalance.close();
                } catch(SQLException s) {}
            }
            if (savePayment != null) {
                try {
                    savePayment.close();
                } catch(SQLException s) {}
            }
            if (updateBalance != null) {
                try {
                    updateBalance.close();
                } catch(SQLException s) {}
            }
        }
        return null;
    }
}
