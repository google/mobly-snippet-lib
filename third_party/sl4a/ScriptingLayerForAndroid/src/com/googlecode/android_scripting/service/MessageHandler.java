package com.googlecode.android_scripting.service;

import android.annotation.TargetApi;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.RemoteException;

import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.jsonrpc.JsonRpcResult;
import com.googlecode.android_scripting.jsonrpc.RpcReceiverManager;
import com.googlecode.android_scripting.jsonrpc.RpcReceiverManagerFactory;
import com.googlecode.android_scripting.rpc.MethodDescriptor;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class responsible for Handling messages that came through the FacadeService
 * interface.
 * <br>
 * Please refer to {@link FacadeService} for details on how to use.
 */
@TargetApi(3)
public class MessageHandler extends Handler {

    private static final int SL4A_ACTION = 0;
    private static final int DEFAULT_SENDING_ID = 0;

    // Android sets this to -1 when the message is not sent by a Messenger.
    // see http://developer.android.com/reference/android/os/Message.html#sendingUid
    private static final int DEFAULT_UNSET_SENDING_ID = 1;

    // Keys for the Bundles.
    private static final String SL4A_METHOD = "sl4aMethod";
    private static final String SL4A_RESULT = "sl4aResult";

    private final RpcReceiverManagerFactory mRpcReceiverManagerFactory;

    public MessageHandler(HandlerThread handlerThread,
                          RpcReceiverManagerFactory rpcReceiverManagerFactory) {
        super(handlerThread.getLooper());
        this.mRpcReceiverManagerFactory = rpcReceiverManagerFactory;
    }

    /**
     * Handles messages for the service. It does this via the same mechanism used
     * for RPCs through RpcManagers.
     *
     * @param message The message that contains the method and parameters to
     *     execute.
     */
    @Override
    public void handleMessage(Message message) {
        Log.d("Handling Remote request");
        int senderId = message.sendingUid == DEFAULT_UNSET_SENDING_ID ?
                DEFAULT_SENDING_ID : message.sendingUid;
        if (message.what == SL4A_ACTION) {
            RpcReceiverManager receiverManager;
            if (mRpcReceiverManagerFactory.getRpcReceiverManagers().containsKey(senderId)) {
                receiverManager = mRpcReceiverManagerFactory.getRpcReceiverManagers().get(senderId);
            } else {
                receiverManager = mRpcReceiverManagerFactory.create(senderId);
            }
            Bundle sl4aRequest = message.getData();
            String method = sl4aRequest.getString(SL4A_METHOD);
            if (method == null || "".equals(method)) {
                Log.e("No SL4A method specified on the Bundle. Specify one with "
                        + SL4A_METHOD);
                return;
            }
            MethodDescriptor rpc = receiverManager.getMethodDescriptor(method);
            if (rpc == null) {
                Log.e("Unknown RPC: \"" + method + "\"");
                return;
            }
            try {
                Log.d("Invoking method " + rpc.getName());
                Object result = rpc.invoke(receiverManager, sl4aRequest);
                // Only return a result if we were passed a Messenger. Otherwise assume
                // client did not care for the response.
                if (message.replyTo != null) {
                    Message reply = Message.obtain();
                    Bundle sl4aResponse = new Bundle();
                    putResult(senderId, result, sl4aResponse);
                    reply.setData(sl4aResponse);
                    message.replyTo.send(reply);
                }
            } catch (RemoteException e) {
                Log.e("Could not send reply back to client", e);
            } catch (Throwable t) {
                Log.e("Exception while executing sl4a method", t);
            }
        }
    }

    private void putResult(int id, Object result, Bundle reply) {
        JSONObject json;
        try {
            if (result instanceof Throwable) {
                json = JsonRpcResult.error(id, (Throwable) result);
            } else {
                json = JsonRpcResult.result(id, result);
            }
        } catch (JSONException e) {
            // There was an error converting the result to JSON. This shouldn't
            // happen normally.
            Log.e("Caught exception when filling JSON result.", e);
            reply.putString(SL4A_RESULT, e.toString());
            return;
        }
        Log.d("Returning result: " + json.toString());
        reply.putString(SL4A_RESULT, json.toString());
    }
}
