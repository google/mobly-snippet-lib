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

package com.google.android.mobly.snippet.rpc;

import com.google.android.mobly.snippet.util.Log;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A JSON RPC server that forwards RPC calls to a specified receiver object.
 */
public class JsonRpcServer extends SimpleServer {

    private static final String CMD_CLOSE_SESSION = "closeSl4aSession";

    private final SnippetManagerFactory mSnippetManagerFactory;

    /**
     * Construct a {@link JsonRpcServer} connected to the provided {@link SnippetManager}.
     *
     * @param managerFactory the {@link SnippetManager} to register with the server
     */
    public JsonRpcServer(SnippetManagerFactory managerFactory) {
        mSnippetManagerFactory = managerFactory;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        // Notify all RPC receiving objects. They may have to clean up some of their state.
        for (SnippetManager manager : mSnippetManagerFactory.getSnippetManagers()
                .values()) {
            manager.shutdown();
        }
    }

    @Override
    protected void handleRPCConnection(Socket sock, Integer UID, BufferedReader reader,
            PrintWriter writer) throws Exception {
        SnippetManager receiverManager = null;
        Map<Integer, SnippetManager> mgrs = mSnippetManagerFactory.getSnippetManagers();
        synchronized (mgrs) {
            Log.d("UID " + UID);
            Log.d("manager map keys: "
                    + mSnippetManagerFactory.getSnippetManagers().keySet());
            if (mgrs.containsKey(UID)) {
                Log.d("Look up existing session");
                receiverManager = mgrs.get(UID);
            } else {
                Log.d("Create a new session");
                receiverManager = mSnippetManagerFactory.create(UID);
            }
        }
        String data;
        while ((data = reader.readLine()) != null) {
            Log.v("Session " + UID + " Received: " + data);
            JSONObject request = new JSONObject(data);
            int id = request.getInt("id");
            String method = request.getString("method");
            JSONArray params = request.getJSONArray("params");

            if (method.equals("help")) {
                help(writer, id, receiverManager, UID);
                continue;
            }

            MethodDescriptor rpc = receiverManager.getMethodDescriptor(method);
            if (rpc == null) {
                send(writer, JsonRpcResult.error(id, new RpcError("Unknown RPC: " + method)), UID);
                continue;
            }
            try {
                send(writer, JsonRpcResult.result(id, rpc.invoke(receiverManager, params)), UID);
            } catch (Throwable t) {
                Log.e("Invocation error.", t);
                send(writer, JsonRpcResult.error(id, t), UID);
            }
            if (method.equals(CMD_CLOSE_SESSION)) {
                Log.d("Got shutdown signal");
                synchronized (writer) {
                    receiverManager.shutdown();
                    reader.close();
                    writer.close();
                    sock.close();
                    shutdown();
                    mgrs.remove(UID);
                }
                return;
            }
        }
    }

    private void help(PrintWriter writer, int id, SnippetManager receiverManager, Integer UID)
        throws JSONException {
        StringBuilder result = new StringBuilder("Known methods:\n");
        for (String method : receiverManager.getMethodNames()) {
            MethodDescriptor descriptor = receiverManager.getMethodDescriptor(method);
            result.append("  ").append(descriptor.getHelp()).append("\n");
        }
        send(writer, JsonRpcResult.result(id, result), UID);
    }

    private void send(PrintWriter writer, JSONObject result, int UID) {
        writer.write(result + "\n");
        writer.flush();
        Log.v("Session " + UID + " Sent: " + result);
    }

    @Override
    protected void handleConnection(Socket socket) throws Exception {
    }
}
