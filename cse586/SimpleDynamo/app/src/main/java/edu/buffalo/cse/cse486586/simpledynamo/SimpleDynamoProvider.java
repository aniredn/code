package edu.buffalo.cse.cse486586.simpledynamo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
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

//Anirudh Reddy

public class SimpleDynamoProvider extends ContentProvider {

     /*Creation of SQLite DB referred from
    http://developer.android.com/guide/topics/data/data-storage.html#db
    http://stackoverflow.com/questions/3037767/create-sqlite-database-in-android
    http://developer.android.com/training/basics/data-storage/databases.html
    */

    private SQLiteDatabase dba;
    private static final int DB_VERSION = 1;
    private static final String DATABASE_NAME = "MYDB";
    private static final String TABLE_NAME = "DYNAMO_DATA";
    private static final String CREATE_TABLE = "CREATE TABLE " +TABLE_NAME+ " (key TEXT NOT NULL PRIMARY KEY, value TEXT NOT NULL )";
    private static Uri mUri;
    private static boolean RECOVERY = false;

    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final int SERVER_PORT = 10000;

    LinkedList<String> ll = new LinkedList<String>();

    class Port{
        String port;
        String port_hash;
        String next_port;
        String next_port_hash;
        String next_next_port;

        String prev_port;
        String prev_port_hash;
        String prev_prev_port;


        public String getPrev_prev_port() {
            return prev_prev_port;
        }

        public void setPrev_prev_port(String prev_prev_port) {
            this.prev_prev_port = prev_prev_port;
        }

        public String getNext_next_port() {
            return next_next_port;
        }

        public void setNext_next_port(String next_next_port) {
            this.next_next_port = next_next_port;
        }


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

    ArrayList<Port> all_ports = new ArrayList<Port>();
    int all_ports_position;

    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";
    private static final String INSERT = "INSERT";
    private static final String INSERTONLY = "INSERTONLY";
    private static final String SERVER_ALIVE = "SERVER_ALIVE";
    private static final String QUERYONLY = "QUERYONLY";
    private static final String QUERY_ALL = "QUERY_ALL";
    private static final String QUERY_AVD = "QUERY_AVD";
    private static final String NONE = "NONE";
    private static final String RECOVER = "RECOVER";
    static String FAILED_AVD = NONE;
    private static final String[] COLUMNS ={KEY_FIELD,VALUE_FIELD};

    @Override
    public boolean onCreate() {

        ll.add(REMOTE_PORT0);
        ll.add(REMOTE_PORT1);
        ll.add(REMOTE_PORT2);
        ll.add(REMOTE_PORT3);
        ll.add(REMOTE_PORT4);

        Context context = getContext();
        context.deleteDatabase(DATABASE_NAME);


        DatabaseHelper dbh = new DatabaseHelper(context);
        dba = dbh.getWritableDatabase();


        mUri = buildUri("content", "edu.buffalo.cse.cse486586.simpledynamo.provider");

        TelephonyManager tel = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        LOCAL_PORT.setPort(myPort);
        all_ports = nodeJoin();
        LOCAL_PORT = all_ports.get(all_ports_position);

        //set next next port and prev prev port
        for (int i =0;i<all_ports.size();i++){
            if (LOCAL_PORT.getNext_port().equals(all_ports.get(i).getPort())){
                LOCAL_PORT.setNext_next_port(all_ports.get(i).getNext_port());
            }
        }
        for (int i =0;i<all_ports.size();i++){
            if (LOCAL_PORT.getPrev_port().equals(all_ports.get(i).getPort())){
                LOCAL_PORT.setPrev_prev_port(all_ports.get(i).getPrev_port());
            }
        }
        Log.v("LOCAL PORT",LOCAL_PORT.getPrev_prev_port()+" "+LOCAL_PORT.getPrev_port()+" "+ LOCAL_PORT.getPort() +" "+ LOCAL_PORT.getNext_port() +" "+ LOCAL_PORT.getNext_next_port());

        try{
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
            //Task to copy values from other avds

        }catch(IOException e)
        {
            Log.e("SERVER", "Cant Create Server");
            return false;
        }

        new RecoverTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, LOCAL_PORT.getPort());

        if(dba==null)
            return false;
        else
            return true;
    }

