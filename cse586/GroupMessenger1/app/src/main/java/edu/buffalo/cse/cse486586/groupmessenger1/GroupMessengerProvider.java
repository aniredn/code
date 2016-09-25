package edu.buffalo.cse.cse486586.groupmessenger1;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;
import java.sql.SQLException;

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 * 
 * Please read:
 * 
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 * 
 * before you start to get yourself familiarized with ContentProvider.
 * 
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 * 
 * @author stevko
 *
 */
public class GroupMessengerProvider extends ContentProvider {

    /*Creation of SQLite DB referred from
    http://developer.android.com/guide/topics/data/data-storage.html#db
    http://stackoverflow.com/questions/3037767/create-sqlite-database-in-android
    http://developer.android.com/training/basics/data-storage/databases.html
    */
    private SQLiteDatabase dba;
    private static final int DB_VERSION = 1;
    private static final String DATABASE_NAME = "MYDATABASE";
    private static final String TABLE_NAME = "MSGDATA";
    private static final String CREATE_TABLE = "CREATE TABLE " +TABLE_NAME+ " (key TEXT NOT NULL, value TEXT NOT NULL )";

    private static class DatabaseHelper extends SQLiteOpenHelper{

        private DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            //db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            db.execSQL(CREATE_TABLE);
            Log.v("Database", "Created");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }
    }


    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        /*
         * TODO: You need to implement this method. Note that values will have two columns (a key
         * column and a value column) and one row that contains the actual (key, value) pair to be
         * inserted.
         * 
         * For actual storage, you can use any option. If you know how to use SQL, then you can use
         * SQLite. But this is not a requirement. You can use other storage options, such as the
         * internal storage option that we used in PA1. If you want to use that option, please
         * take a look at the code for PA1.
         */

           long rows_inserted = dba.insert(TABLE_NAME, null, values);

           if (rows_inserted > 0) {
               Log.v("insert", values.toString());
               Log.v("SQL", "DATA INSERTED");
           }

        return uri;
    }

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
        Context context = getContext();
        DatabaseHelper dbh = new DatabaseHelper(context);

        dba = dbh.getWritableDatabase();
        if(dba == null)
            return false;
        else
            return true;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        /*
         * TODO: You need to implement this method. Note that you need to return a Cursor object
         * with the right format. If the formatting is not correct, then it is not going to work.
         *
         * If you use SQLite, whatever is returned from SQLite is a Cursor object. However, you
         * still need to be careful because the formatting might still be incorrect.
         *
         * If you use a file storage option, then it is your job to build a Cursor * object. I
         * recommend building a MatrixCursor described at:
         * http://developer.android.com/reference/android/database/MatrixCursor.html
         */


        /*Referred from
         http://developer.android.com/reference/android/database/sqlite/SQLiteQueryBuilder.html
        */

        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(TABLE_NAME);


        Cursor cursor = queryBuilder.query(
                dba,
                projection,
                "key = '"+selection+"'",
                selectionArgs,
                sortOrder,
                null,
                null);


        if(cursor == null)
        {
            Log.e("SQL QUERY", "QUERY FAILED");
        }

        Log.v("query", selection);
        return cursor;
    }
}
