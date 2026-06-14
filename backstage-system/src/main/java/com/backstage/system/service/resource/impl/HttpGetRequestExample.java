package com.backstage.system.service.resource.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpGetRequestExample {
    public static void main(String[] args) {
        try {
            URL url = new URL("https://openapi.baidu.com/oauth/2.0/device/code?response_type=device_code&client_id=q1srNKOg5TsWrTMDowO6ugtFEU1440dU&scope=basic,netdisk");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            System.out.println(response);
        } catch (Exception e) {
            System.out.println(e);
        }
    }
} 
