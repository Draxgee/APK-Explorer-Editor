package com.apk.editor.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;

import com.apk.editor.R;
import com.apk.editor.utils.APKData;
import com.apk.editor.utils.AppData;
import com.apk.editor.utils.Common;
import com.apk.editor.utils.recyclerViewItems.PackageItems;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textview.MaterialTextView;

import java.io.File;
import java.util.Objects;

import in.sunilpaulmathew.sCommon.Utils.sAPKUtils;
import in.sunilpaulmathew.sCommon.Utils.sPackageUtils;
import in.sunilpaulmathew.sCommon.Utils.sUtils;

/*
 * Created by APK Explorer & Editor <apkeditor@protonmail.com> on March 04, 2021
 */
public class InstallerActivity extends AppCompatActivity {

    private AppCompatImageButton mIcon;
    private MaterialCardView mCancel, mOpen;
    private MaterialTextView mStatus, mTitle;
    private ProgressBar mProgress;
    public static final String HEADING_INTENT = "heading", PATH_INTENT = "path";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_installer);

        mIcon = findViewById(R.id.icon);
        mProgress = findViewById(R.id.progress);
        mOpen = findViewById(R.id.open);
        mCancel = findViewById(R.id.cancel);
        MaterialTextView mHeading = findViewById(R.id.heading);
        mTitle = findViewById(R.id.title);
        mStatus = findViewById(R.id.status);

        String path = getIntent().getStringExtra(PATH_INTENT);
        if (path != null) {
            try {
                Common.setPackageName(Objects.requireNonNull(sAPKUtils.getPackageName(path, this)).toString());
                mTitle.setText(sAPKUtils.getAPKName(path, this));
                mIcon.setImageDrawable(sAPKUtils.getAPKIcon(path, this));
            } catch (NullPointerException ignored) {}
        } else {
            Common.setPackageName(APKData.findPackageName(this));
            mTitle.setText(getName());
            mIcon.setImageDrawable(getIcon());
        }

        mHeading.setText(getIntent().getStringExtra(HEADING_INTENT));

        mOpen.setOnClickListener(v -> {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(Common.getPackageName());
            if (launchIntent != null) {
                startActivity(launchIntent);
                finish();
            }
        });

        mCancel.setOnClickListener(v -> onBackPressed());

        refreshStatus(this);
    }

    public void refreshStatus(Activity activity) {
        new Thread() {
            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(500);
                        runOnUiThread(() -> {
                            String installationStatus = sUtils.getString("installationStatus", "waiting", activity);
                            if (installationStatus.equals("waiting")) {
                                try {
                                    if (getIntent().getStringExtra(PATH_INTENT) != null) {
                                        mStatus.setText(getString(R.string.installing, sAPKUtils.getAPKName(getIntent().getStringExtra(PATH_INTENT), activity)));
                                    } else {
                                        mStatus.setText(getString(R.string.installing, getName()));
                                    }
                                } catch (NullPointerException ignored) {}
                            } else {
                                mStatus.setText(installationStatus);
                                mProgress.setVisibility(View.GONE);
                                mCancel.setVisibility(View.VISIBLE);
                                if (installationStatus.equals(getString(R.string.installation_status_success))) {
                                    try {
                                        mTitle.setText(sPackageUtils.getAppName(Common.getPackageName(), activity));
                                        mIcon.setImageDrawable(sPackageUtils.getAppIcon(Common.getPackageName(), activity));
                                        mOpen.setVisibility(View.VISIBLE);
                                    } catch (NullPointerException ignored) {}
                                }
                            }
                        });
                    }
                } catch (InterruptedException ignored) {}
            }
        }.start();
    }

    private CharSequence getName() {
        CharSequence name = null;
        for (String mAPKs : Common.getAPKList()) {
            if (sAPKUtils.getAPKName(mAPKs, this) != null) {
                name = sAPKUtils.getAPKName(mAPKs, this);
            }
        }
        return name;
    }

    private Drawable getIcon() {
        Drawable icon = null;
        for (String mAPKs : Common.getAPKList()) {
            if (sAPKUtils.getAPKIcon(mAPKs, this) != null) {
                icon = sAPKUtils.getAPKIcon(mAPKs, this);
            }
        }
        return icon;
    }

    @Override
    public void onBackPressed() {
        if (sUtils.getString("installationStatus", "waiting", this).equals("waiting")) {
            return;
        }
        if (sUtils.getString("installationStatus", "waiting", this).equals(getString(R.string.installation_status_success))) {
            Common.getPackageData().add(new PackageItems(
                    sPackageUtils.getAppName(Common.getPackageName(), this).toString(),
                    Common.getPackageName(),
                    sAPKUtils.getVersionName(sPackageUtils.getSourceDir(Common.getPackageName(), this), this),
                    new File(sPackageUtils.getSourceDir(Common.getPackageName(), this)).length(),
                    Objects.requireNonNull(AppData.getPackageInfo(Common.getPackageName(), this)).firstInstallTime,
                    Objects.requireNonNull(AppData.getPackageInfo(Common.getPackageName(), this)).lastUpdateTime,
                    sPackageUtils.getAppIcon(Common.getPackageName(), this)
            ));
        }
        if (sUtils.exist(new File(getCacheDir(),"splits"))) {
            sUtils.delete(new File(getCacheDir(),"splits"));
        }
        super.onBackPressed();
    }

}