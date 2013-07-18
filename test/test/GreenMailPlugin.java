package test;

import play.Application;
import play.api.Plugin;

import com.icegreen.greenmail.util.GreenMail;

public class GreenMailPlugin implements Plugin {

  private final GreenMail greenMail;

  public GreenMailPlugin(Application app) {
    greenMail = new GreenMail(); // uses test ports by default
  }

  @Override
  public boolean enabled() { return true; }

  public GreenMail get() {
    return greenMail;
  }

  @Override
  public void onStart() {
    greenMail.start();
  }

  @Override
  public void onStop() {
    greenMail.stop();
  }

}
