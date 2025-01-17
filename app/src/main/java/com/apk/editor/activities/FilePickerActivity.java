package com.apk.editor.activities;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.apk.editor.R;
import com.apk.editor.adapters.FilePickerAdapter;
import com.apk.editor.utils.APKExplorer;
import com.apk.editor.utils.Common;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textview.MaterialTextView;

import java.io.File;
import java.util.Objects;

import in.sunilpaulmathew.sCommon.Utils.sExecutor;
import in.sunilpaulmathew.sCommon.Utils.sPermissionUtils;
import in.sunilpaulmathew.sCommon.Utils.sUtils;

/*
 * Created by APK Explorer & Editor <apkeditor@protonmail.com> on March 05, 2021
 */
public class FilePickerActivity extends AppCompatActivity {

    private FilePickerAdapter mRecycleViewAdapter;
    private LinearLayoutCompat mProgressLayout;
    private MaterialTextView mTitle;
    private RecyclerView mRecyclerView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filepicker);

        AppCompatImageButton mBack = findViewById(R.id.back);
        mTitle = findViewById(R.id.title);
        AppCompatImageButton mSortButton = findViewById(R.id.sort);
        mProgressLayout = findViewById(R.id.progress_layout);
        mRecyclerView = findViewById(R.id.recycler_view);

        mBack.setOnClickListener(v -> super.onBackPressed());

        if (Build.VERSION.SDK_INT < 29 && sPermissionUtils.isPermissionDenied(android.Manifest.permission.WRITE_EXTERNAL_STORAGE,this)) {
            LinearLayout mPermissionLayout = findViewById(R.id.permission_layout);
            MaterialCardView mPermissionGrant = findViewById(R.id.grant_card);
            mPermissionLayout.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.GONE);
            mPermissionGrant.setOnClickListener(v -> sPermissionUtils.requestPermission(
                    new String[] {
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },this));
            return;
        }

        mRecyclerView.setLayoutManager(new GridLayoutManager(this, APKExplorer.getSpanCount(this)));
        mRecycleViewAdapter = new FilePickerAdapter(APKExplorer.getData(getFilesList(), true, this));
        mRecyclerView.setAdapter(mRecycleViewAdapter);

        mTitle.setText(Common.getFilePath().equals(Environment.getExternalStorageDirectory().toString() + File.separator) ? getString(R.string.sdcard) : new File(Common.getFilePath()).getName());

        mRecycleViewAdapter.setOnItemClickListener((position, v) -> {
            if (new File(APKExplorer.getData(getFilesList(), true, this).get(position)).isDirectory()) {
                Common.setFilePath(APKExplorer.getData(getFilesList(), true, this).get(position));
                reload(this);
            } else {
                new MaterialAlertDialogBuilder(this)
                        .setIcon(R.mipmap.ic_launcher)
                        .setTitle(R.string.app_name)
                        .setMessage(Common.getFileToReplace() != null ? getString(R.string.replace_question, new File(Common.getFileToReplace()).getName()) + " " +
                                new File(APKExplorer.getData(getFilesList(), true, this).get(position)).getName() + "?" : getString(R.string.signing_question,
                                new File(APKExplorer.getData(getFilesList(), true, this).get(position)).getName()) + " " + getString(Common.hasPrivateKey() ?
                                R.string.private_key : R.string.rsa_template))
                        .setNegativeButton(R.string.cancel, (dialog, id) -> {
                        })
                        .setPositiveButton(Common.getFileToReplace() != null ? R.string.replace : R.string.select, (dialog, id) -> {
                            if (Common.getFileToReplace() != null) {
                                sUtils.copy(new File(APKExplorer.getData(getFilesList(), true, this).get(position)), new File(Common.getFileToReplace()));
                                Common.setFileToReplace(null);
                            }  else {
                                new File(getFilesDir(), "signing").mkdirs();
                                if (Common.hasPrivateKey()) {
                                    sUtils.saveString("PrivateKey", APKExplorer.getData(getFilesList(), true, this).get(position), this);
                                    sUtils.copy(new File(APKExplorer.getData(getFilesList(), true, this).get(position)), new File(getFilesDir(), "signing/APKEditor.pk8"));
                                    Common.setPrivateKeyStatus(false);
                                } else {
                                    sUtils.saveString("RSATemplate", APKExplorer.getData(getFilesList(), true, this).get(position), this);
                                    sUtils.copy(new File(APKExplorer.getData(getFilesList(), true, this).get(position)), new File(getFilesDir(), "signing/APKEditor"));
                                    Common.setRSATemplateStatus(false);
                                }
                            }
                            finish();
                        }).show();
            }
        });

        mSortButton.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(this, mSortButton);
            Menu menu = popupMenu.getMenu();
            menu.add(Menu.NONE, 0, Menu.NONE, getString(R.string.sort_order)).setCheckable(true)
                    .setChecked(sUtils.getBoolean("az_order", true, this));
            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 0) {
                    sUtils.saveBoolean("az_order", !sUtils.getBoolean("az_order", true, this), this);
                    reload(this);
                }
                return false;
            });
            popupMenu.show();
        });
    }

    private File[] getFilesList() {
        if (Common.getFilePath() == null) {
            Common.setFilePath(Environment.getExternalStorageDirectory().toString());
        }
        if (!Common.getFilePath().endsWith(File.separator)) {
            Common.setFilePath(Common.getFilePath() + File.separator);
        }
        return new File(Common.getFilePath()).listFiles();
    }

    private void reload(Activity activity) {
        new sExecutor() {

            @Override
            public void onPreExecute() {
                APKExplorer.getData(getFilesList(), true, activity).clear();
                mProgressLayout.setVisibility(View.VISIBLE);
                mRecyclerView.setVisibility(View.GONE);
            }

            @Override
            public void doInBackground() {
                mRecycleViewAdapter = new FilePickerAdapter(APKExplorer.getData(getFilesList(), true, activity));
            }

            @Override
            public void onPostExecute() {
                mRecyclerView.setAdapter(mRecycleViewAdapter);
                mTitle.setText(Common.getFilePath().equals(Environment.getExternalStorageDirectory().toString() + File.separator) ? getString(R.string.sdcard)
                        : new File(Common.getFilePath()).getName());
                mProgressLayout.setVisibility(View.GONE);
                mRecyclerView.setVisibility(View.VISIBLE);
            }
        }.execute();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == 0 && Build.VERSION.SDK_INT < 30 && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            this.recreate();
        }
    }

    @Override
    public void onBackPressed() {
        if (Common.getFilePath().equals(Environment.getExternalStorageDirectory().toString() + File.separator)) {
            super.onBackPressed();
        } else {
            Common.setFilePath(Objects.requireNonNull(new File(Common.getFilePath()).getParentFile()).getPath());
            reload(this);
        }
    }

}