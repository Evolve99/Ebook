package com.book.mmbookstore.activity;

import static com.book.mmbookstore.util.Constant.BANNER_READING_PAGE;
import static com.book.mmbookstore.util.Constant.BOOK_PDF_UPLOAD;
import static com.book.mmbookstore.util.Constant.EXTRA_OBJECT;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.book.mmbookstore.R;
import com.book.mmbookstore.database.pref.SharedPref;
import com.book.mmbookstore.database.room.AppDatabase;
import com.book.mmbookstore.database.room.DAO;
import com.book.mmbookstore.model.Book;
import com.book.mmbookstore.rest.ApiInterface;
import com.book.mmbookstore.rest.RestAdapter;
import com.book.mmbookstore.util.AdsManager;
import com.book.mmbookstore.util.Constant;
import com.book.mmbookstore.util.ExecutorTasks;
import com.book.mmbookstore.util.FileUtil;
import com.book.mmbookstore.util.InputFilterIntRange;
import com.book.mmbookstore.util.Tools;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.link.DefaultLinkHandler;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.listener.OnPageErrorListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.github.barteksc.pdfviewer.util.FitPolicy;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;
import com.shockwave.pdfium.PdfDocument;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityPDFView extends AppCompatActivity implements OnPageChangeListener, OnLoadCompleteListener, OnPageErrorListener {

    private static final String TAG = "ActivityPdfView";
    private PDFView pdfView;
    Toolbar toolbar;
    boolean flag = true;
    AdsManager adsManager;
    InputStream inputStream = null;
    OutputStream outputStream = null;
    private ShimmerFrameLayout lytShimmer;
    CoordinatorLayout parentView;
    TextView txtPage;
    AppBarLayout lytTop;
    LinearLayout lytBottom;
    Tools tools;
    File filePath;
    View lytFailed;
    Button btnRetry;
    String fileExtension = "pdf";
    TextView txtPercentage;
    private final Handler handler = new Handler();
    SharedPref sharedPref;
    DAO db;
    ImageButton btnBookmark;
    int savedReadingPages = 0;
    int lastReadPage = 0;
    private boolean flag_read_later;
    Book book;
    String pdfUrl;
    ResponseBody mResponseBody;

    private static final String DOCUMENT_URI_ARGUMENT =
            "com.example.android.actionopendocument.args.DOCUMENT_URI_ARGUMENT";

    ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Log.d(TAG, "mStartForResult: RESULT_OK");
                    if (result.getData() != null) {
                        Uri uri = result.getData().getData();
                        int takeFlags = getIntent().getFlags();
                        takeFlags &= (Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        getContentResolver().takePersistableUriPermission(uri, takeFlags);
                        Log.d(TAG, "mStartForResult: uri: " + uri);
                        new ExecutorTasks() {

                            @Override
                            public void onPreExecute() {
                            }

                            @Override
                            public void doInBackground() {
                                alterDocument(uri);
                            }

                            @Override
                            public void onPostExecute() {
                            }
                        }.execute();

                    }
                } else {
                    Log.d(TAG, "mStartForResult: " + result.getResultCode());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        setContentView(R.layout.activity_pdf_view);
        db = AppDatabase.getDb(this).get();

        sharedPref = new SharedPref(this);
        tools = new Tools(this);
        tools.setNavigation();

        book = (Book) getIntent().getSerializableExtra(EXTRA_OBJECT);
        if (book.type.equals(BOOK_PDF_UPLOAD)) {
            pdfUrl = sharedPref.getApiUrl() + "/upload/pdf/" + book.pdf_name;
        } else {
            pdfUrl = book.pdf_name;
        }

        if (db.getBookmark(book.book_id) != null) {
            Book page = db.getBookmark(book.book_id);
            savedReadingPages = page.page_position;
            Log.d(TAG, "last page visited : " + savedReadingPages);
        }

        adsManager = new AdsManager(this);
        adsManager.loadBannerAd(BANNER_READING_PAGE);

        toolbar = findViewById(R.id.toolbar);
        lytBottom = findViewById(R.id.lytBottom);
        lytTop = findViewById(R.id.appBarLayout);
        lytShimmer = findViewById(R.id.shimmerViewContainer);
        parentView = findViewById(R.id.coordinatorLayout);
        txtPage = findViewById(R.id.txtPage);
        lytFailed = findViewById(R.id.lytFailed);
        btnRetry = findViewById(R.id.btnRetry);
        txtPercentage = findViewById(R.id.txtPercentage);
        btnBookmark = findViewById(R.id.btnBookmark);
        pdfView = findViewById(R.id.pdfView);
        pdfView.setVisibility(View.GONE);
        pdfView.setOnClickListener(v -> {
            if (flag) {
                tools.fullScreenMode(lytTop, lytBottom, true);
                flag = false;
            } else {
                tools.fullScreenMode(lytTop, lytBottom, false);
                flag = true;
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            filePath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/" + getString(R.string.app_name) + "/" + book.book_name + "." + fileExtension);
        } else {
            filePath = new File(Environment.getExternalStorageDirectory() + "/" + getString(R.string.app_name) + "/" + book.book_name + "." + fileExtension);
        }
        if (filePath.exists()) {
            Log.d(TAG, "file is exist + " + filePath);
            loadPdfFromFile(filePath);
        } else {
            Log.d(TAG, "file is not exist and load from url first");
            loadFile(pdfUrl);
        }

        refreshBookmark();
        tools.setupToolbar(this, toolbar, "", true);
        setupToolbar();
    }

    private void createFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, book.book_name + "." + fileExtension);

        mStartForResult.launch(intent);
    }

    private void alterDocument(Uri uri) {
        try {
            ParcelFileDescriptor pfd = getContentResolver()
                    .openFileDescriptor(uri, "w");
            FileOutputStream fileOutputStream =
                    new FileOutputStream(pfd.getFileDescriptor());

            inputStream = mResponseBody.byteStream();
            byte[] data = new byte[4096];
            int count;
            int progress = 0;
            while ((count = inputStream.read(data)) != -1) {
                fileOutputStream.write(data, 0, count);
                progress += count;
                int finalProgress = progress;
                handler.post(() -> txtPercentage.setText((int) ((finalProgress * 100) / mResponseBody.contentLength()) + "%"));
                Log.d(TAG, "Progress: " + progress + "/" + mResponseBody.contentLength() + " >>>> " + (float) progress / mResponseBody.contentLength());
            }
            fileOutputStream.close();
            pfd.close();
            db.insertBook(book.book_id, book.book_name, uri.toString());

            File file = FileUtil.from(this, uri);
            Log.d(TAG, "alterDocument: File:::::::: uti" + file.getPath() + " file - " + file + ": " + file.exists());
            loadPdfFromFile(file);
            Log.d(TAG, "File saved successfully!");
        } catch (IOException e) {
            e.printStackTrace();
            loadPdfFromUrl(mResponseBody.byteStream());
            Log.d(TAG, "Failed to save the file! " + e.getMessage());
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void pdfFromFile(Uri documentUri) {
        try {
//            File file = FileUtil.from(this, documentUri);
            ParcelFileDescriptor pfd = getContentResolver()
                    .openFileDescriptor(documentUri, "r");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void setupToolbar() {
        ((TextView) findViewById(R.id.toolbar_title)).setText(book.book_name);
    }

    @Override
    public void onPageChanged(int page, int pageCount) {
        lastReadPage = page;
        txtPage.setText(String.format("%s %s / %s", "", page + 1, pageCount));
        onBookmarkPage(page);
    }

    @Override
    public void loadComplete(int nbPages) {
        swipeProgress(false);
        txtPage.setVisibility(View.VISIBLE);
        findViewById(R.id.btnJumpPage).setOnClickListener(v -> showPageDialog(pdfView.getPageCount()));
        findViewById(R.id.btnShare).setOnClickListener(v -> Tools.shareContent(ActivityPDFView.this, book.book_name));
        PdfDocument.Meta meta = pdfView.getDocumentMeta();
        Log.d(TAG, "title = " + meta.getTitle());
        Log.d(TAG, "author = " + meta.getAuthor());
        Log.d(TAG, "subject = " + meta.getSubject());
        Log.d(TAG, "keywords = " + meta.getKeywords());
        Log.d(TAG, "creator = " + meta.getCreator());
        Log.d(TAG, "producer = " + meta.getProducer());
        Log.d(TAG, "creationDate = " + meta.getCreationDate());
        Log.d(TAG, "modDate = " + meta.getModDate());
        printBookmarksTree(pdfView.getTableOfContents(), "-");
    }

    public void printBookmarksTree(List<PdfDocument.Bookmark> tree, String sep) {
        for (PdfDocument.Bookmark b : tree) {
            Log.e(TAG, String.format("%s %s, p %d", sep, b.getTitle(), b.getPageIdx()));
            if (b.hasChildren()) {
                printBookmarksTree(b.getChildren(), sep + "-");
            }
        }
    }

    @Override
    public void onPageError(int page, Throwable t) {
        Log.e(TAG, "Cannot load page " + page);
        showFailedView();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        updateLastPageRead();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            super.onBackPressed();
            updateLastPageRead();
        } else {
            return super.onOptionsItemSelected(menuItem);
        }
        return true;
    }

    private void updateLastPageRead() {
        flag_read_later = db.getBookmark(book.book_id) != null;
        if (flag_read_later) {
            db.updateBookmark(book.book_id, lastReadPage);
            Log.d(TAG, "update last page bookmarked");
        }
    }

    private void onBookmarkPage(int pagePosition) {
        btnBookmark.setOnClickListener(v -> {
            String str;
            if (flag_read_later) {
                db.deleteBookmark(book.book_id);
                str = getString(R.string.bookmark_removed);
            } else {
                db.insertBookmark(
                        book.book_id,
                        book.category_id,
                        book.book_name,
                        book.book_image,
                        book.author,
                        book.type,
                        book.pdf_name,
                        pagePosition,
                        System.currentTimeMillis()
                );
                str = getString(R.string.bookmark_added);
            }
            Snackbar.make(parentView, str, Snackbar.LENGTH_SHORT).show();
            refreshBookmark();
        });
    }

    public void showPageDialog(int totalPages) {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(ActivityPDFView.this);
        LayoutInflater inflater = LayoutInflater.from(ActivityPDFView.this);
        View view = inflater.inflate(R.layout.dialog_jump_page, null);

        LinearLayout lytDialogHeader = view.findViewById(R.id.lytDialogHeader);
        LinearLayout lytDialogContent = view.findViewById(R.id.lytDialogContent);
        if (sharedPref.getIsDarkTheme()) {
            lytDialogHeader.setBackgroundColor(getResources().getColor(R.color.colorToolbarDark));
            lytDialogContent.setBackgroundColor(getResources().getColor(R.color.colorBackgroundDark));
        }

        TextView txtInputPageNumber = view.findViewById(R.id.txtInputPageNumber);
        txtInputPageNumber.setText(String.format("%s %s - %s", getString(R.string.input_page_number), "1", totalPages));

        EditText edtPageNumber = view.findViewById(R.id.edtPageNumber);
        edtPageNumber.setHint(String.format("%s - %s", "1", totalPages));

        edtPageNumber.requestFocus();
        tools.showKeyboard(true);

        InputFilterIntRange rangeFilter = new InputFilterIntRange(1, totalPages);
        edtPageNumber.setFilters(new InputFilter[]{rangeFilter});
        edtPageNumber.setOnFocusChangeListener(rangeFilter);

        dialog.setView(view);
        dialog.setCancelable(false);

        AlertDialog alertDialog = dialog.create();

        TextView btnPositive = view.findViewById(R.id.btnPositive);
        btnPositive.setOnClickListener(v -> new Handler().postDelayed(() -> {
            if (!edtPageNumber.getText().toString().equals("")) {
                int pageNumber = (Integer.parseInt(edtPageNumber.getText().toString()) - 1);
                new Handler().postDelayed(() -> pdfView.jumpTo(pageNumber, true), 200);
                tools.showKeyboard(false);
                alertDialog.dismiss();
            } else {
                Snackbar.make(parentView, getString(R.string.msg_input_page), Snackbar.LENGTH_SHORT).show();
            }
        }, 300));

        TextView btnNegative = view.findViewById(R.id.btnNegative);
        btnNegative.setOnClickListener(v -> new Handler().postDelayed(() -> {
            tools.showKeyboard(false);
            alertDialog.dismiss();
        }, 300));

        alertDialog.show();
    }

    private void swipeProgress(final boolean show) {
        if (show) {
            lytShimmer.setVisibility(View.VISIBLE);
            lytShimmer.startShimmer();
        } else {
            lytShimmer.setVisibility(View.GONE);
            txtPercentage.setVisibility(View.GONE);
            lytShimmer.stopShimmer();
        }
    }

    private void showFailedView() {
        swipeProgress(false);
        pdfView.setVisibility(View.GONE);
        lytFailed.setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.no_item_message)).setText(getString(R.string.msg_no_book));
        btnRetry.setOnClickListener(v -> {
            swipeProgress(true);
            lytFailed.setVisibility(View.GONE);
            pdfView.setVisibility(View.GONE);
            loadFile(pdfUrl);
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        lytShimmer.stopShimmer();
    }

    private void loadFile(String fileUrl) {
        ApiInterface apiInterface = RestAdapter.createAPI(fileUrl);
        Call<ResponseBody> call = apiInterface.downloadFileWithDynamicUrl(fileUrl);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Got the body for the file");
                    if (response.body() != null) {
                        mResponseBody = response.body();
                        createFile();
//                        new ExecutorTasks() {
//                            @Override
//                            public void onPreExecute() {
//                            }
//
//                            @Override
//                            public void doInBackground() {
//                                // background task here
////                                saveToStorage(response.body(), fileExtension);
//                                mResponseBody = response.body();
//                                createFile();
//                            }
//
//                            @Override
//                            public void onPostExecute() {
//                                // Ui task here
//                            }
//                        }.execute();
                    }
                } else {
                    showFailedView();
                    Log.d(TAG, "Connection failed " + response.errorBody());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
                showFailedView();
                Log.e(TAG, t.getMessage());
            }
        });
    }

    public void saveToStorage(ResponseBody body, String extension) {
        try {
            File dir;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/" + getString(R.string.app_name));
            } else {
                dir = new File(Environment.getExternalStorageDirectory() + "/" + getString(R.string.app_name));
            }
            boolean success = true;
            if (!dir.exists()) {
                success = dir.mkdirs();
            }
            if (success) {
                try {
                    Log.d(TAG, "File Size = " + body.contentLength());
                    inputStream = body.byteStream();
                    outputStream = new FileOutputStream(dir + "/" + book.book_name + "." + extension);

                    byte[] data = new byte[4096];
                    int count;
                    int progress = 0;
                    while ((count = inputStream.read(data)) != -1) {
                        outputStream.write(data, 0, count);
                        progress += count;
                        int finalProgress = progress;
                        handler.post(() -> txtPercentage.setText((int) ((finalProgress * 100) / body.contentLength()) + "%"));
//                        Log.d(TAG, "Progress: " + progress + "/" + body.contentLength() + " >>>> " + (float) progress / body.contentLength());
                    }
                    outputStream.flush();
                    loadPdfFromFile(filePath);
                    Log.d(TAG, "File saved successfully!");
                } catch (IOException e) {
                    e.printStackTrace();
                    loadPdfFromUrl(body.byteStream());
                    Log.d(TAG, "Failed to save the file! " + e.getMessage());
                } finally {
                    if (inputStream != null) inputStream.close();
                    if (outputStream != null) outputStream.close();
                }
            } else {
                Log.d(TAG, "not success");
                loadPdfFromUrl(body.byteStream());
            }
        } catch (IOException e) {
            e.printStackTrace();
            loadPdfFromUrl(body.byteStream());
            Log.d(TAG, "Failed to save the file! " + e.getMessage());
        }
    }

    public void loadPdfFromFile(File pdfPath) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            pdfView.fromFile(pdfPath)
                    .linkHandler(new DefaultLinkHandler(pdfView))
                    .defaultPage(savedReadingPages)
                    .onPageChange(ActivityPDFView.this)
                    .enableAnnotationRendering(true)
                    .onLoad(ActivityPDFView.this)
                    .scrollHandle(new DefaultScrollHandle(ActivityPDFView.this))
                    .spacing(0) // in dp
                    .onPageError(ActivityPDFView.this)
                    .swipeHorizontal(true)
                    .pageSnap(true)
                    .autoSpacing(true)
                    .pageFling(true)
                    .pageFitPolicy(FitPolicy.WIDTH)
                    .onError(t -> {
                        loadFile(pdfUrl);
                        Log.d(TAG, "failed load pdf and try reload from url");
                    })
                    .nightMode(sharedPref.getIsDarkTheme())
                    .load();
            pdfView.setVisibility(View.VISIBLE);
        }, Constant.DELAY_TIME);
    }

    public void loadPdfFromUrl(InputStream inputStream) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            pdfView.fromStream(inputStream)
                    .linkHandler(new DefaultLinkHandler(pdfView))
                    .defaultPage(savedReadingPages)
                    .onPageChange(ActivityPDFView.this)
                    .enableAnnotationRendering(true)
                    .onLoad(ActivityPDFView.this)
                    .scrollHandle(new DefaultScrollHandle(ActivityPDFView.this))
                    .spacing(0) // in dp
                    .onPageError(ActivityPDFView.this)
                    .swipeHorizontal(true)
                    .pageSnap(true)
                    .autoSpacing(true)
                    .pageFling(true)
                    .pageFitPolicy(FitPolicy.WIDTH)
                    .nightMode(sharedPref.getIsDarkTheme())
                    .load();
            pdfView.setVisibility(View.VISIBLE);
        }, Constant.DELAY_TIME);
    }

    private void refreshBookmark() {
        flag_read_later = db.getBookmark(book.book_id) != null;
        if (flag_read_later) {
            btnBookmark.setImageResource(R.drawable.ic_bookmark_white);
        } else {
            btnBookmark.setImageResource(R.drawable.ic_bookmark_outline_white);
        }
    }

}