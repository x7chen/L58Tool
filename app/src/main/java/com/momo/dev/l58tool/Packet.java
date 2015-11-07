package com.momo.dev.l58tool;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Administrator on 2015/10/15.
 */
public class Packet {

    static String TAG = "com.momo.dev.l58tool.packet";
    List<Byte> mPacket = new ArrayList<Byte>();
    static short mSequenceId = 0;
    CRC16 crc16 = new CRC16();
    int mPacketError;

    public Packet(byte[] value) {

    }

    public Packet() {

    }

    private L1Header genL1Header() {
        int packetvalue_length = this.getPacketValue().toList().size();
        Byte[] packetvalue_bytes = new Byte[packetvalue_length];
        L1Header aL1Header = new L1Header();
        this.getPacketValue().toList().toArray(packetvalue_bytes);

        int crc = crc16.getCrc(packetvalue_bytes);
        aL1Header.setCRC16((short) crc);
        aL1Header.setLength((short) packetvalue_length);
        aL1Header.setSequenceId(mSequenceId++);
        aL1Header.setAckError(false, false);
        setL1Header(aL1Header);
        return aL1Header;
    }

    public byte[] toByteArray() {

        int packet_length = mPacket.size();
        Byte[] packet_bytes = new Byte[packet_length];
        mPacket.toArray(packet_bytes);
        byte[] aPacket = byteArrayUnBox(packet_bytes);
        return aPacket;
    }

    public void setPacket(byte[] packet) {
        mPacket.clear();
        mPacket.addAll(Arrays.asList(byteArrayBox(packet)));
    }

    public void setL1Header(L1Header l1header) {
        List<Byte> nPacket = new ArrayList<Byte>();
        nPacket.addAll(l1header.toList());
        //subList(起始（包含），终止（不包含））
        if(mPacket.size() > 8) {
            nPacket.addAll(mPacket.subList(8, mPacket.size()));
        }
        mPacket = nPacket;
    }

    public L1Header getL1Header() {
        if (mPacket.size() < 8) {
            return null;
        }
        List<Byte> aL1Header = mPacket.subList(0, 8);
        L1Header nL1Header = new L1Header(aL1Header);
        return nL1Header;
    }

    public void setPacketValue(PacketValue packetValue, boolean genL1) {
        if (mPacket.size() == 0) {
            mPacket.addAll(new L1Header().toList());
        }

        List<Byte> nPacket = new ArrayList<Byte>();
        nPacket.addAll(mPacket.subList(0, 8));
        if (packetValue == null) {

        } else {
            nPacket.addAll(packetValue.toList());
        }
        mPacket = nPacket;
        if (genL1) {
            genL1Header();
        }
    }

    public PacketValue getPacketValue() {
        if (mPacket.size() < 13) {
            return null;
        }
        List<Byte> aPacketValue = mPacket.subList(8, mPacket.size());
        PacketValue mPacketValue = new PacketValue(aPacketValue);
        return mPacketValue;
    }

    public void append(byte[] data) {
        Byte[] aData = byteArrayBox(data);
        mPacket.addAll(Arrays.asList(aData));

        //checkPacket();
    }

    public int checkPacket() {

        if (mPacket.size() > 512) {
            mPacket.clear();
        }
        L1Header aL1Header = getL1Header();
        PacketValue aPacketValue = getPacketValue();
        mPacketError = 0;

        if ((aL1Header == null)) {
            mPacketError = 0x03;
            return mPacketError;
        }

        if (aL1Header.toList().get(0).byteValue() != (byte)0xAB) {
            mPacketError = 0x05;
            return mPacketError;
        }
        if (aL1Header.getAckError() != 0x00) {
            mPacketError = aL1Header.getAckError();
            return mPacketError;
        }
//        Log.i(BluetoothLeService.TAG,"Length:"+Short.toString(aL1Header.getLength()));

        if ((aPacketValue == null)) {
            mPacketError = 0x07;
            return mPacketError;
        }
//        Log.i(BluetoothLeService.TAG,"Size:"+Integer.toString(aPacketValue.toList().size()));
        if (aL1Header.getLength() > aPacketValue.toList().size()) {
            mPacketError = 0x09;
            return mPacketError;
        }
        Byte[] aPacketValueBytes = new Byte[aPacketValue.toList().size()];
        aPacketValue.toList().toArray(aPacketValueBytes);
        short crc = crc16.getCrc(aPacketValueBytes);
//        Log.i(BluetoothLeService.TAG,"CRC:"+Integer.toHexString(crc));
//        Log.i(BluetoothLeService.TAG,"L1Header.CRC:"+Integer.toHexString(aL1Header.getCRC16()));
        if (aL1Header.getCRC16() != crc) {

            mPacketError = 0x0B;
        } else {
            mPacketError = 0;
        }
        return mPacketError;
    }

