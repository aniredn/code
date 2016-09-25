package edu.buffalo.cse.cse486586.simpledht;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SimpleDhtProvider extends ContentProvider {

     /*Creation of SQLite DB referred from
    http://developer.android.com/guide/topics/data/data-storage.html#db
    http://stackoverflow.com/questions/3037767/create-sqlite-database-in-android
    http://developer.android.com/training/basics/data-storage/databases.html
    */

    private SQLiteDatabase dba;
    private static final int DB_VERSION = 1;
    private static final String DATABASE_NAME = "MYDB";
    private static final String TABLE_NAME = "CHORDDATA";
    private static final String CREATE_TABLE = "CREATE TABLE " +TABLE_NAME+ " (key TEXT NOT NULL, value TEXT NOT NULL )";

    private static Uri mUri;

    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final int SERVER_PORT = 10000;

    LinkedList ll = new LinkedList();

    class Port{
        String port;
        String port_hash;
        String next_port;
        String next_port_hash;

        String prev_port;
        String prev_port_hash;
        public String getPrev_port_hash() {
            return prev_port_hash;
        }

        public void setPrev_port_hash(String prev_port_hash) {
            this.prev_port_hash = prev_port_hash;
        }

        public String getNext_port_hash() {
            return next_port_hash;
        }

        public void setNext_port_hash(String next_port_hash) {
            this.next_port_hash = next_port_hash;
        }

        public String getNext_port() {
            return next_port;
        }

        public void setNext_port(String next_port) {
            this.next_port = next_port;
        }

        public String getPrev_port() {
            return prev_port;
        }

        public void setPrev_port(String prev_port) {
            this.prev_port = prev_port;
        }

        public String getPort() {
            return port;
        }

        public void setPort(String port) {
            this.port = port;
        }

        public String getPort_hash() {
            return port_hash;
        }

        public void setPort_hash(String port_hash) {
            this.port_hash = port_hash;
        }
    }

    class Entry{
        String key, value;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    Port LOCAL_PORT = new Port();

    ArrayList<Port> connected_ports = new ArrayList<Port>();

    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";
    private static final String JOIN_TEXT = "JOIN";
    private static final String JOIN_COMP = "JOIN_COMP";
    private static final String SEND_TO_NEXT = "SEND_TO_NEXT";
    private static final String QUERY_NEXT = "QUERY_NEXT";
    private static final String QUERY_ALL = "QUERY_ALL";
    static boolean JOIN_COMPLETE = false;

    @Override
    public boolean onCreate() {

        ll.add(REMOTE_PORT0);
        ll.add(REMOTE_PORT1);
        ll.add(REMOTE_PORT2);
        ll.add(REMOTE_PORT3);
        ll.add(REMOTE_PORT4);

        Context context = getContext();
        DatabaseHelper dbh = new DatabaseHelper(context);
        dba = dbh.getWritableDatabase();

        mUri = buildUri("content", "edu.buffalo.cse.cse486586.simpledht.provider");

        TelephonyManager tel = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        LOCAL_PORT.setPort(myPort);
        Log.v("LOCAL PORT", LOCAL_PORT.getPort());

        try {
            LOCAL_PORT.setPort_hash(genHash(Integer.toString(Integer.parseInt(LOCAL_PORT.getPort()) / 2)));
        }catch(NoSuchAlgorithmException n){
            n.printStackTrace();
        }

        try {

            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, JOIN_TEXT);

        } catch (IOException e) {
            Log.e("SERVER", "Cant Create Server");
            return false;
        }

        //Check if all nodes have joined
        if (LOCAL_PORT.getPort().equals(REMOTE_PORT0)) {

            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, JOIN_COMP);

        }


        if(dba == null)
            return false;
        else
            return true;
    }


    private static final String[] COLUMNS ={KEY_FIELD,VALUE_FIELD};

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        String key_value = values.get(KEY_FIELD).toString();
        long rows_inserted =0;


        if (LOCAL_PORT.getNext_port() !=null && LOCAL_PORT.getPrev_port_hash() !=null) {
            try {
                String key_value_hash = genHash(key_value);

                //Normal Case
                if ((LOCAL_PORT.getPort_hash().compareTo(LOCAL_PORT.getNext_port_hash()) < 0) &&
                        (LOCAL_PORT.getPort_hash().compareTo(LOCAL_PORT.getPrev_port_hash()) > 0)) {

                    if ((key_value_hash.compareTo(LOCAL_PORT.getPort_hash()) < 0) &&
                            (key_value_hash.compareTo(LOCAL_PORT.prev_port_hash) > 0)) {
                        rows_inserted = dba.insert(TABLE_NAME, null, values);
                        Log.v("DB INSERT", "DATA INSERTED " + values.toString());
                    }
                    else {
                        String key_send = values.get(KEY_FIELD).toString();
                        String value_send = values.get(VALUE_FIELD).toString();
                        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, SEND_TO_NEXT, key_send, value_send);
                    }

                }
                //For smallest avd
                else if ((LOCAL_PORT.getPort_hash().compareTo(LOCAL_PORT.getNext_port_hash()) < 0) &&
                        (LOCAL_PORT.getPort_hash().compareTo(LOCAL_PORT.getPrev_port_hash()) < 0)) {

                    Log.v("DB INSERT","SMALLEST AVD");
                    if ((key_value_hash.compareTo(LOCAL_PORT.getPort_hash())) < 0 || (key_value_hash.compareTo(LOCAL_PORT.getPrev_port_hash()) > 0)) {
                        rows_inserted = dba.insert(TABLE_NAME, null, values);
                        Log.v("DB INSERT", "DATA INSERTED " + values.toString());
                    }
                    else {
                        String key_send = values.get(KEY_FIELD).toString();
                        String value_send = values.get(VALUE_FIELD).toString();
                        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, SEND_TO_NEXT, key_send, value_send);
                    }


                }
                //for largest avd
                else if ((LOCAL_PORT.getPort_hash().compareTo(LOCAL_PORT.getNext_port_hash()) > 0) &&
                        (LOCAL_PORT.getPort_hash().compareTo(LOCAL_PORT.getPrev_port_hash()) > 0)) {


                    Log.v("DB INSERT","LARGEST AVD");
                    if ((key_value_hash.compareTo(LOCAL_PORT.getPort_hash()) < 0) &&
                            (key_value_hash.compareTo(LOCAL_PORT.prev_port_hash) > 0)) {
                        rows_inserted = dba.insert(TABLE_NAME, null, values);
                        Log.v("DB INSERT", "DATA INSERTED " + values.toString());
                    }
                    else {
                        String key_send = values.get(KEY_FIELD).toString();
                        String value_send = values.get(VALUE_FIELD).toString();
                        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, SEND_TO_NEXT, key_send, value_send);
                    }

                }
                else {
                    rows_inserted = -1;
                    Log.v("DB INSERT", "CASE MAY NOT HAVE BEEN HANDLED");
                }

            } catch (NoSuchAlgorithmException n) {
                n.printStackTrace();
            }
        }


        else if (LOCAL_PORT.getNext_port() == null)
        {
            rows_inserted = dba.insert(TABLE_NAME, null, values);
            if (rows_inserted > 0) {
                Log.v("DB INSERT", "DATA INSERTED " + values.toString());
            }
            return uri;
        }


        return uri;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        if (selection.equals("*")){

            if (LOCAL_PORT.getNext_port() == null) {
                DatabaseHelper dbh = new DatabaseHelper(getContext());
                SQLiteDatabase sqLiteDatabase = dbh.getReadableDatabase();

                Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM " + TABLE_NAME, null);

                if (cursor == null) {
                    Log.e("DB QUERY", "QUERY FAILED " + selection);
                }
                return cursor;

            }
            else {
                Cursor cursor = null;

                try {
                    cursor = new QueryTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, QUERY_ALL, selection).get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
                return cursor;
            }

        }

        else if (selection.equals("@")){

            DatabaseHelper dbh = new DatabaseHelper(getContext());
            SQLiteDatabase sqLiteDatabase = dbh.getReadableDatabase();

            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM "+TABLE_NAME,null);

            if(cursor == null)
            {
                Log.e("DB QUERY", "QUERY FAILED "+selection);
            }

            return cursor;
        }

        else {
            if (LOCAL_PORT.getNext_port() != null && LOCAL_PORT.getPrev_port_hash() != null) {
                try {
                    String key_value_hash = genHash(selection);

                    //Normal Case
                    if ((LOCAL_PORT.getPort_hash().compareTo(LOCAL_PORT.getNext_port_hash()) < 0) &&
                            (LOCAL_PORT.getPort_hash().compareTo(LOCAL_PORT.getPrev_port_hash()) > 0)) {

                        if ((key_value_hash.compareTo(LOCAL_PORT.getPort_hash()) < 0) &&
                                (key_value_hash.compareTo(LOCAL_PORT.prev_port_hash) > 0)) {

                            SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
                            queryBuilder.setTables(TABLE_NAME);
                            Cursor cursor = queryBuilder.query(dba, projection, "key = '" + selection + "'", selectionArgs, sortOrder, null, null);
                            return cursor;
                        } else {

                            Log.v("DB QUERY","QUERY FORWARDED");
                            try {
                                Cursor cursor = new QueryTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, QUERY_NEXT,selection).get();
                                return cursor;
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                return null;
                            } catch (ExecutionException e) {
                                e.printStackTrace();
                                return null;
                            }

                        }

                    }
                    //For smallest avd
                    else if ((LOCAL_PORT.getPort_hash().compareTo(LOCAL_PORT.getNext_port_hash()) < 0) &&
                            (LOCAL_PORT.getPort_hash().compareTo(LOCAL_PORT.getPrev_port_hash()) < 0)) {

                        //Log.v("DB QUERY", "SMALLEST AVD");
                        if ((key_value_hash.compareTo(LOCAL_PORT.getPort_hash())) < 0 || (key_value_hash.compareTo(LOCAL_PORT.getPrev_port_hash()) > 0)) {

                            SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
                            queryBuilder.setTables(TABLE_NAME);
                            Cursor cursor = queryBuilder.query(dba, projection, "key = '" + selection + "'", selectionArgs, sortOrder, null, null);
                            return cursor;
                        } else {

                            Log.v("DB QUERY","QUERY FORWARDED");
                            try {
                                Cursor cursor = new QueryTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,QUERY_NEXT,selection).get();
                                return cursor;
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                return null;
                            } catch (ExecutionException e) {
                                e.printStackTrace();
                                return null;
                            }

                        }
                    }
                    //for largest avd
                    else if ((LOCAL_PORT.getPort_hash().compareTo(LOCAL_PORT.getNext_port_hash()) > 0) &&
                            (LOCAL_PORT.getPort_hash().compareTo(LOCAL_PORT.getPrev_port_hash()) > 0)) {


                        //Log.v("DB INSERT", "LARGEST AVD");
                        if ((key_value_hash.compareTo(LOCAL_PORT.getPort_hash()) < 0) &&
                                (key_value_hash.compareTo(LOCAL_PORT.prev_port_hash) > 0)) {

                            SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
                            queryBuilder.setTables(TABLE_NAME);
                            Cursor cursor = queryBuilder.query(dba, projection, "key = '" + selection + "'", selectionArgs, sortOrder, null, null);
                            return cursor;
                        } else {

                            Log.v("DB QUERY","QUERY FORWARDED");
                            try {
                                Cursor cursor = new QueryTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,QUERY_NEXT,selection).get();
                                return cursor;
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                return null;
                            } catch (ExecutionException e) {
                                e.printStackTrace();
                                return null;
                            }

                        }

                    } else {

                        Log.v("DB QUERY", "CASE MAY NOT HAVE BEEN HANDLED");

                        return null;
                    }

                } catch (NoSuchAlgorithmException n) {
                    n.printStackTrace();
                    return null;
                }

            }
            //Only 1 avd case
            else {
                /*Referred from
                http://developer.android.com/reference/android/database/sqlite/SQLiteQueryBuilder.html
                */

                SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
                queryBuilder.setTables(TABLE_NAME);
                Cursor cursor = queryBuilder.query(dba, projection, "key = '" + selection + "'", selectionArgs, sortOrder, null, null);


                if (cursor == null) {
                    Log.e("DB QUERY", "QUERY FAILED");
                }

                Log.v("DB QUERY", selection);

                return cursor;
            }
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        Log.v("IN DELETE", "HERE");

        int rows_deleted = -1;
        if (selection.equals("*")){
            rows_deleted = dba.delete(TABLE_NAME,null,null);
            // send message to all avds to delete
            Log.v("DB DELETE", "* DELETE");

        }
        else if (selection.equals("@")){
            rows_deleted = dba.delete(TABLE_NAME,null,null);
            Log.v("DB DELETE", "@ DELETE");
        }
        else {
            rows_deleted = dba.delete(TABLE_NAME, "key = '"+selection+"'", null);
        }


        if (rows_deleted>0){
            Log.v("DB DELETE","ROWS DELETED "+Integer.toString(rows_deleted));
        }
        else {
            Log.v("DB DELETE", "Deletion Failed" + Integer.toString(rows_deleted));
        }

        return rows_deleted;
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {
        @Override
        protected Void doInBackground(ServerSocket... params) {
            ServerSocket serverSocket = params[0];
            String token;


            try{
                while(true){
                    Socket clientSocket = serverSocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    //ObjectOutputStream obj_out = new ObjectOutputStream(clientSocket.getOutputStream());

                    token = in.readLine();

                    Port client_port = new Port();
                    if (token.equals(JOIN_TEXT)){

                        token = "";
                        token = in.readLine();
                        client_port.setPort(token);
                        Log.v("TOKEN",token+" "+client_port.getPort());
                        try {
                            client_port.setPort_hash(genHash(Integer.toString(Integer.parseInt(client_port.getPort()) / 2)));
                            connected_ports.add(client_port);
                        }catch (NoSuchAlgorithmException e){
                            e.printStackTrace();
                        }

                        // Sorting the list of ports
                        if (connected_ports.size()>=2) {
                            for (int i = 0; i < connected_ports.size(); i++) {
                                for (int j = i; j > 0; j--) {
                                    Port port1, port2;

                                    port1 = connected_ports.get(j-1);
                                    port2 = connected_ports.get(j);

                                    if (port1.getPort_hash().compareTo(port2.getPort_hash())>0){
                                        connected_ports.set(j-1,port2);
                                        connected_ports.set(j,port1);

                                    }

                                }
                            }

                            for (int i = 0;i< connected_ports.size();i++){
                                Log.v("BUFFER", connected_ports.get(i).getPort());
                            }


                        }//finish sorting

                    }//finish Joining

                    if (token.equals(JOIN_COMP)){

                        String next_port = in.readLine();
                        String prev_port = in.readLine();

                        LOCAL_PORT.setNext_port(next_port);
                        LOCAL_PORT.setPrev_port(prev_port);



                        try {
                            LOCAL_PORT.setNext_port_hash(genHash(Integer.toString(Integer.parseInt(LOCAL_PORT.getNext_port()) / 2)));
                        } catch (NoSuchAlgorithmException e) {
                            e.printStackTrace();
                        }

                        try {
                            LOCAL_PORT.setPrev_port_hash(genHash(Integer.toString(Integer.parseInt(LOCAL_PORT.getPrev_port()) / 2)));
                        } catch (NoSuchAlgorithmException e) {
                            e.printStackTrace();
                        }

                        Log.v("LOCAL PORT",LOCAL_PORT.getPort()+ " "+LOCAL_PORT.getPort_hash());
                        Log.v("NEXT PORT",LOCAL_PORT.getNext_port() + " "+ LOCAL_PORT.getNext_port_hash());
                        Log.v("PREV PORT", LOCAL_PORT.getPrev_port() + " "+ LOCAL_PORT.getPrev_port_hash());


                    }

                    if (token.equals(SEND_TO_NEXT)){
                        String key_rec = in.readLine();
                        String value_rec = in.readLine();

                        ContentValues contentValues = new ContentValues();
                        contentValues.put(KEY_FIELD,key_rec);
                        contentValues.put(VALUE_FIELD, value_rec);

                        Uri uri = null;
                        insert(uri,contentValues);
                    }

                    if (token.equals(QUERY_NEXT)){
                        String key_rec = in.readLine();

                        Log.v("DB QUERY SERVER", "QUERY REQUEST RECEIVED FROM PREV AVD");
                        Uri uri = mUri;
                        Cursor cursor = query(uri, null, key_rec, null, null);


                        if (cursor.moveToFirst()) {
                            String key_send = cursor.getString(cursor.getColumnIndex(KEY_FIELD));
                            String value_send = cursor.getString(cursor.getColumnIndex(VALUE_FIELD));

                            Entry entry_send = new Entry();
                            entry_send.setKey(key_send);
                            entry_send.setValue(value_send);

                            String send_packet = entry_send.getKey()+","+entry_send.getValue();
                            out.println(send_packet);
                            //Log.v("DB SERVER QUERY", "SENT STRING BACK TO PREV");

                        }
                    }

                    if (token.equals(QUERY_ALL)){

                        //Log.v("DB QUERY SERVER", "QUERY REQUEST RECEIVED FROM PREV AVD");
                        Uri uri = mUri;
                        String key_rec = "@";
                        Cursor cursor = query(uri, null, key_rec, null, null);

                        String send_packet =new String();

                        if (cursor.moveToFirst()){
                            while(!cursor.isAfterLast()){

                                //Log.v("HERE","HERE");
                                String key_send = cursor.getString(cursor.getColumnIndex(KEY_FIELD));
                                String value_send = cursor.getString(cursor.getColumnIndex(VALUE_FIELD));

                                if (send_packet.isEmpty()) {
                                    send_packet = key_send + "," + value_send+";" ;

                                }
                                else {
                                    send_packet =  key_send + "," + value_send+";"+send_packet;
                                }
                                cursor.moveToNext();
                            }
                        }

                        Log.v("DB QUERY ALL SERVER", "SENT PACKET" + send_packet);

                        out.println(send_packet);

                    }


                    clientSocket.close();
                }//finish while
            }catch (IOException e){
                e.printStackTrace();
            }

            return null;
        }
    }

    private class ClientTask extends AsyncTask<String,Void,Void>{
        @Override
        protected Void doInBackground(String... params) {

            if (params[0].equals(JOIN_TEXT)) {
                String[] remotePort = new String[ll.size()];
                ll.toArray(remotePort);
                try {

                    Socket ClientSocket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remotePort[0]));
                    PrintWriter out = new PrintWriter(ClientSocket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(ClientSocket.getInputStream()));

                    out.println(JOIN_TEXT);
                    out.flush();
                    out.println(LOCAL_PORT.getPort());
                    out.flush();

                    ClientSocket.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            if (params[0].equals(JOIN_COMP)){
                String[] remotePort = new String[ll.size()];
                ll.toArray(remotePort);

                try {
                    Thread.sleep(6000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    for (int i = 0; i < connected_ports.size(); i++) {

                        Socket ClientSocket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(connected_ports.get(i).getPort()));
                        PrintWriter out = new PrintWriter(ClientSocket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(ClientSocket.getInputStream()));

                        out.println(JOIN_COMP);
                        out.flush();
                        String next,prev;

                        //Set next and prev (including corner cases)
                        if (i==0){
                            next = connected_ports.get(i+1).getPort();
                            prev = connected_ports.get(connected_ports.size()-1).getPort();
                        }
                        else if (i == connected_ports.size()-1){
                            next = connected_ports.get(0).getPort();
                            prev = connected_ports.get(i-1).getPort();
                        }
                        else{
                            next = connected_ports.get(i+1).getPort();
                            prev = connected_ports.get(i-1).getPort();
                        }

                        out.println(next);
                        out.flush();
                        out.println(prev);
                        out.flush();
                        ClientSocket.close();
                    }
                }catch (IOException e){
                    e.printStackTrace();
                }
            }

            if (params[0].equals(SEND_TO_NEXT)){

                String key_send = params[1];
                String value_send = params[2];

                try {

                    Socket ClientSocket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(LOCAL_PORT.getNext_port()));
                    PrintWriter out = new PrintWriter(ClientSocket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(ClientSocket.getInputStream()));


                    out.println(SEND_TO_NEXT);
                    out.flush();
                    out.println(key_send);
                    out.flush();
                    out.println(value_send);
                    out.flush();

                    ClientSocket.close();
                }catch (IOException e) {
                    e.printStackTrace();
                }
            }


            return null;
        }
    }

    private class QueryTask extends AsyncTask<String,Void,Cursor> {
        @Override
        protected Cursor doInBackground(String... params) {
            Cursor cursor = null;

            //Log.v("DB QUERY CLIENT", "START");
            if (params[0].equals(QUERY_NEXT)) {
                try {
                    String key_send = params[1];
                    Entry entry_rec = new Entry();
                    Socket ClientSocket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(LOCAL_PORT.getNext_port()));
                    //Log.v("DB QUERY CLIENT", "Connected");
                    PrintWriter out = new PrintWriter(ClientSocket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(ClientSocket.getInputStream()));

                    out.println(QUERY_NEXT);
                    out.println(key_send);
                    //Log.v("DB QUERY CLIENT", "sent key");

                    String temp = in.readLine();

                    String[] tokens = temp.split("[,]");
                    entry_rec.setKey(tokens[0]);
                    entry_rec.setValue(tokens[1]);


                    // Creation of matrix cursor http://stackoverflow.com/questions/18290864/create-a-cursor-from-hardcoded-array-instead-of-db
                    MatrixCursor matrixCursor = new MatrixCursor(COLUMNS);
                    matrixCursor.addRow(new Object[] {entry_rec.getKey(),entry_rec.getValue()});

                    cursor = matrixCursor;

                    //Log.v("DB QUERY CLIENT","CURSOR RECEIVED");
                    ClientSocket.close();
                }
                catch (IOException e){
                    e.printStackTrace();
                }
            }

            if (params[0]==QUERY_ALL){
                String[] remotePort = new String[ll.size()];
                ll.toArray(remotePort);
                MatrixCursor matrixCursor = new MatrixCursor(COLUMNS);
                try {
                    for (int i = 0;i<remotePort.length;i++) {
                        Socket ClientSocket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remotePort[i]));
                        PrintWriter out = new PrintWriter(ClientSocket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(ClientSocket.getInputStream()));


                        out.println(QUERY_ALL);
                        out.flush();

                        String temp = in.readLine();

                        if (temp !=null) {
                            String[] tokens = temp.split("[;]");

                            for (int j = 0; j < tokens.length; j++) {

                                if (!tokens[j].isEmpty()) {
                                    String[] key_value = tokens[j].split("[,]");
                                    matrixCursor.addRow(new Object[]{key_value[0], key_value[1]});
                                }
                            }
                            ClientSocket.close();

                            cursor = matrixCursor;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return cursor;
        }

        @Override
        protected void onPostExecute(Cursor cursor) {
            super.onPostExecute(cursor);
        }
    }

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {

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
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

}

