package com.nfc_tpi.Activity;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import android.content.pm.ActivityInfo;

import com.nfc_tpi.Devices.AbstractDevice;
import com.nfc_tpi.Devices.DeviceTest;
import com.nfc_tpi.NfcSupportFunctions;
import com.nfc_tpi.R;
import com.nfc_tpi.analyze;


/*31.10.2017*/
@TargetApi(21)
public class MainActivity extends AppCompatActivity {



    //элементы формы
    Button bSendParams, bReadParams, bFinish, bSendFirmware, bEditParams;
    TextView textView1;

    //nfc параметры
    NfcAdapter nfcAdapter;
    Tag myTag;
    NfcSupportFunctions NfcFunctions = new NfcSupportFunctions();

    // интенты и фильтр
    Intent myintent;
    PendingIntent pendingIntent;
    IntentFilter writeTagFilters[];

    // контекст
    Context context;


    int CurrentDeviceID = 0;                                // текущий ID устройства

    // флаги
    boolean isDataEmpty = true;
    boolean isNfcOn = true;
    boolean isIdReaded = false;
    boolean isIdReadedReport = false;
    boolean isParamsReaded = false;
    boolean isError = false;
    boolean isReadComplete = false;

    // адрес первого полезного элемента
    private int start_nfc_data_address = 3;

    //анализ данных
    analyze AnalyzeData = new analyze();
    public int COMMAND_IN = 1;                                  // команда которая пришла с устройства
    public  int command;                                        // буферное хранилище команды
    public int COMMAND_OUT = 0x11;                              // команда которую отправим устройству.
    private byte[] ReceivedData;                                // принятые данные


    public String IdInfo;                                       // строка для хранения данныых о устройстве
    private int[] payload;                                      // полезные данные
    private byte[] ParamsToSend;                                // массив параметров для отправки
    private List<List<Byte>> Package = new ArrayList<>();       // лист с пакетами данных

    public static final int NO_COMMAND		    = 0x00FF;		// нет команды
    public static final int ERROR				= 0x0000;		// ошибка
    public static final int READ_COMPLETE       = 0x0001;		// успешное чтение
    public static final int READ_ID			    = 0x0002;		// отправить ID, ver, date
    public static final int SEND_PARAM			= 0x0003;		// прием параметор
    public static final int SEND_PROGRAM		= 0x0004;		// прием прошивки
    public static final int READ_PARAM			= 0x0005;		// отправить параметры
    public static final int FINISH				= 0x0006;		// конец связи

    public int PackageCounter = 0;                              // счетчик пакетов
    public List<List<Byte>> FirmwarePackage;                    // лист с пакетами прошивки


    //создание объектов классов
   public  DeviceTest deviceTest = new DeviceTest();            // объект класса устройства
    AbstractDevice abstractDevice;                              // объект абстрактного класса





    //________________*отработка события создания формы*______________//
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        super.onCreate(savedInstanceState);



        // создание папки приложения на SD-карте
        createDirs();

        // поиск кнопок на экране и их отключение
        setContentView(R.layout.activity_main);
        bSendParams = (Button) findViewById(R.id.bSendParams);
        bSendParams.setEnabled(false);
        bReadParams = (Button) findViewById(R.id.bReadParams);
        bReadParams.setEnabled(false);
        bFinish = (Button) findViewById(R.id.bFinish);
      //  bFinish.setEnabled(false);
        bSendFirmware = (Button) findViewById(R.id.bSendFirmare);
        bSendFirmware.setEnabled(false);
        bEditParams = (Button) findViewById(R.id.bEditParams);
        bEditParams.setEnabled(false);
        textView1 = (TextView)findViewById(R.id.textView1);
        textView1.setMovementMethod(new ScrollingMovementMethod());

        context = this;

