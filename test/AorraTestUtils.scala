package test

import com.wingnest.play2.jackrabbit.plugin.ConfigConsts
import play.api.test.FakeApplication

object AorraTestUtils {
  val REPOSITORY_CONFIG_PATH = "test/repository.xml"
  val REPOSITORY_DIRECTORY_PATH = "file:./target/jackrabbittestrepository"

  def fakeApp = {
    new FakeApplication(
      additionalConfiguration = Map(
        ConfigConsts.CONF_JCR_REPOSITORY_URI -> REPOSITORY_DIRECTORY_PATH,
        ConfigConsts.CONF_JCR_REPOSITORY_CONFIG -> REPOSITORY_CONFIG_PATH,
        ConfigConsts.CONF_JCR_HAS_RECREATION_REQUIRE -> true))
  }
}