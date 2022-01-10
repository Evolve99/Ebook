package com.book.mmbookstore.activity;

import static com.book.mmbookstore.util.Constant.BANNER_READING_PAGE;
import static com.book.mmbookstore.util.Constant.BOOK_PDF_UPLOAD;
import static com.book.mmbookstore.util.Constant.EXTRA_OBJECT;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.lifecycle.ViewModelProvider;

import com.book.mmbookstore.R;
import com.book.mmbookstore.database.pref.SharedPref;
import com.book.mmbookstore.database.room.BookEntity;
import com.book.mmbookstore.model.Book;
import com.book.mmbookstore.util.AdsManager;
import com.book.mmbookstore.util.Constant;
import com.book.mmbookstore.util.ExecutorTasks;
import com.book.mmbookstore.util.FileUtil;
import com.book.mmbookstore.util.InputFilterIntRange;
import com.book.mmbookstore.util.Tools;
import com.book.mmbookstore.viewmodel.ActivityPDFViewModel;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import okhttp3.ResponseBody;

public class ActivityPDFView extends AppCompatActivity implements OnPageChangeListener, OnLoadCompleteListener, OnPageErrorListener {

    // ViewModel
    private ActivityPDFViewModel viewModel;

    private Uri uri;
    private final Handler handler = new Handler();

    private static final String TAG = "ActivityPdfView";
    private PDFView pdfView;
    Toolbar toolbar;
    boolean flag = true;
    AdsManager adsManager;
    InputStream inputStream = null;
    private ShimmerFrameLayout lytShimmer;
    CoordinatorLayout parentView;
    TextView txtPage;
    AppBarLayout lytTop;
    LinearLayout lytBottom;
    Tools tools;
    View lytFailed;
    Button btnRetry;
    String fileExtension = "pdf";
    TextView txtPercentage;

    SharedPref sharedPref;
    ImageButton btnBookmark;
    int savedReadingPages = 0;
    int lastReadPage = 0;
    private boolean flag_read_later;
    Book book;
    String pdfUrl;
    ResponseBody mResponseBody;

    ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Log.d(TAG, "mStartForResult: RESULT_OK");
                    if (result.getData() != null) {
                        uri = result.getData().getData();
                        int takeFlags = getIntent().getFlags();
                        takeFlags &= (Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        getContentResolver().takePersistableUriPermission(uri, takeFlags);
                        Log.d(TAG, "mStartForResult: uri: " + uri);
                        viewModel.loadFile(pdfUrl);
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

        viewModel = new ViewModelProvider(this).get(ActivityPDFViewModel.class);

        subscribeObservers();

        sharedPref = new SharedPref(this);
        tools = new Tools(this);
        tools.setNavigation();

        book = (Book) getIntent().getSerializableExtra(EXTRA_OBJECT);

        if (book.type.equals(BOOK_PDF_UPLOAD)) {
            pdfUrl = sharedPref.getApiUrl() + "/upload/pdf/" + book.pdf_name;
        } else {
            pdfUrl = book.pdf_name;
        }

        if (viewModel.getDao().getBookmark(book.book_id) != null) {
            Book page = viewModel.getDao().getBookmark(book.book_id);
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

        if (!viewModel.isBookExists(book.book_id)) {
            Log.d(TAG, "file is not exist and load from url first");
            createFile();
        } else {
            try {
                BookEntity bookEntity = viewModel.getBookById(book.book_id);
                Uri bookUri = Uri.parse(bookEntity.content_uri);
                Log.d(TAG, "onCreate: bookUri: " + bookUri);

                if (FileUtil.checkUriResource(this, bookUri)) {
                    File myFile = FileUtil.from(this, bookUri);
                    Log.d(TAG, "file is exist + " + myFile);
                    loadPdfFromFile(myFile);
                } else {
                    Log.d(TAG, "file is not exist and load from url first");
                    viewModel.deleteBook(bookEntity.book_id);
                    createFile();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        refreshBookmark();
        tools.setupToolbar(this, toolbar, "", true);
        setupToolbar();
    }


    private void subscribeObservers() {
        viewModel.getResponseLiveData().observe(this, response -> {
            if (response == null) {
                showFailedView();
            }
            if (response.isSuccessful()) {
                Log.d(TAG, "Got the body for the file");
                if (response.body() != null) {
                    mResponseBody = response.body();
                    new ExecutorTasks() {
                        @Override
                        public void onPreExecute() {

                        }

                        @Override
                        public void doInBackground() {
                            alterDocument(uri, mResponseBody);
                        }

                        @Override
                        public void onPostExecute() {

                        }
                    }.execute();

                }
            } else {
                showFailedView();
                Log.d(TAG, "Connection failed " + response.errorBody());
            }
        });
    }

    private void createFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, book.book_name + "." + fileExtension);

        mStartForResult.launch(intent);
    }

    private void alterDocument(Uri uri, ResponseBody body) {
        try {
            ParcelFileDescriptor pfd = getApplication().getContentResolver()
                    .openFileDescriptor(uri, "w");
            FileOutputStream fileOutputStream =
                    new FileOutputStream(pfd.getFileDescriptor());

            inputStream = body.byteStream();
            byte[] data = new byte[4096];
            int count;
            int progress = 0;
            while ((count = inputStream.read(data)) != -1) {
                fileOutputStream.write(data, 0, count);
                progress += count;
                int finalProgress = progress;
                handler.post(() -> txtPercentage.setText((int) ((finalProgress * 100) / body.contentLength()) + "%"));
                Log.d(TAG, "Progress: " + progress + "/" + body.contentLength() + " >>>> "
                        + (float) progress / body.contentLength());
            }
            fileOutputStream.close();
            pfd.close();
            viewModel.getDao().insertBook(book.book_id, book.book_name, uri.toString());

            File file = FileUtil.from(getApplication(), uri);
            Log.d(TAG, "alterDocument: File:::::::: uti" + file.getPath() + " file - " + file + ": " + file.exists());
            loadPdfFromFile(file);
            Log.d(TAG, "File saved successfully!");
        } catch (IOException e) {
            e.printStackTrace();
            loadPdfFromUrl(body.byteStream());
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
        flag_read_later = viewModel.getDao().getBookmark(book.book_id) != null;
        if (flag_read_later) {
            viewModel.getDao().updateBookmark(book.book_id, lastReadPage);
            Log.d(TAG, "update last page bookmarked");
        }
    }

    private void onBookmarkPage(int pagePosition) {
        btnBookmark.setOnClickListener(v -> {
            String str;
            if (flag_read_later) {
                viewModel.getDao().deleteBookmark(book.book_id);
                str = getString(R.string.bookmark_removed);
            } else {
                viewModel.getDao().insertBookmark(
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
            createFile();
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        lytShimmer.stopShimmer();
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
                        createFile();
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
        flag_read_later = viewModel.getDao().getBookmark(book.book_id) != null;
        if (flag_read_later) {
            btnBookmark.setImageResource(R.drawable.ic_bookmark_white);
        } else {
            btnBookmark.setImageResource(R.drawable.ic_bookmark_outline_white);
        }
    }
}