        //обработчик нажатий кнопок
        View.OnClickListener btnClk = new View.OnClickListener() {

            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onClick(View v) {

                switch (v.getId()) {
                    case R.id.bSendParams:
                        //Запись параметров в устройство с мобилы
                        runOnUiThread(new Runnable() {
                            public void run()
                            {
                                textView1.append("Отправка параметров..." + "\n");
                                textView1.refreshDrawableState();
                            }
                        });
                        command = SEND_PARAM;

                        break;
                    case R.id.bSendFirmare:
                        break;
                    case R.id.bReadParams:
                        //запрос параметров с устройства
                        COMMAND_OUT = READ_PARAM;
                       command = READ_PARAM;
                        break;
                    case R.id.bFinish:
                        command = FINISH;
                        break;
                    default:break;
                }
            }
        };

        // устанавливаем один обработчик для кнопок
        bSendParams.setOnClickListener(btnClk);
        bReadParams.setOnClickListener(btnClk);
        bFinish.setOnClickListener(btnClk);

        // настройка nfc передачи данных
        setNfc();
    }
    //-----------------------------------/**/-----------------------------------//

    //__________________________*настройка NFC*_________________________________//
    public void setNfc(){
        Intent intent = new Intent(NfcAdapter.ACTION_TAG_DISCOVERED);
        intent.putExtra(NfcAdapter.EXTRA_TAG, "");
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (nfcAdapter == null) {
            // Stop here, we definitely need NFC
            Toast.makeText(this, "Устройство не поддерживает NFC.", Toast.LENGTH_LONG).show();
            finish();
        }

        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writeTagFilters = new IntentFilter[]{tagDetected};
    }




    //__________________________*запись нфц метки*______________________________//
    public synchronized void write(byte[] text, Tag tag) throws IOException, FormatException {
        try {
            NdefRecord[] records = {createRecord(text)};
            NdefMessage message = new NdefMessage(records);
            // Get an instance of Ndef for the tag.
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                // Enable I/O
                ndef.connect();
                // Write the message
                ndef.writeNdefMessage(message);
                // Close the connection
                ndef.close();
            }
        }
        catch (Exception e) {
            return;
        }
    }


    //__________________________*чтение нфц метки*______________________________//
    public synchronized byte[] read(Tag tag) throws IOException, FormatException {
        String mas = new String();
        byte[] my_bf = null;

        // Get an instance of Ndef for the tag.
        Ndef ndef = Ndef.get(tag);
        if (ndef != null) {
            // get NDEF message details
            NdefMessage ndefMessage = ndef.getCachedNdefMessage();
            if (ndefMessage == null) {
                return null;
            }
            NdefRecord[] records = ndefMessage.getRecords();
            if (records == null) {
                return null;
            }

            for (int i = 0; i < records.length; i++) {
                mas += records[i].toString();
                my_bf = records[i].getPayload();
            }
            AnalyzeData.analyzeReceivedData(my_bf, start_nfc_data_address);
            if (CurrentDeviceID != AnalyzeData.DeviceID)
            {
                isIdReaded = false;
            }
            if (0 != AnalyzeData.DeviceID)
            {
                CurrentDeviceID = AnalyzeData.DeviceID;
                if (!isIdReaded)
                {
                    showToast("Устройство определено");
                    isIdReaded = true;
                }

            }

            COMMAND_IN = AnalyzeData.CommandNumber;
            //через хендлер достаем инфу из потока и выводим на экран
            Message msg = handler.obtainMessage();
            Bundle bundle = new Bundle();
            if(isIdReaded)
            {
                bundle.putString("Key", AnalyzeData.getVersion());
            }
            else
            {
                bundle.putString("Key", "");
            }
            msg.setData(bundle);
            handler.sendMessage(msg);
    }
        return my_bf;
    }
    //-----------------------------------/**/-----------------------------------//



    //__________________*через хендлер достаем инфу из потока*_________________//
    public Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            String data = bundle.getString("Key");
            ScrollView scrollView = (ScrollView) findViewById(R.id.scrollView);
           // scrollView.fullScroll(scrollView.FOCUS_DOWN);
            if (data != "")
            {
                // при первом считывании
                if (!isIdReadedReport)
                {
                    // выводим информацию об обнаруженном устройстве
                    textView1.append(AnalyzeData.getVersion() + "\n");
                    isIdReadedReport = true;
                    // активируем кнопки
                    bReadParams.setEnabled(true);
                    bReadParams.refreshDrawableState();
                    bSendFirmware.setEnabled(true);
                    bSendFirmware.refreshDrawableState();
                }
            }
            textView1.refreshDrawableState();
            scrollView.fullScroll(View.FOCUS_DOWN);
        }
    };
    //-----------------------------------/**/-----------------------------------//


    //__________________________*упаковка нфс данных*___________________________//
    private NdefRecord createRecord(byte[] text) throws UnsupportedEncodingException {
        String lang = "en";
        byte[] textBytes = text;
        byte[] langBytes = lang.getBytes("US-ASCII");
        int langLength = langBytes.length;
        int textLength = textBytes.length;
        byte[] payload = new byte[1 + langLength + textLength];

        // set status byte (see NDEF spec for actual bits)
        payload[0] = (byte) langLength;

        // copy langbytes and textbytes into payload
        System.arraycopy(langBytes, 0, payload, 1, langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

        NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);
        return recordNFC;
    }
    //-----------------------------------/**/-----------------------------------//



    //                          отработка NFC события
    protected void onNewIntent(Intent intent) {
       if (isNfcOn) {
           setIntent(intent);

           myintent = intent;
           if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction()) & (true == isNfcOn)) {
               myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

               if (myTag != null) {
                   //ежели прилетел таг и он не пустой, запускаем поток, в котором отработаем инфу (читаем и пишем)
                   thread_nfc();
                   intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                   //startActivity(intent);
               }
           }
       }
    }




   // в этом потоке мы читаем данные из нфц и если требуется, пишем данные туда же + реализация протокола
    public void thread_nfc(){
            Runnable runnable = new Runnable() {
                public void run() {
                    try {
                        try {
                            //читаем метку
                            ReceivedData = read(myTag);
                            NfcFunctions.AnalyzeIncomePack(ReceivedData, start_nfc_data_address);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (FormatException e) {
                            e.printStackTrace();
                        }


                        if (NfcFunctions.command_in == ERROR)
                        {
                            isError = true;
                        }
                        else if (NfcFunctions.command_in == READ_COMPLETE)
                        {
                            isReadComplete = true;
                        }

                        switch (command)
                        {
                            case SEND_PARAM: // отправка параметорв
                                if (isError)
                                {
                                    if (0 != PackageCounter)
                                    {
                                        PackageCounter--;
                                    }
                                    isError = false;
                                    runOnUiThread(new Runnable() {
                                        public void run()
                                        {
                                            textView1.append("Сбой \n");
                                            textView1.refreshDrawableState();
                                        }
                                    });
                                }
                                else if (isReadComplete)
                                {
                                    isReadComplete = false;
                                    PackageCounter++;
                                }
                                if (PackageCounter < Package.size())
                                {
                                    byte[] pack = new byte[Package.get(PackageCounter).size()];
                                    for (int i = 0; i < Package.get(PackageCounter).size(); i++)
                                    {
                                        pack[i] = Package.get(PackageCounter).get(i);
                                    }
                                    write(pack, myTag);
                                    runOnUiThread(new Runnable() {
                                        public void run()
                                        {
                                            textView1.append("Отправка пакета " + (PackageCounter+1) +
                                                    " из " + Package.size() +"\n");
                                            textView1.refreshDrawableState();
                                        }
                                    });
                                    command = SEND_PARAM;
                                }
                                else
                                {
                                    runOnUiThread(new Runnable() {
                                        public void run()
                                        {
                                            textView1.append("Отправка параметров завершена\n");
                                            textView1.append("Устройство будет перезагружено\n");
                                            textView1.append("Пожалуйста, подождите\n");
                                            textView1.append("Не убирайте телефон от антенны\n");
                                            textView1.refreshDrawableState();

                                        }
                                    });
                                    PackageCounter = 0;
                                    command = FINISH;
                                }
                                break;
                            case SEND_PROGRAM:      // отправка прошивки
                                if (isError)
                                {
                                    if (0 != PackageCounter)
                                    {
                                        PackageCounter--;
                                    }
                                    isError = false;
                                    runOnUiThread(new Runnable() {
                                        public void run()
                                        {
                                            textView1.append("Сбой \n");
                                            textView1.refreshDrawableState();
                                        }
                                    });
                                }
                                else if (isReadComplete)
                                {
                                    isReadComplete = false;
                                    PackageCounter++;
                                }

                                if (PackageCounter < FirmwarePackage.size())
                                {
                                    byte[] pack = new byte[FirmwarePackage.get(PackageCounter).size()];
                                    for (int i = 0; i < FirmwarePackage.get(PackageCounter).size(); i++)
                                    {
                                        pack[i] = FirmwarePackage.get(PackageCounter).get(i);
                                    }
                                    write(pack, myTag);
                                    runOnUiThread(new Runnable() {
                                        public void run()
                                        {
                                       textView1.append("Отправка пакета " + (PackageCounter + 1)  +
                                               " из " + FirmwarePackage.size() +"\n");
                                       textView1.refreshDrawableState();
                                        }
                                    });
                                    //PackageCounter++;
                                    command = SEND_PROGRAM;

                                }
                                else
                                {
                                    runOnUiThread(new Runnable() {
                                        public void run()
                                        {
                                            textView1.append("Отправка ПО завершена\n");
                                            textView1.append("Устройство будет перезагружено\n");
                                            textView1.append("Пожалуйста, подождите\n");
                                            textView1.append("Не отводите телефон от антенны\n");
                                            textView1.refreshDrawableState();

                                        }
                                    });
                                    PackageCounter = 0;
                                    command = FINISH;
                                }
                                break;
                            case READ_PARAM:     // считывание параметров
                             //   StateMachine(READ_PARAM);

                                if (NfcFunctions.command_in != READ_PARAM)
                                {
                                    Package.clear();
                                    byte[] nul = new byte[0];
                                    Package.add(NfcFunctions.CreateOnePack(nul, READ_PARAM));
                                    byte[] rp = new byte[Package.get(0).size()];
                                    for (int i = 0; i < Package.get(0).size(); i++)
                                    {
                                        rp[i] = Package.get(0).get(i);
                                    }

                                    write(rp, myTag);

                                    if (!isParamsReaded)
                                    {
                                        runOnUiThread(new Runnable() {
                                            public void run()
                                            {
                                                textView1.append("Чтение параметров...");
                                                textView1.refreshDrawableState();
                                            }
                                        });
                                    }
                                }
                                else
                                {
                                    NfcFunctions.ReadingParams(ReceivedData, start_nfc_data_address);
                                    if (!isParamsReaded)
                                    {
                                        isParamsReaded = true;
                                        showToast("Параметры считаны");
                                    }
                                    runOnUiThread(new Runnable() {
                                        public void run()
                                        {
                                            bEditParams.setEnabled(true);
                                            bEditParams.refreshDrawableState();
                                        }
                                    });

                                }

                                if (NfcFunctions.command_in == READ_PARAM) {
                                    command = NO_COMMAND;
                                }
                                else
                                {
                                    command = READ_PARAM;
                                }
                                break;
                            case FINISH:     // окончание сеанса связи
                                Package.clear();
                                byte[] nul = new byte[0];
                                Package.add(NfcFunctions.CreateOnePack(nul, FINISH));
                                byte[] pack = new byte[Package.get(0).size()];
                                for (int i = 0; i < Package.get(0).size(); i++)
                                {
                                    pack[i] = Package.get(0).get(i);
                                }
                                write(pack, myTag);

                                ClearForRestart();
                                command = NO_COMMAND;
                                break;
                            default:
                                break;
                        }

                    } catch (Exception e) {
                        return;
                    }
                }
            };
            Thread thread = new Thread(runnable);
            thread.start();

    }


//=============================================================================================
// 							игнор событий NFC если активность на паузе
//=============================================================================================


    public void onPause() {
        super.onPause();
        nfcAdapter.disableForegroundDispatch(this);
    }

//=============================================================================================
// 							НЕ игнор событий NFC если активность на паузе
//=============================================================================================
    public void onResume() {
        super.onResume();
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, writeTagFilters, null);
    }






