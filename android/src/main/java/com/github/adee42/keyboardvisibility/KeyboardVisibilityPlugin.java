package com.github.adee42.keyboardvisibility;

import io.flutter.Log;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.EventChannel.EventSink;
import io.flutter.plugin.common.EventChannel.StreamHandler;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.embedding.engine.plugins.FlutterPlugin;

import android.app.Activity;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;


/**
 * Compatible v1/v2 plugin 插件问题
 *
 * @author WangKing
 */
public class KeyboardVisibilityPlugin implements StreamHandler, ActivityAware, ViewTreeObserver.OnGlobalLayoutListener , FlutterPlugin {
    private static final String TAG = "WangKing:KeyboardVisibilityPlugin";

    private static final String STREAM_CHANNEL_NAME = "github.com/adee42/flutter_keyboard_visibility";
    private View mainView = null;

    private EventSink eventsSink;
    private EventChannel eventChannel;

    private boolean isVisible;

    /**
     * Plugin v1 版本
     * @param registrar Receiver of registrations from a single plugin.
     */
    public static void registerWith(Registrar registrar) {
        KeyboardVisibilityPlugin instance = new KeyboardVisibilityPlugin();
        instance.onAttachedToEngine(registrar.messenger());
    }

    /**
     * Plugin v2 版本支持
     * @param binding  Resources made available to all plugins registered with a given from FlutterEngine
     */
    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        onAttachedToEngine(binding.getBinaryMessenger());
    }

    private void onAttachedToEngine(BinaryMessenger messenger){
        eventChannel = new EventChannel(messenger,STREAM_CHANNEL_NAME);
        eventChannel.setStreamHandler(this);
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        eventsSink = null;
        eventChannel.setStreamHandler(null);
        eventChannel = null ;
    }

    @Override
    public void onGlobalLayout() {
        Rect r = new Rect();

        if (mainView != null) {
            mainView.getWindowVisibleDisplayFrame(r);

            // check if the visible part of the screen is less than 85%
            // if it is then the keyboard is showing
            boolean newState = ((double)r.height() / (double)mainView.getRootView().getHeight()) < 0.85;

            if (newState != isVisible) {
                isVisible = newState;
                if (eventsSink != null) {
                    eventsSink.success(isVisible ? 1 : 0);
                }
            }
        }
    }

    private void registerListener(Activity activity){
        try {
            mainView = ((ViewGroup)activity.findViewById(android.R.id.content)).getChildAt(0);
            mainView.getViewTreeObserver().addOnGlobalLayoutListener(this);
        } catch (Exception e) {
            Log.w(TAG,"exception: " + e.getMessage());
        }
    }

    private void unregisterListener() {
        if (mainView != null) {
            mainView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            mainView = null;
        }
    }

    @Override
    public void onListen(Object arguments, final EventSink eventsSink) {
        // register listener
        this.eventsSink = eventsSink;

        // is keyboard is visible at startup, let our subscriber know
        if (isVisible) {
            eventsSink.success(1);
        }
    }

    @Override
    public void onCancel(Object arguments) {
        eventsSink = null;
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) { registerListener(binding.getActivity()); }

    @Override
    public void onDetachedFromActivity() { unregisterListener(); }

    @Override
    public void onDetachedFromActivityForConfigChanges() {}

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {}

}

