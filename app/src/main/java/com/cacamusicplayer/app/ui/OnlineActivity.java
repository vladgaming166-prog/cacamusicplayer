package com.cacamusicplayer.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.cacamusicplayer.app.R;
import com.cacamusicplayer.app.online.MusicProvider;
import com.cacamusicplayer.app.online.ProviderRegistry;
import com.cacamusicplayer.app.util.HttpUtil;
import com.cacamusicplayer.app.util.Prefs;
import com.cacamusicplayer.app.util.UiUtil;
import java.util.List;

public class OnlineActivity extends BaseActivity {
    private List<MusicProvider> mProviders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online);
        if (!Prefs.onlineEnabled()) {
            UiUtil.toast(this, R.string.pref_online);
        }
        mProviders = ProviderRegistry.all(this);
        ListView list = (ListView) findViewById(R.id.list);
        list.setAdapter(new BaseAdapter() {
            public int getCount() { return mProviders.size(); }
            public Object getItem(int position) { return mProviders.get(position); }
            public long getItemId(int position) { return position; }
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    v = getLayoutInflater().inflate(R.layout.item_two_line, parent, false);
                }
                MusicProvider p = mProviders.get(position);
                ((TextView) v.findViewById(R.id.title)).setText(p.title(OnlineActivity.this));
                ((TextView) v.findViewById(R.id.subtitle)).setText(p.description(OnlineActivity.this));
                return v;
            }
        });
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MusicProvider p = mProviders.get(position);
                if (!"official".equals(p.id()) && !HttpUtil.isOnline(OnlineActivity.this)) {
                    UiUtil.toast(OnlineActivity.this, R.string.network_unavailable);
                    return;
                }
                Intent i = new Intent(OnlineActivity.this, OnlineSearchActivity.class);
                i.putExtra("providerId", p.id());
                startActivity(i);
            }
        });
    }
}
