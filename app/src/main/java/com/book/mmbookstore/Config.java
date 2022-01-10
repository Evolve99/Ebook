package com.book.mmbookstore;

import com.book.mmbookstore.util.Constant;

public class Config {

    //your Server Key obtained from the admin panel
    public static final String SERVER_KEY = "WVVoU01HTklUVFpNZVRsMFlsZEtkbUl5ZEhwa1J6bDVXbE0xTkdWWWIzWllNa1ozWTBkNGNGa3lSakJoVnpsMVUxZFNabGt5T1hSTWJVcDJZakp6ZFdKWE1XbGlNamx5WXpOU2RtTnRWVDA9";

    //your Rest API Key obtained from the admin panel
    public static final String REST_API_KEY = "cda11PGvFU3E15HCJNBYW0f2mewZLtxD84hjRqScX79paguVAQ";

	//Books columns count, supported value : Constant.BOOKS_3_COLUMNS or Constant.BOOKS_2_COLUMNS
    public static final int DEFAULT_BOOK_COLUMNS_COUNT = Constant.BOOKS_3_COLUMNS;

    //RTL Direction e.g : for Arabic Language
    public static final boolean ENABLE_RTL_MODE = false;

    //GDPR EU Consent
    public static final boolean ENABLE_LEGACY_GDPR = true;

    //Copy ebook stories content
    public static final boolean ENABLE_TEXT_SELECTION = false;

}