package test

import com.wingnest.play2.jackrabbit.plugin.ConfigConsts
import play.api.test.FakeApplication
import play.api.Play
import play.core.j.JavaGlobalSettingsAdapter
import com.google.inject.Injector
import scala.collection.JavaConversions._

object AorraTestUtils {
  val REPOSITORY_CONFIG_PATH = "test/repository.xml"
  val REPOSITORY_DIRECTORY_PATH = "file:./target/jackrabbittestrepository"

  def fakeApp = {
    new FakeApplication(
      additionalConfiguration = Map(
        ConfigConsts.CONF_JCR_REPOSITORY_URI -> REPOSITORY_DIRECTORY_PATH,
        ConfigConsts.CONF_JCR_REPOSITORY_CONFIG -> REPOSITORY_CONFIG_PATH,
        ConfigConsts.CONF_JCR_HAS_RECREATION_REQUIRE -> true,
        "crash.enabled" -> false))
  }
  
  def fakeJavaApp = {
    val scalaApp = fakeApp
    new play.test.FakeApplication(
        scalaApp.path,
        scalaApp.classloader,
        scalaApp.additionalConfiguration,
        scalaApp.additionalPlugins,
        scalaApp.global.asInstanceOf[JavaGlobalSettingsAdapter].underlying)
  }
  
  def injector() = {
    val global = Play.current.global
        .asInstanceOf[JavaGlobalSettingsAdapter].underlying
    global.getClass().getDeclaredField("INJECTOR").get(null)
          .asInstanceOf[Injector]
  }
}