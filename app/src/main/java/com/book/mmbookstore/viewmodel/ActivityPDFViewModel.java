package com.book.mmbookstore.viewmodel;

import android.app.Application;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.book.mmbookstore.database.room.AppDatabase;
import com.book.mmbookstore.database.room.BookEntity;
import com.book.mmbookstore.database.room.DAO;
import com.book.mmbookstore.rest.ApiInterface;
import com.book.mmbookstore.rest.RestAdapter;
import com.book.mmbookstore.util.ExecutorTasks;
import com.book.mmbookstore.util.FileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityPDFViewModel extends AndroidViewModel {

    private MutableLiveData<Response<ResponseBody>> responseMutableLiveData;
    private DAO dao;
    private boolean isNewBook = true;
    private static final String TAG = "ActivityPDFViewModel";

    public ActivityPDFViewModel(@NonNull Application application) {
        super(application);
        responseMutableLiveData = new MutableLiveData();
        dao = AppDatabase.getDb(application).get();
    }

    public DAO getDao() {
        return dao;
    }

    public LiveData<Response<ResponseBody>> getResponseLiveData() {
        return responseMutableLiveData;
    }

    public boolean isNewBook(String book_id) {
        if (dao.isBookExists(book_id)) {
            isNewBook = false;
        } else {
            isNewBook = true;
        }
        return isNewBook;
    }

    public BookEntity getBookById(String book_id) {
        return dao.getBookById(book_id);
    }

    public void deleteBook(String book_id) {
        dao.deleteBook(book_id);
    }

    public void loadFile(String fileUrl) {
        ApiInterface apiInterface = RestAdapter.createAPI(fileUrl);
        Call<ResponseBody> call = apiInterface.downloadFileWithDynamicUrl(fileUrl);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                new ExecutorTasks() {
                    @Override
                    public void onPreExecute() {

                    }

                    @Override
                    public void doInBackground() {
                        responseMutableLiveData.postValue(response);
                    }

                    @Override
                    public void onPostExecute() {

                    }
                }.execute();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
                responseMutableLiveData.setValue(null);
                Log.e(TAG, t.getMessage());
            }
        });
    }

    private void alterDocument(Uri uri) {
        try {
            ParcelFileDescriptor pfd = getApplication().getContentResolver()
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
            viewModel.getDao().insertBook(book.book_id, book.book_name, uri.toString());

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
}
