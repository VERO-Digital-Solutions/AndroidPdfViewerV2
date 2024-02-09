/**
 * Copyright 2016 Bartosz Schiller
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.barteksc.sample;

import static com.github.barteksc.sample.LoggerKt.logDebug;
import static com.github.barteksc.sample.LoggerKt.toast;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnErrorListener;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnLongPressListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.listener.OnPageErrorListener;
import com.github.barteksc.pdfviewer.listener.OnTapListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.github.barteksc.pdfviewer.util.Constants;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.NonConfigurationInstance;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.ViewById;
import org.benjinus.pdfium.Bookmark;
import org.benjinus.pdfium.Meta;

import java.io.File;
import java.util.List;
import java.util.Objects;

@EActivity(R.layout.activity_main)
@OptionsMenu(R.menu.options)
public class PDFViewActivity extends AppCompatActivity implements OnPageChangeListener, OnLoadCompleteListener, OnErrorListener,
        OnPageErrorListener, OnTapListener, OnLongPressListener {

    private static final String TAG = PDFViewActivity.class.getSimpleName();

    public static final int PERMISSION_CODE = 42042;
    public static final String READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE";
    public static final String SAMPLE_FILE = "sample.pdf";

    PDFView.Configurator configurator = null;

    @ViewById
    PDFView pdfView;

    @NonConfigurationInstance
    Integer pageNumber = 0;

    String pdfFileName;

    @NonConfigurationInstance
    Uri currUri = null;

    @OptionsItem(R.id.pickFile)
    void pickFile() {
        int permissionCheck = ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{READ_EXTERNAL_STORAGE},
                    PERMISSION_CODE
            );
            return;
        }
        launchPicker();
    }

    void launchPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        try {
            startActivityForResult(intent, Constants.KEY_REQUEST_FILE_PICKER);
        } catch (ActivityNotFoundException e) {
            //alert user that file manager not working
            Toast.makeText(this, R.string.toast_pick_file_error, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        this.pdfView = findViewById(R.id.pdfView);
    }

    @AfterViews
    void afterViews() {
        pdfView.setBackgroundColor(Color.LTGRAY);
        if (currUri != null) {
            displayFileFromUri(getApplicationContext());
        } else {
            displayFromAsset(SAMPLE_FILE);
        }
        setTitle(pdfFileName);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == Constants.KEY_REQUEST_FILE_PICKER) {
            if (data != null && data.getData() != null) {
                this.currUri = data.getData();
                displayFileFromUri(getApplicationContext());
            } else {
                Log.e(TAG, "onActivityResult, requestCode:" + requestCode + "resultCode:" + resultCode);
            }
        }
    }

    private void displayFromAsset(String assetFileName) {
        pdfFileName = assetFileName;

        pdfView.fromAsset(SAMPLE_FILE)
                .defaultPage(pageNumber)
                .onPageChange(this)
                .enableAnnotationRendering(true)
                .onLoad(this)
                .scrollHandle(new DefaultScrollHandle(this))
                .spacing(10) // in dp
                .onPageError(this)
                .onTap(this)
                .onLongPress(this)
                .load();
    }

    private void displayFileFromUri(Context context) {
        if (currUri == null) {
            toast(this, "currUri is null");
            return;
        }
        String uriName = UriExKt.getFileName(currUri, context);
        if (uriName != null) {
            // From the uri, create a file in internal storage
            File pdfFile = UriExKt.toFileOrNull(currUri, context, uriName);

            // Get the uri for the created file
            Uri copiedPdfFileUri = Uri.fromFile(pdfFile);
            this.currUri = copiedPdfFileUri;

            if (pdfFile != null) {
                pdfFileName = pdfFile.getName();
                this.configurator = pdfView.fromUri(currUri)
                        .defaultPage(Constants.DEFAULT_PAGE_NUMBER)
                        .onPageChange(this)
                        .enableAnnotationRendering(true)
                        .onLoad(this)
                        .enableSwipe(true)
                        .scrollHandle(new DefaultScrollHandle(this))
                        .linkHandler(pdfFile.getAbsolutePath(), (schemaId, documentationId) ->
                                logDebug(TAG, "Clicked annotation with schemaId " + schemaId +
                                        " and documentationId " + documentationId))
                        .spacing(10) // in dp
                        .onPageError(this)
                        .onTap(this)
                        .onLongPress(this)
                        .onError(this);

                this.configurator.load();
            } else {
                toast(this, "Couldn't copy file to internal storage");
            }
        } else {
            toast(this, "Couldn't extract uri's name");
        }
    }

    @Override
    public void onPageChanged(int page, int pageCount) {
        pageNumber = page;
        setTitle(String.format("%s %s / %s", pdfFileName, page + 1, pageCount));
    }

    @Override
    public void loadComplete(int nbPages) {
        Meta meta = pdfView.getDocumentMeta();
        Log.e(TAG, "title = " + meta.getTitle());
        Log.e(TAG, "author = " + meta.getAuthor());
        Log.e(TAG, "subject = " + meta.getSubject());
        Log.e(TAG, "keywords = " + meta.getKeywords());
        Log.e(TAG, "creator = " + meta.getCreator());
        Log.e(TAG, "producer = " + meta.getProducer());
        Log.e(TAG, "creationDate = " + meta.getCreationDate());
        Log.e(TAG, "modDate = " + meta.getModDate());

        printBookmarksTree(pdfView.getTableOfContents(), "-");

    }

    public void printBookmarksTree(List<Bookmark> tree, String sep) {
        for (Bookmark b : tree) {

            Log.e(TAG, String.format("%s %s, p %d", sep, b.getTitle(), b.getPageIdx()));

            if (b.hasChildren()) {
                printBookmarksTree(b.getChildren(), sep + "-");
            }
        }
    }

    /**
     * Listener for response to user permission request
     *
     * @param requestCode  Check that permission request code matches
     * @param permissions  Permissions that requested
     * @param grantResults Whether permissions granted
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchPicker();
            }
        }
    }

    @Override
    public void onPageError(int page, Throwable t) {
        Log.e(TAG, "Cannot load page " + page);
    }

    @Override
    public void onLongPress(MotionEvent e) {
        Log.i(TAG, "onLongPress --> X: " + e.getX() + " | Y: " + e.getY());
        Log.i(TAG, "--------------------------------------------------");
    }

    @Override
    public boolean onTap(MotionEvent e) {
        Log.i(TAG, "onTap --> X: " + e.getX() + " | Y: " + e.getY());
        Log.i(TAG, "--------------------------------------------------");

        // check zoom and scale
        Log.i(TAG, "zoom --> " + pdfView.getZoom() + " | scale " + pdfView.getScaleX() + " , " + pdfView.getScaleY());
        Log.i(TAG, "--------------------------------------------------");

        return false;
    }

    @Override
    public void onError(Throwable t) {
        toast(this, Objects.requireNonNull(t.getMessage()));
    }
}
