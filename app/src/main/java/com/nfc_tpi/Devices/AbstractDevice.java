package com.nfc_tpi.Devices;

/**
 * Created by ashch on 21.08.2017.
 */

//-------------------------------------------------
//  Абстрактный класс. Все классы устройств должны от него наследоваться!!111
//-------------------------------------------------

import java.io.Serializable;


public abstract class AbstractDevice implements Serializable {
    public String[] ParamsName;
    public String[] ParamsValue;
    public int DeviceID;

    public abstract void ConvertValuesToString();
    public abstract void ConvertValuesFromString();
    public abstract void ConvertValuesFromNFC(byte[] DataIncome);
    public abstract byte[] ConvertValuesForNFC();
}