//=============================================================================================
// 							Клик по кнопке "Изменить параметры"
//=============================================================================================

    public void OnClickEditParams(View view) {
       //  считываем айди устройства и
        // значения параметров, и по айди находим нужный класс и
        // объект класса и в каждом кейсе делаем эти строчки кейса с нужным классом после привоения
        // абстрактному классу.
        boolean IdExists = false;
         if ((0 != CurrentDeviceID)& (READ_PARAM == COMMAND_IN)) {
                switch (CurrentDeviceID)
                {
                    case 1:
                        if (true == isDataEmpty) {

                            deviceTest.ConvertValuesFromNFC(NfcFunctions.DataNfcReturn());
                        }
                        abstractDevice = deviceTest;
                        abstractDevice.DeviceID = DeviceTest.DeviceID;
                        deviceTest.ConvertValuesToString();
                        abstractDevice.ParamsName = deviceTest.ParamsName;
                        abstractDevice.ParamsValue = deviceTest.ParamsValue;
                        IdExists = true;
                        break;

                    default:
                        Toast.makeText(context, "Устройство не определено", Toast.LENGTH_LONG).show();
                        IdExists = false;
                        break;
                }
                // если в ID получен, то переходим в новую активность для редактирования
                // и передаем туда через "info" объект абстрактного класса
                if (IdExists) {
                    Intent intent = new Intent(MainActivity.this, EditParamActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("info", abstractDevice);
                    intent.putExtras(bundle);
                    nfcAdapter.disableReaderMode(this);
                    startActivityForResult(intent, 0);
                }

        }
        // если id не считан
        else if (0 == CurrentDeviceID)
        {
            Toast.makeText(context, "Считайте устройство", Toast.LENGTH_LONG).show();

        }
        // если не были счтианы параметры
        else if (READ_PARAM != COMMAND_IN)
        {
            Toast.makeText(context, "Считайте параметры", Toast.LENGTH_LONG).show();
        }
    }

//=============================================================================================
// 							Клик по кнопке "Записать прошивку"
//=============================================================================================



    public void onClickSendFirmware(View view)
    {
        // если Id считан, открываем файловый менеджер
        if (0 != CurrentDeviceID)
        {
            Intent intent = new Intent(MainActivity.this, FileManager.class);
            startActivityForResult(intent, 2);

        }
    }

//=============================================================================================
// 							Обработка возврата из экранов
//=============================================================================================

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        // возврат с экрана редактирования параметров
        if (requestCode == 0)
        {
            // если есть результат
            if (resultCode == 1)
            {
                // извелкаем данные из бандла
                Bundle b = data.getExtras();
                // если данные вообще есть
                if (b != null)
                {
                    // то присваиваем абстрактному классу (устройству)
                    abstractDevice = (AbstractDevice) b.getSerializable("EditedParams");
                    // и возвращаем в нужный объект
                    switch (CurrentDeviceID)
                    {
                        case 1:
                            deviceTest.ParamsValue = abstractDevice.ParamsValue;
                            deviceTest.ConvertValuesFromString();
                            Package = NfcFunctions.CreatePacks(deviceTest.ConvertValuesForNFC(), SEND_PARAM);
                            break;
                        default:
                            break;

                    }
                    isDataEmpty = false;        // данные есть
                    //AnalyzeData.ClearDataNfc();         // очищаем данные
                    bSendParams.setEnabled(true);
                    bSendParams.refreshDrawableState();

                }
            }
        }
        // возврат с экрана выбора прошивки
        else if (requestCode == 2) {
            if (data == null) {
                return;
            }
            String path;
            String FileNameSD;
            // получение пути и имени файла
            path = data.getStringExtra("path");
            FileNameSD = data.getStringExtra("FileNameSD");

            // чтение прошивки
            List<Byte> FirmwareBuff = new ArrayList<>();
            FirmwareBuff = readFirmwareSD(path, FileNameSD);

            byte[] Firm = new byte[FirmwareBuff.size()];
            for (int i = 0; i < FirmwareBuff.size(); i++) {
                Firm[i] = FirmwareBuff.get(i);
            }
            byte[] Firmware;
            Firmware = Firm;
            FirmwarePackage = NfcFunctions.CreatePacks(Firmware, SEND_PROGRAM);
            runOnUiThread(new Runnable() {
                public void run()
                {
                    textView1.append("Отправка ПО...\n");
                    textView1.refreshDrawableState();
                }
            });
            command = 4;
           // NfcFunctions.FirmwareToNfc(Firmware);

        }
    }
