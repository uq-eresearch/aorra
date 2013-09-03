package notification;

import java.net.MalformedURLException;
import java.net.URL;

import play.Application;
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

  private final Application application;

  public NotificationManager(Application application) {
    this.application = application;
  }

  @Override
  public void onStart() {
    TypedActor.get(Akka.system()).typedActorOf(
      new TypedProps<NotifierImpl>(Notifier.class,
        new Creator<NotifierImpl>() {
          @Override
          public NotifierImpl create() {
            return injector().getInstance(NotifierImpl.class);
          }
        }));
  }

  @Override
  public void onStop() {}

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
