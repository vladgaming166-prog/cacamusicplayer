package com.cacamusicplayer.app.ui;

import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import com.cacamusicplayer.app.R;
import com.cacamusicplayer.app.util.Prefs;

public class ThemePickerActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_theme);
        RadioGroup group = (RadioGroup) findViewById(R.id.themes);
        int theme = Prefs.theme();
        if (theme == Prefs.THEME_DARK) {
            ((RadioButton) findViewById(R.id.theme_dark)).setChecked(true);
        } else if (theme == Prefs.THEME_CLASSIC) {
            ((RadioButton) findViewById(R.id.theme_classic)).setChecked(true);
        } else {
            ((RadioButton) findViewById(R.id.theme_light)).setChecked(true);
        }
        group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.theme_dark) {
                    Prefs.setTheme(Prefs.THEME_DARK);
                } else if (checkedId == R.id.theme_classic) {
                    Prefs.setTheme(Prefs.THEME_CLASSIC);
                } else {
                    Prefs.setTheme(Prefs.THEME_LIGHT);
                }
                recreateCompat();
            }
        });
    }

    private void recreateCompat() {
        finish();
        startActivity(getIntent());
    }
}
