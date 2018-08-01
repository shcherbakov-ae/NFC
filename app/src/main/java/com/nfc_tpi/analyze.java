package com.nfc_tpi;

/**
 * Created by Ilia Tikhomirov on 16.05.2017.
 */

public class analyze {

    public static final int NO_COMMAND		    = 0x00FF;				// нет команды
    public static final int ERROR				= 0x0000;				// ошибка
    public static final int READ_COMPLETE       = 0x0001;				// успешное чтение
    public static final int READ_ID			    = 0x0002;				// отправить ID, ver, date
    public static final int SEND_PARAM			= 0x0003;				// прием параметор
    public static final int SEND_PROGRAM		= 0x0004;				// прием прошивки
    public static final int READ_PARAM			= 0x0005;				// отправить параметры
    public static final int FINISH				= 0x0006;				// конец связи


    crc16 crc = new crc16();                               // объект для класса CRC
    public int PayloadLength = 0;
    public int DeviceID = 0;
    public int CommandNumber = NO_COMMAND;
    String version;


    //--------------------------------------------------------------------------
    //                      Разбор пакета с принятыми данными
    //--------------------------------------------------------------------------
    public void analyzeReceivedData(byte[] nfc_data, int start_data_address) {

    //    String formated_data = null;

        //определяем номер команды с устройства (преобразовываем два байта в int)
        CommandNumber = ((nfc_data[start_data_address + 0] & 0xFF) << 8) + (nfc_data[start_data_address + 1] & 0xFF);
    //  formated_data = "command_in = " + CommandNumber + "\n";

        //определяем количество данных (преобразовываем два байта в int)
        PayloadLength = ((nfc_data[start_data_address + 2] & 0xFF) << 8) + (nfc_data[start_data_address + 3] & 0xFF);

    //  formated_data = formated_data + "PayloadLength = " + PayloadLength + "\n";

        //берем crc с утройства (преобразовываем два байта в int)
        int CrcIn = ((nfc_data[start_data_address+ PayloadLength + 4] & 0xFF) << 8) + (nfc_data[start_data_address+ PayloadLength + 5] & 0xFF);
    //    formated_data = formated_data + "CrcIn = " + CrcIn + "\n";


        //объявляем массив для данных
        int[] nfc_data_int = new int[PayloadLength];

        //заполняем массив данных(чтобы привести к беззнаковоому типу - & 0xff)
    //    formated_data = formated_data + "[";
        for (int i = 0; i < nfc_data.length; i++) {
            if ((i >= start_data_address + 4) && (i < (start_data_address+ PayloadLength + 4)) )
            {
                nfc_data_int[i - start_data_address - 4] = nfc_data[i] & 0xff;
          //    System.out.println("нагрузка = " + nfc_data_int[i - start_data_address - 4]);
          //    formated_data = formated_data + " " + nfc_data_int[i - start_data_address - 4];
            }
            else if (i > (start_data_address+ PayloadLength))
            {
                break;
            }
        }
    //   formated_data = formated_data + " ]" + "\n";
        //проверяем контрольную сумму
        byte crcccitt[] = crc.getCrc(nfc_data, start_data_address, start_data_address + 4 + PayloadLength); //тут считаем контрольную сумму для всей посылки, включая команду и длину
        int phone_crc = ((crcccitt[1] & 0xFF) << 8) + (crcccitt[0]& 0xFF);
    //   formated_data = formated_data + "phone_crc = " + phone_crc + "\n";
        // сравниваем CRC высчитанное и полученное
        if (CrcIn == phone_crc)
        {
    //       formated_data = formated_data + "crc confirmed" + "\n";
        }

        //отрабатываю команды
        switch (CommandNumber){
            //команда 2 - устройство отправило ID, версию и дату прошивки
            // выедергиваем эти данные в отдельные поля класса
            case READ_ID:
                String id_device = Integer.toString(nfc_data_int[0]);
                version = "ID = "+ id_device +"\n"+
                        "Версия ПО - "+(char)nfc_data_int[1]+(char)nfc_data_int[2]+(char)nfc_data_int[3]+(char)nfc_data_int[4]+(char)nfc_data_int[5]+"\n"+
                        "Дата ПО - "+(char)nfc_data_int[6]+(char)nfc_data_int[7]+(char)nfc_data_int[8]+(char)nfc_data_int[9]+
                        (char)nfc_data_int[10]+(char)nfc_data_int[11]+(char)nfc_data_int[12]+(char)nfc_data_int[13];;
                try{
                DeviceID = nfc_data_int[0];
                }
                catch (NumberFormatException e){}
                break;
            //другое
            default:
                break;
        }

        //return formated_data;

    }

    // возврат версии
    public String getVersion()
    {
        return version;
    }

}
