
package com.googlecode.android_scripting.facade.wifi;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.FileUtils;
import com.googlecode.android_scripting.facade.FacadeManager;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcParameter;
import com.googlecode.android_scripting.rpc.RpcOptional;

/**
 * Basic http operations.
 */
public class HttpFacade extends RpcReceiver {

    private ServerSocket mServerSocket = null;
    private int mServerTimeout = -1;
    private HashMap<Integer, Socket> mSockets = null;
    private int socketCnt = 0;

    public HttpFacade(FacadeManager manager) throws IOException {
        super(manager);
        mSockets = new HashMap<Integer, Socket>();
    }

    private void inputStreamToOutputStream(InputStream in, OutputStream out) throws IOException {
        if (in == null) {
            Log.e("InputStream is null.");
            return;
        }
        if (out == null) {
            Log.e("OutputStream is null.");
            return;
        }
        try {
            int read = 0;
            byte[] bytes = new byte[1024];
            while ((read = in.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            in.close();
            out.close();
        }

    }

    private String inputStreamToString(InputStream in) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String str = null;
        while ((str = r.readLine()) != null) {
            sb.append(str);
        }
        return sb.toString();
    }

    /**
     * Send an http request and get the response.
     *
     * @param url The url to send request to.
     * @return The HttpURLConnection object.
     */
    private HttpURLConnection httpRequest(String url) throws IOException {
        URL targetURL = new URL(url);
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) targetURL.openConnection();
            urlConnection.connect();
            int respCode = urlConnection.getResponseCode();
            String respMsg = urlConnection.getResponseMessage();
            Log.d("Got response code: " + respCode + " and response msg: " + respMsg);
        } catch (IOException e) {
            Log.e("Failed to open a connection to " + url);
            Log.e(e.toString());
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return urlConnection;
    }

    @Rpc(description = "Start waiting for a connection request on a specified port.",
            returns = "The index of the connection.")
    public Integer httpAcceptConnection(@RpcParameter(name = "port") Integer port) throws IOException {
        mServerSocket = new ServerSocket(port);
        if (mServerTimeout > 0) {
            mServerSocket.setSoTimeout(mServerTimeout);
        }
        Socket sock = mServerSocket.accept();
        socketCnt += 1;
        mSockets.put(socketCnt, sock);
        return socketCnt;
    }

    @Rpc(description = "Download a file from specified url, to an (optionally) specified path.")
    public void httpDownloadFile(@RpcParameter(name = "url") String url,
            @RpcParameter(name="outPath") @RpcOptional String outPath) throws IOException {
        // Create the input stream
        HttpURLConnection urlConnection = httpRequest(url);
        InputStream in = new BufferedInputStream(urlConnection.getInputStream());
        // Parse destination path and create the output stream. The function assumes that the path
        // is specified relative to the system default Download dir.
        File outFile = FileUtils.getExternalDownload();
        if (outPath != null) {
            // Check to see if the path is absolute.
            if (outPath.startsWith("/")) {
                outFile = new File(outPath);
            } else {
                outFile = new File(outFile, outPath);
            }
            // Check to see if specified path should be a dir.
            if (outPath.endsWith("/")) {
                if (!outFile.isDirectory() && !outFile.mkdirs()) {
                    throw new IOException("Failed to create the path: " + outPath);
                }
            }
        }
        // If no filename was specified, use the filename provided by the server.
        if (outFile.isDirectory()) {
            String filename = null;
            String contentDisposition = urlConnection.getHeaderField("Content-Disposition");
            // Try to figure out the name of the file being downloaded.
            // If the server returned a filename, use it.
            if (contentDisposition != null) {
                int idx = contentDisposition.toLowerCase().indexOf("filename");
                if (idx != -1) {
                    filename = contentDisposition.substring(idx + 9);
                    Log.d("Using filename returned by server: " + filename);
                }
            }
            // If the server did not provide a filename to us, use the last part of url.
            if (filename.equals("")) {
               int lastIdx = url.lastIndexOf('/');
                filename = url.substring(lastIdx + 1);
                Log.d("Using name from url: " + filename);
            }
            outFile = new File(outFile, filename);
        }
        OutputStream output = new FileOutputStream(outFile);
        inputStreamToOutputStream(in, output);
        Log.d("Downloaded file to " + outPath);
    }

    @Rpc(description = "Make an http request and return the response message.")
    public HttpURLConnection httpPing(@RpcParameter(name = "url") String url) throws IOException {
        try {
            HttpURLConnection urlConnection = null;
            urlConnection = httpRequest(url);
            return urlConnection;
        } catch (UnknownHostException e) {
            return null;
        }
    }

    @Rpc(description = "Make an http request and return the response content as a string.")
    public String httpRequestString(@RpcParameter(name = "url") String url) throws IOException {
        HttpURLConnection urlConnection = httpRequest(url);
        InputStream in = new BufferedInputStream(urlConnection.getInputStream());
        String result = inputStreamToString(in);
        Log.d("Fetched: " + result);
        return result;
    }

    @Rpc(description = "Set how many milliseconds to wait for an incoming connection.")
    public void httpSetServerTimeout(@RpcParameter(name = "timeout") Integer timeout)
            throws SocketException {
        mServerSocket.setSoTimeout(timeout);
        mServerTimeout = timeout;
    }

    @Rpc(description = "Ping to host(URL or IP), return success (true) or fail (false).")
    // The optional timeout parameter is in unit of second.
    public Boolean pingHost(@RpcParameter(name = "host") String hostString,
            @RpcParameter(name = "timeout") @RpcOptional Integer timeout) {
        try {
            String host;
            try {
                URL url = new URL(hostString);
                host = url.getHost();
            } catch (java.net.MalformedURLException e) {
                Log.d("hostString is not URL, it may be IP address.");
                host = hostString;
            }

            Log.d("Host:" + host);
            String pingCmdString = "ping -c 1 ";
            if (timeout != null) {
                pingCmdString = pingCmdString + "-W " + timeout + " ";
            }
            pingCmdString = pingCmdString + host;
            Log.d("Execute command: " + pingCmdString);
            Process p1 = java.lang.Runtime.getRuntime().exec(pingCmdString);
            int returnVal = p1.waitFor();
            boolean reachable = (returnVal == 0);
            Log.d("Ping return Value:" + returnVal);
            return reachable;
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
        /*TODO see b/18899134 for more information.
        */
    }

    @Override
    public void shutdown() {
        for (int key : mSockets.keySet()) {
            Socket sock = mSockets.get(key);
            try {
                sock.close();
            } catch (IOException e) {
                Log.e("Failed to close socket " + key + " on port " + sock.getLocalPort());
                e.printStackTrace();
            }
        }
    }
}
