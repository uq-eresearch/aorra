package service;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;

public class OrderedEventTest {

  @Test
  public void compareById() {
    final OrderedEvent oe1 = new OrderedEvent("foo", null);
    final OrderedEvent oe2 = new OrderedEvent("foo", null);
    final OrderedEvent oe3 = new OrderedEvent("bar", null);
    assertThat(oe1.compare(oe2)).isEqualTo(0);
    assertThat(oe1.compare(oe3)).isNotEqualTo(0);
  }

}
