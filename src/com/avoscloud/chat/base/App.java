package com.avoscloud.chat.base;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.StrictMode;
import com.avos.avoscloud.*;
import com.avos.avospush.push.AVPushServiceAppManager;
import com.avoscloud.chat.R;
import com.avoscloud.chat.avobject.AddRequest;
import com.avoscloud.chat.avobject.ChatGroup;
import com.avoscloud.chat.avobject.UpdateInfo;
import com.avoscloud.chat.service.ChatService;
import com.avoscloud.chat.service.UpdateService;
import com.avoscloud.chat.ui.activity.SplashActivity;
import com.avoscloud.chat.util.Logger;
import com.avoscloud.chat.util.PhotoUtil;
import com.avoscloud.chat.util.Utils;
import com.baidu.mapapi.SDKInitializer;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.utils.StorageUtils;

import java.io.File;

/**
 * Created by lzw on 14-5-29.
 */
public class App extends Application {
  public static final String DB_NAME = "chat.db3";
  public static final int DB_VER = 4;
  public static boolean debug = false;
  public static App ctx;

  @Override
  public void onCreate() {
    super.onCreate();
    ctx = this;
    Utils.fixAsyncTaskBug();

    String publicId = "g7gz9oazvrubrauf5xjmzp3dl12edorywm0hy8fvlt6mjb1y";
    String publicKey = "01p70e67aet6dvkcaag9ajn5mff39s1d5jmpyakzhd851fhx";

    String appId = "x3o016bxnkpyee7e9pa5pre6efx2dadyerdlcez0wbzhw25g";
    String appKey = "057x24cfdzhffnl3dzk14jh9xo2rq6w1hy1fdzt5tv46ym78";

    AVOSCloud.initialize(this, appId, appKey);

    //AVOSCloud.initialize(this, publicId,publicKey);

    AVObject.registerSubclass(AddRequest.class);
    AVObject.registerSubclass(ChatGroup.class);
    AVObject.registerSubclass(UpdateInfo.class);

    AVInstallation.getCurrentInstallation().saveInBackground();
    PushService.setDefaultPushCallback(ctx, SplashActivity.class);
    AVOSCloud.setDebugLogEnabled(debug);
    if (App.debug) {
      Logger.level = Logger.VERBOSE;
    } else {
      Logger.level = Logger.NONE;
    }
    initImageLoader(ctx);
    initBaidu();
    openStrictMode();
    if (AVUser.getCurrentUser() != null) {
      ChatService.openSession();
    }
  }

  public void openStrictMode() {
    if (App.debug) {
      StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
          .detectDiskReads()
          .detectDiskWrites()
          .detectNetwork()   // or .detectAll() for all detectable problems
          .penaltyLog()
          .build());
      StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
          .detectLeakedSqlLiteObjects()
          .detectLeakedClosableObjects()
          .penaltyLog()
              //.penaltyDeath()
          .build());
    }
  }

  private void initBaidu() {
    SDKInitializer.initialize(this);
  }

  public static App getInstance() {
    return ctx;
  }

  /**
   * 初始化ImageLoader
   */
  public static void initImageLoader(Context context) {
    File cacheDir = StorageUtils.getOwnCacheDirectory(context,
        "leanchat/Cache");
    ImageLoaderConfiguration config = PhotoUtil.getImageLoaderConfig(context, cacheDir);
    // Initialize ImageLoader with configuration.
    ImageLoader.getInstance().init(config);
  }

  public static void initTables() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {

          if (AVUser.getCurrentUser() == null) {
            throw new NullPointerException("Please run it when login");
          }
          //create AddRequest Table
          AddRequest addRequest = new AddRequest();
          addRequest.setFromUser(AVUser.getCurrentUser());
          addRequest.setToUser(AVUser.getCurrentUser());
          addRequest.setStatus(AddRequest.STATUS_WAIT);
          addRequest.save();
          addRequest.delete();

          UpdateService.createUpdateInfo();

          //create Avatar Table for default avatar
          Bitmap bitmap = BitmapFactory.decodeResource(App.ctx.getResources(), R.drawable.head);
          byte[] bs = Utils.getBytesFromBitmap(bitmap);
          AVFile file = new AVFile("head", bs);
          file.save();
          AVObject avatar = new AVObject("Avatar");
          avatar.put("file", file);
          avatar.save();

        } catch (AVException e) {
          e.printStackTrace();
        }
      }
    }).start();
  }
}
