package com.nfc_tpi;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ashch on 09.04.2018.
 */

public class NfcSupportFunctions {

    //public byte[] ParamsToSend;                                     // параметры для отправки
    //private byte[] FullPack;
    //private byte[] ReceivedData;
    //private byte[] Firmware;
    //private List<List<Byte>> Package = new ArrayList<>();
    private ArrayList<Byte> DataNFC = new ArrayList<Byte>();           // принятые параметры
    //private int command;
    private boolean CrcConfirmed = false;                           // флаг совпадения контрольных сумм


    //Tag myTag;
   // public String IdInfo;

    public static final int NO_COMMAND		    = 0x00FF;				// нет команды
    public static final int ERROR				= 0x0000;				// ошибка
    public static final int READ_COMPLETE       = 0x0001;				// успешное чтение
    public static final int READ_ID			    = 0x0002;				// отправить ID, ver, date
    public static final int SEND_PARAM			= 0x0003;				// прием параметор
    public static final int SEND_PROGRAM		= 0x0004;				// прием прошивки
    public static final int READ_PARAM			= 0x0005;				// отправить параметры
    public static final int FINISH				= 0x0006;				// конец связи

    crc16 crc = new crc16();                               // объект класса с CRC
    public int payload_length = 0;                                      // длина полезной нагрузки
    public int DeviceID = 0;                                            // Id устройства
    public int command_in = NO_COMMAND;                                 // принятая команда
  // private int start_nfc_data_address = 3;
    private int[] payload;

    // конструктор
    public NfcSupportFunctions()
    {
       // myTag = tag;
    }



//=============================================================================================
// 							Создание пакетов с данными
//=============================================================================================

    public List<List<Byte>> CreatePacks(byte[] data, int command)
    {
        List<List<Byte>> Packs = new ArrayList<>();
        if (data.length < 3000)
        {
            Packs.add(CreateOnePack(data, command));
        }
        else
        {
            int PacksQuantity, residue;
            PacksQuantity = data.length / 3000;
            residue = data.length - PacksQuantity * 3000;
            int j = 0;
            for (int i = 0; i < PacksQuantity; i++)
            {
               byte[] pack;
                pack = copyPartArray(data,j,j+3000);
                Packs.add(CreateOnePack(pack, command));
                j = j + 3000;
            }
            byte[] pack;
            pack = copyPartArray(data, j, j + residue);
            Packs.add(CreateOnePack(pack, command));
        }
        return Packs;
    }


//=============================================================================================
// 							Копирование части массива
//=============================================================================================


    private byte[] copyPartArray(byte[] a, int start, int stop) {
        if (a == null)
            return null;
        if (start > a.length)
            return null;
        byte[] r = new byte[stop - start];
        System.arraycopy(a, start, r, 0, stop - start);
        return r;
    }

//=============================================================================================
// 							Чтение параметров
//=============================================================================================

    public void ReadingParams(byte[] nfc_data, int start_data_address)
    {
        if (CrcConfirmed)
        {
            for (int i = 0; i < nfc_data.length; i++)
            {
                if ((i >= start_data_address + 4) && (i < (start_data_address + payload_length + 4)))
                {
                    DataNFC.add(nfc_data[i]);
                } else if (i > (start_data_address + payload_length)) {
                    break;
                }
            }

        }
    }
/*
    public String ReadID (int[] nfc_data)
    {
        String id_info = null;
        String id_device = Integer.toString(nfc_data[0]);
        id_info = "ID ="+ id_device +"\n"+
                  "Версия ПО ="+(char)nfc_data[1]+(char)nfc_data[2]+(char)nfc_data[3]+(char)nfc_data[4]+(char)nfc_data[5]+"\n"+
                  "Дата ПО ="+(char)nfc_data[6]+(char)nfc_data[7]+(char)nfc_data[8]+(char)nfc_data[9]+
                  (char)nfc_data[10]+(char)nfc_data[11]+(char)nfc_data[12]+(char)nfc_data[13];
        try{
            DeviceID = nfc_data[0];
        }
        catch (NumberFormatException e){}
        return id_info;
    }*/
/*
    public void toNfcProtocol (byte[] DataIn)
    {
        ParamsToSend = DataIn;
    }

    public void FirmwareToNfc(byte[] DataIn)
    {
        Firmware = DataIn;
    }
*/
//=============================================================================================
// 							Создание части пакета
//=============================================================================================

