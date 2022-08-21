package com.dacosys.warehouseCounter.scanners.vh75;

import java.util.Locale;

public class Utility {
    public static byte[] convert2HexArray(String hexString) {
        int len = hexString.length() / 2;
        char[] chars = hexString.toCharArray();
        String[] hexes = new String[len];
        byte[] bytes = new byte[len];
        for (int i = 0, j = 0; j < len; i = i + 2, j++) {
            hexes[j] = "" + chars[i] + chars[i + 1];
            bytes[j] = (byte) Integer.parseInt(hexes[j], 16);
        }
        return bytes;
    }

    public static String bytes2String(byte[] b) {
        return new String(b).trim();
    }

    //byte Hexadecimal
    public static String bytes2HexString(byte[] b, int size) {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < size; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = "0" + hex;
            }
            ret.append(hex.toUpperCase(Locale.getDefault()));
        }
        return ret.toString();
    }

    public static String bytes2HexString(byte[] b) {
        StringBuilder ret = new StringBuilder();
        for (byte aB : b) {
            String hex = Integer.toHexString(aB & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            ret.append(hex.toUpperCase(Locale.getDefault()));
        }
        return ret.toString();
    }

    public static String bytes2HexStringWithSperator(byte[] b) {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            ret.append(hex.toUpperCase(Locale.getDefault()));
            if ((i + 1) % 4 == 0 && (i + 1) != b.length)
                ret.append("-");
        }
        return ret.toString();
    }

    public static byte BYTE(int i) {
        return (byte) i;
    }

    /**
     * check whether the str is a hex str
     *
     * @param str  str
     * @param bits bits
     * @return true or false
     */
    public static boolean isHexString(String str, int bits) {
        String patten = "[abcdefABCDEF0123456789]*" + bits + "}";
        return str.matches(patten);
    }

    public static boolean isHexString(String str) {
        String patten = "[abcdefABCDEF0123456789]+";
        return str.matches(patten);
    }

    public static boolean isNumber(String str) {
        String patten = "[-]?[0123456789]*";
        return str.matches(patten);
    }
}
