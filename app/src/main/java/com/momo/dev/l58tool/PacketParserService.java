package com.momo.dev.l58tool;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Random;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Created by Administrator on 2015/10/16.
 */
public class PacketParserService extends Service {

    private int mVERSION = 101;

    public final static String ACTION_PACKET_HANDLE =
            "com.momo.dev.l58tool.packet.parser.ACTION_PACKET_HANDLE";

    public final static String HANDLE = "PacketHandle";
    boolean BLE_CONNECT_STATUS = false;
    private int GattStatus = 0;
    private int resent_cnt = 0;
    private Intent GattCommand = new Intent(BluetoothLeService.ACTION_GATT_HANDLE);

    private Intent PacketHandle = new Intent(ACTION_PACKET_HANDLE);
    private CallBack mPacketCallBack;
    private ArrayList<Alarm> mAlarms = new ArrayList<Alarm>();
    private ArrayList<SportData> mSportData = new ArrayList<SportData>();
    private ArrayList<SleepData> mSleepData = new ArrayList<SleepData>();
    private ArrayList<SleepSetting> mSleepSetting = new ArrayList<SleepSetting>();
    private DailyData mDailyData = new DailyData();
    private LocalBinder mBinder = new LocalBinder();

    private TimerThread sendTimerThread;
    private TimerThread receiveTimerThread;
    public static final byte RECEIVED_ALARM = 1;
    public static final byte RECEIVED_SPORT_DATA = 2;
    public static final byte RECEIVED_DAILY_DATA = 3;

    private Packet send_packet = new Packet();
    private Packet receive_packet = new Packet();

