package com.book.mmbookstore.database.room;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.OnConflictStrategy;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.Expose;

import java.io.Serializable;

@Entity(tableName = "book_table")
public class BookEntity implements Serializable {

    @PrimaryKey
    public long id = -1;

    @Expose
    @ColumnInfo(name = "book_id")
    public String book_id = "";

    @Expose
    @ColumnInfo(name = "book_name")
    public String book_name = "";

    @Expose
    @ColumnInfo(name = "content_uri")
    public String content_uri = "";

}
