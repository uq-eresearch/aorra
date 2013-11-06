package helpers

import org.specs2.mutable.Specification

class AppVersionSpec extends Specification {

  "App Version" can {

    "evaluate as a conforming version string" in {
      AppVersion.toString must beMatching("""^\d+\.\d+\.\d+(-\w+)?""")
    }

  }
}
