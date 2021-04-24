/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.javarush.testcase;

import java.time.Instant;

/**
 *
 * @author Vladimir
 */
public class IncorrectEntryInfo {
    private Instant lastEntryTime;
    private int     countEntries;

    public void setLastEntryTime(Instant lastEntryTime) {
        this.lastEntryTime = lastEntryTime;
    }

    public void setCountEntries(int countEntries) {
        this.countEntries = countEntries;
    }

    public Instant getLastEntryTime() {
        return lastEntryTime;
    }

    public int getCountEntries() {
        return countEntries;
    }
    
    public IncorrectEntryInfo() {
        this.lastEntryTime = Instant.now();
        this.countEntries  = 1;
    }
}