    private class RecoverTask extends AsyncTask<String,Void,Void>{
        @Override
        protected Void doInBackground(String... params) {

            String[] remotePort = new String[ll.size()];
            ll.toArray(remotePort);

            for (int i =0; i<remotePort.length;i++){
                try{
                    Socket ClientSocket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remotePort[i]));
                    PrintWriter out = new PrintWriter(ClientSocket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(ClientSocket.getInputStream()));

                    out.println(RECOVER);
                    out.flush();

                    String rec_temp = in.readLine();

                    if (rec_temp==null){
                        Log.v("RECOVER","READ LINE IS NULL");
                    }
                    else if (rec_temp.equals(SERVER_ALIVE)){

                        out.println(LOCAL_PORT.getPort());
                        out.flush();

                        rec_temp = in.readLine();

                        if (rec_temp!=null) {

                            if (rec_temp.equals(LOCAL_PORT.getPort())) {
                                RECOVERY = true;
                            }
                        }
                    }

                    ClientSocket.close();
                    out.close();
                    in.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }

            if (RECOVERY){


                Log.v("RECOVER", "START COPY");

                String request_next[] = {LOCAL_PORT.getNext_port(),LOCAL_PORT.getPrev_port()};
                MatrixCursor matrixCursor = new MatrixCursor(COLUMNS);

                for (int i=0;i<=1;i++) {
                    try {
                        Socket ClientSocket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(request_next[i]));
                        PrintWriter out = new PrintWriter(ClientSocket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(ClientSocket.getInputStream()));

                        out.println(QUERY_ALL);
                        out.flush();

                        String rec_temp = in.readLine();


                        if (rec_temp==null){
                            Log.v("Read line","is NULL");
                        }

                        else if (rec_temp.equals(SERVER_ALIVE)) {

                            rec_temp = in.readLine();

                            if (rec_temp != null) {
                                String[] tokens = rec_temp.split("[;]");

                                for (int j = 0; j < tokens.length; j++) {

                                    if (!tokens[j].isEmpty()) {
                                        String[] key_value = tokens[j].split("[,]");
                                        matrixCursor.addRow(new Object[]{key_value[0], key_value[1]});
                                    }
                                }
                            }

                        }


                        ClientSocket.close();
                        in.close();
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                Cursor cursor = matrixCursor;

                if (cursor.moveToFirst()) {
                    ContentValues contentValues = new ContentValues();
                    while (!cursor.isAfterLast()) {

                        String key_rec = cursor.getString(cursor.getColumnIndex(KEY_FIELD));
                        String value_rec = cursor.getString(cursor.getColumnIndex(VALUE_FIELD));

                        try {
                            String key_rec_hash = genHash(key_rec);

                            String sendTo = sendKeyTo(key_rec_hash)[0];

                            if (sendTo.equals(LOCAL_PORT.getPort())
                                    || sendTo.equals(LOCAL_PORT.getPrev_port()) ||sendTo.equals(LOCAL_PORT.getPrev_prev_port()) ) {


                                contentValues.put(KEY_FIELD, key_rec);
                                contentValues.put(VALUE_FIELD, value_rec);
                                dba.insertWithOnConflict(TABLE_NAME, null, contentValues, SQLiteDatabase.CONFLICT_IGNORE);

                                contentValues.clear();

                            }


                        } catch (NoSuchAlgorithmException e) {
                            e.printStackTrace();
                        }
                        cursor.moveToNext();
                    }
                }

                Log.v("RECOVER","COPY COMPLETE");
                cursor.close();

            }

            return null;
        }
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void>{
        @Override
        protected Void doInBackground(ServerSocket... params) {
            ServerSocket serverSocket = params[0];

            try{
                while(true){

                    Socket clientSocket = serverSocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                    String marker = in.readLine();

                    if (marker.equals(RECOVER)){

                        out.println(SERVER_ALIVE);

                        String rec_avd = in.readLine();

                        if (!FAILED_AVD.equals(NONE)){

                            if (rec_avd.equals(FAILED_AVD)){
                                out.println(FAILED_AVD);
                                FAILED_AVD = NONE;
                                Log.v(RECOVER,rec_avd);
                            }

                        }else{
                            out.println(NONE);
                        }

                    }//end recover

                    //insert call
                    if (marker.equals(INSERT)) {

                        out.println(SERVER_ALIVE);

                        String key_rec = in.readLine();
                        String value_rec = in.readLine();
                        if (key_rec!=null && value_rec != null){
                            ContentValues contentValues = new ContentValues();
                            contentValues.put(KEY_FIELD, key_rec);
                            contentValues.put(VALUE_FIELD, value_rec);
                            Uri uri = null;
                            insert(uri, contentValues);
                        }

                    }//end insert

                    if (marker.equals(QUERY_AVD)) {

                        out.println(SERVER_ALIVE);

                        String key_rec = in.readLine();
                        Log.v("DB QUERY SERVER", "QUERY REQUEST RECEIVED FROM ORIGINAL AVD");

                        Uri uri = mUri;

                        Cursor cursor = query(uri, null, key_rec, null, null);

                        if (cursor != null) {
                            if (cursor.moveToFirst()) {
                                String key_send = cursor.getString(cursor.getColumnIndex(KEY_FIELD));
                                String value_send = cursor.getString(cursor.getColumnIndex(VALUE_FIELD));

                                Entry entry_send = new Entry();
                                entry_send.setKey(key_send);
                                entry_send.setValue(value_send);

                                String send_packet = entry_send.getKey() + "," + entry_send.getValue();
                                cursor.close();
                                out.println(send_packet);
                                out.flush();
                            }
                        }
                    }//end query avd

                    if (marker.equals(QUERY_ALL)){

                        out.println(SERVER_ALIVE);

                        Uri uri = mUri;
                        String key_rec = "@";
                        Cursor cursor = query(uri, null, key_rec, null, null);

                        String send_packet =new String();

                        if (cursor!=null) {

                            if (cursor.moveToFirst()) {
                                while (!cursor.isAfterLast()) {

                                    String key_send = cursor.getString(cursor.getColumnIndex(KEY_FIELD));
                                    String value_send = cursor.getString(cursor.getColumnIndex(VALUE_FIELD));

                                    if (send_packet.isEmpty()) {
                                        send_packet = key_send + "," + value_send + ";";
                                    } else {
                                        send_packet = key_send + "," + value_send + ";" + send_packet;
                                    }
                                    cursor.moveToNext();
                                }
                            }
                            cursor.close();
                        }

                        out.println(send_packet);
                        out.flush();
                    }//end query all


                    in.close();
                    out.close();
                    clientSocket.close();
                }//end while
            } //end try
            catch(IOException e){
                e.printStackTrace();
            }

            return null;
        }//end do in background
    }//send server task

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        long rows_inserted;

