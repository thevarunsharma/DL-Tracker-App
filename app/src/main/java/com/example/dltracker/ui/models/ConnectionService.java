package com.example.dltracker.ui.models;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.MessageProperties;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class ConnectionService extends Service {

    private static HashMap<String, ConnectionObject> connections = new HashMap<String, ConnectionObject>();
    public IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        ConnectionService getService() {
            return  ConnectionService.this;
        }
    }

    public ConnectionService() {
    }

    public void addActivity(EpochActivity activity, String modelKey) {
        ConnectionObject connection;
        if (!connections.containsKey(modelKey)) {
            connection = new ConnectionObject(modelKey);
            connections.put(modelKey, connection);
            connection.start();
        } else{
            connection = connections.get(modelKey);
        }
        connection.setActivity(activity);
    }

    public void closeConnection(String modelKey) {
        ConnectionObject connection = connections.get(modelKey);
        if (connection != null && connection.isRunning)
            connection.close();
            connections.remove(modelKey);
    }

    public void stopConnection(String modelKey) {
        ConnectionObject connection = connections.get(modelKey);
        if (connection != null && connection.isRunning)
            connection.stop();
            connections.remove(modelKey);
    }

    public void unbindActivity(String modelKey) {
        ConnectionObject connection = connections.get(modelKey);
        connection.setActivity(null);
    }

    public int getNumEpochs(String modelKey) {
        return connections.get(modelKey).done_epochs;
    }

    public boolean isTrainingRunning(String modelKey) {
        return connections.get(modelKey).isRunning;
    }


    private class ConnectionObject {

        Channel channel = null;
        String modelKey;
        ConnectionFactory factory;
        Connection connection;
        String uRI = "amqp://lvqhjqeg:yuWOooEzwrwmRpBki77RgoQFA3dN4ZIA@prawn.rmq.cloudamqp.com/lvqhjqeg";
        boolean isRunning = true;
        int done_epochs = 0;
        EpochActivity activity = null;

        ConnectionObject(String modelKey){
            this.modelKey = modelKey;
        }

        public void setActivity(EpochActivity activity) {this.activity = activity;}

        private class PublishAMQP extends AsyncTask<String, Boolean, Boolean> {

            @Override
            protected Boolean doInBackground(String... strings) {
                try {
                    publishProgress(false);
                    channel.basicPublish("", "signal_"+modelKey, true,
                            MessageProperties.TEXT_PLAIN,
                            strings[0].getBytes());
                    isRunning = false;
                    publishProgress(true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            }

            @Override
            protected void onProgressUpdate(Boolean... values) {
                boolean status = values[0];
                if (!status){
                    activity.loader.setVisibility(View.VISIBLE);
                }else {
                    Toast.makeText(activity, "Epoch stopped", Toast.LENGTH_SHORT).show();
                    activity.loader.setVisibility(View.GONE);
                }
            }
        }

        private class ConsumeAMQP extends AsyncTask<Void, String, Boolean> {

            @Override
            protected Boolean doInBackground(Void... voids) {
                setupConnectionFactory();
                try {
                    connection = factory.newConnection();
                } catch (IOException | TimeoutException e) {
                    e.printStackTrace();
                }

                try {
                    channel = connection.createChannel();
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }

                Log.i("AQMP", "Connection Established");

                Consumer consumer = new DefaultConsumer(channel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                        final String message = new String(body, "UTF-8");
//                        Log.i("AQMP", " [x] Received '" + message + "'");

                        publishProgress(message);
                    }
                };

                try {
                    channel.basicConsume("updates_"+modelKey, true, consumer);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Log.i("AQMP", " [*] Waiting for messages");
                while(isRunning) {
                    if (isCancelled()) {
                        break;
                    }
                }
                return true;
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
                try {
                    connection.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            protected void onProgressUpdate(String... params) {
                super.onProgressUpdate();
                String jsonString = params[0];

                HashMap<String, String> json = new HashMap<String, String>();
                JSONObject jObject = null;
                try {
                    jObject = new JSONObject(jsonString);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Iterator<?> keys = jObject.keys();

                while( keys.hasNext() ){
                    String key = (String)keys.next();
                    String value = null;
                    try {
                        value = jObject.getString(key);
                    } catch (JSONException e) {
                        Log.i("DEBUG JSON", jsonString);
                        e.printStackTrace();
                    }
                    json.put(key, value);
                }

                String event_type = (String) json.remove("type");
                if (event_type.equals("epoch_begin")) {
                    activity.addNewEpoch(json);
                } else if (event_type.equals("batch")) {
                    activity.updateProgressBar(json, false);
                } else if (event_type.equals("epoch_end")){
                    activity.updateProgressBar(json, true);
                } else if (event_type.equals("train_end")) {
                    activity.trainingEnded();
                }
            }
        }

        private void setupConnectionFactory() {
            try {
                factory = new ConnectionFactory();
                factory.setAutomaticRecoveryEnabled(true);
                factory.setUri(uRI);
            } catch (Exception e) {
                Toast.makeText(activity, "Error: "+e, Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }

        public void start() {
            ConsumeAMQP consumeRun = new ConsumeAMQP();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                consumeRun.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            else
                consumeRun.execute();
        }

        public void stop() {
            PublishAMQP publishRun = new PublishAMQP();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                publishRun.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "stop");
            else
                publishRun.execute("stop");
        }

        public void close() {
            try {
                connection.close();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


}
