package com.nfc_tpi.Devices;

/**
 * Created by ashch on 15.08.2017.
 */


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
// класс устройства
// наследуемся от абстрактоного класса


public class DeviceTest extends AbstractDevice {
    public static       int          DeviceID;      // id устройства (жестко фиксированный для каждого устройства)
    // параметры
    public              int          param1;
    public              int          param2;
    public              int          param3;
    public              float        param4;
    public              float        param5;


    // назваиня параметров
    public final String[] ParamsName = new String[]{
            "Параметр 1",
            "Параметр 2",
            "Параметр 3",
            "Параметр 4",
            "Параметр 5"};



    public String[] ParamsValue = new String[5];   // стринговый буфер для хранения и вывода значений
                                                    // параметорв
    // конструктор
    public DeviceTest()
    {
        DeviceID = 1;
        param1 = 0;
        param2 = 0;
        param3 = 0;
        param4 = 0f;
        param5 = 0f;

    }
    // преобразование параметров в текст
    public void ConvertValuesToString()
    {
        String[] values = new String[5];
        int[]        int_params = new int[]{param1, param2, param3 };
        float[]      float_params = new float[]{param4, param5};
        for (int i = 0; i < 3; i++)
        {
          values[i] = Integer.toString(int_params[i]);
        }
        for (int i = 3; i < 5; i++)
        {
            values[i] = Float.toString(float_params[i-3]);
        }
        ParamsValue = Arrays.copyOf(values,values.length);
    }
    // преобразование из текста в числовые значения
    public void ConvertValuesFromString()
    {
        try{
        param1 = Integer.parseInt(ParamsValue[0]);
        }
        catch (NumberFormatException e){}

        try {
        param2 = Integer.parseInt(ParamsValue[1]);
        }catch (NumberFormatException e){}

        try {
            param3 = Integer.parseInt(ParamsValue[2]);
        }catch (NumberFormatException e){}

        try {
        param4 = Float.parseFloat(ParamsValue[3]);
        }catch (Exception e){}

        try {
        param5 = Float.parseFloat(ParamsValue[4]);
        }catch (Exception e){}
    }

    // конвертация значений параметров из типа byte
    public void ConvertValuesFromNFC( byte[] DataIncome)
    {
        byte[] ParamFloat = new byte[4];

        param1 = DataIncome[1];
        param2 = DataIncome[2];
        param3 = DataIncome[3];

        for (int i=4; i<8; i++)
        {
           ParamFloat[i-4] = DataIncome[i];
        }
        param4 = ByteBuffer.wrap(ParamFloat).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        for (int i=8; i<12; i++)
        {
            ParamFloat[i-8] = DataIncome[i];
        }
        param5 =  ByteBuffer.wrap(ParamFloat).order(ByteOrder.LITTLE_ENDIAN).getFloat();

    }
    // конвертация в byte
    public byte[] ConvertValuesForNFC()
    {
        byte[] ParamsPack = new byte[12];
        byte[] floatBuf;

       // ParamsPack[0] = new Integer(DeviceID).toString().getBytes();
        ParamsPack[0] = (byte) DeviceID;
        ParamsPack[1] = (byte) param1;
        ParamsPack[2] = (byte) param2;
        ParamsPack[3] = (byte) param3;

/*
        ParamsPack[0] = 1;
        ParamsPack[1] = 3;
        ParamsPack[2] = 2;
        ParamsPack[3] = 1;
*/
        floatBuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(param4).array();
        System.arraycopy(floatBuf,0,ParamsPack,4, 4);

        floatBuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(param5).array();
        System.arraycopy(floatBuf,0,ParamsPack,8, 4);

        return ParamsPack;
    }
}
