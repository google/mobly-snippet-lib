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

import com.google.android.mobly.snippet.manager.SnippetManager;
import com.google.android.mobly.snippet.manager.SnippetManagerFactory;
import com.google.android.mobly.snippet.util.Log;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** A JSON RPC server that forwards RPC calls to a specified receiver object. */
public class JsonRpcServer extends SimpleServer {
    private static final String CMD_CLOSE_SESSION = "closeSl4aSession";
    private static final String CMD_HELP = "help";

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
    protected void handleRPCConnection(
            Socket sock, Integer UID, BufferedReader reader, PrintWriter writer) throws Exception {
        SnippetManager receiverManager = null;
        Map<Integer, SnippetManager> mgrs = mSnippetManagerFactory.getSnippetManagers();
        synchronized (mgrs) {
            Log.d("UID " + UID);
            Log.d("manager map keys: " + mSnippetManagerFactory.getSnippetManagers().keySet());
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

            // Handle builtin commands
            if (method.equals(CMD_HELP)) {
                help(writer, id, receiverManager, UID);
                continue;
            } else if (method.equals(CMD_CLOSE_SESSION)) {
                Log.d("Got shutdown signal");
                synchronized (writer) {
                    // Shut down all RPC receivers.
                    for (SnippetManager manager :
                            mSnippetManagerFactory.getSnippetManagers().values()) {
                        manager.shutdown();
                    }

                    // Shut down this client connection. As soon as this happens, the client will
                    // kill us by triggering the 'stop' action from another instrumentation, so no
                    // other cleanup steps are guaranteed to execute.
                    send(writer, JsonRpcResult.empty(id), UID);
                    reader.close();
                    writer.close();
                    sock.close();

                    // Shut down this server.
                    shutdown();
                    mgrs.clear();
                }
                return;
            }

            MethodDescriptor rpc = receiverManager.getMethodDescriptor(method);
            if (rpc == null) {
                send(writer, JsonRpcResult.error(id, new RpcError("Unknown RPC: " + method)), UID);
                continue;
            }
            try {
                /** If calling an {@link AsyncRpc}, put the message ID as the first param. */
                if (rpc.isAsync()) {
                    String callbackId = String.format("%d-%d", UID, id);
                    JSONArray newParams = new JSONArray();
                    newParams.put(callbackId);
                    for (int i = 0; i < params.length(); i++) {
                        newParams.put(params.get(i));
                    }
                    Object returnValue = rpc.invoke(receiverManager, newParams);
                    send(writer, JsonRpcResult.callback(id, returnValue, callbackId), UID);
                } else {
                    Object returnValue = rpc.invoke(receiverManager, params);
                    send(writer, JsonRpcResult.result(id, returnValue), UID);
                }
            } catch (Throwable t) {
                Log.e("Invocation error.", t);
                send(writer, JsonRpcResult.error(id, t), UID);
            }
        }
    }

    private void help(PrintWriter writer, int id, SnippetManager receiverManager, Integer UID)
            throws JSONException {
        // Create a map from class simple name to the methods inside it.
        Map<String, Set<MethodDescriptor>> methods = new TreeMap<>();
        for (String method : receiverManager.getMethodNames()) {
            MethodDescriptor descriptor = receiverManager.getMethodDescriptor(method);
            String snippetClassName = descriptor.getSnippetClass().getSimpleName();
            Set<MethodDescriptor> snippetClassMethods = methods.get(snippetClassName);
            if (snippetClassMethods == null) {
                // Preserve insertion order (alphabetical)
                snippetClassMethods = new LinkedHashSet<>();
                methods.put(snippetClassName, snippetClassMethods);
            }
            snippetClassMethods.add(descriptor);
        }
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, Set<MethodDescriptor>> entry : methods.entrySet()) {
            result.append("\nRPC Methods in ").append(entry.getKey()).append(":\n");
            for (MethodDescriptor descriptor : entry.getValue()) {
                result.append("  ").append(descriptor.getHelp()).append("\n");
            }
        }
        send(writer, JsonRpcResult.result(id, result), UID);
    }

    private void send(PrintWriter writer, JSONObject result, int UID) {
        writer.write(result + "\n");
        writer.flush();
        Log.v("Session " + UID + " Sent: " + result);
    }

    @Override
    protected void handleConnection(Socket socket) throws Exception {}
}
