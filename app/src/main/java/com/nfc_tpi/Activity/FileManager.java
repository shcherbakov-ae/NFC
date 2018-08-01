package com.nfc_tpi.Activity;

/**
 * Created by ashch on 16.08.2017.
 */

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.nfc_tpi.R;


public class FileManager extends ListActivity {
    private List<String> item = null;       // папка/файл
    private List<String> path = null;       // хранение пути
    private String root="/sdcard/TPI";      // начальная директория
    private TextView myPath;                // текстовое поле



//=============================================================================================
//                                      Создание активности
//=============================================================================================

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_manager);
        myPath = (TextView)findViewById(R.id.path);
        getDir(root);
    }

//=============================================================================================
//                                      Перемещение по директориям
//=============================================================================================


    private void getDir(String dirPath)
    {
        myPath.setText(dirPath);
        item = new ArrayList<String>();
        path = new ArrayList<String>();
        File f = new File(dirPath);
        File[] files = f.listFiles();
        if(!dirPath.equals(root))
        {
            item.add(root);
            path.add(root);
            item.add("../");
            path.add(f.getParent());
        }

        for(int i=0; i < files.length; i++)
        {
            File file = files[i];
            path.add(file.getPath());
            if(file.isDirectory())
                item.add(file.getName() + "/");
            else
                item.add(file.getName());
        }

        // вывод на экран через адаптер
        ArrayAdapter<String> fileList = new ArrayAdapter<String>(this, R.layout.row, item);
        setListAdapter(fileList);
    }

//=============================================================================================
//                                      Обработка нажатий на папки/файлы
//=============================================================================================

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final File file = new File(path.get(position));
        if (file.isDirectory())
        {
            if(file.canRead())
                getDir(path.get(position));
            else
            {
                new AlertDialog.Builder(this)
                        .setTitle("'" + file.getName() + "' нет доступа к папке")
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {}})
                        .show();

            }

        }
        else
        {
            new AlertDialog.Builder(this)
                    .setTitle("Загрузить параметры из файла "+ "'" + file.getName() + "'?")
                    .setPositiveButton("Да",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent();
                                    intent.putExtra("path", myPath.getText().toString());
                                    intent.putExtra("FileNameSD",file.getName());
                                    setResult(1,intent);
                                    finish();

                                }

                            })
                    .setNegativeButton("Нет",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {}  })
                    .show();

        }
    }

//=============================================================================================
//                     игнор событий NFC
//=============================================================================================

    public void onResume() {
        super.onResume();
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
    }

    public void onPause() {
        super.onPause();
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcAdapter.disableForegroundDispatch(this);
    }

    public void onNewIntent(Intent intent) {
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            // drop NFC events
        }
    }

}