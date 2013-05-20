package test;

import static play.test.Helpers.fakeApplication;

import java.util.Map;

import org.jcrom.Jcrom;

import play.Application;
import play.Play;
import play.test.FakeApplication;
import service.GuiceInjectionPlugin;
import service.JcrSessionFactory;
import service.filestore.FileStore;
import service.filestore.FileStoreImpl;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Injector;
import com.wingnest.play2.jackrabbit.plugin.ConfigConsts;

public class AorraTestUtils {

  public static final String REPOSITORY_CONFIG_PATH = "test/repository.xml";
  public static final String REPOSITORY_DIRECTORY_PATH = "file:./target/jackrabbittestrepository";

  public static FakeApplication fakeAorraApp() {
    return fakeAorraApp(true);
  }

  public static FakeApplication fakeAorraApp(boolean muteErrors) {
    return fakeApplication(additionalConfig(muteErrors));
  }

  private static Map<String, Object> additionalConfig(boolean muteErrors) {
    ImmutableMap.Builder<String, Object> m = ImmutableMap
        .<String, Object> builder();
    if (muteErrors) {
      m.put("logger.play", "ERROR");
      m.put("logger.application", "ERROR");
    }
    m.put(ConfigConsts.CONF_JCR_REPOSITORY_URI, REPOSITORY_DIRECTORY_PATH);
    m.put(ConfigConsts.CONF_JCR_REPOSITORY_CONFIG, REPOSITORY_CONFIG_PATH);
    m.put(ConfigConsts.CONF_JCR_HAS_RECREATION_REQUIRE, true);
    m.put("crash.enabled", false);
    return m.build();
  }

  public static JcrSessionFactory sessionFactory() {
    return injector().getInstance(JcrSessionFactory.class);
  }

  public static FileStore fileStore() {
    return injector().getInstance(FileStoreImpl.class);
  }

  public static Jcrom jcrom() {
    return injector().getInstance(Jcrom.class);
  }

  protected static Injector injector() {
    return Play.application().plugin(GuiceInjectionPlugin.class)
        .getInjector();
  }

}