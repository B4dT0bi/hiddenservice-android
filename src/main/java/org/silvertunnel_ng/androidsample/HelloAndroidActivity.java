package org.silvertunnel_ng.androidsample;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;

import org.silvertunnel_ng.androidsample.dexutil.DexHelper;
import org.silvertunnel_ng.androidsample.instance.R;
import org.silvertunnel_ng.netlib.api.NetFactory;
import org.silvertunnel_ng.netlib.api.NetLayer;
import org.silvertunnel_ng.netlib.api.NetLayerIDs;
import org.silvertunnel_ng.netlib.api.NetSocket;
import org.silvertunnel_ng.netlib.api.util.TcpipNetAddress;
import org.silvertunnel_ng.netlib.layer.tor.TorHiddenServicePortPrivateNetAddress;
import org.silvertunnel_ng.netlib.layer.tor.TorHiddenServicePrivateNetAddress;
import org.silvertunnel_ng.netlib.layer.tor.TorNetLayerUtil;
import org.silvertunnel_ng.netlib.layer.tor.TorNetServerSocket;
import org.silvertunnel_ng.netlib.util.ByteArrayUtil;
import org.silvertunnel_ng.netlib.util.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;

public class HelloAndroidActivity extends Activity {

    private DexHelper dexHelper;
    private BootStrapper bootstrapper;
    private TorNetLayerUtil torNetLayerUtil;

    private TorHiddenServicePortPrivateNetAddress netAddress;

    /**
     * Called when the activity is first created.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the
     *                           data it most recently supplied in onSaveInstanceState(Bundle). <b>Note: Otherwise it is null.</b>
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        dexHelper = new DexHelper(getApplicationContext(), getClassLoader());
        bootstrapper = new BootStrapper();
        DexHelper[] helpers = {dexHelper};
        bootstrapper.execute(helpers);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private class BootStrapper extends AsyncTask<DexHelper, Void, Void> {

        @Override
        protected Void doInBackground(DexHelper... onlyAsingleHelper) {

            try {
                onlyAsingleHelper[0].copyDexFiles();
                onlyAsingleHelper[0].loadClasses();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            try {
                System.out.println("Getting instance");
                torNetLayerUtil = TorNetLayerUtil.getInstance();

                System.out.println("Getting NetAddress");
                netAddress = setupHiddenServiceKeyPair(torNetLayerUtil);
                final NetLayer netLayer = NetFactory.getInstance().getNetLayerById(NetLayerIDs.TOR);
                System.out.println(netLayer.getStatus().toString());
                new Thread() {
                    public void run() {
                        while (true) {
                            try {
                                sleep(5000);
                            } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                            System.out.println(netLayer.getStatus().toString());

                        }
                    }
                }.start();
                System.out.println(netLayer.getStatus().toString());
                System.out.println("Waiting... " + netAddress.toString());
                netLayer.waitUntilReady();
                System.out.println("READY: " + netAddress.toString());
                runOnUiThread(new Runnable() {
                    public void run() {
                        TextView txt = (TextView) findViewById(R.id.txt_info);
                        txt.setText(netAddress.toString());
                    }
                });

                final String TORCHECK_HOSTNAME = "httptest.silvertunnel-ng.org";
                final TcpipNetAddress TORCHECK_NETADDRESS = new TcpipNetAddress(TORCHECK_HOSTNAME, 80);

                // create connection
                final NetSocket topSocket = NetFactory.getInstance().getNetLayerById(NetLayerIDs.TOR_OVER_TLS_OVER_TCPIP)
                        .createNetSocket(null, null, TORCHECK_NETADDRESS);

                HttpUtil.getInstance();
                // communicate with the remote side
                final byte[] httpResponse = HttpUtil.get(topSocket, TORCHECK_NETADDRESS, "/checktor.php", 5000);
                final String httpResponseStr = ByteArrayUtil.showAsString(httpResponse);

                Log.d("AndroidSample", httpResponseStr);
                runOnUiThread(new Runnable() {
                    public void run() {
                        TextView txt = (TextView) findViewById(R.id.txtStatus);
                        txt.setText(httpResponseStr);
                    }
                });

                final TorNetServerSocket netServerSocket = (TorNetServerSocket) netLayer
                        .createNetServerSocket(null, netAddress);
                new Thread() {
                    public void run() {
                        while (true) {
                            final NetSocket netSocket;
                            try {
                                netSocket = netServerSocket.accept();

                                System.out.println("accepted connection");
                                new Thread() {
                                    @Override
                                    public void run() {
                                        try {
                                            // receive data (e.g. HTTP request) and send data (e.g. HTTP response)
                                            // hint: to avoid dead locks: use separate threads for each direction
                                            InputStream is = netSocket.getInputStream();
                                            OutputStream os = netSocket.getOutputStream();
                                            os.write("HTTP/1.1 200 OK\nContent-Type: text/pain; charset=utf-8\nContent-Length: 10\n\n12334\n"
                                                    .getBytes());
                                        } catch (Exception e) {

                                        } finally {
                                            try {
                                                netSocket.close();
                                            } catch (IOException e) {
                                                // TODO Auto-generated catch block
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                }.start();

                            } catch (IOException e1) {
                                // TODO Auto-generated catch block
                                e1.printStackTrace();
                            }
                        }
                    }
                }.start();
            } catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            TextView txt = (TextView) findViewById(R.id.txt_info);
            txt.setText(netAddress.toString());
        }
    }

    private TorHiddenServicePortPrivateNetAddress setupHiddenServiceKeyPair(TorNetLayerUtil util)
            throws IOException {
        File directory = new File(getFilesDir().getPath() + File.separator + "hiddenServ");
        if (!(directory.exists() && directory.isDirectory())) {
            directory.mkdir();
            TorHiddenServicePrivateNetAddress newNetAddress = util.createNewTorHiddenServicePrivateNetAddress();

            util.writeTorHiddenServicePrivateNetAddressToFiles(directory, newNetAddress);

        }
        TorHiddenServicePrivateNetAddress netAddress = util.readTorHiddenServicePrivateNetAddressFromFiles(directory, true);
        int port = 55555;
        return new TorHiddenServicePortPrivateNetAddress(netAddress, port);
    }
}
