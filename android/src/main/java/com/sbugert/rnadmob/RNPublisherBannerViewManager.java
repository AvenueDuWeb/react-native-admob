package com.sbugert.rnadmob;

import android.content.Context;
import android.location.Location;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableNativeMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableNativeArray;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.PixelUtil;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.ThemedReactContext;

import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.facebook.react.views.view.ReactViewGroup;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.doubleclick.AppEventListener;
import com.google.android.gms.ads.doubleclick.PublisherAdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.doubleclick.PublisherAdView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class ReactPublisherAdView extends RelativeLayout implements AppEventListener, LifecycleEventListener {

    protected PublisherAdView adView;

    ReadableNativeMap kvs;
    Location location;
    String contentUrl;
    String[] testDevices;
    AdSize[] validAdSizes;
    String adUnitID;
    AdSize adSize;

    public ReactPublisherAdView(final Context context) {
        super(context);
        this.createAdView();
    }

    private void createAdView() {
        if (this.adView != null){
            this.adView.destroy();
        }

        final Context context = getContext();
        this.adView = new PublisherAdView(context);
        this.adView.setAppEventListener(this);
        // TMP : Test config for fluid format
        this.adView.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL));
        this.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL));


        this.adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                int width = adView.getAdSize().getWidthInPixels(context);
                int height = adView.getAdSize().getHeightInPixels(context);
                int left = adView.getLeft();
                int top = adView.getTop();

                //int test = adView.getAdSize();
                // 1120 / 175
                adView.measure(width, height);
                adView.layout(left, top, left + width, top + height);
                sendOnSizeChangeEvent();
                sendEvent(RNPublisherBannerViewManager.EVENT_AD_LOADED, null);
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                String errorMessage = "Unknown error";
                switch (errorCode) {
                    case PublisherAdRequest.ERROR_CODE_INTERNAL_ERROR:
                        errorMessage = "Internal error, an invalid response was received from the ad server.";
                        break;
                    case PublisherAdRequest.ERROR_CODE_INVALID_REQUEST:
                        errorMessage = "Invalid ad request, possibly an incorrect ad unit ID was given.";
                        break;
                    case PublisherAdRequest.ERROR_CODE_NETWORK_ERROR:
                        errorMessage = "The ad request was unsuccessful due to network connectivity.";
                        break;
                    case PublisherAdRequest.ERROR_CODE_NO_FILL:
                        errorMessage = "The ad request was successful, but no ad was returned due to lack of ad inventory.";
                        break;
                }
                WritableMap event = Arguments.createMap();
                WritableMap error = Arguments.createMap();
                error.putString("message", errorMessage);
                event.putMap("error", error);
                sendEvent(RNPublisherBannerViewManager.EVENT_AD_FAILED_TO_LOAD, event);
            }

            @Override
            public void onAdOpened() {
                sendEvent(RNPublisherBannerViewManager.EVENT_AD_OPENED, null);
            }

            @Override
            public void onAdClosed() {
                sendEvent(RNPublisherBannerViewManager.EVENT_AD_CLOSED, null);
            }

            @Override
            public void onAdLeftApplication() {
                sendEvent(RNPublisherBannerViewManager.EVENT_AD_LEFT_APPLICATION, null);
            }
        });

        this.addView(this.adView);
    }

    private void sendOnSizeChangeEvent() {
        int width;
        int height;
        ReactContext reactContext = (ReactContext) getContext();
        WritableMap event = Arguments.createMap();
        AdSize adSize = this.adView.getAdSize();

        if (adSize == AdSize.SMART_BANNER) {
            width = (int) PixelUtil.toDIPFromPixel(adSize.getWidthInPixels(reactContext));
            height = (int) PixelUtil.toDIPFromPixel(adSize.getHeightInPixels(reactContext));
        } else {
            width = adSize.getWidth();
            height = adSize.getHeight();
        }

        int width1 = adSize.getWidth();
        int height1 = adSize.getHeight();

        int width2 = this.getWidth();
        int height2 = this.getHeight();

        int width22 = this.getMeasuredWidth();
        int height22 = this.getMeasuredHeight();

        int width3 = this.adView.getWidth();
        int height3 = this.adView.getHeight();

        int width4 = this.adView.getMeasuredWidth();
        int height4 = this.adView.getMeasuredHeight();

        // 320 - 50 // 400 - 110 // 1120 - 175 // 1440 - 875 // 1120 - 71
        event.putDouble("width", 400);
        event.putDouble("height", 110);
        sendEvent(RNPublisherBannerViewManager.EVENT_SIZE_CHANGE, event);
    }

    private void sendEvent(String name, @Nullable WritableMap event) {
        ReactContext reactContext = (ReactContext) getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                        getId(),
                        name,
                        event);
    }

    public void loadBanner() {
        ArrayList<AdSize> adSizes = new ArrayList<AdSize>();
        if (this.adSize != null) {
            adSizes.add(this.adSize);
        }
        if (this.validAdSizes != null) {
            for (int i = 0; i < this.validAdSizes.length; i++) {
                adSizes.add(this.validAdSizes[i]);
            }
        }

        if (adSizes.size() == 0) {
            adSizes.add(AdSize.BANNER);
        }

        AdSize[] adSizesArray = adSizes.toArray(new AdSize[adSizes.size()]);
        this.adView.setAdSizes(adSizesArray);

        PublisherAdRequest.Builder adRequestBuilder = new PublisherAdRequest.Builder();
        if (testDevices != null) {
            for (int i = 0; i < testDevices.length; i++) {
                String testDevice = testDevices[i];
                if (testDevice == "SIMULATOR") {
                    testDevice = PublisherAdRequest.DEVICE_ID_EMULATOR;
                }
                // adRequestBuilder.addTestDevice(testDevice);
            }
        }
        if (kvs != null) {
            ReadableMapKeySetIterator iterator = kvs.keySetIterator();
            while (iterator.hasNextKey()) {
                String key = iterator.nextKey();
                String value = kvs.getString(key);
                adRequestBuilder.addCustomTargeting(key, value);
            }
        }
        if (contentUrl != null) {
            adRequestBuilder.setContentUrl(contentUrl);
        }
        if (location != null) {
            adRequestBuilder.setLocation(location);
        }
        PublisherAdRequest adRequest = adRequestBuilder.build();
        this.adView.loadAd(adRequest);
    }

    public void setAdUnitID(String adUnitID) {
        if (this.adUnitID != null) {
            // We can only set adUnitID once, so when it was previously set we have
            // to recreate the view
            this.createAdView();
        }
        this.adUnitID = adUnitID;
        this.adView.setAdUnitId(adUnitID);
    }

    public void setTestDevices(String[] testDevices) {
        this.testDevices = testDevices;
    }

    public void setAdSize(AdSize adSize) {
        this.adSize = adSize;
    }

    public void setValidAdSizes(AdSize[] adSizes) {
        this.validAdSizes = adSizes;
    }

    public void setContentUrl(String contentUrl) {
        this.contentUrl = contentUrl;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setKvs (ReadableNativeMap kvs) { this.kvs = kvs; }

    @Override
    public void onAppEvent(String name, String info) {
        WritableMap event = Arguments.createMap();
        event.putString("name", name);
        event.putString("info", info);
        sendEvent(RNPublisherBannerViewManager.EVENT_APP_EVENT, event);
    }

    @Override
    public void onHostDestroy() {
        this.adView.destroy();
    }

    @Override
    public void onHostResume() {
        this.adView.resume();
    }

    @Override
    public void onHostPause() {
        this.adView.pause();
    }
}