        try {
            String check_insert = NONE;
            String key_send = values.get(KEY_FIELD).toString();
            String value_send = values.get(VALUE_FIELD).toString();

            Log.v("DB INSERT","KEY "+ key_send);

            String key_check[] = key_send.split("[;]");

            if (key_check.length==2 || key_send.contains(INSERTONLY)){
                check_insert = key_check[0];
                key_send = key_check[1];
                values.remove(KEY_FIELD);
                values.put(KEY_FIELD, key_send);
                values.remove(VALUE_FIELD);
                values.put(VALUE_FIELD,value_send);

            }

            String key_send_hash = genHash(key_send);
            String sendTo[] = sendKeyTo(key_send_hash);

            if (check_insert.equals(INSERTONLY)){
                rows_inserted = dba.replace(TABLE_NAME, null, values);
                if (rows_inserted > 0) {
                    Log.v("DB INSERT", "Insert ONLY " + values.toString());
                }
            }

            else {

                //check if the current avd is the correct avd
                if (sendTo[0].equals(LOCAL_PORT.getPort())) {
                    rows_inserted = dba.replace(TABLE_NAME, null, values);
                    new InsertTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, sendTo[1], INSERTONLY + ";" + key_send, value_send);
                    new InsertTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, sendTo[2], INSERTONLY + ";" + key_send, value_send);
                    if (rows_inserted > 0) {
                        Log.v("DB INSERT", "CASE 1" + values.toString() +sendTo[0]+" "+sendTo[1]+" "+sendTo[2]);
                    }
                } else if (sendTo[1].equals(LOCAL_PORT.getPort())) {
                    new InsertTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, sendTo[0], INSERTONLY + ";" + key_send, value_send);
                    rows_inserted = dba.replace(TABLE_NAME, null, values);
                    new InsertTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, sendTo[2], INSERTONLY + ";" + key_send, value_send);
                    if (rows_inserted > 0) {
                        Log.v("DB INSERT", "CASE 2 " + values.toString()+sendTo[0]+" "+sendTo[1]+" "+sendTo[2]);
                    }
                } else if (sendTo[2].equals(LOCAL_PORT.getPort())) {
                    new InsertTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, sendTo[0], INSERTONLY + ";" + key_send, value_send);
                    new InsertTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, sendTo[1], INSERTONLY + ";" + key_send, value_send);
                    rows_inserted = dba.replace(TABLE_NAME, null, values);
                    if (rows_inserted > 0) {
                        Log.v("DB INSERT", "CASE 3 " + values.toString()+sendTo[0]+" "+sendTo[1]+" "+sendTo[2]);
                    }
                } else {
                    new Insert_ALL_Task().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,sendTo[0],sendTo[1],sendTo[2],key_send,value_send);
                    Log.v("DB INSERT", " CASE 4" + values.toString()+sendTo[0]+" "+sendTo[1]+" "+sendTo[2]);
                }
            }

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return uri;
    }

    private class InsertTask extends AsyncTask<String,Void,Void>{
        @Override
        protected Void doInBackground(String... params) {

            String sendTo = params[0];
            String key_send = params[1];
            String value_send = params[2];
            String rec_temp;

            try {
                Socket ClientSocket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(sendTo));
                PrintWriter out = new PrintWriter(ClientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(ClientSocket.getInputStream()));

                out.println(INSERT);
                out.flush();

                rec_temp = in.readLine();

                if (rec_temp==null){
                    FAILED_AVD = sendTo;
                }

                else if (rec_temp.equals(SERVER_ALIVE)) {
                    out.println(key_send);
                    out.flush();
                    out.println(value_send);
                    out.flush();
                }

                out.close();
                in.close();
                ClientSocket.close();

            }catch(ConnectException c){
                c.printStackTrace();
                FAILED_AVD = sendTo;
            }
            catch (IOException e) {
                e.printStackTrace();
                FAILED_AVD = sendTo;
            }


            if (!FAILED_AVD.equals(NONE)){
                Log.v("FAILED AVD ",FAILED_AVD);
            }

            return null;
        }


    }


    private class Insert_ALL_Task extends AsyncTask<String,Void,Void>{
        @Override
        protected Void doInBackground(String... params) {
            String sendTo[] = {params[0],params[1],params[2]};
            String key_send = params[3];
            String value_send = params[4];
            String rec_temp;

            for (int i =0;i<=2;i++) {
                try {
                    Socket ClientSocket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(sendTo[i]));
                    PrintWriter out = new PrintWriter(ClientSocket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(ClientSocket.getInputStream()));

                    out.println(INSERT);
                    out.flush();

                    rec_temp = in.readLine();

                    if (rec_temp == null) {
                        FAILED_AVD = sendTo[i];
                    } else if (rec_temp.equals(SERVER_ALIVE)) {
                        out.println(key_send);
                        out.flush();
                        out.println(value_send);
                        out.flush();
                    }

                    out.close();
                    in.close();
                    ClientSocket.close();

                } catch (ConnectException c) {
                    c.printStackTrace();
                    FAILED_AVD = sendTo[i];
                } catch (IOException e) {
                    e.printStackTrace();
                    FAILED_AVD = sendTo[i];
                }

                if (!FAILED_AVD.equals(NONE)){
                    Log.v("FAILED AVD ",FAILED_AVD);
                }
            }


            return null;
        }
    }


    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

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
            Log.e("DB DELETE", "Deletion Failed" + Integer.toString(rows_deleted));
        }

        return rows_deleted;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        if (selection==null){

            Log.v("DB QUERY","Selection NULL");
            return null;
        }

        if (selection.equals("*")){

            Cursor cursor = null;
            try {
                cursor = new QueryTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,QUERY_ALL, QUERY_ALL, selection).get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            return cursor;
        }

        else if(selection.equals("@")){

            DatabaseHelper dbh = new DatabaseHelper(getContext());
            SQLiteDatabase sqLiteDatabase = dbh.getReadableDatabase();

            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM "+TABLE_NAME,null);

            if(cursor == null)
            {
                Log.e("DB QUERY", "QUERY FAILED "+selection);
            }
            return cursor;
        }

        else{
            try{

                String key_check[] = selection.split("[;]");
                String check_value = NONE;

                if (key_check.length>1){
                    selection = key_check[1];
                    check_value = key_check[0];
                }

                String key_send_hash = genHash(selection);
                String sendTo[] = sendKeyTo(key_send_hash);

                Log.v("DB QUERY","QUERY KEY "+selection+" "+sendTo[0]);

                if (check_value.equals(QUERYONLY) || sendTo[0].equals(LOCAL_PORT.getPort())){
                    SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
                    queryBuilder.setTables(TABLE_NAME);

                    Cursor cursor =queryBuilder.query(dba, projection, "key = '" + selection + "'", selectionArgs, sortOrder, null, null);

                    return cursor;
                }
                else{
                    return new QueryTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,QUERY_AVD,sendTo[0]+";"+sendTo[1]+";"+sendTo[2],QUERYONLY+";"+selection).get();
                }

            }catch (NoSuchAlgorithmException n){
                n.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private class QueryTask extends AsyncTask<String,Void,Cursor>{
        @Override
        protected Cursor doInBackground(String... params) {
            Cursor cursor = null;
            if (params[0].equals(QUERY_ALL)){

                String[] remotePort = new String[ll.size()];
                ll.toArray(remotePort);
                String rec_temp;
                MatrixCursor matrixCursor = new MatrixCursor(COLUMNS);

                for (int i = 0; i < remotePort.length; i++) {
                    try{
                        Socket ClientSocket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remotePort[i]));
                        PrintWriter out = new PrintWriter(ClientSocket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(ClientSocket.getInputStream()));

                        out.println(QUERY_ALL);
                        out.flush();

                        rec_temp = in.readLine();

                        if (rec_temp==null){
                            Log.v("Read line","is NULL");
                            FAILED_AVD = remotePort[i];
                        }

                        else if (rec_temp.equals(SERVER_ALIVE)) {

                            rec_temp = in.readLine();

                            if (rec_temp != null) {
                                String[] tokens = rec_temp.split("[;]");

                                for (int j = 0; j < tokens.length; j++) {

                                    if (!tokens[j].isEmpty()) {
                                        String[] key_value = tokens[j].split("[,]");
                                        matrixCursor.addRow(new Object[]{key_value[0], key_value[1]});
                                    }
                                }
                            }
                            else{
                                FAILED_AVD = remotePort[i];
                            }
                        }

                        ClientSocket.close();
                        cursor = matrixCursor;
                        in.close();
                        out.close();

                    }catch (ConnectException c){
                        c.printStackTrace();
                        FAILED_AVD = remotePort[i];
                    } catch (IOException e){
                        e.printStackTrace();
                        FAILED_AVD = remotePort[i];
                    }
                }


            }
            else if (params[0].equals(QUERY_AVD)) {

                String sendTo[] = params[1].split("[;]");
                String selection = params[2];
                Entry entry_rec = new Entry();
                String rec_temp;

                for (int i = 0; i < sendTo.length; i++) {
                    try {

                        Socket ClientSocket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(sendTo[i]));
                        PrintWriter out = new PrintWriter(ClientSocket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(ClientSocket.getInputStream()));

                        out.println(QUERY_AVD);
                        out.flush();

                        rec_temp = in.readLine();

                        if (rec_temp == null) {
                            Log.e("Read line", "is NULL");
                            FAILED_AVD = sendTo[i];
                        }

                        else if (rec_temp.equals(SERVER_ALIVE)) {

                            out.println(selection);
                            out.flush();

                            Log.v("DB QUERY", "QUERY SENT");

                            rec_temp = in.readLine();

                            Log.v("DB QUERY", "QUERY RETURN");
                            if (rec_temp != null) {
                                String tokens[] = rec_temp.split("[,]");
                                entry_rec.setKey(tokens[0]);
                                entry_rec.setValue(tokens[1]);

                                // Creation of matrix cursor http://stackoverflow.com/questions/18290864/create-a-cursor-from-hardcoded-array-instead-of-db
                                MatrixCursor matrixCursor = new MatrixCursor(COLUMNS);
                                matrixCursor.addRow(new Object[]{entry_rec.getKey(), entry_rec.getValue()});
                                cursor = matrixCursor;
                            } else {
                                Log.e("Read line", "is NULL");
                                FAILED_AVD = sendTo[i];
                            }
                        }
                        in.close();
                        out.close();
                        ClientSocket.close();

                    }catch (ConnectException c) {
                        c.printStackTrace();
                        FAILED_AVD = sendTo[i];
                    } catch (IOException e) {
                        e.printStackTrace();
                        FAILED_AVD = sendTo[i];
                    }


                    if (!FAILED_AVD.equals(NONE)){
                        Log.v("DB QUERY", "FAILED AVD "+FAILED_AVD);
                    }
                    else{
                        break;
                    }
                }
            }

            if (!FAILED_AVD.equals(NONE)){
                Log.v("FAILED AVD ",FAILED_AVD);
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
            db.execSQL(CREATE_TABLE);
            Log.v("Database", "Created");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }
    }

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    private String[] sendKeyTo (String key_hash_check){

        String sendTo[] = new String[3];

        //for smallest avd
        if ((key_hash_check.compareTo(all_ports.get(0).getPort_hash())<0) ||
                (key_hash_check.compareTo(all_ports.get(all_ports.size()-1).getPort_hash())>0)) {
            sendTo[0] = all_ports.get(0).getPort();
        }
        //for all other cases
        else {
            for (int i = 1; i < 5; i++) {
                if ((key_hash_check.compareTo(all_ports.get(i).getPort_hash())<0) &&
                        key_hash_check.compareTo(all_ports.get(i).getPrev_port_hash())>0){
                    sendTo[0] = all_ports.get(i).getPort();
                }
            }
        }

        for (int i=0;i<all_ports.size();i++){
            if (sendTo[0].equals(all_ports.get(i).getPort())){
                sendTo[1] = all_ports.get(i).getNext_port();
            }
        }

        for (int i=0;i<all_ports.size();i++){
            if (sendTo[1].equals(all_ports.get(i).getPort())){
                sendTo[2] = all_ports.get(i).getNext_port();
            }
        }

        if (sendTo==null){
            Log.v("SEND KEY TO","CASE NOT HANDLED");
        }

        return sendTo;
    }

    private ArrayList<Port> nodeJoin(){

        ArrayList<Port> node_list = new ArrayList<Port>();

        //create an Array list of nodes from the linked list
        for (int i=0;i<5;i++){
            Port port = new Port();

            port.setPort(ll.get(i));

            try {
                port.setPort_hash(genHash(Integer.toString(Integer.parseInt(port.getPort()) / 2)));
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

            node_list.add(port);
        }

        //sort the array list based on hashed port
        for (int i = 0; i < node_list.size(); i++) {
            for (int j = i; j > 0; j--) {
                Port port1, port2;
                port1 = node_list.get(j-1);
                port2 = node_list.get(j);

                if (port1.getPort_hash().compareTo(port2.getPort_hash())>0){
                    node_list.set(j-1,port2);
                    node_list.set(j,port1);

                }
            }
        }



        //set next and prev ports
        for (int i=0;i<5;i++){

            Port port = node_list.get(i);

            if (i == 0){

                port.setNext_port(node_list.get(i+1).getPort());
                port.setNext_port_hash(node_list.get(i+1).getPort_hash());

                port.setPrev_port(node_list.get(node_list.size()-1).getPort());
                port.setPrev_port_hash(node_list.get(node_list.size()-1).getPort_hash());
            }

            else if (i == 4){

                port.setNext_port(node_list.get(0).getPort());
                port.setNext_port_hash(node_list.get(0).getPort_hash());

                port.setPrev_port(node_list.get(i-1).getPort());
                port.setPrev_port_hash(node_list.get(i-1).getPort_hash());

            }

            else{

                port.setNext_port(node_list.get(i+1).getPort());
                port.setNext_port_hash(node_list.get(i+1).getPort_hash());

                port.setPrev_port(node_list.get(i-1).getPort());
                port.setPrev_port_hash(node_list.get(i-1).getPort_hash());

            }

            node_list.set(i,port);

        }

        for(int i = 0;i<5;i++){

            if (LOCAL_PORT.getPort().equals(node_list.get(i).getPort())){
                all_ports_position = i;
                break;

            }

        }


        for (int i = 0;i< node_list.size();i++){
            Log.v("SORTED NODES LIST",node_list.get(i).getPort() + " " + node_list.get(i).getNext_port());
        }
        Log.v("SORTED NODES LIST","Positon "+ Integer.toString(all_ports_position));


        return node_list;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }
    

}
