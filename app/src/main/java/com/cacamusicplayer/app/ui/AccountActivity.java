package com.cacamusicplayer.app.ui;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.cacamusicplayer.app.R;
import com.cacamusicplayer.app.account.LocalAccountStore;
import com.cacamusicplayer.app.account.SyncClient;
import com.cacamusicplayer.app.util.UiUtil;

public class AccountActivity extends BaseActivity {
    private final Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);
        refreshStatus();

        final EditText user = (EditText) findViewById(R.id.username);
        final EditText display = (EditText) findViewById(R.id.display_name);
        final EditText pass = (EditText) findViewById(R.id.password);

        ((Button) findViewById(R.id.btn_create)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String u = user.getText().toString().trim();
                String p = pass.getText().toString();
                if (u.length() == 0 || p.length() == 0) {
                    UiUtil.toast(AccountActivity.this, R.string.fill_fields);
                    return;
                }
                if (LocalAccountStore.find(AccountActivity.this, u) != null) {
                    UiUtil.toast(AccountActivity.this, R.string.username_taken);
                    return;
                }
                if (LocalAccountStore.create(AccountActivity.this, u, display.getText().toString(), p)) {
                    UiUtil.toast(AccountActivity.this, R.string.account_created);
                    refreshStatus();
                } else {
                    UiUtil.toast(AccountActivity.this, R.string.login_failed);
                }
            }
        });
        findViewById(R.id.btn_signin).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean ok = LocalAccountStore.signIn(AccountActivity.this,
                        user.getText().toString(), pass.getText().toString());
                UiUtil.toast(AccountActivity.this, ok ? R.string.sign_in : R.string.login_failed);
                refreshStatus();
            }
        });
        findViewById(R.id.btn_signout).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                LocalAccountStore.signOut();
                refreshStatus();
            }
        });
        findViewById(R.id.btn_sync).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            SyncClient.sync(AccountActivity.this);
                            mHandler.post(new Runnable() {
                                public void run() {
                                    UiUtil.toast(AccountActivity.this, R.string.sync_ok);
                                }
                            });
                        } catch (Throwable t) {
                            mHandler.post(new Runnable() {
                                public void run() {
                                    UiUtil.toast(AccountActivity.this, R.string.sync_fail);
                                }
                            });
                        }
                    }
                }).start();
            }
        });
    }

    private void refreshStatus() {
        TextView status = (TextView) findViewById(R.id.status);
        LocalAccountStore.Account a = LocalAccountStore.current(this);
        if (a == null) {
            status.setText(R.string.not_signed_in);
        } else {
            status.setText(getString(R.string.signed_in_as, a.displayName));
        }
    }
}
