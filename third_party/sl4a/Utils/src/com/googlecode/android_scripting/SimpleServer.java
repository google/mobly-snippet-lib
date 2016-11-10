/*
 * Copyright (C) 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.googlecode.android_scripting;

import com.google.common.collect.Lists;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
//import com.googlecode.android_scripting.jsonrpc.RpcReceiverManager;

/**
 * A simple server.
 */
public abstract class SimpleServer {
  private static int threadIndex = 0;
  private final ConcurrentHashMap<Integer, ConnectionThread> mConnectionThreads =
      new ConcurrentHashMap<Integer, ConnectionThread>();
  private final List<SimpleServerObserver> mObservers = Lists.newArrayList();
  private volatile boolean mStopServer = false;
  private ServerSocket mServer;
  private Thread mServerThread;

  public interface SimpleServerObserver {
    public void onConnect();
    public void onDisconnect();
  }

  protected abstract void handleConnection(Socket socket) throws Exception;
  protected abstract void handleRPCConnection(Socket socket,
                                              Integer UID,
                                              BufferedReader reader,
                                              PrintWriter writer) throws Exception;

  /** Adds an observer. */
  public void addObserver(SimpleServerObserver observer) {
    mObservers.add(observer);
  }

  /** Removes an observer. */
  public void removeObserver(SimpleServerObserver observer) {
    mObservers.remove(observer);
  }

  private void notifyOnConnect() {
    for (SimpleServerObserver observer : mObservers) {
      observer.onConnect();
    }
  }

  private void notifyOnDisconnect() {
    for (SimpleServerObserver observer : mObservers) {
      observer.onDisconnect();
    }
  }

  private final class ConnectionThread extends Thread {
    private final Socket mmSocket;
    private final BufferedReader reader;
    private final PrintWriter writer;
    private final Integer UID;
    private final boolean isRpc;

    private ConnectionThread(Socket socket, boolean rpc, Integer uid, BufferedReader reader, PrintWriter writer) {
      setName("SimpleServer ConnectionThread " + getId());
      mmSocket = socket;
      this.UID = uid;
      this.reader = reader;
      this.writer = writer;
      this.isRpc = rpc;
    }

    @Override
    public void run() {
      Log.v("Server thread " + getId() + " started.");
      try {
        if(isRpc) {
          Log.d("Handling RPC connection in "+getId());
          handleRPCConnection(mmSocket, UID, reader, writer);
        }else{
          Log.d("Handling Non-RPC connection in "+getId());
          handleConnection(mmSocket);
        }
      } catch (Exception e) {
        if (!mStopServer) {
          Log.e("Server error.", e);
        }
      } finally {
        close();
        mConnectionThreads.remove(this.UID);
        notifyOnDisconnect();
        Log.v("Server thread " + getId() + " stopped.");
      }
    }

    private void close() {
      if (mmSocket != null) {
        try {
          mmSocket.close();
        } catch (IOException e) {
          Log.e(e.getMessage(), e);
        }
      }
    }
  }

  /** Returns the number of active connections to this server. */
  public int getNumberOfConnections() {
    return mConnectionThreads.size();
  }

  public static InetAddress getPrivateInetAddress() throws UnknownHostException, SocketException {

    InetAddress candidate = null;
    Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
    for (NetworkInterface netint : Collections.list(nets)) {
      if (!netint.isLoopback() || !netint.isUp()) { // Ignore if localhost or not active
        continue;
      }
      Enumeration<InetAddress> addresses = netint.getInetAddresses();
      for (InetAddress address : Collections.list(addresses)) {
        if (address instanceof Inet4Address) {
          Log.d("local address " + address);
          return address; // Prefer ipv4
        }
        candidate = address; // Probably an ipv6
      }
    }
    if (candidate != null) {
      return candidate; // return ipv6 address if no suitable ipv6
    }
    return InetAddress.getLocalHost(); // No damn matches. Give up, return local host.
  }

  public static InetAddress getPublicInetAddress() throws UnknownHostException, SocketException {

    InetAddress candidate = null;
    Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
    for (NetworkInterface netint : Collections.list(nets)) {
      if (netint.isLoopback() || !netint.isUp()) { // Ignore if localhost or not active
        continue;
      }
      Enumeration<InetAddress> addresses = netint.getInetAddresses();
      for (InetAddress address : Collections.list(addresses)) {
        if (address instanceof Inet4Address) {
          return address; // Prefer ipv4
        }
        candidate = address; // Probably an ipv6
      }
    }
    if (candidate != null) {
      return candidate; // return ipv6 address if no suitable ipv6
    }
    return InetAddress.getLocalHost(); // No damn matches. Give up, return local host.
  }

