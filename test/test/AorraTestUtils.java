package test;

import static play.test.Helpers.fakeApplication;

import java.util.Map;

import play.test.FakeApplication;

import com.google.common.collect.ImmutableMap;
import com.wingnest.play2.jackrabbit.plugin.ConfigConsts;

public class AorraTestUtils {

  public static final String REPOSITORY_CONFIG_PATH = "test/repository.xml";
  public static final String REPOSITORY_DIRECTORY_PATH = "file:./target/jackrabbittestrepository";

  public static FakeApplication fakeAorraApp() {
    return fakeApplication(additionalConfig());
  }

  private static Map<String, Object> additionalConfig() {
    ImmutableMap.Builder<String, Object> m = ImmutableMap
        .<String, Object> builder();
    m.put(ConfigConsts.CONF_JCR_REPOSITORY_URI, REPOSITORY_DIRECTORY_PATH);
    m.put(ConfigConsts.CONF_JCR_REPOSITORY_CONFIG, REPOSITORY_CONFIG_PATH);
    m.put(ConfigConsts.CONF_JCR_HAS_RECREATION_REQUIRE, true);
    m.put("crash.enabled", false);
    return m.build();
  }
}