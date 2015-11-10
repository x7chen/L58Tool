package com.momo.dev.l58tool;

import android.os.Environment;

import java.io.File;
import java.io.IOException;
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

    public static byte[] read(String hzk, String hz) throws IOException {
        int[] res = getQw(hz);
        int qh = res[0];
        int wh = res[1];
        long location = (94 * (qh - 1) + (wh - 1)) * 32L;

        // 点阵缓冲 16x16 = 32x8
        byte[] bs = new byte[32];
        byte[] trans = new byte[32];
        RandomAccessFile r = new RandomAccessFile(new File(hzk), "r");// 只读方式打开文件
        r.seek(location);                   // 指定下一次的开始位置
        r.read(bs);
        r.close();

        int i, j, k;
        for (i = 0; i < 16; i++) {          /* 点阵行数索引 */
            for (j = 0; j < 2; j++) {       /* 点阵左子行右子行索引 */
                for (k = 0; k < 8; k++) {   /* 点阵每个点 */

                    // 判断是0、还是1
                    if (((bs[i * 2 + j] >> (7 - k)) & 0x1) == 1) {
                        trans[(8 * j + k) * 2 + i / 8] |= (1 << (7-(i % 8)));
                    } else {
                        trans[(8 * j + k) * 2 + i / 8] &= ~(1 << (7-(i % 8)));
                    }
                }
            }
        }
        return trans;
    }
    public static byte[] getMatrix(String hz) throws IOException {
        String logFileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/L58Tool/hzk16";
        File file = new File(logFileName);
        if (!file.exists()) {
            return null;
        }
        return read(logFileName,hz);

    }
}
