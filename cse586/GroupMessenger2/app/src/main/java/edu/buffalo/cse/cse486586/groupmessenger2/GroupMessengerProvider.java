package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

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
//Anirudh Reddy
public class GroupMessengerProvider extends ContentProvider {


    private SQLiteDatabase dba;
    private static final int DB_VERSION = 1;
    private static final String DATABASE_NAME = "MYDATABASE";
    private static final String TABLE_NAME = "MSGDATA";
    private static final String CREATE_TABLE = "CREATE TABLE " +TABLE_NAME+ " (key TEXT NOT NULL PRIMARY KEY , value TEXT NOT NULL )";

    private static class DatabaseHelper extends SQLiteOpenHelper {

        private DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
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

        long rows_inserted = dba.insert(TABLE_NAME, null, values);

        if (rows_inserted > 0) {
            Log.v("insert", values.toString());
            Log.v("SQL", "DATA INSERTED");
        }


        return uri;
    }

    @Override
    public boolean onCreate() {
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
