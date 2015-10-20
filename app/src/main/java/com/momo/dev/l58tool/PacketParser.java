package com.momo.dev.l58tool;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Administrator on 2015/10/16.
 */
public class PacketParser extends Service {

    public final static String ACTION_PACKET_HANDLE =
            "com.momo.dev.l58tool.packet.parser.ACTION_PACKET_HANDLE";

    public final static String HANDLE = "PacketHandle";
    int GattStatus = 0;
    int resent_cnt = 0;
    Intent GattCommand = new Intent(BluetoothLeService.ACTION_GATT_HANDLE);

    Packet send_packet = new Packet();
    Packet receive_packet = new Packet();
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerReceiver(MyReceiver, MyIntentFilter());
    }
    public void sendACK(Packet rPacket,boolean error){
        Packet.L1Header l1Header = new Packet.L1Header();
        l1Header.setLength((short)0);
        l1Header.setACK(true);
        l1Header.setError(error);
        l1Header.setSequenceId(rPacket.getL1Header().getSequenceId());
        l1Header.setCRC16((short) 0);
        send_packet.setL1Header(l1Header);
        send_packet.setPacketValue(null,false);
        send(send_packet);
    }

    public void setTime(){
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        int time;
        time = (year-2000) & 0x3F;
        time = time << 4 | ((month+1) & 0x0F);
        time = time << 5 | (day & 0x1F);
        time = time << 5 | (hour & 0x1F);
        time = time << 6 | (minute & 0x3F);
        time = time << 6 | (second & 0x1F);

        Packet.PacketValue packetValue = new Packet.PacketValue();
        packetValue.setCommandId((byte) (0x02));
        packetValue.setKey((byte) (0x01));
        packetValue.setValueLength((short) 0x04);
        packetValue.setValue(Packet.intToByte(time));
        send_packet.setPacketValue(packetValue, true);
        send_packet.print();
        send(send_packet);
        resent_cnt = 3;
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
    public void send(Packet packet){

        final Packet packet1 = packet;
        new Thread(new Runnable(){
            @Override
            public void run() {
                final int packLength = 10;
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
    List<Byte> mReceiveData = new ArrayList<Byte>();

    public void resolve(byte[] value){
        int commandid = value[0];
        int key = value [2];
        int length = (value[3]&0x000000ff)<<8|value[4];

    }
    public void resolve(Packet.PacketValue packetValue){
        switch (packetValue.getCommandId()){
            case 2:
                switch (packetValue.getKey()){
                    case 1:
                        break;
                    case 2:
                        break;
                    default:
                        break;
                }
                break;
            case 3:
                break;
            case 4:
                break;
            case 5:
                switch (packetValue.getKey()){
                    case 7:
                        Log.i(BluetoothLeService.TAG,"sportData get");
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
                    sendACK(receive_packet,false);
                    receive_packet.clear();
                }
                //接收数据包校验错误
                if (checkResult == 0x0b){
                    sendACK(receive_packet,true);
                    receive_packet.clear();
                }

            }
            else if(PacketParser.ACTION_PACKET_HANDLE.equals(action)){
                int handle = intent.getIntExtra(HANDLE,0);
                switch (handle){
                    case 0:
                        break;
                    case 1:
                        setTime();
                        break;
                    case 2:
                        getSportData();
                    default:
                        break;
                }
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
        intentFilter.addAction(PacketParser.ACTION_PACKET_HANDLE);

//        intentFilter.addAction(BluetoothLeService.ACTION_GATT_HANDLE);
        return intentFilter;
    }
}
