package com.qq452651705.DataMGM.Monitor;

public class MonitorDevice {
    public Integer ID;
    public String subject;
    public String unit;
    public String address;

    public MonitorDevice(Integer ID,String subject,String unit,String address){
        this.ID=ID;
        this.subject=subject;
        this.unit=unit;
        this.address=address;
    }
}
