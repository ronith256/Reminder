package com.lucario.reminder;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class LocalDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "remainder_db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "rem_table";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_DATA = "data";

    public LocalDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableQuery = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_DATA + " TEXT)";
        db.execSQL(createTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String dropTableQuery = "DROP TABLE IF EXISTS " + TABLE_NAME;
        db.execSQL(dropTableQuery);
        onCreate(db);
    }

    public void writeData(String data) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_DATA, data);

        db.insert(TABLE_NAME, null, contentValues);
        db.close();
    }

    public void removeData(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_NAME, COLUMN_ID + "=?", new String[] { String.valueOf(id) });
        db.close();
    }
}
