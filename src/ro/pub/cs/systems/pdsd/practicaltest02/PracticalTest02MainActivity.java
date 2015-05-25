package ro.pub.cs.systems.pdsd.practicaltest02;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class PracticalTest02MainActivity extends Activity {
    EditText serverPortEditText,clientAddressEditText,clientPortEditText;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practical_test02_main);

        serverPortEditText = (EditText) findViewById(R.id.serverPortEditText);
        clientAddressEditText = (EditText) findViewById(R.id.clientAddressEditText);
        clientPortEditText = (EditText) findViewById(R.id.clientPortEditText);

        ((Button) findViewById(R.id.startServerButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String port = serverPortEditText.getText().toString();
                ServerThread serverThread = new ServerThread();
                serverThread.startServer(Integer.parseInt(port));
            }
        });

        ((Button) findViewById(R.id.sendCommandButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String address = clientAddressEditText.getText().toString();
                        String port = clientPortEditText.getText().toString();

                        if (address != null && port != null) {
                            try {
                                Socket socket = new Socket(
                                        address,
                                        Integer.parseInt(port)
                                );
                                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(socket.getOutputStream());
                                PrintWriter printWriter = new PrintWriter(bufferedOutputStream, true);

                                InputStreamReader inputStreamReader = new InputStreamReader(socket.getInputStream());
                                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                                final String response;
                                response = bufferedReader.readLine();
                                findViewById(R.id.resultTextView).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((TextView) findViewById(R.id.resultTextView)).setText(response);
                                    }
                                });
                                socket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
            }
        });
    }

    private class CachedData{
        private HashMap<String,DateTime> data = new HashMap<String, DateTime>();

        public synchronized void setData(String key, DateTime value){
            data.put(key,value);
        }

        public synchronized DateTime getData(String key){
            if (data.containsKey(key)){
                return data.get(key);
            }
            return null;
        }
    }


    private class CommunicationThread extends Thread {
        private Socket socket;
        private CachedData cachedData;

        public CommunicationThread(Socket socket,CachedData cachedData) {
            this.socket = socket;
            this.cachedData = cachedData;
        }

        @Override
        public void run() {
            try {
                Log.v(Constants.TAG, "Connection opened with " + socket.getInetAddress() + ":" + socket.getLocalPort());
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(socket.getOutputStream());
                PrintWriter printWriter = new PrintWriter(bufferedOutputStream, true);
                InputStreamReader inputStreamReader = new InputStreamReader(socket.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                String receiverID = socket.getRemoteSocketAddress().toString().split(":")[0];
                System.out.println("I was contacted by " + receiverID);

                HttpClient httpClient = new DefaultHttpClient();
                HttpGet httpGet = new HttpGet("http://www.timeapi.org/utc/now");
                HttpResponse httpGetResponse = httpClient.execute(httpGet);
                HttpEntity httpGetEntity = httpGetResponse.getEntity();
                if (httpGetEntity != null) {
                    String time = EntityUtils.toString(httpGetEntity);
                    String[] timeParts = time.split("[-+:T]");

                    DateTime dateTime = new DateTime(
                            timeParts[0],
                            timeParts[1],
                            timeParts[2],
                            timeParts[3],
                            timeParts[4],
                            timeParts[5]
                    );
                    DateTime oldTime = cachedData.getData(receiverID);
                    System.out.println("HERE IS OLD TIME: " + oldTime);
                    if (oldTime == null){
                        printWriter.println("Here is ur time: " + time);
                        cachedData.setData(receiverID,dateTime);
                    }
                    else {
                        if (oldTime.toDouble() + 60 > dateTime.toDouble()){
                            printWriter.println("Wait 1 minute, man!!");
                        }
                        else {
                            printWriter.println("Here is ur time: " + time);
                            cachedData.setData(receiverID,dateTime);
                        }
                    }
                }

                socket.close();
                Log.v(Constants.TAG, "Connection closed");
            } catch (IOException ioException) {
                Log.e(Constants.TAG, "An exception has occurred: "+ioException.getMessage());
                if (Constants.DEBUG) {
                    ioException.printStackTrace();
                }
            }
        }
    }

    private class ServerThread extends Thread {
        private boolean isRunning;
        private int serverPort;
        private ServerSocket serverSocket;
        private CachedData cachedData;

        public void startServer(int serverPort) {
            isRunning = true;
            this.serverPort = serverPort;
            cachedData = new CachedData();
            start();
        }

        public void stopServer() {
            isRunning = false;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (serverSocket != null) {
                            serverSocket.close();
                        }
                        Log.v(Constants.TAG, "stopServer() method invoked " + serverSocket);
                    } catch(IOException ioException) {
                        Log.e(Constants.TAG, "An exception has occurred: "+ioException.getMessage());
                        if (Constants.DEBUG) {
                            ioException.printStackTrace();
                        }
                    }
                }
            }).start();
        }

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(serverPort);
                while (isRunning) {
                    Socket socket = serverSocket.accept();
                    new CommunicationThread(socket,cachedData).start();
                }
            } catch (IOException ioException) {
                Log.e(Constants.TAG, "An exception has occurred: "+ioException.getMessage());
                if (Constants.DEBUG) {
                    ioException.printStackTrace();
                }
            }
        }
    }

}
