package com.swordriver.offrecord;

import com.google.gson.Gson;

/**
 * Created by jcli on 3/15/17.
 */

public class TheGson {
    private static Gson gson=null;
    public static Gson getGson(){
        if (gson==null){
            gson = new Gson();
        }
        return gson;
    }
}