    public void clear() {
        mPacket.clear();
    }

    public boolean isChecked() {
        return (mPacketError == 0);
    }

    public void print() {
        StringBuilder strBuilder = new StringBuilder();
        for (byte bb : toByteArray()) {
            strBuilder.append(String.format("%02X ", bb));
        }
        strBuilder.append("\n");
        Log.i(BluetoothLeService.TAG, strBuilder.toString());
    }
    @Override
    public  String toString(){
        StringBuilder strBuilder = new StringBuilder();
        for (byte bb : toByteArray()) {
            strBuilder.append(String.format("%02X ", bb));
        }
        strBuilder.append("\n");
        return strBuilder.toString();
    }
    public static void Print(List<Byte> data){
        StringBuilder strBuilder = new StringBuilder();
        for (byte bb : data) {
            strBuilder.append(String.format("%02X ", bb));
        }
        strBuilder.append("\n");
        Log.i(BluetoothLeService.TAG, "Print:"+strBuilder.toString());
    }


    //L2Header就不具体分了，与数据一同处理
    static public class L1Header {
        List<Byte> L1Header;
        private Byte[] aL1Header = {(byte) 0xAB, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};

        public L1Header() {
            L1Header = new ArrayList<Byte>(8);
            //L1Header.clear();
        }

        public L1Header(List<Byte> aData) {
            aData.toArray(aL1Header);
        }

        public void setLength(short length) {
            byte[] len = shortToByte(length);
            aL1Header[2] = len[0];
            aL1Header[3] = len[1];
        }

        public short getLength() {
            int value;
            value = aL1Header[2] & 0x000000ff;
            value = value << 8;
            value |= aL1Header[3] & 0x000000ff;
            return (short) value;
        }

        public void setCRC16(short crc16) {
            byte[] crc = shortToByte(crc16);
            aL1Header[4] = crc[0];
            aL1Header[5] = crc[1];
        }

        public short getCRC16() {
            int value;
            value = aL1Header[4] & 0x000000ff;
            value = value << 8;
            value |= aL1Header[5] & 0x000000ff;
            return (short) value;
        }

        public void setSequenceId(short sequenceid) {
            byte[] sid = shortToByte(sequenceid);
            aL1Header[6] = sid[0];
            aL1Header[7] = sid[1];
        }

        public short getSequenceId() {
            int value;
            value = aL1Header[6] & 0x000000ff;
            value = value << 8;
            value |= aL1Header[7] & 0x000000ff;
            return (short) value;
        }

        public void setACK(boolean ack) {
            byte sta = aL1Header[1];
            if (ack == true) {
                sta |= (byte) 0x10;
            } else {
                sta &= ~(byte) 0x10;
            }
            aL1Header[1] = sta;
        }

        public boolean getACK() {
            return ((aL1Header[1] & 0x10) == 0x10);
        }

        public void setError(boolean err) {
            byte sta = aL1Header[1];
            if (err == true) {
                sta |= (byte) 0x20;
            } else {
                sta &= ~(byte) 0x20;
            }
            aL1Header[1] = sta;
        }

        public boolean getError() {
            return ((aL1Header[1] & 0x20) == 0x20);
        }

        public void setAckError(boolean ack, boolean err) {
            setACK(ack);
            setError(err);
        }

        public byte getAckError() {
            return aL1Header[1];
        }

        public List<Byte> toList() {
            L1Header = Arrays.asList(aL1Header);
            return L1Header;
        }
    }

    static public class PacketValue implements Cloneable {
        List<Byte> aPacketValue;

        public PacketValue() {
            aPacketValue = new ArrayList<Byte>();
            aPacketValue.add((byte) 0);
            aPacketValue.add((byte) 0);
            aPacketValue.add((byte) 0);
            aPacketValue.add((byte) 0);
            aPacketValue.add((byte) 0);
        }

        public PacketValue(List<Byte> aData) {
            aPacketValue = aData;
        }

        public void setCommandId(byte commandid) {
            aPacketValue.set(0, commandid);
            aPacketValue.set(1, (byte) 0x00);
        }

        byte getCommandId() {
            return aPacketValue.get(0);
        }

        public void setKey(byte key) {
            aPacketValue.set(2, key);
        }

        byte getKey() {
            return aPacketValue.get(2);
        }

        public void setValueLength(short lenth) {
            byte[] len = shortToByte(lenth);
            aPacketValue.set(3, len[0]);
            aPacketValue.set(4, len[1]);
        }

        short getValueLength() {
            int value;
            value = aPacketValue.get(3) & 0x000000ff;
            value = value << 8;
            value |= aPacketValue.get(4) & 0x000000ff;
            return (short) value;
        }

