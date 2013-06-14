package service;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

import java.util.LinkedList;
import java.util.List;
import java.util.NavigableMap;

import org.junit.Test;

import service.EventTimeline.ForgottenEventException;

public class InMemoryEventTimelineTest {

  @Test
  public void canRecordAndRetrieve() throws ForgottenEventException {
    final EventTimeline<String,String> et = new InMemoryEventTimeline<String>();
    assertThat(et.getKnown()).isEmpty();
    et.record("Hello World!");
    final NavigableMap<String, String> events = et.getKnown();
    assertThat(events).isNotEmpty();
    assertThat(events).hasSize(1);
    assertThat(events.lastEntry().getValue()).isEqualTo("Hello World!");
    assertThat(et.getSince(events.lastKey())).isEmpty();
  }

  @Test
  public void canForget() throws ForgottenEventException {
    final EventTimeline<String,Object> et = new InMemoryEventTimeline<Object>();
    final String id1;
    final String id2;
    {
      assertThat(et.getLastEventId().endsWith("0")).isTrue();
      id1 = et.record(new Object());
      assertThat(et.getLastEventId()).isEqualTo(id1);
      id2 = et.record(new Object());
    }
    assertThat(et.getLastEventId()).isEqualTo(id2);
    assertThat(et.getKnown()).hasSize(2);
    assertThat(et.getSince(id1)).hasSize(1);
    assertThat(et.getSince(id2)).hasSize(0);
    forceOutOfMemory();
    assertThat(et.getKnown()).isEmpty();
    try {
      et.getSince(id1);
      fail("Expected ForgottenEventException");
    } catch (ForgottenEventException e) {
      // All good
    }
    assertThat(et.getLastEventId()).isEqualTo(id2);
  }

  public void forceOutOfMemory() {
    try {
      final List<Object[]> memhog = new LinkedList<Object[]>();
      while (true) {
        memhog.add(new Object[102400]);
      }
    } catch (OutOfMemoryError e) {
      return;
    }
  }

}