  /**
   * Starts the RPC server bound to the localhost address.
   *
   * @param port
   *          the port to bind to or 0 to pick any unused port
   *
   * @return the port that the server is bound to
   * @throws IOException
   */
  public InetSocketAddress startLocal(int port) {
    InetAddress address;
    try {
      // address = InetAddress.getLocalHost();
      address = getPrivateInetAddress();
      mServer = new ServerSocket(port, 5, address);
    } catch (BindException e) {
      Log.e("Port " + port + " already in use.");
      try {
        address = getPrivateInetAddress();
        mServer = new ServerSocket(0, 5, address);
      } catch (IOException e1) {
        e1.printStackTrace();
        return null;
      }
    } catch (Exception e) {
      Log.e("Failed to start server.", e);
      return null;
    }
    int boundPort = start();
    return InetSocketAddress.createUnresolved(mServer.getInetAddress().getHostAddress(), boundPort);
  }

  /**
   * data Starts the RPC server bound to the public facing address.
   *
   * @param port
   *          the port to bind to or 0 to pick any unused port
   *
   * @return the port that the server is bound to
   */
  public InetSocketAddress startPublic(int port) {
    InetAddress address;
    try {
      // address = getPublicInetAddress();
      address = null;
      mServer = new ServerSocket(port, 5 /* backlog */, address);
    } catch (Exception e) {
      Log.e("Failed to start server.", e);
      return null;
    }
    int boundPort = start();
    return InetSocketAddress.createUnresolved(mServer.getInetAddress().getHostAddress(), boundPort);
  }

  /**
   * data Starts the RPC server bound to all interfaces
   *
   * @param port
   *          the port to bind to or 0 to pick any unused port
   *
   * @return the port that the server is bound to
   */
  public InetSocketAddress startAllInterfaces(int port) {
    try {
      mServer = new ServerSocket(port, 5 /* backlog */);
    } catch (Exception e) {
      Log.e("Failed to start server.", e);
      return null;
    }
    int boundPort = start();
    return InetSocketAddress.createUnresolved(mServer.getInetAddress().getHostAddress(), boundPort);
  }

  private int start() {
    mServerThread = new Thread() {
      @Override
      public void run() {
        while (!mStopServer) {
          try {
            Socket sock = mServer.accept();
            if (!mStopServer) {
              startConnectionThread(sock);
            } else {
              sock.close();
            }
          } catch (IOException e) {
            if (!mStopServer) {
              Log.e("Failed to accept connection.", e);
            }
          } catch (JSONException e) {
            if (!mStopServer) {
              Log.e("Failed to parse request.", e);
            }
          }
        }
      }
    };
    mServerThread.start();
    Log.v("Bound to " + mServer.getInetAddress());
    return mServer.getLocalPort();
  }

  private void startConnectionThread(final Socket sock) throws IOException, JSONException {
    BufferedReader reader =
        new BufferedReader(new InputStreamReader(sock.getInputStream()), 8192);
    PrintWriter writer = new PrintWriter(sock.getOutputStream(), true);
    String data;
    if((data = reader.readLine()) != null) {
      Log.v("Received: " + data);
      JSONObject request = new JSONObject(data);
      if(request.has("cmd") && request.has("uid")) {
        String cmd = request.getString("cmd");
        int uid = request.getInt("uid");
        JSONObject result = new JSONObject();
        if(cmd.equals("initiate")) {
          Log.d("Initiate a new session");
          threadIndex += 1;
          int mUID = threadIndex;
          ConnectionThread networkThread = new ConnectionThread(sock,true,mUID,reader,writer);
          mConnectionThreads.put(mUID, networkThread);
          networkThread.start();
          notifyOnConnect();
          result.put("uid", mUID);
          result.put("status",true);
          result.put("error", null);
        }else if(cmd.equals("continue")) {
          Log.d("Continue an existing session");
          Log.d("keys: "+mConnectionThreads.keySet().toString());
          if(!mConnectionThreads.containsKey(uid)) {
            result.put("uid", uid);
            result.put("status",false);
            result.put("error", "Session does not exist.");
          }else{
            ConnectionThread networkThread = new ConnectionThread(sock,true,uid,reader,writer);
            mConnectionThreads.put(uid, networkThread);
            networkThread.start();
            notifyOnConnect();
            result.put("uid", uid);
            result.put("status",true);
            result.put("error", null);
          }
        }else {
          result.put("uid", uid);
          result.put("status",false);
          result.put("error", "Unrecognized command.");
        }
        writer.write(result + "\n");
        writer.flush();
        Log.v("Sent: " + result);
      }else{
        ConnectionThread networkThread = new ConnectionThread(sock,false,0,reader,writer);
        mConnectionThreads.put(0, networkThread);
        networkThread.start();
        notifyOnConnect();
      }
    }
  }

  public void shutdown() {
    // Stop listening on the server socket to ensure that
    // beyond this point there are no incoming requests.
    mStopServer = true;
    try {
      mServer.close();
    } catch (IOException e) {
      Log.e("Failed to close server socket.", e);
    }
    // Since the server is not running, the mNetworkThreads set can only
    // shrink from this point onward. We can just stop all of the running helper
    // threads. In the worst case, one of the running threads will already have
    // shut down. Since this is a CopyOnWriteList, we don't have to worry about
    // concurrency issues while iterating over the set of threads.
    for (ConnectionThread connectionThread : mConnectionThreads.values()) {
      connectionThread.close();
    }
    for (SimpleServerObserver observer : mObservers) {
      removeObserver(observer);
    }
  }
}