//=============================================================================================
// 							Чтение прошивки из HEX-файла
//=============================================================================================

    private ArrayList<Byte> readFirmwareSD(String path, String FileNameSD) {

        ArrayList<Byte> Firmware = new ArrayList<>();
        // проверяем доступность SD
        if (!Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            Toast.makeText(context, "SD-карта не доступна", Toast.LENGTH_LONG).show();
            return null;
        }
        // получаем путь к SD
        File sdPath;
        // добавляем свой каталог к пути
        sdPath = new File(path);
        // формируем объект File, который содержит путь к файлу
        File sdFile = new File(sdPath, FileNameSD);
        String str = "";
        try {
            // открываем поток для чтения
            BufferedReader br = new BufferedReader(new FileReader(sdFile));
            // читаем содержимое
            byte[] b;
            while ((str = br.readLine()) != null)
            {
                // временно убираем спец символ
                String str1 = str.substring(1);
                // конвертим строку в байты
                b = hexToBinary(str1);
                // возвращаем спец-символ
                Firmware.add((byte)58);
                for (int i =0; i <b.length;i++)

                    Firmware.add(b[i]);
                }
            // закрываем поток
            br.close();
            Toast.makeText(context, "Прошивка готова к загрузке", Toast.LENGTH_LONG).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Firmware;
    }

//=============================================================================================
// 							Конвертация в 16ричный вид
//=============================================================================================

    public byte[] hexToBinary(String str){
        byte[] data = new byte[str.length()/2];
        for(int i=0; i < data.length; i++){
            data[i] = (byte) ((Character.digit(str.charAt(2*i), 16) << 4)
                    + Character.digit(str.charAt(2*i+1), 16));
        }
        return data;
    }

//=============================================================================================
// 							Клик по значку выхода
//=============================================================================================

    @Override
    public void onBackPressed() {
        // запрашиваем подтверждение выхода
        openQuitDialog();
    }

//=============================================================================================
// 							Диалог выхода
//=============================================================================================


    private void openQuitDialog() {
        AlertDialog.Builder quitDialog = new AlertDialog.Builder(
                MainActivity.this);
        quitDialog.setTitle("Выйти из приложения?");

        quitDialog.setPositiveButton("Да", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                finish();
            }
        });

        quitDialog.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
            }
        });

        quitDialog.show();
    }