        public void setValue(byte[] value) {

            if (aPacketValue.size() == 0) {
                aPacketValue.add((byte) 0);
                aPacketValue.add((byte) 0);
                aPacketValue.add((byte) 0);
                aPacketValue.add((byte) 0);
                aPacketValue.add((byte) 0);
            }

            List<Byte> nPacketValue = new ArrayList<Byte>();
            nPacketValue.addAll(aPacketValue.subList(0, 5));
            if (value == null) {

            } else {
                for (byte b : value) {
                    nPacketValue.add(b);
                }
            }
            aPacketValue = nPacketValue;
            setValueLength((short) (aPacketValue.size() - 5));
        }

        public byte[] getValue() {
            if (aPacketValue.size() < 6) {
                return null;
            }
            return Arrays.copyOfRange(byteArrayUnBox(this.toArray()), 5, aPacketValue.size());
        }

        public void appendValue(byte[] value) {
            if (aPacketValue.size() == 0) {
                aPacketValue.add((byte) 0);
                aPacketValue.add((byte) 0);
                aPacketValue.add((byte) 0);
                aPacketValue.add((byte) 0);
                aPacketValue.add((byte) 0);
            }
            for (byte b : value) {
                aPacketValue.add(b);
            }
            setValueLength((short) (aPacketValue.size() - 5));
        }

        public void setPacketValue(byte[] value) {
            aPacketValue.clear();
            for (byte b : value) {
                aPacketValue.add(b);
            }

        }

        public List<Byte> toList() {
            return aPacketValue;
        }

        public Byte[] toArray() {
            Byte[] value = new Byte[aPacketValue.size()];
            aPacketValue.toArray(value);
            return value;
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }

    public class CRC16 {
        private short[] crcTable = new short[256];
        private int gPloy = 0x1021; // 生成多项式

        public CRC16() {
            computeCrcTable();
        }

        private short getCrcOfByte(int aByte) {
            int value = aByte << 8;

            for (int count = 7; count >= 0; count--) {
                if ((value & 0x8000) != 0) { // 高第16位为1，可以按位异或
                    value = (value << 1) ^ gPloy;
                } else {
                    value = value << 1; // 首位为0，左移
                }

            }
            value = value & 0xFFFF; // 取低16位的值
            return (short) value;
        }

        /*
         * 生成0 - 255对应的CRC16校验码
         */
        private void computeCrcTable() {
            for (int i = 0; i < 256; i++) {
                crcTable[i] = getCrcOfByte(i);
            }
        }

        public short getCrc(Byte[] data) {
            int crc = 0;
            int length = data.length;
            for (int i = 0; i < length; i++) {
                crc = ((crc & 0xFF) << 8) ^ crcTable[(((crc & 0xFF00) >> 8) ^ data[i]) & 0xFF];
            }
            crc = crc & 0xFFFF;
            return (short) crc;
        }
    }

    static public byte[] byteToByte(byte value) {
        byte[] abyte = new byte[1];
        abyte[0] = (byte) ((0x00ff & value) >> 0);
        return abyte;
    }

    static public byte[] shortToByte(short value) {
        byte[] abyte = new byte[2];
        abyte[0] = (byte) ((0xff00 & value) >> 8);
        abyte[1] = (byte) ((0x00ff & value) >> 0);
        return abyte;
    }

    static public byte[] intToByte(int value) {
        byte[] abyte = new byte[4];
        abyte[0] = (byte) ((0xff000000 & value) >> 24);
        abyte[1] = (byte) ((0x00ff0000 & value) >> 16);
        abyte[2] = (byte) ((0x0000ff00 & value) >> 8);
        abyte[3] = (byte) ((0x000000ff & value) >> 0);
        return abyte;
    }

    static public byte[] longToByte(long value) {
        byte[] abyte = new byte[8];
        abyte[0] = (byte) ((0xff00000000000000L & value) >> 56);
        abyte[1] = (byte) ((0x00ff000000000000L & value) >> 48);
        abyte[2] = (byte) ((0x0000ff0000000000L & value) >> 40);
        abyte[3] = (byte) ((0x000000ff00000000L & value) >> 32);
        abyte[4] = (byte) ((0x00000000ff000000L & value) >> 24);
        abyte[5] = (byte) ((0x0000000000ff0000L & value) >> 16);
        abyte[6] = (byte) ((0x000000000000ff00L & value) >> 8);
        abyte[7] = (byte) ((0x00000000000000ffL & value) >> 0);
        return abyte;
    }

    static public byte[] byteArrayUnBox(Byte[] ByteArray) {
        byte[] byteArray = new byte[ByteArray.length];
        int i = 0;
        for (Byte B : ByteArray) {
            byteArray[i++] = B.byteValue();
        }
        return byteArray;
    }

    static public Byte[] byteArrayBox(byte[] byteArray) {
        Byte[] ByteArray = new Byte[byteArray.length];
        int i = 0;
        for (byte b : byteArray) {
            ByteArray[i++] = b;
        }
        return ByteArray;
    }

}
