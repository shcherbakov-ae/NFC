package com.nfc_tpi.Adapters;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Filter;

import com.nfc_tpi.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ashch on 14.08.2017.
 */

public class ListAdapter extends ArrayAdapter<String> implements Filterable {

    private final Context context;

    private ArrayList<String> values;                           // лист со значениями
    private  ArrayList<String> params;                          // лист с параметрами
    private ArrayList<String> key = new ArrayList<>();          // лист с ключами

    private List<List<String>> table = new ArrayList<>();       // таблица
    private List<List<String>> table_full = new ArrayList<>();  // полная таблица

//=============================================================================================
// 							конструктор
//=============================================================================================

    public ListAdapter(Context context, ArrayList<String> value, ArrayList<String> param) {
        super(context, R.layout.list_item, value);
        this.context = context;

        // хаваем входные значения
            values = value;
            params = param;

        // присваиваем строкам ключи
        String str;
        for (int i = 0; i < values.size(); i++)
        {
            str = Integer.toString(i);
            key.add(str);
        }
        // сводим в таблицу
        table.add(key);
        table.add(params);
        table.add(values);
        table_full = table;
    }

    private MyFilter filter;        // фильтр



    @Override
    public Filter getFilter() {
        if (filter == null)
            filter = new MyFilter();
        return filter;
    }

//=============================================================================================
// 							Адартер
//=============================================================================================
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // находим элементы экрана
        final View rowView = inflater.inflate(R.layout.list_item, parent, false);
        final TextView textView = (TextView) rowView.findViewById(R.id.text_view);
        final EditText editText = (EditText) rowView.findViewById(R.id.edit_text);

        // заполняем строку именем параметра и его значением
        textView.setText(table.get(1).get(position));
        editText.setText(table.get(2).get(position));

        // отслеживаем изменения значения пользователем
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
               table.get(2).set(position, editText.getText().toString());
               // values.set(position, editText.getText().toString());
            }
        });

        return rowView;
    }

    @Override
    public int getCount() {
        return table.get(1).size();
    }

    @Override
    public String getItem(int position) {
        return table.get(1).get(position);
    }

    private class MyFilter extends Filter {

//=============================================================================================
// 							Фильтрация элементов списка
//=============================================================================================
        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            // приводим к нижнему регистру
            charSequence = charSequence.toString().toLowerCase();
            FilterResults results = new FilterResults();
            List<List<String>> TableFiltered = new ArrayList<>();
            // если поисковый запрос не пустой
            if (charSequence.toString().length() > 0) {

                ArrayList<String> tempKey = new ArrayList<String>();
                ArrayList<String> tempList = new ArrayList<String>();
                ArrayList<String> tempListValue = new ArrayList<String>();

                // проходим по строкам
                for (int i = 0; params.size() > i; i++) {
                    // если есть символы
                    if (table.get(1).get(i).toLowerCase().contains(charSequence))
                    {
                        // копипастим во временные листы
                        tempKey.add(table.get(0).get(i));
                        tempList.add(table.get(1).get(i));
                        tempListValue.add(table.get(2).get(i));
                    }
                }
                // заполняем временную таблицу
                TableFiltered.add(tempKey);
                TableFiltered.add(tempList);
                TableFiltered.add(tempListValue);
                // записываем число подходящих результатов
                results.count = TableFiltered.get(1).size();
                // и их значения
                results.values = TableFiltered;
            } else {
                // если поисковый запрос очистился, то копируем значения параметров в полную таблицу
                for (int i = 0; i < table_full.get(0).size(); i++) {
                    for (int j = 0; j < table.get(0).size(); j++)
                    {
                        if ((table_full.get(0).get(i)) == (table.get(0).get(j)))
                        {
                            table_full.get(2).set(i, table.get(2).get(j));
                        }
                    }
                }
                table = table_full;
            }
            return results;
        }

//=============================================================================================
// 							вывод результата
//=============================================================================================
        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {

            if (filterResults.values != null) {
                table = (List<List<String>>) filterResults.values;
            }
            notifyDataSetChanged();
        }
    }
}