//=============================================================================================
// 							Создание папки на SD-карте
//=============================================================================================
    private  void createDirs()
    {
        File dir = new File (Environment.getExternalStorageDirectory().getPath()
                + File.separator + "TPI");
        dir.mkdirs();

    }


    public void showToast(final String toast)
    {
        runOnUiThread(new Runnable() {
            public void run()
            {
                Toast.makeText(context, toast, Toast.LENGTH_LONG).show();
                textView1.append("\n" + toast + "\n");
            }
        });
    }

    public void ClearForRestart()
    {
        runOnUiThread(new Runnable() {
            public void run()
            {
                textView1.setText("Установка соединения...");
                textView1.refreshDrawableState();

                // Сброс флагов
                isParamsReaded = false;
                isIdReaded = false;
                isIdReadedReport = false;
                isError = false;
                isReadComplete = false;

                // Сброс Id  команды
                CurrentDeviceID = 0;
                FirmwarePackage = new ArrayList<List<Byte>>();
                AnalyzeData.DeviceID = 0;
                command = 0;

                // Отключение кнопок
                bSendParams.setEnabled(false);
                bReadParams.setEnabled(false);
                bSendFirmware.setEnabled(false);
                bEditParams.setEnabled(false);

                // применить изменения
                bSendParams.refreshDrawableState();
                bReadParams.refreshDrawableState();
                bFinish.refreshDrawableState();
                bSendFirmware.refreshDrawableState();
                bEditParams.refreshDrawableState();
            }
        });
    }
}

