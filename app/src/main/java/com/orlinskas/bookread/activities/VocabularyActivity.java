package com.orlinskas.bookread.activities;

import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.orlinskas.bookread.Book;
import com.orlinskas.bookread.R;
import com.orlinskas.bookread.ToastBuilder;
import com.orlinskas.bookread.Word;
import com.orlinskas.bookread.data.DatabaseAdapter;
import com.orlinskas.bookread.helpers.ActivityOpenHelper;
import com.orlinskas.bookread.helpers.LibraryHelper;

import java.util.ArrayList;

public class VocabularyActivity extends ListActivity {
    private ArrayList<Book> books;
    private ArrayList<Word> words = new ArrayList<>();
    private static final String FIRST_TITLE = "Список ваших книг..";
    private EditText searchRow;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vocabulary);

        ImageView rowIconHelp = findViewById(R.id.activity_vocabulary_iv_help);
        RelativeLayout rowRelativeLayout = findViewById(R.id.activity_vocabulary_rl);
        Spinner spinner = findViewById(R.id.activity_vocabulary_sp);
        searchRow = findViewById(R.id.activity_vocabulary_edit_text_search);

        rowIconHelp.setImageResource(R.drawable.ic_help_2_0);
        rowRelativeLayout.setBackgroundColor(getResources().getColor(R.color.colorPrimary));

        LibraryHelper libraryHelper = new LibraryHelper(this);
        try {
            books = libraryHelper.getBooks();
        } catch (Exception e) {
            e.printStackTrace();
        }
        ArrayAdapter<String> adapterBookTitles = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, getBookTitles());
        spinner.setAdapter(adapterBookTitles);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String bookTitle = getBookTitles()[position];
                for(Book book : books) {
                    if(book.getBookTitle().equals(bookTitle)){
                        fillList(book);
                    }
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        try {
            Book book = (Book) getIntent().getSerializableExtra("book");
            if(book != null) {
                fillList(book);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        searchRow.addTextChangedListener(new TextWatcher(){
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    findWord(searchRow.getText().toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
    }

    private String[] getBookTitles() {
        String[] titles = new String[0];
        try {
            titles = new String[books.size() + 1];
            titles[0] = FIRST_TITLE;
            for(int i = 0; i < books.size(); i++) {
                titles[i + 1] = books.get(i).getBookTitle();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return titles;
    }

    private boolean findWords(Book book) {
        try {
            DatabaseAdapter databaseAdapter = new DatabaseAdapter(getApplicationContext(), book.getDataTableName());
            databaseAdapter.openWithTransaction();
            words = databaseAdapter.getWords(book.getDataTableName());
            databaseAdapter.closeWithTransaction();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return words.size() > 0;
    }

    private String[] getWordsRus(@NonNull ArrayList<Word> words) {
        String[] wordsRus = new String[words.size()];
        for(int i = 0; i < words.size(); i++)  {
            wordsRus[i] = words.get(i).getRussian();
        }
        return wordsRus;
    }

    private void fillList (Book book) {
        if(findWords(book)){
            this.setListAdapter(new VocabularyAdapter(this, R.layout.vocabulary_row, getWordsRus(words)));
            ToastBuilder.create(this, "Всего слов: " + words.size());
        }
        else {
            ToastBuilder.create(this,"Ошибка");
        }
    }

    private class VocabularyAdapter extends ArrayAdapter<String> {
        String[] wordsOnlyRussian;

        VocabularyAdapter(Context context, int textViewResourceId, String[] objects) {
            super(context, textViewResourceId, objects);
            wordsOnlyRussian = objects;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            LayoutInflater inflater = getLayoutInflater();
            @SuppressLint("ViewHolder") View row = inflater.inflate(R.layout.vocabulary_row, parent, false);
            TextView rowTextEng = row.findViewById(R.id.vocabulary_row_eng_tv);
            TextView rowTextRus = row.findViewById(R.id.vocabulary_row_rus_tv);
            rowTextEng.setTextColor(getResources().getColor(R.color.colorAccent));
            rowTextRus.setTextColor(getResources().getColor(R.color.colorAccent));
            String russian = wordsOnlyRussian[position];
            String english = (position + 1) + ".  " + getEnglish(russian);
            rowTextRus.setText(russian);
            rowTextEng.setText(english);

            if (position%2 == 0) {
                try {
                    row.setBackgroundColor(getResources().getColor(R.color.colorLowGREY));
                } catch (Resources.NotFoundException e) {
                    e.printStackTrace();
                }
            }
            else{
                row.setBackgroundColor(getResources().getColor(R.color.colorWHITE));
            }

            return row;
        }
    }

    private String getEnglish(String russian) {
        for(Word word : words) {
            if(word.getRussian().equals(russian)){
                return word.getEnglish();
            }
        }
        return "Не найденно";
    }


    private void findWord(String needWordPart) {
        int position = 0;

            for(Word word : words) {
                String wordPartRus = null;
                try {
                    wordPartRus = word.getRussian().substring(0, needWordPart.length());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String wordPartEng = null;
                try {
                    wordPartEng = word.getEnglish().substring(0, needWordPart.length());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (wordPartRus != null) {
                        if(wordPartRus.equals(needWordPart)) {
                            getListView().smoothScrollToPosition(position);
                            break;
                        }
                }
                if (wordPartEng != null ) {
                    if(wordPartEng.equals(needWordPart)) {
                        getListView().smoothScrollToPosition(position);
                        break;
                    }
                }
                position++;
            }

    }

    public void helpButton(View view) {
        ActivityOpenHelper.openActivity(this, HelpActivity.class);
    }

}