public class RNPublisherBannerViewManager extends ViewGroupManager<ReactPublisherAdView> {

    public static final String REACT_CLASS = "RNDFPBannerView";

    public static final String PROP_AD_SIZE = "adSize";
    public static final String PROP_VALID_AD_SIZES = "validAdSizes";
    public static final String PROP_AD_UNIT_ID = "adUnitID";
    public static final String PROP_TEST_DEVICES = "testDevices";
    public static final String PROP_KVS = "kvs";
    public static final String PROP_CONTENT_URL = "contentUrl";
    public static final String PROP_LOCATION = "location";

    public static final String EVENT_SIZE_CHANGE = "onSizeChange";
    public static final String EVENT_AD_LOADED = "onAdLoaded";
    public static final String EVENT_AD_FAILED_TO_LOAD = "onAdFailedToLoad";
    public static final String EVENT_AD_OPENED = "onAdOpened";
    public static final String EVENT_AD_CLOSED = "onAdClosed";
    public static final String EVENT_AD_LEFT_APPLICATION = "onAdLeftApplication";
    public static final String EVENT_APP_EVENT = "onAppEvent";

    public static final int COMMAND_LOAD_BANNER = 1;

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    protected ReactPublisherAdView createViewInstance(ThemedReactContext themedReactContext) {
        ReactPublisherAdView adView = new ReactPublisherAdView(themedReactContext);
        return adView;
    }

