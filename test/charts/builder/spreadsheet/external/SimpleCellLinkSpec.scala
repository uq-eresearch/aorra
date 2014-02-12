package charts.builder.spreadsheet.external

import org.specs2.mutable.Specification

class SimpleCellLinkSpec extends Specification {

  "descriptively convert to string" in {
    val (src, dst) = ("parrot!$N$2", "volts$K$40")
    val link = new SimpleCellLink(src, dst)
    link.source must_== src
    link.destination must_== dst
    link.toString must contain(src)
    link.toString must contain(dst)
  }

}