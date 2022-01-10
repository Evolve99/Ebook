package com.book.mmbookstore.viewmodel;

import android.app.Application;
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

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityPDFViewModel extends AndroidViewModel {

    private MutableLiveData<Response<ResponseBody>> responseMutableLiveData;
    private DAO dao;
    private boolean isBookExists;
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

    public boolean isBookExists(String book_id) {
        isBookExists = dao.isBookExists(book_id);
        return isBookExists;
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
}
