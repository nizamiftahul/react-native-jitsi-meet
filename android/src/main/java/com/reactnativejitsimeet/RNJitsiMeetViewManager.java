package com.reactnativejitsimeet;

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

import org.jitsi.meet.sdk.JitsiMeetViewListener;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

@ReactModule(name = RNJitsiMeetViewManager.REACT_CLASS)
public class RNJitsiMeetViewManager extends SimpleViewManager<RNJitsiMeetView> implements JitsiMeetViewListener {
    public static final String REACT_CLASS = "RNJitsiMeetView";
    private IRNJitsiMeetViewReference mJitsiMeetViewReference;
    private ReactApplicationContext mReactContext;

    public RNJitsiMeetViewManager(ReactApplicationContext reactContext, IRNJitsiMeetViewReference jitsiMeetViewReference) {
        mJitsiMeetViewReference = jitsiMeetViewReference;
        mReactContext = reactContext;
    }

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    public RNJitsiMeetView createViewInstance(ThemedReactContext context) {
        if (mJitsiMeetViewReference.getJitsiMeetView() == null) {
            RNJitsiMeetView view = new RNJitsiMeetView(context.getCurrentActivity());
            view.setListener(this);
            mJitsiMeetViewReference.setJitsiMeetView(view);
        }
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
                    .setFeatureFlag("chat.enabled", false)
                    .setFeatureFlag("pip.enabled", false)
                    .setAudioOnly(audioOnly)
                    .build();

            mJitsiMeetViewReference.getJitsiMeetView().join(jitsiOptions);
        }
    }

    public void onConferenceJoined(Map<String, Object> data) {
        WritableMap event = Arguments.createMap();
        event.putString("url", (String) data.get("url"));
        mReactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                mJitsiMeetViewReference.getJitsiMeetView().getId(),
                "conferenceJoined",
                event);
    }

    public void onConferenceTerminated(Map<String, Object> data) {
        WritableMap event = Arguments.createMap();
        event.putString("url", (String) data.get("url"));
        event.putString("error", (String) data.get("error"));
        mReactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                mJitsiMeetViewReference.getJitsiMeetView().getId(),
                "conferenceTerminated",
                event);
    }

    public void onConferenceWillJoin(Map<String, Object> data) {
        WritableMap event = Arguments.createMap();
        event.putString("url", (String) data.get("url"));
        mReactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                mJitsiMeetViewReference.getJitsiMeetView().getId(),
                "conferenceWillJoin",
                event);
    }

    public Map getExportedCustomBubblingEventTypeConstants() {
        return MapBuilder.builder()
                .put("conferenceJoined", MapBuilder.of("phasedRegistrationNames", MapBuilder.of("bubbled", "onConferenceJoined")))
                .put("conferenceTerminated", MapBuilder.of("phasedRegistrationNames", MapBuilder.of("bubbled", "onConferenceTerminated")))
                .put("conferenceWillJoin", MapBuilder.of("phasedRegistrationNames", MapBuilder.of("bubbled", "onConferenceWillJoin")))
                .build();
    }
}