    public List<Byte> CreateOnePack(byte[] data, int command_in)
    {
        List<Byte> FullPack = new ArrayList<>();
        byte[] Pack = new byte[data.length + 6];

        //обозначаем команду
        //разбиваем  ее на два байта
        byte[] byteStr1= new byte[2];
        byteStr1[0] = (byte) ((command_in & 0x000000ff));
        byteStr1[1] = (byte) ((command_in & 0x0000ff00) >>> 8);
        Pack[0] = byteStr1[1];
        Pack[1] = byteStr1[0];

        //указываем длину посылки
        //разбиваем  ее на два байта
        byte[] byteStr2 = new byte[2];
        byteStr2[0] = (byte) ((data.length & 0x000000ff));
        byteStr2[1] = (byte) ((data.length & 0x0000ff00) >>> 8);
        Pack[2] = byteStr2[1];
        Pack[3] = byteStr2[0];
        for (int i = 0; i < data.length; i++)
        {
            Pack[i + 4] = data[i];
        }
        //добавляем контрольную сумму
        byte crcccitt[] = crc.getCrc(Pack,0, Pack.length-2); //тут считаем контрольную сумму для всей посылки, включая команду и длину
        Pack[Pack.length-2] = crcccitt[1];
        Pack[Pack.length-1] = crcccitt[0];
        for (int i = 0; i < Pack.length; i++)
        {
            FullPack.add(Pack[i]);
        }
        return FullPack;
    }


    //-----------------------------------------------------------------------------
    //                          Анализ принятого пакета
    //-----------------------------------------------------------------------------
    public void AnalyzeIncomePack(byte[] nfc_data, int start_data_address) {

     //String formated_data = null;

        //определяем номер команды с устройства (преобразовываем два байта в int)
        command_in = ((nfc_data[start_data_address + 0] & 0xFF) << 8) + (nfc_data[start_data_address + 1] & 0xFF);
     //formated_data = "command_in = " + command_in + "\n";

        //определяем количество данных (преобразовываем два байта в int)
        payload_length = ((nfc_data[start_data_address + 2] & 0xFF) << 8) + (nfc_data[start_data_address + 3] & 0xFF);

     //formated_data = formated_data + "PayloadLength = " + PayloadLength + "\n";

        //берем crc с утройства (преобразовываем два байта в int)
        int crc_from_device = ((nfc_data[start_data_address+payload_length + 4] & 0xFF) << 8) + (nfc_data[start_data_address+payload_length + 5] & 0xFF);
     //formated_data = formated_data + "crc_from_device = " + crc_from_device + "\n";


        //объявляем массив для данных
        int[] nfc_data_int = new int[payload_length];

        //заполняем массив данных(чтобы привести к без знаковоому типу мутим & 0xff)
     //formated_data = formated_data + "[";
        for (int i = 0; i < nfc_data.length; i++) {
            if ((i >= start_data_address + 4) && (i < (start_data_address+payload_length + 4)) )
            {
                nfc_data_int[i - start_data_address - 4] = nfc_data[i] & 0xff;
     //      formated_data = formated_data + " " + nfc_data_int[i - start_data_address - 4];
            }
            else if (i > (start_data_address+payload_length))
            {
                break;
            }
        }
        payload = nfc_data_int;
     //formated_data = formated_data + " ]" + "\n";
        //проверяем контрольную сумму
        byte crcccitt[] = crc.getCrc(nfc_data, start_data_address, start_data_address + 4 + payload_length); //тут считаем контрольную сумму для всей посылки, включая команду и длину
        int phone_crc = ((crcccitt[1] & 0xFF) << 8) + (crcccitt[0]& 0xFF);
     //formated_data = formated_data + "phone_crc = " + phone_crc + "\n";
        // проверка на совпадение контрольных сумм
        if (crc_from_device == phone_crc)
        {
     //  formated_data = formated_data + "crc confirmed" + "\n";
            CrcConfirmed = true;
        }
        else
        {
     //    formated_data = formated_data + "crc fail" + "\n";
            CrcConfirmed = false;
     //    return formated_data;
        }
     // return formated_data;
    }

    //-----------------------------------------------------------------------------
    //                          Возврат принятых параметров
    //-----------------------------------------------------------------------------
    public byte[] DataNfcReturn()
    {
        byte[] ParamsNfcReturn = new byte[payload_length];
        for (int i=0; i<payload_length; i++)
        {
            ParamsNfcReturn[i] = DataNFC.get(i);
        }
        return ParamsNfcReturn;
    }


}
