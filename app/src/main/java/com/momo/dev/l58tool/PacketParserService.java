package com.momo.dev.l58tool;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Administrator on 2015/10/16.
 */
public class PacketParserService extends Service {

    private int mVERSION = 100;

    public final static String ACTION_PACKET_HANDLE =
            "com.momo.dev.l58tool.packet.parser.ACTION_PACKET_HANDLE";

    public final static String HANDLE = "PacketHandle";
    boolean BLE_CONNECT_STATUS;
    private int GattStatus = 0;
    private int resent_cnt = 0;
    private Intent GattCommand = new Intent(BluetoothLeService.ACTION_GATT_HANDLE);

    private Packet send_packet = new Packet();
    private Packet receive_packet = new Packet();
    private List<Alarm> mAlarms = new ArrayList<Alarm>();
    private List<SportData> mSportData = new ArrayList<SportData>();
    private List<SleepData> mSleepData = new ArrayList<SleepData>();
    private List<SleepSetting> mSleepSetting = new ArrayList<SleepSetting>();
    private DailyData mDailyData = new DailyData();

    private LocalBinder mBinder = new LocalBinder();
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    public class LocalBinder extends Binder {
        public PacketParserService getService() {
            return PacketParserService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerReceiver(MyReceiver, MyIntentFilter());
        final Intent intent = new Intent(this,BluetoothLeService.class);
        startService(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        final Intent intent = new Intent(this,BluetoothLeService.class);
        stopService(intent);
    }

    public void connect(String address){

        BluetoothLeService.HandleCommand cmd = BluetoothLeService.HandleCommand.NORDIC_BLE_CONNECT;
        GattCommand.putExtra(BluetoothLeService.HandleCMD, cmd.getHandleCommandIndex(cmd.getCommand()));
        GattCommand.putExtra(BluetoothLeService.HandleDeviceAddress, address);
        sendBroadcast(GattCommand);
    }
    public void disconnect(){
        BluetoothLeService.HandleCommand cmd = BluetoothLeService.HandleCommand.NORDIC_BLE_DISCONNECT;
        GattCommand.putExtra(BluetoothLeService.HandleCMD, cmd.getHandleCommandIndex(cmd.getCommand()));
        sendBroadcast(GattCommand);
    }
    private void sendACK(Packet rPacket,boolean error){
        Packet.L1Header l1Header = new Packet.L1Header();
        l1Header.setLength((short)0);
        l1Header.setACK(true);
        l1Header.setError(error);
        l1Header.setSequenceId(rPacket.getL1Header().getSequenceId());
        l1Header.setCRC16((short) 0);
        send_packet.setL1Header(l1Header);
        send_packet.setPacketValue(null, false);
        send(send_packet);
    }
    public int getVersion(){
        return mVERSION;
    }

    public List<SportData> getSportDataList() {
        List<SportData> nSportData = new ArrayList<SportData>();
        nSportData.addAll(mSportData);
        mSportData.clear();
        return nSportData;
    }

    public List<SleepData> getSleepDataList(){
        List<SleepData> nSleepData = new ArrayList<SleepData>();
        nSleepData.addAll(mSleepData);
        mSleepData.clear();
        return nSleepData;
    }

    public List<SleepSetting> getSleepSettingList() {
        List<SleepSetting> nSleepSetting = new ArrayList<SleepSetting>();
        nSleepSetting.addAll(mSleepSetting);
        mSleepSetting.clear();
        return nSleepSetting;
    }

    public List<Alarm> getAlarmsList() {
        return mAlarms;
    }

    public void setTime(){
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        int aData;
        aData = (year-2000) & 0x3F;
        aData = aData << 4 | ((month+1) & 0x0F);
        aData = aData << 5 | (day & 0x1F);
        aData = aData << 5 | (hour & 0x1F);
        aData = aData << 6 | (minute & 0x3F);
        aData = aData << 6 | (second & 0x1F);

        Packet.PacketValue packetValue = new Packet.PacketValue();
        packetValue.setCommandId((byte) (0x02));
        packetValue.setKey((byte) (0x01));
        packetValue.setValue(Packet.intToByte(aData));
        send_packet.setPacketValue(packetValue, true);
        send_packet.print();
        send(send_packet);
        resent_cnt = 3;
    }
    public static class Alarm{
        public int ID;
        public int Year;
        public int Month;
        public int Day;
        public int Hour;
        public int Minute;
        public int Repeat;
    }
    private byte[] AlarmToByte(Alarm alarm){
        long aData;
        aData = (long)(alarm.Year - 2000) & 0x3F;
        aData = aData << 4 | (alarm.Month & 0x0F);
        aData = aData << 5 | (alarm.Day & 0x1F);
        aData = aData << 5 | (alarm.Hour & 0x1F);
        aData = aData << 6 | (alarm.Minute & 0x1F);
        aData = aData << 3 | (alarm.ID & 0x07);
        aData = aData << 4;
        aData = aData << 7 | (alarm.Repeat & 0x7F);
        return Arrays.copyOfRange(Packet.longToByte(aData), 3, 8);
    }
    private Alarm AlarmFromByte(byte[] data){
        Alarm alarm = new Alarm();
        long aData = data[0] & 0xFFL;
        aData = (aData << 8) | (data[1] & 0xFFL);
        aData = (aData << 8) | (data[2] & 0xFFL);
        aData = (aData << 8) | (data[3] & 0xFFL);
        aData = (aData << 8) | (data[4] & 0xFFL);

        alarm.Repeat = (int)(aData & 0x7F);
        aData >>>= 11;
        alarm.ID = (int)(aData & 0x07);
        aData >>>= 3;
        alarm.Minute = (int)(aData &0x3F);
        aData >>>= 6;
        alarm.Hour = (int)(aData & 0x1F);
        aData >>>= 5;
        alarm.Day = (int)(aData & 0x1F);
        aData >>>= 5;
        alarm.Month = (int)(aData &0x0F);
        aData >>>= 4;
        alarm.Year = (int)(aData &0x3F);
        return alarm;
    }
    public void setAlarmList(List<Alarm> alarmList){

        byte[] aData;
        Packet.PacketValue packetValue = new Packet.PacketValue();
        packetValue.setCommandId((byte) (0x02));
        packetValue.setKey((byte) (0x02));
        for(Alarm alarm:alarmList){
            aData = AlarmToByte(alarm);
            packetValue.appendValue(aData);
        }
        send_packet.setPacketValue(packetValue, true);
        send_packet.print();
        send(send_packet);
        resent_cnt = 3;
    }
    public void getAlarms(){
        Packet.PacketValue packetValue = new Packet.PacketValue();
        packetValue.setCommandId((byte) (0x02));
        packetValue.setKey((byte) (0x03));
        send_packet.setPacketValue(packetValue,true);
        send_packet.print();
        send(send_packet);
        resent_cnt = 3;
    }
    public void setTarget(int target){
        Packet.PacketValue packetValue = new Packet.PacketValue();
        packetValue.setCommandId((byte) (0x02));
        packetValue.setKey((byte) (0x05));
        packetValue.setValue(Packet.intToByte(target));
        send_packet.setPacketValue(packetValue,true);
        send_packet.print();
        send(send_packet);
        resent_cnt = 3;
    }
    public static class UserProfile{
        public int Sex;
        public int Age;
        public int Stature;     //0.5cm
        public int Weight;      //0.5Kg
    }
    public void setUserProfile(UserProfile userProfile){
        Packet.PacketValue packetValue = new Packet.PacketValue();
        packetValue.setCommandId((byte) (0x02));
        packetValue.setKey((byte) (0x10));
        int aData;
        aData = userProfile.Sex;
        aData = aData<<7 | (userProfile.Age & 0x7F);
        aData = aData<<9 | (userProfile.Stature & 0x1FF);
        aData = aData<<10 | (userProfile.Weight & 0x3FF);
        packetValue.setValue(Packet.intToByte(aData));
        send_packet.setPacketValue(packetValue,true);
        send_packet.print();
        send(send_packet);
        resent_cnt = 3;
    }
    public void setLossAlert(int lossAlert){
        Packet.PacketValue packetValue = new Packet.PacketValue();
        packetValue.setCommandId((byte) (0x02));
        packetValue.setKey((byte) (0x20));
        packetValue.setValue(Packet.byteToByte((byte)lossAlert));
        send_packet.setPacketValue(packetValue,true);
        send_packet.print();
        send(send_packet);
        resent_cnt = 3;
    }
    public static class LongSitSetting{
        public int Enable;
        public int Threshold;
        public int DurationTime;
        public int StartTime;
        public int EndTime;
        public int Repeat;
    }
    public void setLongSit(LongSitSetting longSit){
        Packet.PacketValue packetValue = new Packet.PacketValue();
        packetValue.setCommandId((byte) (0x02));
        packetValue.setKey((byte) (0x21));
        long aData=0;
        aData = aData<<8  | (longSit.Enable&0xFF);
        aData = aData<<16 | (longSit.Threshold & 0xFFFF);
        aData = aData<<8  | (longSit.DurationTime & 0xFF);
        aData = aData<<8  | (longSit.StartTime & 0xFF);
        aData = aData<<8  | (longSit.EndTime & 0xFF);
        aData = aData<<8  | (longSit.Repeat & 0xFF);
        packetValue.setValue(Packet.longToByte(aData));
        send_packet.setPacketValue(packetValue, true);
        send_packet.print();
        send(send_packet);
        resent_cnt = 3;
    }
    public static class DailyData{
        int Steps;
        int Distance;
        int Calory;
    }

    public DailyData getDailyDataList() {
        return mDailyData;
    }

    public void getDailyData(){
        Packet.PacketValue packetValue = new Packet.PacketValue();
        packetValue.setCommandId((byte) (0x05));
        packetValue.setKey((byte) (0x09));
        send_packet.setPacketValue(packetValue,true);
        send_packet.print();
        send(send_packet);
        resent_cnt = 3;
    }

    public void setSYNC(byte sync){
        Packet.PacketValue packetValue = new Packet.PacketValue();
        packetValue.setCommandId((byte) (0x05));
        packetValue.setKey((byte) (0x06));
        packetValue.setValue(Packet.byteToByte(sync));
        send_packet.setPacketValue(packetValue,true);
        send_packet.print();
        send(send_packet);
        resent_cnt = 3;
    }

    public static class SportData{
        public int Year;
        public int Month;
        public int Day;
        public int Hour;
        public int Minute;
        public int Mode;
        public int Steps;
        public int ActiveTime;
        public int Distance;
        public int Calory;
    }
    public static class SleepData{
        public int Year;
        public int Month;
        public int Day;
        public int Hour;
        public int Minute;
        public int Mode;
    }
    public static class SleepSetting{
        public int Year;
        public int Month;
        public int Day;
        public int Hour;
        public int Minute;
        public int Mode;
    }
    private SportData SportDataFromByte(byte[] header,byte[] data){
        SportData sportData = new SportData();

        long aData = 0;
        aData = header[0] &0xFFL;
        aData = (aData << 8) |(header[1] & 0xFFL);

        sportData.Day = (int)(aData &0x1F);
        aData >>>= 5;
        sportData.Month = (int)(aData &0x0F);
        aData >>>= 4;
        sportData.Year = (int)(aData &0x3F);

        aData = data[0] & 0xFFL;
        aData = (aData << 8) | (data[1] & 0xFFL);
        aData = (aData << 8) | (data[2] & 0xFFL);
        aData = (aData << 8) | (data[3] & 0xFFL);
        aData = (aData << 8) | (data[4] & 0xFFL);
        aData = (aData << 8) | (data[5] & 0xFFL);
        aData = (aData << 8) | (data[6] & 0xFFL);
        aData = (aData << 8) | (data[7] & 0xFFL);

        sportData.Distance = (int)(aData &0xFFFF);
        aData >>>= 16;
        sportData.Calory = (int)(aData &0x7FFFF);
        aData >>>= 19;
        sportData.ActiveTime = (int)(aData & 0x0F);
        aData >>>= 4;
        sportData.Steps = (int)(aData & 0xFFF);
        aData >>>= 12;
        sportData.Mode = (int)(aData & 0x03);
        aData >>>= 2;
        int time = (int)(aData & 0x7FF);
        sportData.Hour = time/4;
        sportData.Minute = (time%4) * 15;

        return sportData;
    }
    private SleepData SleepDataFromByte(byte[] header,byte[] data){
        SleepData sleepData = new SleepData();

        long aData = 0;
        aData = header[0] &0xFFL;
        aData = (aData << 8) |(header[1] & 0xFFL);

        sleepData.Day = (int)(aData &0x1F);
        aData >>>= 5;
        sleepData.Month = (int)(aData &0x0F);
        aData >>>= 4;
        sleepData.Year = (int)(aData &0x3F);

        aData = data[0] & 0xFFL;
        aData = (aData << 8) | (data[1] & 0xFFL);
        aData = (aData << 8) | (data[2] & 0xFFL);
        aData = (aData << 8) | (data[3] & 0xFFL);

        sleepData.Mode = (int)(aData &0x0F);
        aData >>>= 16;
        int time = (int)(aData & 0xFFFF);
        sleepData.Hour = time/60;
        sleepData.Minute = time%60;

        return sleepData;
    }
    private SleepSetting SleepSettingFromByte(byte[] header,byte[] data){
        SleepSetting sleepData = new SleepSetting();

        long aData = 0;
        aData = header[0] &0xFFL;
        aData = (aData << 8) |(header[1] & 0xFFL);

        sleepData.Day = (int)(aData &0x1F);
        aData >>>= 5;
        sleepData.Month = (int)(aData &0x0F);
        aData >>>= 4;
        sleepData.Year = (int)(aData &0x3F);

        aData = data[0] & 0xFFL;
        aData = (aData << 8) | (data[1] & 0xFFL);
        aData = (aData << 8) | (data[2] & 0xFFL);
        aData = (aData << 8) | (data[3] & 0xFFL);

        sleepData.Mode = (int)(aData &0x0F);
        aData >>>= 16;
        int time = (int)(aData & 0xFFFF);
        sleepData.Hour = time/60;
        sleepData.Minute = time%60;

        return sleepData;
    }
    public void getSportData(){

        Packet.PacketValue packetValue = new Packet.PacketValue();
        packetValue.setCommandId((byte) (0x05));
        packetValue.setKey((byte) (0x01));
        packetValue.setValueLength((short) 0x00);
        send_packet.setPacketValue(packetValue,true);
        send_packet.print();
        send(send_packet);
    }
    private void send(Packet packet){

        final Packet packet1 = packet;
        new Thread(new Runnable(){
            @Override
            public void run() {
                final int packLength = 20;
                byte[] data = packet1.getPacket();
                int lastLength = data.length;
                byte[] sendData;
                int sendIndex = 0;
                while(lastLength>0) {
                    if (lastLength <= packLength) {
                        sendData = Arrays.copyOfRange(data, sendIndex, sendIndex + lastLength);
                        sendIndex += lastLength;
                        lastLength = 0;
                    } else {
                        sendData = Arrays.copyOfRange(data, sendIndex, sendIndex + packLength);
                        sendIndex += packLength;
                        lastLength -= packLength;
                    }
                    BluetoothLeService.HandleCommand command = BluetoothLeService.HandleCommand.NUS_WRITE_CHARACTERISTIC;
                    GattCommand.putExtra(BluetoothLeService.HandleCMD, command.getIndex());
                    GattCommand.putExtra(BluetoothLeService.HandleData, sendData);
                    try {
                        Thread.sleep(50L);
                    }catch (InterruptedException e){

                    }
                    sendBroadcast(GattCommand);
                    GattStatus = 1;
                }
            }
        },"Tread-BLESend").start();
    }

    private void resolve(Packet.PacketValue packetValue){
        switch (packetValue.getCommandId()){
            case 2:
                switch (packetValue.getKey()){
                    //获取闹钟
                    case 4:
                        int length = packetValue.getValueLength();
                        byte[] data= packetValue.getValue();
                        mAlarms.clear();
                        for(int i=0;i<length;i+=5)
                            mAlarms.add(AlarmFromByte(Arrays.copyOfRange(data,i,i+5)));
                        break;
                    default:
                        break;
                }
                break;
            case 5:
                int length = packetValue.getValueLength();
                byte[] data= packetValue.getValue();
                byte[] header;
                switch (packetValue.getKey()){

                    case 2:
                        //获取运动数据
                        mSportData.clear();
                        header = Arrays.copyOfRange(data,0,2);
                        for(int i=4;i<length;i+=8){
                            mSportData.add(SportDataFromByte(header,Arrays.copyOfRange(data,i,i+8)));
                        }

                        break;

                    case 3:
                        //获取睡眠数据
                        //mSleepData.clear();
                        header = Arrays.copyOfRange(data,0,2);
                        for (int i = 4; i < length; i +=4){
                            mSleepData.add(SleepDataFromByte(header,Arrays.copyOfRange(data,i,i+4)));
                        }
                        break;

                    case 5:
                        //获取睡眠设定
                        //mSleepSetting.clear();
                        header = Arrays.copyOfRange(data,0,2);
                        for(int i=4;i<length;i+=4){
                            mSleepSetting.add(SleepSettingFromByte(header, Arrays.copyOfRange(data, i, i + 4)));
                        }
                        break;

                    case 7:
                        //开始同步
                        Log.i(BluetoothLeService.TAG,"sportData get");
                        break;

                    case 8:
                        //同步结束
                        break;
                    case 9:
                        mDailyData.Steps = data[0]&0xFF;
                        mDailyData.Steps = (mDailyData.Steps << 8) | (data[1]&0xFF);
                        mDailyData.Steps = (mDailyData.Steps << 8) | (data[2]&0xFF);
                        mDailyData.Steps = (mDailyData.Steps << 8) | (data[3]&0xFF);
                        mDailyData.Distance = data[4]&0xFF;
                        mDailyData.Distance = (mDailyData.Steps << 8) | (data[5]&0xFF);
                        mDailyData.Distance = (mDailyData.Steps << 8) | (data[6]&0xFF);
                        mDailyData.Distance = (mDailyData.Steps << 8) | (data[7]&0xFF);
                        mDailyData.Calory = data[8]&0xFF;
                        mDailyData.Calory = (mDailyData.Steps << 8) | (data[9]&0xFF);
                        mDailyData.Calory = (mDailyData.Steps << 8) | (data[10]&0xFF);
                        mDailyData.Calory = (mDailyData.Steps << 8) | (data[11]&0xFF);
                        break;
                    default:
                        Log.i(BluetoothLeService.TAG,"sportData get");
                        break;
                }
                break;
        }
    }
    private BroadcastReceiver MyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothLeService.ACTION_GATT_HANDLE.equals(action)) {

            }
            else if(BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)){
                byte[] data=intent.getByteArrayExtra(BluetoothLeService.HandleData);
                receive_packet.append(data);
                int checkResult = receive_packet.checkPacket();
                Log.i(BluetoothLeService.TAG, "Check:" + Integer.toHexString(checkResult));
                receive_packet.print();
                //数据头错误，清空
                if (checkResult == 0x05){
                    receive_packet.clear();
                }
                //发送成功
                if(checkResult==0x10){
                    receive_packet.clear();
                }
                //ACK错误，需要重发
                if (checkResult == 0x30){
                    if(0<resent_cnt--) {
                        Log.i(BluetoothLeService.TAG,"Resent Packet!");
                        send(send_packet);
                    }
                    receive_packet.clear();
                }
                //接收数据包校验正确
                if(checkResult == 0){
                    try {
                        Packet.PacketValue packetValue = (Packet.PacketValue)receive_packet.getPacketValue().clone();
                        resolve(packetValue);
                    }
                    catch (CloneNotSupportedException e){
                        Log.i(BluetoothLeService.TAG,"Packet.PacketValue:CloneNotSupportedException");
                    }
                    Log.i(BluetoothLeService.TAG,"Send ACK!");
                    sendACK(receive_packet, false);
                    receive_packet.clear();
                }
                //接收数据包校验错误
                if (checkResult == 0x0b){
                    sendACK(receive_packet,true);
                    receive_packet.clear();
                }

            }
            else if(PacketParserService.ACTION_PACKET_HANDLE.equals(action)){
                int handle = intent.getIntExtra(HANDLE,0);
                switch (handle){
                    case 0:
                    default:
                        break;
                }
            }
            else if(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)){
                BluetoothLeService.HandleCommand cmd = BluetoothLeService.HandleCommand.NUS_TX_SET_NOTIFICATION;
                GattCommand.putExtra(BluetoothLeService.HandleCMD, cmd.getIndex());
                GattCommand.putExtra(BluetoothLeService.HandleData, true);
                sendBroadcast(GattCommand);
            }
            else if(BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)){
                BLE_CONNECT_STATUS = true;
                receive_packet.clear();
                send_packet.clear();
            }
            else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)){
                BLE_CONNECT_STATUS = false;
                receive_packet.clear();
                send_packet.clear();
            }
            else if (BluetoothLeService.ACTION_GATT_IDLE.equals(action)){
                GattStatus = 0;
                Log.i(BluetoothLeService.TAG,"send complete");
            }

        }
    };
    private static IntentFilter MyIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_IDLE);
        intentFilter.addAction(PacketParserService.ACTION_PACKET_HANDLE);

//        intentFilter.addAction(BluetoothLeService.ACTION_GATT_HANDLE);
        return intentFilter;
    }
}
