package com.osfans.mcpdict;

import java.io.IOException;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class UserDatabase extends SQLiteOpenHelper {

    // STATIC VARIABLES AND METHODS

    private static final String DATABASE_NAME = "user";
    private static final int DATABASE_VERSION = 1;

    private static Context context;
    private static SQLiteDatabase db = null;

    public static void initialize(Context c) {
        if (db != null) return;
        context = c;
        db = new UserDatabase(context).getWritableDatabase();
    }

    public static String getDatabasePath() {
        return context.getDatabasePath(DATABASE_NAME).getAbsolutePath();
    }

    public static String getBackupPath() {
        return context.getExternalFilesDir(null) + "/" + DATABASE_NAME + ".db";
    }

    // "READ" OPERATIONS

    public static Cursor selectAllFavorites() {
        String query = "SELECT rowid AS _id, unicode, comment, " +
                       "STRFTIME('%Y/%m/%d', timestamp, 'localtime') AS local_timestamp " +
                       "FROM favorite ORDER BY timestamp DESC";
        return db.rawQuery(query, null);
    }

    // "WRITE" OPERATIONS

    public static void insertFavorite(int unicode, String comment) {
        ContentValues values = new ContentValues();
        values.put("unicode", Orthography.Hanzi.getHex(unicode));
        values.put("comment", comment);
        db.insert("favorite", null, values);
    }

    public static void updateFavorite(int unicode, String comment) {
        ContentValues values = new ContentValues();
        values.put("comment", comment);
        String[] args = {Orthography.Hanzi.getHex(unicode)};
        db.update("favorite", values, "unicode = ?", args);
    }

    public static void deleteFavorite(int unicode) {
        String[] args = {Orthography.Hanzi.getHex(unicode)};
        db.delete("favorite", "unicode = ?", args);
    }

    public static void deleteAllFavorites() {
        db.delete("favorite", null, null);
    }

    // EXPORTING AND IMPORTING

    public static void exportFavorites() throws IOException {
        FileUtils.copyFile(getDatabasePath(), getBackupPath());
    }

    public static int selectBackupFavoriteCount() {
        db.execSQL("ATTACH DATABASE '" + getBackupPath() + "' AS backup");
        String query = "SELECT rowid, unicode, comment, timestamp FROM backup.favorite";
        int count = db.rawQuery(query, null).getCount();
        db.execSQL("DETACH DATABASE backup");
        return count;
    }

    public static void importFavoritesOverwrite() throws IOException {
        FileUtils.copyFile(getBackupPath(), getDatabasePath());
    }

    public static void importFavoritesMix() {
        db.execSQL("ATTACH DATABASE '" + getBackupPath() + "' AS backup");
        db.execSQL("DELETE FROM favorite WHERE unicode IN (SELECT unicode FROM backup.favorite)");
        db.execSQL("INSERT INTO favorite(unicode, comment, timestamp) SELECT unicode, comment, timestamp FROM backup.favorite");
        db.execSQL("DETACH DATABASE backup");
    }

    public static void importFavoritesAppend() {
        db.execSQL("ATTACH DATABASE '" + getBackupPath() + "' AS backup");
        db.execSQL("DELETE FROM favorite WHERE unicode IN (SELECT unicode FROM backup.favorite)");
        db.execSQL("INSERT INTO favorite(unicode, comment) SELECT unicode, comment FROM backup.favorite");
        db.execSQL("DETACH DATABASE backup");
    }

    // NON-STATIC METHODS IMPLEMENTING THOSE OF THE ABSTRACT SUPER-CLASS

    public UserDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE favorite (" +
                   "    unicode TEXT UNIQUE NOT NULL," +
                   "    comment TEXT," +
                   "    timestamp REAL DEFAULT (JULIANDAY('now')) NOT NULL" +
                   ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
}