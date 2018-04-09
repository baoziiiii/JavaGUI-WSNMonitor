package com.qq452651705.SerialComm;

import com.qq452651705.DataMGM.Tourists.TouristManager;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.qq452651705.DataMGM.Tourists.TouristManager.Tourist;
import com.sun.xml.internal.fastinfoset.util.CharArray;
import gnu.io.SerialPort;


public class CommHandler {

    private static Boolean[] cookiesStatus = new Boolean[255];
    private static StringBuffer buffer = new StringBuffer();
    private static Map<String, TouristThread> cookieMap = new HashMap<>();
    private static SerialComm serialComm = SerialComm.getSerialComm();
    private static SerialPort comm;

    static {
        Arrays.fill(cookiesStatus, false);
    }

    public static Boolean parseRawData(byte[] bytes, SerialPort comm2) {
        comm = comm2;
        String data = new String(bytes);
        List<String> groups;
        try {
            groups = extractGroups(buffer.append(data).toString());
        } catch (Exception e) {
            buffer = new StringBuffer();
            return false;
        }
        if (groups == null) {
            return false;
        } else {
            buffer = new StringBuffer();
        }
        for (String t : groups) {
            String[] f = t.split(":");
            if (f.length < 2)
                continue;
            if (t.startsWith("GUEST:")) {
                GuestHandler(t.replace("GUEST:", ""));
            } else if (t.startsWith("Connected:")) {
                String cookie = t.replace("Connected:", "");
                TouristThread thread = cookieMap.get(cookie);
                if (thread != null) {
                    thread.connectCount = 10;
                    System.out.println("【Cookie:" + cookie + "】 Refresh CountDown!");
                }
            } else if (t.startsWith("Disconnected:")){
                String cookie = t.replace("Disconnected:", "");
                TouristThread thread = cookieMap.get(cookie);
                if (thread != null) {
                    thread.connectCount = -10;
                    System.out.println("【Cookie:" + cookie + "】 Connection Shutdown");
                }
                break;
            }
        }
        return true;
    }


    private static Boolean GuestHandler(String guest) {
        List<String> groups;
        try {
            groups = extractGroups(guest);
        } catch (Exception e) {
            buffer = new StringBuffer();
            return false;
        }
        if (groups.size() != 2)
            return false;
        String[] IMEIgroup = groups.get(0).split(":");
        String[] Cookiegroup = groups.get(1).split(":");
        if (IMEIgroup.length == 2 && Cookiegroup.length == 2 && IMEIgroup[1].matches("[0-9]{15}")) {
            String IMEI = IMEIgroup[1];
            String cookie = Cookiegroup[1];
            try {
                Integer.parseInt(cookie);
                TouristThread touristThread = new TouristThread(IMEI, cookie);
                cookieMap.put(cookie, touristThread);
                touristThread.start();
            }catch (NumberFormatException e){}
        }
        return true;
    }

    static class TouristThread extends Thread {
        String IMEI;
        String cookie;
        Integer connectCount = 10;

        TouristThread(String IMEI, String cookie) {
            super();
            this.IMEI = IMEI;
            this.cookie = cookie;
        }

        @Override
        public void run() {
            Tourist tourist = new Tourist(IMEI, "baowenqiang", "13801691551", "aaa");
            tourist.connect();
            System.out.println("【Cookie:" + cookie + "】 Connected!");
            while (connectCount-- > 0) {
                try {
                    Thread.sleep(3000);
                    System.out.println("【Cookie:" + cookie + "】 CountDown:" + connectCount);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("【Cookie:" + cookie + "】 Disconnected!");
            tourist.disconnect();
            cookieMap.remove(cookie);
            System.out.println("cookieMapSize:"+cookieMap.size());
            if(cookieMap.size()==0){
                clearCookie(":");
            }
            else {
                if (connectCount > -10) {
                    clearCookie(cookie);
                }
            }
        }
    }

    public static void clearCookie(String cookie) {
        System.out.println("Clear cookie:"+cookie);
        byte c;
        if(cookie.equals(":")){
            c=':';
        }else {
            int ck = Integer.parseInt(cookie);
            c = (byte) (ck);
        }
        byte[] s = "<cookie:".getBytes();
        byte[] e = ">".getBytes();
        byte[] data = Arrays.copyOf(s, s.length + 1 + e.length);
        data[s.length] = c;
        System.arraycopy(e, 0, data, s.length + 1, e.length);
        System.out.println("Clear cookie array:"+Arrays.toString(data));
        System.out.println("Clear cookie string:"+new String(data));
        serialComm.sendToPort(comm, data);
    }


    private static List<String> extractGroups(String s) throws Exception {
        List<String> groups = new ArrayList<>();
        int count = 0;
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '<') {
                if (count == 0) {
                    start = i + 1;
                }
                count++;
            } else if (c == '>') {
                count--;
                if (count == 0) {
                    groups.add(s.substring(start, i));
                } else if (count < 0)
                    throw new Exception();
            }
        }
        if (count == 0)
            return groups;
        return null;
    }

    private static List<String> Groups(String s) throws Exception {
        List<String> groups = new ArrayList<>();
        int count = 0;
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '<') {
                if (count == 0) {
                    start = i + 1;
                }
                count++;
            } else if (c == '>') {
                count--;
                if (count == 0) {
                    groups.add(s.substring(start, i));
                } else if (count < 0) {
                    throw new Exception();
                }
            }
        }
        if (count == 0)
            return groups;
        return null;
    }
}
