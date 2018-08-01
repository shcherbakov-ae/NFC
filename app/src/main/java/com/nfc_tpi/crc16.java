package com.nfc_tpi;

/**
 * Created by Ilia Tikhomirov on 18.04.2017.
 */

// вычисление CRC-16
public class crc16 {

    public byte[] getCrc(byte[] args, int start_index, int length) {
        int crc = 0xFFFF;          // initial value
        int polynomial = 0x1021;   // 0001 0000 0010 0001  (0, 5, 12)

        // byte[] testBytes = "123456789".getBytes("ASCII");

        byte[] bytes = args;

       // for (byte b : bytes) {
        byte b = 0;
        for(int j = start_index; j < length; j++) {
            b = bytes[j];
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b   >> (7-i) & 1) == 1);
                boolean c15 = ((crc >> 15    & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit) crc ^= polynomial;
            }
        }

        crc &= 0xffff;

        // crc это int с контрольной суммой, дальше разбиваем ее на два байта
        long ccrc = crc;
        byte[] byteStr = new byte[2];
        byteStr[0] = (byte) ((ccrc & 0x000000ff));
        byteStr[1] = (byte) ((ccrc & 0x0000ff00) >>> 8);
        return byteStr;
    }

}
