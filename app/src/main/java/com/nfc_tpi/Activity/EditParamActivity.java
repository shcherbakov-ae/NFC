package com.nfc_tpi.Activity;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import java.util.ArrayList;
import android.widget.Button;

import com.nfc_tpi.Adapters.ListAdapter;
import com.nfc_tpi.Devices.AbstractDevice;
import com.nfc_tpi.R;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import android.content.pm.ActivityInfo;
import android.os.Environment;
import android.widget.Toast;
import android.support.v7.widget.SearchView;
import static java.lang.Boolean.FALSE;
//import android.support.v7.widget.Toolbar;

@TargetApi(21)
public class EditParamActivity extends MainActivity implements View.OnClickListener, SearchView.OnQueryTextListener {

    int ID = 0;                                         // Id принятый
    ArrayList<String> params = new ArrayList<>();       // лист для хранения названий параметров
    ArrayList<String> values = new ArrayList<>();       // лист для хранения значения параметров
    String path;                                        // путь к файлу
    String FileNameSD;                                  // имя файла
    boolean fRewrite = false;                           // флаг перезаписи
    public String key = "16F@m}5x0%";         //шифр
    public Button bApply;
    public AbstractDevice abstractDevice;               // объект абстрактного класса
    ListAdapter gAdapter;                               // адаптер для вывода списка на экран

//=============================================================================================
// 							Создание активности
//=============================================================================================

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_param);
        bApply = (Button)findViewById(R.id.button);
        bApply.setOnClickListener(this);
        Bundle b = getIntent().getExtras();
        final ArrayList<String> list = new ArrayList<>();

        // если есть входные данные с предыдущей активности
        if (null != b)
        {
            // извлекаем данные из инфо
            abstractDevice = (AbstractDevice) getIntent().getExtras().getSerializable("info");
            // конвертим в стринг
            abstractDevice.ConvertValuesToString();
            // выдергиваем АйДи
            ID = abstractDevice.DeviceID;
            // заполняем листы
            for (int i = 0; i < abstractDevice.ParamsName.length; i++) {
                params.add(abstractDevice.ParamsName[i]);
            }
            for (int i = 0; i < abstractDevice.ParamsValue.length; i++) {
                values.add(abstractDevice.ParamsValue[i]);
            }

            // используем адептер для вывода на экран
            ListView listView = (ListView) findViewById(R.id.listView);
            ListAdapter adapter = new ListAdapter(this, values, params);
            listView.setAdapter(adapter);
            gAdapter = adapter;
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);

        }
    }

//=============================================================================================
// 							отображение меню
//=============================================================================================

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {

        // поиск элементов экрана (поисковой строки)
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_edit_params,menu);
        final MenuItem menuSearch = menu.findItem(R.id.action_search);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false);

        // кнопка Х в поисковой строке и ее обработчик
        ImageView closeButton = (ImageView) searchView.findViewById(R.id.search_close_btn);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                EditText et = (EditText) findViewById(R.id.search_src_text);
                //Clear the text from EditText view
                et.setText("");

                //Clear query
                searchView.setQuery("", false);
                //Collapse the action view
                searchView.onActionViewCollapsed();
                //Collapse the search widget
                MenuItemCompat.collapseActionView(menuSearch);
            }
        });

        // отслеживание ввода текста в поисковую строку и вызов фильтра при изменении
        try {
            SearchView.OnQueryTextListener textChangeListener = new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextChange(String newText) {
                    if (newText != null)
                    {
                        gAdapter.getFilter().filter(newText);
                    }
                    return false;
                }
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return false;
                }
            };
            searchView.setOnQueryTextListener(textChangeListener);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return super.onCreateOptionsMenu(menu);
    }

//=============================================================================================
// 							заглушка
//=============================================================================================
    @Override
    public boolean onQueryTextSubmit(String query) {
        return true;
    }
//=============================================================================================
// 							заглушка
//=============================================================================================
    @Override
    public boolean onQueryTextChange(String newText) {
        return true;
    }

//=============================================================================================
//                      клик по кнопке загрузить
//=============================================================================================
    public void onLoadFileClick(MenuItem item)
    {
        // открыть файловый менеджер
        Intent intent = new Intent(EditParamActivity.this, FileManager.class);
        startActivityForResult(intent, 1);
    }
//=============================================================================================
//                      возврат пути и имени файла из файлового менеджера
//=============================================================================================
     @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null) {return;}
        // получение пути и имени файла
        path = data.getStringExtra("path");
        FileNameSD = data.getStringExtra("FileNameSD");
        // чтение значений
        readFileSD();
        // обновление списка
        ListView listView = (ListView) findViewById(R.id.listView);
        ListAdapter adapter = new ListAdapter(this, values, params);
        listView.setAdapter(adapter);
    }

