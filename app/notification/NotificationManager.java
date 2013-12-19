package notification;

import java.net.MalformedURLException;
import java.net.URL;

import play.Application;
import play.Logger;
import play.Play;
import play.Plugin;
import play.api.mvc.Call;
import play.libs.Akka;
import service.GuiceInjectionPlugin;
import service.filestore.FileStore;
import akka.actor.TypedActor;
import akka.actor.TypedProps;
import akka.japi.Creator;

import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Module;

public class NotificationManager extends Plugin {

  private final static String NOTIFICATION_EMAILS = "application.notification.emails";

  private final Application application;

  private EmailNotificationScheduler scheduler;

  public NotificationManager(Application application) {
    this.application = application;
  }

  @Override
  public void onStart() {
    typedActor(Notifier.class, NotifierImpl.class);
    if(application.configuration().getBoolean(
        NOTIFICATION_EMAILS, Boolean.TRUE)) {
      scheduler = new EmailNotificationScheduler(
          typedActor(EmailNotifier.class, EmailNotifierImpl.class));
      scheduler.start();
    } else {
      Logger.info(String.format(
          "No notifications emails are send (%s is false)", NOTIFICATION_EMAILS));
    }
  }

  private <T> T typedActor(final Class<T> iface, final Class <? extends T> impl) {
    return TypedActor.get(Akka.system()).typedActorOf(
        new TypedProps<T>(iface, new Creator<T>() {
          @Override
          public T create() {
            return injector().getInstance(impl);
          }
        }));
  }

  @Override
  public void onStop() {
    scheduler.stop();
  }

  private Injector injector() {
    return GuiceInjectionPlugin.getInjector(application)
        .createChildInjector(new Module() {
          @Override
          public void configure(Binder binder) {
            binder.bind(Application.class).toInstance(application);
          }
        });
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
