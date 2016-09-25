package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;


/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */

//Anirudh Reddy
public class GroupMessengerActivity extends Activity {


    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final int SERVER_PORT = 10000;


    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";
    private static int key_sequence = -1;

    static String uri  = "content://edu.buffalo.cse.cse486586.groupmessenger2.provider";
    static final Uri CONTENT_URI = Uri.parse(uri);

    //Linked List for Storing AVDs
    LinkedList ll = new LinkedList();

    static final String FIRST = "FIRST";
    static final String SECOND = "SECOND";
    static final String DELIVERABLE = "DELIVERABLE";
    static final String UNDELIVERABLE = "UNDELIVERABLE";
    static final String NONE = "NONE";

    static String LOCAL_PORT = new String();
    static String avd;
    static String failed_avd = NONE;

    static int fifo_counter = 0;
    static int priority_counter = 0;

    ArrayList<Packet> message_buffer = new ArrayList<Packet>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        ll.add(REMOTE_PORT0);
        ll.add(REMOTE_PORT1);
        ll.add(REMOTE_PORT2);
        ll.add(REMOTE_PORT3);
        ll.add(REMOTE_PORT4);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        LOCAL_PORT = myPort;

        try {

            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);

        } catch (IOException e) {

            Log.e("SERVER", "Cant Create Server");
            return;
        }


        final TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());


        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        final EditText input = (EditText) findViewById(R.id.editText1);

        Button send = (Button) findViewById(R.id.button4);

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String msg = input.getText().toString() + "\n";
                tv.append("\t" + msg);
                input.setText("");

                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);

            }
        });


    }




    class ServerTask extends AsyncTask<ServerSocket,String, Void>{
        @Override

        protected Void doInBackground(ServerSocket... params) {
            ServerSocket serverSocket = params[0];
            String rec_temp;
            String send_temp;
            String priority_with_avd;

            try{
                while(true){

                    Socket clientSocket = serverSocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                    Packet rec_packet = new Packet();

                    //Receive message from Client
                    rec_temp = in.readLine();
                    Log.v("SERVER RECEIVE", rec_temp);
                    String[] tokens = rec_temp.split("[,]");


                    //tokens[0] - Status
                    //tokens[1] - Sender AVD
                    //tokens[2] - Fifo Counter
                    //tokens[3] - Priority
                    //tokens[4] - Priority AVD
                    //tokens[5] - failed AVD
                    //tokens[6] - Message

                    //send_packet.setStatus(FIRST);
                    //send_packet.setSender_avd(LOCAL_PORT);
                    //send_packet.setFifo_counter(fifo_counter);
                    //send_packet.setPriority(-1);
                    //send_packet.setPriority_avd("NOT SET");
                    //send_packet.setMsg(msg);

                    rec_packet.setStatus(tokens[0]);
                    rec_packet.setSender_avd(tokens[1]);
                    rec_packet.setFifo_counter(Integer.parseInt(tokens[2]));
                    rec_packet.setPriority(Double.parseDouble(tokens[3]));
                    rec_packet.setPriority_avd(tokens[4]);
                    rec_packet.setFailed_avd(tokens[5]);
                    rec_packet.setMsg(tokens[6]);


                    //Remove messages of Failed AVD from Buffer
                    if (!rec_packet.getFailed_avd().equals(NONE)) {
                        Iterator<Packet> failed_iterator = message_buffer.iterator();

                        while (failed_iterator.hasNext()) {
                            Packet failed_packet = failed_iterator.next();

                            if (failed_packet.getSender_avd().equals(rec_packet.getFailed_avd())) {
                                failed_iterator.remove();
                            }
                        }
                    }


                    //Message received for first time
                    if (rec_packet.getStatus().equals(FIRST)) {

                        priority_counter = priority_counter + 1;

                        priority_with_avd = Integer.toString(priority_counter) + "." + LOCAL_PORT.substring(3, 5);

                        rec_packet.setPriority(Double.parseDouble(priority_with_avd));
                        rec_packet.setPriority_avd(LOCAL_PORT);

                        send_temp = rec_packet.getStatus() + ","
                                + rec_packet.getSender_avd() + ","
                                + rec_packet.getFifo_counter() + ","
                                + rec_packet.getPriority() + ","
                                + rec_packet.getPriority_avd() + ","
                                + rec_packet.getFailed_avd() + ","
                                + rec_packet.getMsg();

                        rec_packet.setStatus(UNDELIVERABLE);

                        //Add message with proposed priority to buffer
                        if (message_buffer.add(rec_packet)) {
                            Log.v("SERVER BUFFER ADD OK", send_temp);
                        } else {
                            Log.v("SERVER BUFFER ADD FAIL", send_temp);
                        }

                        //Send message back to client with suggested priority
                        out.println(send_temp);
                        out.flush();

                    }//FIRST time if


                    //Message received for the second time
                    else if (rec_packet.getStatus().equals(SECOND)) {

                        //Update the priority counter value
                        Double d = rec_packet.getPriority();
                        priority_counter = Math.max(priority_counter, d.intValue()) + 1;


                        //Update the value in the buffer
                        Iterator<Packet> iterator2 = message_buffer.iterator();
                        while (iterator2.hasNext()) {
                            Packet packet = iterator2.next();
                            if (packet.getMsg().equals(rec_packet.getMsg())) {
                                packet.setStatus(DELIVERABLE);
                                packet.setPriority(rec_packet.getPriority());
                                packet.setPriority_avd(rec_packet.getPriority_avd());
                                break;
                            }
                        }

                        //View elements in Buffer
                        iterator2 = message_buffer.iterator();
                        while (iterator2.hasNext()) {
                            Packet packet = iterator2.next();
                            Log.v("BUFFER ELEMENTS", packet.getPriority() + "  " + packet.getMsg() + "  " + message_buffer.size());
                        }


                        //Sort the message buffer
                        if (message_buffer.size() >= 2) {

                            for (int i = 0; i < message_buffer.size(); i++) {

                                for (int j = i; j > 0; j--) {
                                    Packet packet1, packet2;
                                    packet1 = message_buffer.get(j - 1);
                                    packet2 = message_buffer.get(j);

                                    if (packet2.getPriority() < packet1.getPriority()) {
                                        message_buffer.set(j, packet1);
                                        message_buffer.set(j - 1, packet2);
                                    }
                                }//end for 2
                            }//end for 1
                        }//end if sort

                        //Send back avd instance to client
                        out.println(LOCAL_PORT);

                    }//end SECOND



                    Iterator<Packet> check_itr = message_buffer.iterator();
                    int flag = 0;
                    while (check_itr.hasNext()) {
                        Packet check_pck = check_itr.next();
                        if (check_pck.getStatus().equals(UNDELIVERABLE)) {
                            flag = -1;
                            break;
                        }
                    }


                    //Message Delivery
                    if ((!message_buffer.isEmpty()) && flag == 0) {

                        Iterator<Packet> iterator1 = message_buffer.iterator();

                        while (iterator1.hasNext()) {
                            Packet deliver_packet = iterator1.next();
                            if (deliver_packet.getStatus().equals(DELIVERABLE)) {
                                avd = deliver_packet.getSender_avd();
                                Log.v("SERVER SAVE DB", deliver_packet.getMsg() + "    " + deliver_packet.getPriority());
                                publishProgress(deliver_packet.getMsg());
                                iterator1.remove();

                            } else
                                break;
                        }//end while
                    }
                    //end delivery if


                    clientSocket.close();

                }//while loop
            }//try block
            catch (IOException e){
                e.printStackTrace();
            }

            return null;
        }//doinBAckground

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);

            String strReceived = values[0].trim();
            TextView tv = (TextView) findViewById(R.id.textView1);

            if (!avd.equals(LOCAL_PORT)) {
                tv.append("\t\t\t\t\t\t\t" + strReceived + "\n");
            }

            key_sequence = key_sequence + 1;

            ContentValues contentValues = new ContentValues();
            contentValues.put(KEY_FIELD, Integer.toString(key_sequence));
            contentValues.put(VALUE_FIELD, strReceived);

            getContentResolver().insert(CONTENT_URI, contentValues);

        }
    }



    class ClientTask extends AsyncTask<String,Void,Void>{
        @Override
        protected Void doInBackground(String... params) {

            String[] remotePort = new String[ll.size()];
            ll.toArray(remotePort);
            String msg,send_temp,rec_temp;
            int max_priority_temp;
            int max_priority = 0;
            int timeout = 500;
            String avd_check;
            ArrayList<String> avds = new ArrayList<String>();

            fifo_counter = fifo_counter+1;

            //Create packet object
            Packet send_packet = new Packet();
            send_packet.setPriority(-1.0);
            send_packet.setPriority_avd(NONE);
            send_packet.setFailed_avd(failed_avd);

            /*
            Packet contents
            1. Msg - String
            2.Status -String
            3.Fifo_counter - int
            4.Sender_avd; - String
            5.Priority - int
            6.Priority_avd - String
            7.Failed_avd - String
             */

            for (int i = 0;i<5;i++){
                try {

                    Socket ClientSocket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remotePort[i]));
                    ClientSocket.setSoTimeout(timeout);


                    PrintWriter out = new PrintWriter(ClientSocket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(ClientSocket.getInputStream()));

                    msg = params[0];

                    //Add entries to Packet
                    send_packet.setStatus(FIRST);
                    send_packet.setSender_avd(LOCAL_PORT);
                    send_packet.setFifo_counter(fifo_counter);
                    send_packet.setMsg(msg);


                    //Sending Message for First Time
                    send_temp = send_packet.getStatus()+","
                            +send_packet.getSender_avd()+","
                            +send_packet.getFifo_counter()+","
                            +send_packet.getPriority()+","
                            +send_packet.getPriority_avd()+","
                            +send_packet.getFailed_avd()+","
                            +send_packet.getMsg();
                    Log.v("CLIENT SEND 1", send_temp);
                    out.println(send_temp);
                    out.flush();



                    //Receive message with Priority from Server
                    rec_temp = in.readLine();

                    if (rec_temp != null)
                    {
                        Log.v("CLIENT RECEIVE 1", rec_temp);
                        String[] tokens = rec_temp.split("[,]");
                        //tokens[0] - Status
                        //tokens[1] - Sender AVD
                        //tokens[2] - Fifo Counter
                        //tokens[3] - Priority
                        //tokens[4] - Priority AVD
                        //tokens[5] - Failed AVD
                        //tokens[6] - Message



                        //Get only the digits before the decimal point
                        int dot = tokens[3].indexOf(".");
                        max_priority_temp = Integer.parseInt(tokens[3].substring(0,dot));


                        Log.v("MAX PRIORITY", Integer.toString(max_priority_temp)+"  "+Integer.toString(max_priority));
                        //Check for max priority and set it
                        if (max_priority_temp>max_priority) {

                            max_priority = max_priority_temp;
                            send_packet.setPriority(Double.parseDouble(tokens[3]));
                            send_packet.setPriority_avd(tokens[4]);
                        }
                    }
                    else if(rec_temp==null)
                    {
                        Log.v("Read line","is NULL");
                        switch(i){
                            case 0: failed_avd = REMOTE_PORT0;
                                break;
                            case 1: failed_avd = REMOTE_PORT1;
                                break;
                            case 2: failed_avd = REMOTE_PORT2;
                                break;
                            case 3: failed_avd = REMOTE_PORT3;
                                break;
                            case 4: failed_avd = REMOTE_PORT4;
                                break;
                        }
                        Log.v("FAILED_AVD",failed_avd);
                    }

                    ClientSocket.close();

                }//try
                catch (SocketTimeoutException s){
                    Log.v("TSOCKET EXCEPTION", "Socket Time out before max priority");
                    Log.v("I VALUE", Integer.toString(i));
                    switch(i){
                        case 0: failed_avd = REMOTE_PORT0;
                            break;
                        case 1: failed_avd = REMOTE_PORT1;
                            break;
                        case 2: failed_avd = REMOTE_PORT2;
                            break;
                        case 3: failed_avd = REMOTE_PORT3;
                            break;
                        case 4: failed_avd = REMOTE_PORT4;
                            break;
                    }
                }catch(ConnectException c){
                    Log.v("CSOCKET EXCEPTION", "Socket Time out after max priority");
                    c.printStackTrace();

                    switch(i){
                        case 0: failed_avd = REMOTE_PORT0;
                            break;
                        case 1: failed_avd = REMOTE_PORT1;
                            break;
                        case 2: failed_avd = REMOTE_PORT2;
                            break;
                        case 3: failed_avd = REMOTE_PORT3;
                            break;
                        case 4: failed_avd = REMOTE_PORT4;
                            break;
                    }

                }
                catch (IOException e) {
                    Log.v("IOSOCKET EXCEPTION", "Socket Time out after max priority");
                    e.printStackTrace();

                    switch(i) {
                        case 0:
                            failed_avd = REMOTE_PORT0;
                            break;
                        case 1:
                            failed_avd = REMOTE_PORT1;
                            break;
                        case 2:
                            failed_avd = REMOTE_PORT2;
                            break;
                        case 3:
                            failed_avd = REMOTE_PORT3;
                            break;
                        case 4:
                            failed_avd = REMOTE_PORT4;
                            break;
                    }
                }catch (NullPointerException n){
                    n.printStackTrace();
                }
            }//for loop

            send_packet.setStatus(SECOND);
            send_packet.setFailed_avd(failed_avd);

            //Send packet again with Max Priority
            for (int i = 0;i<5;i++) {
                try{
                    Socket ClientSocket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remotePort[i]));
                    ClientSocket.setSoTimeout(timeout);
                    PrintWriter out = new PrintWriter(ClientSocket.getOutputStream(), true);

                    send_temp = send_packet.getStatus()+","
                            +send_packet.getSender_avd()+","
                            +send_packet.getFifo_counter()+","
                            +send_packet.getPriority()+","
                            +send_packet.getPriority_avd()+","
                            +send_packet.getFailed_avd()+","
                            +send_packet.getMsg();
                    Log.v("CLIENT SEND 2", send_temp);
                    out.println(send_temp);

                    BufferedReader in = new BufferedReader(new InputStreamReader(ClientSocket.getInputStream()));
                    avd_check = in.readLine();

                    if (avd_check==null){

                        failed_avd = remotePort[i];
                        Log.v("in the second loop",failed_avd);
                    }
                    else if(avd_check != null)
                    {
                        avds.add(avd_check);
                        Log.v("AVD_check","NOT null");
                    }


                    ClientSocket.close();

                }catch (SocketTimeoutException s){
                    Log.v("TSOCKET EXCEPTION", "Socket Time out after max priority");
                }
                catch(ConnectException c){
                    Log.v("CSOCKET EXCEPTION", "Socket Time out after max priority");
                    c.printStackTrace();
                    switch(i) {
                        case 0:
                            failed_avd = REMOTE_PORT0;
                            break;
                        case 1:
                            failed_avd = REMOTE_PORT1;
                            break;
                        case 2:
                            failed_avd = REMOTE_PORT2;
                            break;
                        case 3:
                            failed_avd = REMOTE_PORT3;
                            break;
                        case 4:
                            failed_avd = REMOTE_PORT4;
                            break;
                    }
                }
                catch (IOException e){
                    Log.v("IOSOCKET EXCEPTION", "Socket Time out after max priority");
                    e.printStackTrace();
                }//catch block
            }//for block

            String failed = failed_avd;
            if((avds.size()!=5) && (failed.equals(NONE))) {
                Log.v("AVDS no",Integer.toString(avds.size()));
                int count = 0;
                String avds_string[] = {"11108", "11112", "11116", "11120", "11124"};
                for (int b = 0; b < 5; b++) {
                    String avd_no = avds_string[b];
                    for (int a = 0; a < avds.size(); a++) {
                        if(!avd_no.equals(avds.get(a)))
                        {
                            count++;
                        }
                        else{break;}
                    }
                    if(count==4)
                    {
                        failed_avd = avds_string[b];
                        Log.v("Failed_avd set",failed_avd);
                        break;
                    }
                }
            }

            return null;
        }//end do in background

    }//end Client task


    class Packet{
        String msg;
        String sender_avd;
        String priority_avd;
        String status;
        double priority;
        int fifo_counter;


        public String getFailed_avd() {
            return failed_avd;
        }

        public void setFailed_avd(String failed_avd) {
            this.failed_avd = failed_avd;
        }

        String failed_avd;

        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }

        public String getSender_avd() {
            return sender_avd;
        }

        public void setSender_avd(String sender_avd) {
            this.sender_avd = sender_avd;
        }

        public String getPriority_avd() {
            return priority_avd;
        }

        public void setPriority_avd(String priority_avd) {
            this.priority_avd = priority_avd;
        }

        public double getPriority() {
            return priority;
        }

        public void setPriority(double priority) {
            this.priority = priority;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public int getFifo_counter() {
            return fifo_counter;
        }

        public void setFifo_counter(int fifo_counter) {
            this.fifo_counter = fifo_counter;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }
}
