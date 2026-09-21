package com.cacamusicplayer.app.ui;

import android.os.Bundle;
import android.widget.TextView;
import com.cacamusicplayer.app.BuildConfig;
import com.cacamusicplayer.app.R;

public class AboutActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        TextView v = (TextView) findViewById(R.id.version);
        v.setText(getString(R.string.version_fmt, BuildConfig.VERSION_NAME,
                Integer.valueOf(BuildConfig.VERSION_CODE)));
    }
}
