package com.book.mmbookstore.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.Expose;

import java.io.Serializable;

@Entity(tableName = "tbl_bookmark")
public class Book implements Serializable {

    @PrimaryKey
    public long id = -1;

    @Expose
    @ColumnInfo(name = "book_id")
    public String book_id = "";

    @Expose
    @ColumnInfo(name = "category_id")
    public String category_id = "";

    @Expose
    @ColumnInfo(name = "book_name")
    public String book_name = "";

    @Expose
    @ColumnInfo(name = "book_image")
    public String book_image = "";

    @Expose
    @ColumnInfo(name = "author")
    public String author = "";

    @Expose
    @ColumnInfo(name = "type")
    public String type = "";

    @Expose
    @ColumnInfo(name = "pdf_name")
    public String pdf_name = "";

    @Expose
    @ColumnInfo(name = "page_position")
    public int page_position = 0;

    @Expose
    @ColumnInfo(name = "saved_date")
    public long saved_date = -1;
}