//=============================================================================================
//                     клик по кнопке сохранить
//=============================================================================================
    public void onSaveFileClick(MenuItem item)
    {
        final String FileName = new String();

        // вызов окна диалога
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        final EditText FileNameEdit = new EditText(this);
              alert.setTitle("Сохранение");
              alert.setMessage("Введите имя файла");
              alert.setView(FileNameEdit);
        //кнопка сохранить
        alert.setPositiveButton("Сохранить", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                String Directory = null;

                String FileName = FileNameEdit.getText().toString();
 //               FileName = FileName + ".txt";

                // сохранение файла в нужную папку
                switch (ID)
                {
                    case 1:
                        Directory = "DeviceTest";
                        break;

                    default:
                        break;
                }

                String[] ValuesForWrite = new String[values.size()];
                for (int i=0; i < values.size(); i++)
                {
                    ValuesForWrite[i] = values.get(i);
                }
                writeFileSD( Directory, FileName, ValuesForWrite);
            }
        });

        // кнопка отмена
        alert.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                alert.setCancelable(FALSE);
                //тут все норм
            }
        });
        alert.show();
    }

//=============================================================================================
//                     клик по кнопке принять
//=============================================================================================
    public void onClick(View view)
    {
        for (int i = 0; i < values.size(); i++)
        {
            abstractDevice.ParamsValue[i] = values.get(i);
        }
        abstractDevice.ConvertValuesFromString();
        Intent result = new Intent();
        result.putExtra("EditedParams", abstractDevice);
        setResult(1, result);
        finish();
    }
//=============================================================================================
//                      стрелочка назад
//=============================================================================================
    @Override
    public boolean onSupportNavigateUp() {

        onBackPressed();
        return true;
    }

//=============================================================================================
//                      клик по стрелке назад
//=============================================================================================
    @Override
    public void onBackPressed() {
        //вызов окна диалога
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Внимание!");
        alert.setMessage("Внесенные изменения будут утеряны! Вы уверены?");
        // кнопка да
        alert.setPositiveButton("Да", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                finish();
            }
        });
        // кнопка нет
        alert.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });
        alert.show();
    }

//=============================================================================================
//                     игнор событий NFC
//=============================================================================================
    public void onPause() {
        super.onPause();
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcAdapter.disableForegroundDispatch(this);
    }

    public void onResume() {
        super.onResume();
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
    }

    public void onNewIntent(Intent intent) {
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            // drop NFC events
        }
    }

//=============================================================================================
//                     запись в файл
//=============================================================================================
    void writeFileSD(String DIR, String FileName, String[] values ) {

        // проверка на доступность SD-карты
        if (!Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            Toast.makeText(context, "SD-карта не доступна", Toast.LENGTH_LONG).show();
            return;
        }
        // получаем путь к SD
        File sdPath = Environment.getExternalStorageDirectory();
        // добавляем свой каталог к пути
        sdPath = new File(sdPath.getAbsolutePath() + "/" + "TPI" + "/" + "Saved Params" + "/" + DIR);
        // создаем каталог
        sdPath.mkdirs();
        // формируем объект File, который содержит путь к файлу

        File sdFile = new File(sdPath, FileName);
        if ((sdFile.exists()) && (sdFile.isFile()))
        {
            // если файл уже существует, то уточняем, перезаписать ли его
            final AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Внимание!");
            alert.setMessage("Файл с таким именем уже существует. Перезаписать файл?");
            alert.setPositiveButton("Да", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    fRewrite = true;
                }
            });
            alert.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    fRewrite = false;
                }
            });
            alert.show();
        }
        else
        {
            fRewrite = true;            // устанавливаем флаг о разрешении перезаписи файла
        }

        //  проверка разрешения на запись
        if (true == fRewrite)
        {
            try {
                // открываем поток для записи
                BufferedWriter bw = new BufferedWriter(new FileWriter(sdFile));
                StringBuilder CryptoValue = new StringBuilder();
                // пишем данные
                for (int i = 0; i < values.length; i++) {

                    // шифруем посимвольно с помощью XOR
                    for (int j = 0; j < values[i].length(); j++) {
                        CryptoValue.append((char) (values[i].charAt(j) ^ key.charAt(j % key.length())));
                    }
                    String result = CryptoValue.toString();
                    CryptoValue.setLength(0);
                    bw.write(result);
                    bw.newLine();
                }
                // закрываем поток
                bw.close();
                Toast.makeText(context, "Сохранено", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
            }
        }
        fRewrite = false;
    }


//=============================================================================================
//                                      чтение из файла
//=============================================================================================
    void readFileSD() {

        // проверяем доступность SD
        if (!Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            Toast.makeText(context, "SD-карта не доступна", Toast.LENGTH_LONG).show();
            return;
        }
        // получаем путь к SD
        File sdPath;
        // добавляем свой каталог к пути
        sdPath = new File(path);
        // формируем объект File, который содержит путь к файлу
        File sdFile = new File(sdPath, FileNameSD);
        try {
            // открываем поток для чтения
            BufferedReader br = new BufferedReader(new FileReader(sdFile));
            StringBuilder DecryptoValue = new StringBuilder();
            String str = "";
            // читаем содержимое
            int i = 0;

            while ((str = br.readLine()) != null)
            {
                // расшифровываем посимвольно с помощью XOR
                for(int j = 0; j<str.length(); j++)
                {
                    DecryptoValue.append((char)(str.charAt(j) ^ key.charAt(j % key.length())));
                }
                String result = DecryptoValue.toString();
                DecryptoValue.setLength(0);
               values.set(i, result);
                i++;
            }
            i = 0;
            br.close();
            Toast.makeText(context, "Параметры загружены", Toast.LENGTH_LONG).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

