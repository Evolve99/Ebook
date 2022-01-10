package com.book.mmbookstore.database.room;

import androidx.room.Dao;
import androidx.room.Query;

import com.book.mmbookstore.model.Book;

import java.util.List;

@Dao
public interface DAO {

    @Query("INSERT INTO tbl_bookmark (book_id, category_id, book_name, book_image, author, type, pdf_name, page_position, saved_date) VALUES (:book_id, :category_id, :book_name, :book_image, :author, :type, :pdf_name, :page_position, :save_date)")
    void insertBookmark(String book_id, String category_id, String book_name, String book_image, String author, String type, String pdf_name, int page_position, long save_date);

    @Query("UPDATE tbl_bookmark SET page_position = :page_position WHERE book_id = :book_id")
    void updateBookmark(String book_id, int page_position);

    @Query("DELETE FROM tbl_bookmark WHERE book_id = :book_id")
    void deleteBookmark(String book_id);

    @Query("DELETE FROM tbl_bookmark")
    void deleteAllBookmark();

    @Query("SELECT * FROM tbl_bookmark ORDER BY saved_date DESC")
    List<Book> getAllBookmark();

    @Query("SELECT COUNT(id) FROM tbl_bookmark")
    Integer getAllBookmarkCount();

    @Query("SELECT * FROM tbl_bookmark WHERE book_id = :book_id LIMIT 1")
    Book getBookmark(String book_id);

    @Query("INSERT INTO book_table (book_id, book_name, content_uri) VALUES (:book_id, :book_name, :content_uri)")
    void insertBook(String book_id, String book_name, String content_uri);

    @Query(("DELETE FROM book_table WHERE book_id = :book_id"))
    void deleteBook(String book_id);

    @Query("SELECT * FROM book_table WHERE book_id = :book_id LIMIT 1")
    BookEntity getBookById(String book_id);

    @Query("SELECT EXISTS(SELECT * FROM book_table WHERE book_id = :book_id)")
    Boolean isBookExists(String book_id);
}