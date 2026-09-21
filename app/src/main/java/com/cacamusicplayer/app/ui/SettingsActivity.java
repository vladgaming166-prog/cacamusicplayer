package com.cacamusicplayer.app.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;
import com.cacamusicplayer.app.R;
import com.cacamusicplayer.app.data.LibraryStore;
import com.cacamusicplayer.app.library.MusicScanner;
import com.cacamusicplayer.app.util.ArtworkCache;
import com.cacamusicplayer.app.util.Prefs;
import com.cacamusicplayer.app.util.StoragePermission;
import com.cacamusicplayer.app.util.UiUtil;

public class SettingsActivity extends BaseActivity {
    private final Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ((Button) findViewById(R.id.btn_scan)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                scan();
            }
        });
        findViewById(R.id.btn_theme).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(SettingsActivity.this, ThemePickerActivity.class));
            }
        });
        findViewById(R.id.btn_account).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(SettingsActivity.this, AccountActivity.class));
            }
        });
        findViewById(R.id.btn_about).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(SettingsActivity.this, AboutActivity.class));
            }
        });

        bindCheck((CheckBox) findViewById(R.id.chk_resume), Prefs.resumeOnStart(), new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Prefs.setResumeOnStart(isChecked);
            }
        });
        bindCheck((CheckBox) findViewById(R.id.chk_scan_start), Prefs.scanOnStart(), new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Prefs.setScanOnStart(isChecked);
            }
        });
        bindCheck((CheckBox) findViewById(R.id.chk_notify), Prefs.showNotification(), new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Prefs.setShowNotification(isChecked);
            }
        });
        bindCheck((CheckBox) findViewById(R.id.chk_audio_focus), Prefs.audioFocus(), new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Prefs.setAudioFocus(isChecked);
            }
        });
        bindCheck((CheckBox) findViewById(R.id.chk_online), Prefs.onlineEnabled(), new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Prefs.setOnlineEnabled(isChecked);
            }
        });

        final EditText jamendo = (EditText) findViewById(R.id.edit_jamendo);
        jamendo.setText(Prefs.jamendoClientId());
        final EditText catalog = (EditText) findViewById(R.id.edit_catalog);
        catalog.setText(Prefs.userCatalogUrl());
        final EditText server = (EditText) findViewById(R.id.edit_server);
        server.setText(Prefs.syncServerUrl());

        jamendo.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    Prefs.setJamendoClientId(jamendo.getText().toString());
                }
            }
        });
        catalog.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    Prefs.setUserCatalogUrl(catalog.getText().toString());
                }
            }
        });
        server.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    Prefs.setSyncServerUrl(server.getText().toString());
                }
            }
        });

        findViewById(R.id.btn_clear_cache).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ArtworkCache.get(SettingsActivity.this).clear();
                try {
                    deleteDir(getCacheDir());
                } catch (Throwable ignored) {
                }
                UiUtil.toast(SettingsActivity.this, R.string.cache_cleared);
            }
        });
        findViewById(R.id.btn_reset).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle(R.string.reset_app)
                        .setMessage(R.string.reset_confirm)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                LibraryStore.get(SettingsActivity.this).resetAll();
                                Prefs.clearAll();
                                ArtworkCache.get(SettingsActivity.this).clear();
                                UiUtil.toast(SettingsActivity.this, R.string.app_reset);
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }
        });
    }

    @Override
    protected void onPause() {
        Prefs.setJamendoClientId(((EditText) findViewById(R.id.edit_jamendo)).getText().toString());
        Prefs.setUserCatalogUrl(((EditText) findViewById(R.id.edit_catalog)).getText().toString());
        Prefs.setSyncServerUrl(((EditText) findViewById(R.id.edit_server)).getText().toString());
        super.onPause();
    }

    private static void bindCheck(CheckBox box, boolean value, CompoundButton.OnCheckedChangeListener l) {
        box.setChecked(value);
        box.setOnCheckedChangeListener(l);
    }

    private void scan() {
        if (!StoragePermission.hasLibraryAccess(this)) {
            StoragePermission.requestLibraryAccess(this);
            return;
        }
        Toast.makeText(this, R.string.scanning, Toast.LENGTH_SHORT).show();
        MusicScanner.scanAsync(this, new MusicScanner.Listener() {
            public void onDone(final int count) {
                mHandler.post(new Runnable() {
                    public void run() {
                        Prefs.setLibraryScanned(true);
                        Toast.makeText(SettingsActivity.this,
                                getString(R.string.scan_done, Integer.valueOf(count)),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
            public void onError(String message) {
                mHandler.post(new Runnable() {
                    public void run() {
                        UiUtil.toast(SettingsActivity.this, R.string.scan_failed);
                    }
                });
            }
        });
    }

    private static void deleteDir(java.io.File dir) {
        if (dir == null || !dir.exists()) {
            return;
        }
        java.io.File[] files = dir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDir(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
    }
}
