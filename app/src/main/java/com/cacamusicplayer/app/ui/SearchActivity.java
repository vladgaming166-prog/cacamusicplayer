package com.cacamusicplayer.app.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import com.cacamusicplayer.app.R;
import com.cacamusicplayer.app.data.LibraryStore;
import com.cacamusicplayer.app.data.Song;
import java.util.List;

public class SearchActivity extends BaseActivity {
    private SongAdapter mAdapter;
    private TextView mEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        mAdapter = new SongAdapter(this);
        ListView list = (ListView) findViewById(R.id.list);
        mEmpty = (TextView) findViewById(android.R.id.empty);
        list.setEmptyView(mEmpty);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                playSongs(mAdapter.getSongs(), position);
                startActivity(new android.content.Intent(SearchActivity.this, PlayerActivity.class));
            }
        });
        EditText q = (EditText) findViewById(R.id.query);
        q.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                runSearch(s.toString());
            }
        });
    }

    private void runSearch(String q) {
        if (q == null || q.trim().length() == 0) {
            mAdapter.setSongs(new java.util.ArrayList<Song>());
            if (mEmpty != null) {
                mEmpty.setText(R.string.enter_query);
            }
            return;
        }
        List<Song> songs = LibraryStore.get(this).search(q);
        mAdapter.setSongs(songs);
        if (mEmpty != null) {
            mEmpty.setText(R.string.no_results);
        }
    }
}
