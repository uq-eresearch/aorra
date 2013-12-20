package notification;

import java.net.MalformedURLException;
import java.net.URL;

import play.Application;
import play.Logger;
import play.Play;
import play.Plugin;
import play.api.mvc.Call;
import service.GuiceInjectionPlugin;
import service.filestore.FileStore;

import com.google.inject.Injector;

public class NotificationManager extends Plugin {

  private final static String NOTIFICATION_EMAILS = "application.notification.emails";

  private final Application application;

  private EmailNotificationScheduler scheduler;

  public NotificationManager(Application application) {
    this.application = application;
  }

  @Override
  public void onStart() {
    if(application.configuration().getBoolean(
        NOTIFICATION_EMAILS, Boolean.TRUE)) {
      scheduler = injector().getInstance((EmailNotificationScheduler.class));
      scheduler.start();
    } else {
      Logger.info(String.format(
          "No notifications emails are send (%s is false)", NOTIFICATION_EMAILS));
    }
  }

  @Override
  public void onStop() {
    if(scheduler!=null) {
      scheduler.stop();
    }
  }

  private Injector injector() {
    return GuiceInjectionPlugin.getInjector(application);
  }

  public static String absUrl(FileStore.FileOrFolder fof) {
    try {
      URL baseUrl = new URL(
          Play.application().configuration().getString("application.baseUrl"));
      return (new URL(baseUrl, getCall(fof).url())).toString();
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  private static Call getCall(FileStore.FileOrFolder fof) {
    final String id = fof.getIdentifier();
    if (fof instanceof FileStore.File) {
      return controllers.routes.FileStoreController.showFile(id);
    } else {
      return controllers.routes.FileStoreController.showFolder(id);
    }
  }

}
