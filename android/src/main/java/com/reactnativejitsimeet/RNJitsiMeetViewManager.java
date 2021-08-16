package com.reactnativejitsimeet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import org.jitsi.meet.sdk.BroadcastEvent;
import org.jitsi.meet.sdk.BroadcastIntentHelper;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

@ReactModule(name = RNJitsiMeetViewManager.REACT_CLASS)
public class RNJitsiMeetViewManager extends SimpleViewManager<RNJitsiMeetView> {
    public static final String REACT_CLASS = "RNJitsiMeetView";
    private IRNJitsiMeetViewReference mJitsiMeetViewReference;
    private ReactApplicationContext mReactContext;

    public RNJitsiMeetViewManager(ReactApplicationContext reactContext, IRNJitsiMeetViewReference jitsiMeetViewReference) {
        mJitsiMeetViewReference = jitsiMeetViewReference;
        mReactContext = reactContext;
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onBroadcastReceived(intent);
        }
    };

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    public RNJitsiMeetView createViewInstance(ThemedReactContext context) {
        if (mJitsiMeetViewReference.getJitsiMeetView() == null) {
            RNJitsiMeetView view = new RNJitsiMeetView(context.getCurrentActivity());
            mJitsiMeetViewReference.setJitsiMeetView(view);
        }
        registerForBroadcastMessages();
        return mJitsiMeetViewReference.getJitsiMeetView();
    }

    @ReactProp(name="options")
    public void setProps(RNJitsiMeetView jitsiMeetView,  ReadableMap options) throws MalformedURLException {

        if(mJitsiMeetViewReference.getJitsiMeetView() != null) {
            mJitsiMeetViewReference.getJitsiMeetView().leave();
            mJitsiMeetViewReference.getJitsiMeetView().dispose();
        }

        String url = null;
        boolean audioOnly = false;

        if (mJitsiMeetViewReference.getJitsiMeetView() != null) {

            RNJitsiMeetUserInfo _userInfo = new RNJitsiMeetUserInfo();
            if (options != null) {
                if (options.hasKey("url") && options.getString("url") != null) {
                    url = options.getString("url");
                }
                if (options.hasKey("audioOnly")) {
                    if (options.getBoolean("audioOnly") || !options.getBoolean("audioOnly")) {
                        audioOnly = options.getBoolean("audioOnly");
                    }
                }
                if (options.hasKey("userInfo")) {
                    ReadableMap user = options.getMap("userInfo");
                    if (user.hasKey("displayName")) {
                        _userInfo.setDisplayName(user.getString("displayName"));
                    }
                    if (user.hasKey("email")) {
                        _userInfo.setEmail(user.getString("email"));
                    }
                    if (user.hasKey("avatar")) {
                        String avatarURL = user.getString("avatar");
                        try {
                            _userInfo.setAvatar(new URL(avatarURL));
                        } catch (MalformedURLException e) {
                        }
                    }
                }
            }

            RNJitsiMeetConferenceOptions jitsiOptions
                    = new RNJitsiMeetConferenceOptions.Builder()
                    .setRoom(url)
                    .setUserInfo(_userInfo)
                    .setFeatureFlag("pip.enabled", false)
                    .setFeatureFlag("calendar.enabled", false)
                    .setAudioOnly(audioOnly)
                    .build();

            mJitsiMeetViewReference.getJitsiMeetView().join(jitsiOptions);
        }
    }

    private void registerForBroadcastMessages() {
        IntentFilter intentFilter = new IntentFilter();
        for (BroadcastEvent.Type type : BroadcastEvent.Type.values()) {
            intentFilter.addAction(type.getAction());
        }

        LocalBroadcastManager.getInstance(mJitsiMeetViewReference.getJitsiMeetView().getContext()).registerReceiver(broadcastReceiver, intentFilter);
    }

    private void onBroadcastReceived(Intent intent) {
        if (intent != null) {

            BroadcastEvent event = new BroadcastEvent(intent);
            WritableMap eventMap = Arguments.createMap();

            switch (event.getType()) {
                case CONFERENCE_JOINED:
                    eventMap = Arguments.createMap();
                    eventMap.putString("url", (String) event.getData().get("url"));
                    eventMap.putString("error", (String) event.getData().get("error"));
                    mReactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                            mJitsiMeetViewReference.getJitsiMeetView().getId(),
                            "conferenceJoined",
                            eventMap);
                    break;

                case CONFERENCE_TERMINATED:
                    eventMap = Arguments.createMap();
                    eventMap.putString("url", (String) event.getData().get("url"));
                    eventMap.putString("error", (String) event.getData().get("error"));
                    mReactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                            mJitsiMeetViewReference.getJitsiMeetView().getId(),
                            "conferenceTerminated",
                            eventMap);

                    Intent hangupBroadcastIntent = BroadcastIntentHelper.buildHangUpIntent();
                    LocalBroadcastManager.getInstance(mJitsiMeetViewReference.getJitsiMeetView().getContext()).sendBroadcast(hangupBroadcastIntent);

                    mJitsiMeetViewReference.getJitsiMeetView().dispose();
                    break;

                case CONFERENCE_WILL_JOIN:
                    eventMap = Arguments.createMap();
                    eventMap.putString("url", (String) event.getData().get("url"));
                    eventMap.putString("error", (String) event.getData().get("error"));
                    mReactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                            mJitsiMeetViewReference.getJitsiMeetView().getId(),
                            "conferenceWillJoin",
                            eventMap);
                    break;
            }
        }
    }

    public Map getExportedCustomBubblingEventTypeConstants() {
        return MapBuilder.builder()
                .put("conferenceJoined", MapBuilder.of("phasedRegistrationNames", MapBuilder.of("bubbled", "onConferenceJoined")))
                .put("conferenceTerminated", MapBuilder.of("phasedRegistrationNames", MapBuilder.of("bubbled", "onConferenceTerminated")))
                .put("conferenceWillJoin", MapBuilder.of("phasedRegistrationNames", MapBuilder.of("bubbled", "onConferenceWillJoin")))
                .put("audioMuted", MapBuilder.of("phasedRegistrationNames", MapBuilder.of("bubbled", "onAudioMuted")))
                .put("participantJoined", MapBuilder.of("phasedRegistrationNames", MapBuilder.of("bubbled", "onParticipantJoined")))
                .put("participantLeft", MapBuilder.of("phasedRegistrationNames", MapBuilder.of("bubbled", "onParticipantLeft")))
                .put("messageReceived", MapBuilder.of("phasedRegistrationNames", MapBuilder.of("bubbled", "onMessageReceived")))
                .build();
    }
}