    final android.os.Handler mHandler = new android.os.Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0xAA) {
                if (mPacketCallBack != null) {
                    mPacketCallBack.onTimeOut();
                    Log.i(BluetoothLeService.TAG, "ACK TimeOut!");
                }
            } else if (msg.what == 0xBB) {
                receive_packet.clear();
            }
        }
    };

    static void writeLog(String content) {
        String logFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        logFileName += "/L58Tool/Log.txt";
        FileWriter fileWriter;
        try {
            fileWriter = new FileWriter(logFileName, true);
            fileWriter.append(content);
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
        final Intent intent = new Intent(this, BluetoothLeService.class);
        startService(intent);
        sendTimerThread = new TimerThread().setStatus(TimerThread.STOP).setWhat(0xAA);
        sendTimerThread.start();
        receiveTimerThread = new TimerThread().setStatus(TimerThread.STOP).setWhat(0xBB);
        receiveTimerThread.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        final Intent intent = new Intent(this, BluetoothLeService.class);
        stopService(intent);
        sendTimerThread.setStatus(TimerThread.EXIT);
        receiveTimerThread.setStatus(TimerThread.EXIT);
    }

    public void connect(String address) {

        BluetoothLeService.HandleCommand cmd = BluetoothLeService.HandleCommand.NORDIC_BLE_CONNECT;
        GattCommand.putExtra(BluetoothLeService.HandleCMD, cmd.getHandleCommandIndex(cmd.getCommand()));
        GattCommand.putExtra(BluetoothLeService.HandleDeviceAddress, address);
        sendBroadcast(GattCommand);
    }

    public void disconnect() {
        BluetoothLeService.HandleCommand cmd = BluetoothLeService.HandleCommand.NORDIC_BLE_DISCONNECT;
        GattCommand.putExtra(BluetoothLeService.HandleCMD, cmd.getHandleCommandIndex(cmd.getCommand()));
        sendBroadcast(GattCommand);
    }

    public boolean getConnnetStatus() {
        return BLE_CONNECT_STATUS;
    }

    public interface CallBack {
        void onSendSuccess();

        void onSendFailure();

        void onTimeOut();

        void onConnectStatusChanged(boolean status);

        void onDataReceived(byte category);

        void onCharacteristicNotFound();
    }

    public void registerCallback(CallBack callBack) {
        mPacketCallBack = callBack;
    }

    private void sendACK(Packet rPacket, boolean error) {
        Packet.L1Header l1Header = new Packet.L1Header();
        l1Header.setLength((short) 0);
        l1Header.setACK(true);
        l1Header.setError(error);
        l1Header.setSequenceId(rPacket.getL1Header().getSequenceId());
        l1Header.setCRC16((short) 0);
        send_packet.setL1Header(l1Header);
        send_packet.setPacketValue(null, false);
        send_packet.print();
        send(send_packet);
    }

    public int getVersion() {
        return mVERSION;
    }

    public ArrayList<SportData> getSportDataList() {
        ArrayList<SportData> nSportData = new ArrayList<SportData>();
        nSportData.addAll(mSportData);
        mSportData.clear();
        return nSportData;
    }

    public ArrayList<SleepData> getSleepDataList() {
        ArrayList<SleepData> nSleepData = new ArrayList<SleepData>();
        nSleepData.addAll(mSleepData);
        mSleepData.clear();
        return nSleepData;
    }

    public ArrayList<SleepSetting> getSleepSettingList() {
        ArrayList<SleepSetting> nSleepSetting = new ArrayList<SleepSetting>();
        nSleepSetting.addAll(mSleepSetting);
        mSleepSetting.clear();
        return nSleepSetting;
    }

    public ArrayList<Alarm> getAlarmsList() {
        return mAlarms;
    }

    public void setTime() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        int aData;
        aData = (year - 2000) & 0x3F;
        aData = aData << 4 | ((month + 1) & 0x0F);
        aData = aData << 5 | (day & 0x1F);
        aData = aData << 5 | (hour & 0x1F);
        aData = aData << 6 | (minute & 0x3F);
        aData = aData << 6 | (second & 0x3F);

        Packet.PacketValue packetValue = new Packet.PacketValue();
        packetValue.setCommandId((byte) (0x02));
        packetValue.setKey((byte) (0x01));
        packetValue.setValue(Packet.intToByte(aData));
        send_packet.setPacketValue(packetValue, true);
        send_packet.print();
        send(send_packet);
        resent_cnt = 3;
    }

    public static class Alarm implements Parcelable {
        public int ID;
        public int Year;
        public int Month;
        public int Day;
        public int Hour;
        public int Minute;
        public int Repeat;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.ID);
            dest.writeInt(this.Year);
            dest.writeInt(this.Month);
            dest.writeInt(this.Day);
            dest.writeInt(this.Hour);
            dest.writeInt(this.Minute);
            dest.writeInt(this.Repeat);
        }

        public Alarm() {
        }

        protected Alarm(Parcel in) {
            this.ID = in.readInt();
            this.Year = in.readInt();
            this.Month = in.readInt();
            this.Day = in.readInt();
            this.Hour = in.readInt();
            this.Minute = in.readInt();
            this.Repeat = in.readInt();
        }

        public static final Parcelable.Creator<Alarm> CREATOR = new Parcelable.Creator<Alarm>() {
            public Alarm createFromParcel(Parcel source) {
                return new Alarm(source);
            }

            public Alarm[] newArray(int size) {
                return new Alarm[size];
            }
        };
    }

    private byte[] AlarmToByte(Alarm alarm) {
        long aData;
        aData = (long) (alarm.Year - 2000) & 0x3F;
        aData = aData << 4 | (alarm.Month & 0x0F);
        aData = aData << 5 | (alarm.Day & 0x1F);
        aData = aData << 5 | (alarm.Hour & 0x1F);
        aData = aData << 6 | (alarm.Minute & 0x3F);
        aData = aData << 3 | (alarm.ID & 0x07);
        aData = aData << 4;
        aData = aData << 7 | (alarm.Repeat & 0x7F);
        return Arrays.copyOfRange(Packet.longToByte(aData), 3, 8);
    }

    private Alarm AlarmFromByte(byte[] data) {
        Alarm alarm = new Alarm();
        long aData = data[0] & 0xFFL;
        aData = (aData << 8) | (data[1] & 0xFFL);
        aData = (aData << 8) | (data[2] & 0xFFL);
        aData = (aData << 8) | (data[3] & 0xFFL);
        aData = (aData << 8) | (data[4] & 0xFFL);

        alarm.Repeat = (int) (aData & 0x7F);
        aData >>>= 11;
        alarm.ID = (int) (aData & 0x07);
        aData >>>= 3;
        alarm.Minute = (int) (aData & 0x3F);
        aData >>>= 6;
        alarm.Hour = (int) (aData & 0x1F);
        aData >>>= 5;
        alarm.Day = (int) (aData & 0x1F);
        aData >>>= 5;
        alarm.Month = (int) (aData & 0x0F);
        aData >>>= 4;
        alarm.Year = (int) (aData & 0x3F) + 2000;
        return alarm;
    }

    public void setAlarmList(ArrayList<Alarm> alarmList) {

        byte[] aData;
        Packet.PacketValue packetValue = new Packet.PacketValue();
        packetValue.setCommandId((byte) (0x02));
        packetValue.setKey((byte) (0x02));
        for (Alarm alarm : alarmList) {
            aData = AlarmToByte(alarm);
            packetValue.appendValue(aData);
        }
        send_packet.setPacketValue(packetValue, true);
        send_packet.print();
        send(send_packet);
        resent_cnt = 3;
    }

    public void getAlarms() {
        Packet.PacketValue packetValue = new Packet.PacketValue();
        packetValue.setCommandId((byte) (0x02));
        packetValue.setKey((byte) (0x03));
        send_packet.setPacketValue(packetValue, true);
        send_packet.print();
        send(send_packet);
        resent_cnt = 3;
    }

    public void setTarget(int target) {
        Packet.PacketValue packetValue = new Packet.PacketValue();
        packetValue.setCommandId((byte) (0x02));
        packetValue.setKey((byte) (0x05));
        packetValue.setValue(Packet.intToByte(target));
        send_packet.setPacketValue(packetValue, true);
        send_packet.print();
        send(send_packet);
        resent_cnt = 3;
    }

    public static class UserProfile implements Parcelable {
        public int Sex;
        public int Age;
        public int Stature;     //0.5cm
        public int Weight;      //0.5Kg

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.Sex);
            dest.writeInt(this.Age);
            dest.writeInt(this.Stature);
            dest.writeInt(this.Weight);
        }

        public UserProfile() {
        }

        protected UserProfile(Parcel in) {
            this.Sex = in.readInt();
            this.Age = in.readInt();
            this.Stature = in.readInt();
            this.Weight = in.readInt();
        }

        public static final Parcelable.Creator<UserProfile> CREATOR = new Parcelable.Creator<UserProfile>() {
            public UserProfile createFromParcel(Parcel source) {
                return new UserProfile(source);
            }

            public UserProfile[] newArray(int size) {
                return new UserProfile[size];
            }
        };
    }

    public void setUserProfile(UserProfile userProfile) {
        Packet.PacketValue packetValue = new Packet.PacketValue();
        packetValue.setCommandId((byte) (0x02));
        packetValue.setKey((byte) (0x10));
        int aData;
        aData = userProfile.Sex;
        aData = aData << 7 | (userProfile.Age & 0x7F);
        aData = aData << 9 | (userProfile.Stature & 0x1FF);
        aData = aData << 10 | (userProfile.Weight & 0x3FF);
        aData = aData << 5;
        packetValue.setValue(Packet.intToByte(aData));
        send_packet.setPacketValue(packetValue, true);
        send_packet.print();
        send(send_packet);
        resent_cnt = 3;
    }

    public void setLossAlert(int lossAlert) {
        Packet.PacketValue packetValue = new Packet.PacketValue();
        packetValue.setCommandId((byte) (0x02));
        packetValue.setKey((byte) (0x20));
        packetValue.setValue(Packet.byteToByte((byte) lossAlert));
        send_packet.setPacketValue(packetValue, true);
        send_packet.print();
        send(send_packet);
        resent_cnt = 3;
    }

    public static class LongSitSetting implements Parcelable {
        public int Enable;
        public int ThresholdSteps;
        public int DurationTimeMinutes;
        public int StartTimeHour;
        public int EndTimeHour;
        public int Repeat;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.Enable);
            dest.writeInt(this.ThresholdSteps);
            dest.writeInt(this.DurationTimeMinutes);
            dest.writeInt(this.StartTimeHour);
            dest.writeInt(this.EndTimeHour);
            dest.writeInt(this.Repeat);
        }

        public LongSitSetting() {
        }

        protected LongSitSetting(Parcel in) {
            this.Enable = in.readInt();
            this.ThresholdSteps = in.readInt();
            this.DurationTimeMinutes = in.readInt();
            this.StartTimeHour = in.readInt();
            this.EndTimeHour = in.readInt();
            this.Repeat = in.readInt();
        }

        public static final Parcelable.Creator<LongSitSetting> CREATOR = new Parcelable.Creator<LongSitSetting>() {
            public LongSitSetting createFromParcel(Parcel source) {
                return new LongSitSetting(source);
            }

            public LongSitSetting[] newArray(int size) {
                return new LongSitSetting[size];
            }
        };
    }

    public void setLongSit(LongSitSetting longSit) {
        Packet.PacketValue packetValue = new Packet.PacketValue();
        packetValue.setCommandId((byte) (0x02));
        packetValue.setKey((byte) (0x21));
        long aData = 0;
        aData = aData << 8 | (longSit.Enable & 0xFF);
        aData = aData << 16 | (longSit.ThresholdSteps & 0xFFFF);
        aData = aData << 8 | (longSit.DurationTimeMinutes & 0xFF);
        aData = aData << 8 | (longSit.StartTimeHour & 0xFF);
        aData = aData << 8 | (longSit.EndTimeHour & 0xFF);
        aData = aData << 8 | (longSit.Repeat & 0xFF);
        packetValue.setValue(Packet.longToByte(aData));
        send_packet.setPacketValue(packetValue, true);
        send_packet.print();
        send(send_packet);
        resent_cnt = 3;
    }

    public void setWearHand(int hand) {
        Packet.PacketValue packetValue = new Packet.PacketValue();
        packetValue.setCommandId((byte) (0x02));
        packetValue.setKey((byte) (0x22));
        packetValue.setValue(Packet.byteToByte((byte) hand));
        send_packet.setPacketValue(packetValue, true);
        send_packet.print();
        send(send_packet);
        resent_cnt = 3;
    }

    public void setHourFormat(int format) {
        Packet.PacketValue packetValue = new Packet.PacketValue();
        packetValue.setCommandId((byte) (0x02));
        packetValue.setKey((byte) (0x26));
        packetValue.setValue(Packet.byteToByte((byte) format));
        send_packet.setPacketValue(packetValue, true);
        send_packet.print();
        send(send_packet);
        resent_cnt = 3;
    }

    public static class DailyData implements Parcelable {
        int Steps;
        int Distance;
        int Calory;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.Steps);
            dest.writeInt(this.Distance);
            dest.writeInt(this.Calory);
        }

        public DailyData() {
        }

        protected DailyData(Parcel in) {
            this.Steps = in.readInt();
            this.Distance = in.readInt();
            this.Calory = in.readInt();
        }

        public static final Parcelable.Creator<DailyData> CREATOR = new Parcelable.Creator<DailyData>() {
            public DailyData createFromParcel(Parcel source) {
                return new DailyData(source);
            }

            public DailyData[] newArray(int size) {
                return new DailyData[size];
            }
        };
    }

    public DailyData getDailyDataList() {
        return mDailyData;
    }

    public void getDailyData() {
        Packet.PacketValue packetValue = new Packet.PacketValue();
        packetValue.setCommandId((byte) (0x05));
        packetValue.setKey((byte) (0x0B));
        send_packet.setPacketValue(packetValue, true);
        send_packet.print();
        send(send_packet);
        resent_cnt = 3;
    }

    public void setSportNotify(int notify) {
        Packet.PacketValue packetValue = new Packet.PacketValue();
        packetValue.setCommandId((byte) (0x05));
        packetValue.setKey((byte) (0x06));
        packetValue.setValue(Packet.byteToByte((byte) notify));
        send_packet.setPacketValue(packetValue, true);
        send_packet.print();
        send(send_packet);
        resent_cnt = 3;
    }

    public static class SportData implements Parcelable {
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

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.Year);
            dest.writeInt(this.Month);
            dest.writeInt(this.Day);
            dest.writeInt(this.Hour);
            dest.writeInt(this.Minute);
            dest.writeInt(this.Mode);
            dest.writeInt(this.Steps);
            dest.writeInt(this.ActiveTime);
            dest.writeInt(this.Distance);
            dest.writeInt(this.Calory);
        }

        public SportData() {
        }

        protected SportData(Parcel in) {
            this.Year = in.readInt();
            this.Month = in.readInt();
            this.Day = in.readInt();
            this.Hour = in.readInt();
            this.Minute = in.readInt();
            this.Mode = in.readInt();
            this.Steps = in.readInt();
            this.ActiveTime = in.readInt();
            this.Distance = in.readInt();
            this.Calory = in.readInt();
        }

        public static final Parcelable.Creator<SportData> CREATOR = new Parcelable.Creator<SportData>() {
            public SportData createFromParcel(Parcel source) {
                return new SportData(source);
            }

            public SportData[] newArray(int size) {
                return new SportData[size];
            }
        };
    }

    public static class SleepData implements Parcelable {
        public int Year;
        public int Month;
        public int Day;
        public int Hour;
        public int Minute;
        public int Mode;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.Year);
            dest.writeInt(this.Month);
            dest.writeInt(this.Day);
            dest.writeInt(this.Hour);
            dest.writeInt(this.Minute);
            dest.writeInt(this.Mode);
        }

        public SleepData() {
        }

        protected SleepData(Parcel in) {
            this.Year = in.readInt();
            this.Month = in.readInt();
            this.Day = in.readInt();
            this.Hour = in.readInt();
            this.Minute = in.readInt();
            this.Mode = in.readInt();
        }

        public static final Parcelable.Creator<SleepData> CREATOR = new Parcelable.Creator<SleepData>() {
            public SleepData createFromParcel(Parcel source) {
                return new SleepData(source);
            }

            public SleepData[] newArray(int size) {
                return new SleepData[size];
            }
        };
    }

    public static class SleepSetting implements Parcelable {
        public int Year;
        public int Month;
        public int Day;
        public int Hour;
        public int Minute;
        public int Mode;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.Year);
            dest.writeInt(this.Month);
            dest.writeInt(this.Day);
            dest.writeInt(this.Hour);
            dest.writeInt(this.Minute);
            dest.writeInt(this.Mode);
        }

        public SleepSetting() {
        }

        protected SleepSetting(Parcel in) {
            this.Year = in.readInt();
            this.Month = in.readInt();
            this.Day = in.readInt();
            this.Hour = in.readInt();
            this.Minute = in.readInt();
            this.Mode = in.readInt();
        }

        public static final Parcelable.Creator<SleepSetting> CREATOR = new Parcelable.Creator<SleepSetting>() {
            public SleepSetting createFromParcel(Parcel source) {
                return new SleepSetting(source);
            }

            public SleepSetting[] newArray(int size) {
                return new SleepSetting[size];
            }
        };
    }

    private SportData SportDataFromByte(byte[] header, byte[] data) {
        SportData sportData = new SportData();

        long aData = 0;
        aData = header[0] & 0xFFL;
        aData = (aData << 8) | (header[1] & 0xFFL);

        sportData.Day = (int) (aData & 0x1F);
        aData >>>= 5;
        sportData.Month = (int) (aData & 0x0F);
        aData >>>= 4;
        sportData.Year = (int) (aData & 0x3F) + 2000;

        aData = data[0] & 0xFFL;
        aData = (aData << 8) | (data[1] & 0xFFL);
        aData = (aData << 8) | (data[2] & 0xFFL);
        aData = (aData << 8) | (data[3] & 0xFFL);
        aData = (aData << 8) | (data[4] & 0xFFL);
        aData = (aData << 8) | (data[5] & 0xFFL);
        aData = (aData << 8) | (data[6] & 0xFFL);
        aData = (aData << 8) | (data[7] & 0xFFL);

        sportData.Distance = (int) (aData & 0xFFFF);
        aData >>>= 16;
        sportData.Calory = (int) (aData & 0x7FFFF);
        aData >>>= 19;
        sportData.ActiveTime = (int) (aData & 0x0F);
        aData >>>= 4;
        sportData.Steps = (int) (aData & 0xFFF);
        aData >>>= 12;
        sportData.Mode = (int) (aData & 0x03);
        aData >>>= 2;
        int time = (int) (aData & 0x7FF);
        sportData.Hour = time / 4;
        sportData.Minute = (time % 4) * 15;

        return sportData;
    }

    private SleepData SleepDataFromByte(byte[] header, byte[] data) {
        SleepData sleepData = new SleepData();

        long aData = 0;
        aData = header[0] & 0xFFL;
        aData = (aData << 8) | (header[1] & 0xFFL);

        sleepData.Day = (int) (aData & 0x1F);
        aData >>>= 5;
        sleepData.Month = (int) (aData & 0x0F);
        aData >>>= 4;
        sleepData.Year = (int) (aData & 0x3F) + 2000;

        aData = data[0] & 0xFFL;
        aData = (aData << 8) | (data[1] & 0xFFL);
        aData = (aData << 8) | (data[2] & 0xFFL);
        aData = (aData << 8) | (data[3] & 0xFFL);

        sleepData.Mode = (int) (aData & 0x0F);
        aData >>>= 16;
        int time = (int) (aData & 0xFFFF);
        sleepData.Hour = time / 60;
        sleepData.Minute = time % 60;

        return sleepData;
    }

    private SleepSetting SleepSettingFromByte(byte[] header, byte[] data) {
        SleepSetting sleepData = new SleepSetting();

        long aData = 0;
        aData = header[0] & 0xFFL;
        aData = (aData << 8) | (header[1] & 0xFFL);

        sleepData.Day = (int) (aData & 0x1F);
        aData >>>= 5;
        sleepData.Month = (int) (aData & 0x0F);
        aData >>>= 4;
        sleepData.Year = (int) (aData & 0x3F) + 2000;

        aData = data[0] & 0xFFL;
        aData = (aData << 8) | (data[1] & 0xFFL);
        aData = (aData << 8) | (data[2] & 0xFFL);
        aData = (aData << 8) | (data[3] & 0xFFL);

        sleepData.Mode = (int) (aData & 0x0F);
        aData >>>= 16;
        int time = (int) (aData & 0xFFFF);
        sleepData.Hour = time / 60;
        sleepData.Minute = time % 60;

        return sleepData;
    }

    public void getSportData() {

        Packet.PacketValue packetValue = new Packet.PacketValue();
        packetValue.setCommandId((byte) (0x05));
        packetValue.setKey((byte) (0x01));
        packetValue.setValueLength((short) 0x00);
        send_packet.setPacketValue(packetValue, true);
        send_packet.print();
        send(send_packet);
        resent_cnt = 3;
    }

    public void mock() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    Thread.sleep(50L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Calendar calendar = Calendar.getInstance();
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                int day = calendar.get(Calendar.DAY_OF_MONTH);
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                int minute = calendar.get(Calendar.MINUTE);
                int second = calendar.get(Calendar.SECOND);
                mSportData.clear();
                mSleepData.clear();
                Random random = new Random();

                for (int i = 0; i < 20; i++) {
                    SportData sportData = new SportData();
                    sportData.Year = year;
                    sportData.Month = month;
                    sportData.Day = day;
                    sportData.Hour = Math.abs(random.nextInt()) % 24;
                    sportData.Minute = (Math.abs(random.nextInt()) % 4) * 15;
                    sportData.Steps = Math.abs(random.nextInt()) % 1000;
                    sportData.Calory = Math.abs(random.nextInt()) % 1000;
                    sportData.Distance = Math.abs(random.nextInt()) % 1000;
                    mSportData.add(sportData);
                }
                for (int i = 0; i < 10; i++) {
                    SleepData sleepData = new SleepData();
                    sleepData.Year = year;
                    sleepData.Month = month;
                    sleepData.Day = day;
                    sleepData.Hour = Math.abs(random.nextInt()) % 24;
                    sleepData.Minute = Math.abs(random.nextInt()) % 60;
                    sleepData.Mode = Math.abs(random.nextInt()) % 2;
                    mSleepData.add(sleepData);
                }
                if (mPacketCallBack != null) {
                    mPacketCallBack.onDataReceived(RECEIVED_SPORT_DATA);
                }
                DailyData dailyData = new DailyData();
                dailyData.Steps = Math.abs(random.nextInt()) % 1000;
                dailyData.Distance = Math.abs(random.nextInt()) % 1000;
                dailyData.Calory = Math.abs(random.nextInt()) % 1000;
                mDailyData = dailyData;
                if (mPacketCallBack != null) {
                    mPacketCallBack.onDataReceived(RECEIVED_DAILY_DATA);
                }
                for (int i = 0; i < 8; i++) {
                    Alarm alarm = new Alarm();
                    alarm.Year = 2015;
                    alarm.Month = 11;
                    alarm.Day = 21;
                    alarm.Hour = 15;
                    alarm.Minute = 0;
                    alarm.Repeat = 0x7F;
                    alarm.ID = i;
                    mAlarms.add(alarm);
                }
                if (mPacketCallBack != null) {
                    mPacketCallBack.onDataReceived(RECEIVED_ALARM);
                }
                PacketHandle.putParcelableArrayListExtra("Alarms", mAlarms);
                sendBroadcast(PacketHandle);
            }
        }.start();
    }

    private void send(Packet packet) {

        final byte[] data = packet.getPacket();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final int packLength = 20;
                int lastLength = data.length;
                byte[] sendData;
                int sendIndex = 0;
                while (lastLength > 0) {
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
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    sendBroadcast(GattCommand);
                    GattStatus = 1;
                }
            }
        }, "Tread-BLESend").start();
        sendTimerThread.setTimeOut(100).setStatus(TimerThread.RESTART);

    }

    private void resolve(Packet.PacketValue packetValue) {
        byte command = packetValue.getCommandId();
        byte key = packetValue.getKey();
        int length = packetValue.getValueLength();
        byte[] data = packetValue.getValue();
        switch (command) {
            case 2:
                switch (key) {
                    case 4:
                        //获取闹钟
                        mAlarms.clear();
                        for (int i = 0; i < length; i += 5) {
                            mAlarms.add(AlarmFromByte(Arrays.copyOfRange(data, i, i + 5)));
                        }
                        if (mPacketCallBack != null) {
                            mPacketCallBack.onDataReceived(RECEIVED_ALARM);
                        }

                        break;
                    default:
                        break;
                }
                break;
            case 5:
                byte[] header;
                switch (key) {
                    case 2:
                        //获取运动数据
                        mSportData.clear();
                        header = Arrays.copyOfRange(data, 0, 2);
                        for (int i = 4; i < length; i += 8) {
                            mSportData.add(SportDataFromByte(header, Arrays.copyOfRange(data, i, i + 8)));
                        }
                        break;

                    case 3:
                        //获取睡眠数据
                        header = Arrays.copyOfRange(data, 0, 2);
                        for (int i = 4; i < length; i += 4) {
                            mSleepData.add(SleepDataFromByte(header, Arrays.copyOfRange(data, i, i + 4)));
                        }
                        break;

                    case 5:
                        //获取睡眠设定
                        header = Arrays.copyOfRange(data, 0, 2);
                        for (int i = 4; i < length; i += 4) {
                            mSleepSetting.add(SleepSettingFromByte(header, Arrays.copyOfRange(data, i, i + 4)));
                        }
                        break;

                    case 7:
                        //开始同步
                        Log.i(BluetoothLeService.TAG, "sportData get");
                        break;

                    case 8:
                        //同步结束
                        if (mPacketCallBack != null) {
                            mPacketCallBack.onDataReceived(RECEIVED_SPORT_DATA);
                        } else {
                            writeLog("Error:CallBack is Null!\n");
                        }
                        break;
                    case 0x0C:
                        //获取当日运动数据
                        mDailyData.Steps = data[0] & 0xFF;
                        mDailyData.Steps = (mDailyData.Steps << 8) | (data[1] & 0xFF);
                        mDailyData.Steps = (mDailyData.Steps << 8) | (data[2] & 0xFF);
                        mDailyData.Steps = (mDailyData.Steps << 8) | (data[3] & 0xFF);
                        mDailyData.Distance = data[4] & 0xFF;
                        mDailyData.Distance = (mDailyData.Distance << 8) | (data[5] & 0xFF);
                        mDailyData.Distance = (mDailyData.Distance << 8) | (data[6] & 0xFF);
                        mDailyData.Distance = (mDailyData.Distance << 8) | (data[7] & 0xFF);
                        mDailyData.Calory = data[8] & 0xFF;
                        mDailyData.Calory = (mDailyData.Calory << 8) | (data[9] & 0xFF);
                        mDailyData.Calory = (mDailyData.Calory << 8) | (data[10] & 0xFF);
                        mDailyData.Calory = (mDailyData.Calory << 8) | (data[11] & 0xFF);
                        if (mPacketCallBack != null) {
                            mPacketCallBack.onDataReceived(RECEIVED_DAILY_DATA);
                        }
                        break;
                    default:
                        Log.i(BluetoothLeService.TAG, "sportData get");
                        break;
                }
                break;
        }
    }


    class TimerThread extends Thread {
        static final String START = "start";
        static final String RESTART = "restart";
        static final String PAUSE = "pause";
        static final String STOP = "stop";
        static final String EXIT = "exit";
        int TimeOut = 100;
        int mCount = 0;
        int What = 0;
        public String Status;

        public void run() {
            while (true) {
                if (START.equals(Status)) {
                    try {
                        Thread.sleep(20);
                        mCount++;

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else if (RESTART.equals(Status)) {
                    mCount = 0;
                    Status = START;
                    Thread.yield();
                } else if (PAUSE.equals(Status)) {
                    Thread.yield();
                } else if (STOP.equals(Status)) {
                    mCount = 0;
                    Thread.yield();
                } else if (EXIT.equals(Status)) {
                    break;
                }
                if (mCount >= TimeOut) {
                    mHandler.sendEmptyMessage(What);
                    mCount = 0;
                    Status = STOP;
                }

            }
        }

        public int getWhat() {
            return What;
        }

        public TimerThread setWhat(int what) {
            What = what;
            return this;
        }

        public String getStatus() {
            return Status;
        }

        public TimerThread setStatus(String status) {
            Status = status;
            return this;
        }

        public int getTimeOut() {
            return TimeOut;
        }

        public TimerThread setTimeOut(int mTimeOut) {
            this.TimeOut = mTimeOut;
            return this;
        }
    }

    private BroadcastReceiver MyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_HANDLE.equals(action)) {

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                byte[] data = intent.getByteArrayExtra(BluetoothLeService.HandleData);
                receive_packet.append(data);
                receiveTimerThread.setTimeOut(100).setStatus(TimerThread.RESTART);
                int checkResult = receive_packet.checkPacket();
                Log.i(BluetoothLeService.TAG, "Check:" + Integer.toHexString(checkResult));
                receive_packet.print();
                //数据头错误，清空
                if (checkResult == 0x05) {
                    receive_packet.clear();
                }
                //发送成功
                else if (checkResult == 0x10) {
                    writeLog("Send:" + send_packet.toString());
                    receive_packet.clear();
                    if (mPacketCallBack != null) {
                        mPacketCallBack.onSendSuccess();
                    }
                    sendTimerThread.setStatus(TimerThread.STOP);
                }
                //ACK错误，需要重发
                else if (checkResult == 0x30) {
                    if (0 < resent_cnt--) {
                        Log.i(BluetoothLeService.TAG, "Resent Packet!");
                        send(send_packet);
                    } else {
                        if (mPacketCallBack != null) {
                            mPacketCallBack.onSendFailure();
                        }
                    }
                    receive_packet.clear();
                }
                //接收数据包校验正确
                else if (checkResult == 0) {
                    try {
                        Packet.PacketValue packetValue = (Packet.PacketValue) receive_packet.getPacketValue().clone();
                        resolve(packetValue);
                    } catch (CloneNotSupportedException e) {
                        Log.i(BluetoothLeService.TAG, "Packet.PacketValue:CloneNotSupportedException");
                    }
                    Log.i(BluetoothLeService.TAG, "Send ACK!");
                    sendACK(receive_packet, false);
                    writeLog("Receive:" + receive_packet.toString());
                    receive_packet.clear();
                    receiveTimerThread.setStatus(TimerThread.STOP);
                }
                //接收数据包校验错误
                else if (checkResult == 0x0b) {
                    sendACK(receive_packet, true);
                    receive_packet.clear();
                }

            } else if (PacketParserService.ACTION_PACKET_HANDLE.equals(action)) {
                int handle = intent.getIntExtra(HANDLE, 0);
                switch (handle) {
                    case 0:
                    default:
                        break;
                }
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                BluetoothLeService.HandleCommand cmd = BluetoothLeService.HandleCommand.NUS_TX_SET_NOTIFICATION;
                GattCommand.putExtra(BluetoothLeService.HandleCMD, cmd.getIndex());
                GattCommand.putExtra(BluetoothLeService.HandleData, true);
                sendBroadcast(GattCommand);
            } else if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                BLE_CONNECT_STATUS = true;
                if (mPacketCallBack != null) {
                    mPacketCallBack.onConnectStatusChanged(true);
                }
                receive_packet.clear();
                send_packet.clear();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                BLE_CONNECT_STATUS = false;
                if (mPacketCallBack != null) {
                    mPacketCallBack.onConnectStatusChanged(false);
                }
                receive_packet.clear();
                send_packet.clear();
            } else if (BluetoothLeService.ACTION_GATT_CHARACTERISTIC_NOT_FOUND.equals(action)) {
                if (mPacketCallBack != null) {
                    mPacketCallBack.onCharacteristicNotFound();
                }
            }
        }
    };

    private static IntentFilter MyIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);

        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CHARACTERISTIC_NOT_FOUND);
        intentFilter.addAction(PacketParserService.ACTION_PACKET_HANDLE);

//        intentFilter.addAction(BluetoothLeService.ACTION_GATT_HANDLE);
        return intentFilter;
    }
}
