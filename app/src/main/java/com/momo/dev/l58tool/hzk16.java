package com.momo.dev.l58tool;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;

/**
 * Created by Administrator on 2015/11/10.
 */
public class hzk16 {

    public static int[] getQw(String chinese) {
        byte[] bs;
        try {
            bs = chinese.getBytes("GB2312");
            int ret[] = new int[bs.length];
            for (int i = 0; i < bs.length; i++) {
                int a = bs[i];
                if (a < 0) {
                    a = 256 + a;
                }
                int bb = (a - 0x80 - 0x20);
                ret[i] = bb;
            }
            return ret;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] getMatrix(InputStream hzk, String hz) {
        if(hzk == null){
            return null;
        }
        if (hz == null) {
            return null;
        }
        int[] res = getQw(hz);
        int qh = res[0];
        int wh = res[1];
        long location = (94 * (qh - 1) + (wh - 1)) * 32L;

        // 点阵缓冲 16x16 = 32x8
        byte[] bs = new byte[32];
        byte[] trans = new byte[32];
        try {
            hzk.reset();
//            int i=hzk.available();
//            Log.i(BluetoothLeService.TAG, "hzk16.size:" + String.valueOf(i));
            hzk.skip(location);
            hzk.read(bs);
        } catch (IOException e) {
            e.printStackTrace();
        }


        int i, j, k;
        for (i = 0; i < 16; i++) {          /* 点阵行数索引 */
            for (j = 0; j < 2; j++) {       /* 点阵左子行右子行索引 */
                for (k = 0; k < 8; k++) {   /* 点阵每个点 */

                    // 判断是0、还是1
                    if (((bs[i * 2 + j] >> (7 - k)) & 0x1) == 1) {
                        trans[(8 * j + k) * 2 + i / 8] |= (1 << (7 - (i % 8)));
                    } else {
                        trans[(8 * j + k) * 2 + i / 8] &= ~(1 << (7 - (i % 8)));
                    }
                }
            }
        }
        return trans;
    }
}
