package com.book.mmbookstore.rest;

import com.book.mmbookstore.callback.CallbackBook;
import com.book.mmbookstore.callback.CallbackCategory;
import com.book.mmbookstore.callback.CallbackSetting;
import com.book.mmbookstore.callback.CallbackStory;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

public interface ApiInterface {

    String CACHE = "Cache-Control: max-age=0";
    String AGENT = "Data-Agent: Android Ebook App";

    @Headers({CACHE, AGENT})
    @GET("api.php?books")
    Call<CallbackBook> getBooks(
            @Query("page") int page,
            @Query("count") int count,
            @Query("sort") String sort,
            @Query("order") String order,
            @Query("api_key") String api_key
    );

    @Headers({CACHE, AGENT})
    @GET("api.php?categories")
    Call<CallbackCategory> getCategories(
            @Query("page") int page,
            @Query("count") int count,
            @Query("api_key") String api_key
    );

    @Headers({CACHE, AGENT})
    @GET("api.php?category_details")
    Call<CallbackBook> getCategoryDetails(
            @Query("category_id") String category_id,
            @Query("page") int page,
            @Query("count") int count,
            @Query("sort") String sort,
            @Query("order") String order,
            @Query("api_key") String api_key
    );

    @Headers({CACHE, AGENT})
    @GET("api.php?stories")
    Call<CallbackStory> getStories(
            @Query("book_id") String book_id,
            @Query("page") int page,
            @Query("count") int count,
            @Query("sort") String sort,
            @Query("order") String order,
            @Query("api_key") String api_key
    );

    @Headers({CACHE, AGENT})
    @GET("api.php?search")
    Call<CallbackBook> getSearch(
            @Query("page") int page,
            @Query("count") int count,
            @Query("q") String q,
            @Query("api_key") String api_key
    );

    @Headers({CACHE, AGENT})
    @GET("api.php?settings")
    Call<CallbackSetting> getSettings(
            @Query("api_key") String api_key
    );

    @Streaming
    @GET
    Call<ResponseBody> downloadFileWithDynamicUrl(@Url String fileUrl);

}