    @Override
    public void addView(ReactPublisherAdView parent, View child, int index) {
        throw new RuntimeException("RNPublisherBannerView cannot have subviews");
    }

    @Override
    public void onDropViewInstance(ReactPublisherAdView view) {
        view.adView.setAppEventListener(null);
        view.adView.setAdListener(null);
        view.adView.destroy();
        super.onDropViewInstance(view);
    }

    @Override
    @Nullable
    public Map<String, Object> getExportedCustomDirectEventTypeConstants() {
        MapBuilder.Builder<String, Object> builder = MapBuilder.builder();
        String[] events = {
            EVENT_SIZE_CHANGE,
            EVENT_AD_LOADED,
            EVENT_AD_FAILED_TO_LOAD,
            EVENT_AD_OPENED,
            EVENT_AD_CLOSED,
            EVENT_AD_LEFT_APPLICATION,
            EVENT_APP_EVENT
        };
        for (int i = 0; i < events.length; i++) {
            builder.put(events[i], MapBuilder.of("registrationName", events[i]));
        }
        return builder.build();
    }

    @ReactProp(name = PROP_AD_SIZE)
    public void setPropAdSize(final ReactPublisherAdView view, final String sizeString) {
        AdSize adSize = getAdSizeFromString(sizeString);
        view.setAdSize(adSize);
    }

    @ReactProp(name = PROP_VALID_AD_SIZES)
    public void setPropValidAdSizes(final ReactPublisherAdView view, final ReadableArray adSizeStrings) {
        ReadableNativeArray nativeArray = (ReadableNativeArray)adSizeStrings;
        ArrayList<Object> list = nativeArray.toArrayList();
        String[] adSizeStringsArray = list.toArray(new String[list.size()]);
        AdSize[] adSizes = new AdSize[list.size()];

        for (int i = 0; i < adSizeStringsArray.length; i++) {
                String adSizeString = adSizeStringsArray[i];
                adSizes[i] = getAdSizeFromString(adSizeString);
        }
        view.setValidAdSizes(adSizes);
    }

    @ReactProp(name = PROP_AD_UNIT_ID)
    public void setPropAdUnitID(final ReactPublisherAdView view, final String adUnitID) {
        view.setAdUnitID(adUnitID);
    }

    @ReactProp(name = PROP_TEST_DEVICES)
    public void setPropTestDevices(final ReactPublisherAdView view, final ReadableArray testDevices) {
        ReadableNativeArray nativeArray = (ReadableNativeArray)testDevices;
        ArrayList<Object> list = nativeArray.toArrayList();
        view.setTestDevices(list.toArray(new String[list.size()]));
    }

    @ReactProp(name=PROP_KVS)
    public void setPropKVS(final ReactPublisherAdView view, final ReadableMap kvs) {
        ReadableNativeMap nativeMap = (ReadableNativeMap)kvs;
        view.setKvs(nativeMap);
    }

    @ReactProp(name=PROP_CONTENT_URL)
    public void setPropContentUrl(final ReactPublisherAdView view, final String contentUrl) {
        view.setContentUrl(contentUrl);
    }

    private AdSize getAdSizeFromString(String adSize) {
        switch (adSize) {
            case "banner":
                return AdSize.BANNER;
            case "largeBanner":
                return AdSize.LARGE_BANNER;
            case "mediumRectangle":
                return AdSize.MEDIUM_RECTANGLE;
            case "fullBanner":
                return AdSize.FULL_BANNER;
            case "leaderBoard":
                return AdSize.LEADERBOARD;
            case "smartBannerPortrait":
                return AdSize.SMART_BANNER;
            case "smartBannerLandscape":
                return AdSize.SMART_BANNER;
            case "smartBanner":
                return AdSize.SMART_BANNER;
            case "fluid":
                return AdSize.FLUID;
            default:
                return AdSize.BANNER;
        }
    }

    @Nullable
    @Override
    public Map<String, Integer> getCommandsMap() {
        return MapBuilder.of("loadBanner", COMMAND_LOAD_BANNER);
    }

    @Override
    public void receiveCommand(ReactPublisherAdView root, int commandId, @javax.annotation.Nullable ReadableArray args) {
        switch (commandId) {
            case COMMAND_LOAD_BANNER:
                root.loadBanner();
                break;
        }
    }
}
