package com.swordriver.offrecord;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by jcli on 3/26/17.
 */

public class DataSourcePassGenerator {

    private final StringBuffer mCharSet;

    public DataSourcePassGenerator(){
        mCharSet = new StringBuffer();
        mCharSet.append("abcdefghijklmnopqrstuvwxyz");
        mCharSet.append("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        mCharSet.append("0123456789");
        mCharSet.append("!@#$%^&*_+?");
    }

    public String getPass(int length){
        StringBuffer buf = new StringBuffer();
        int upperBound = mCharSet.length();
        for (int i=0; i<length; i++){
            Random r = new Random();
            buf.append(mCharSet.charAt(r.nextInt(upperBound)));
        }
        return buf.toString();
    }
}
