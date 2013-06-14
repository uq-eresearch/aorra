package service;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;
import java.util.NavigableMap;

import org.junit.Test;

import com.sun.jna.platform.unix.X11.GC;

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
    // Check that we can handle garbage collection
    System.gc();
    assertThat(et.getKnown()).hasSize(1);
  }

  @Test
  public void canForget() throws ForgottenEventException {
    final EventTimeline<String,Object> et = new InMemoryEventTimeline<Object>() {
      // This version will forget on garbage collection
      @Override
      protected Reference<Object> getReference(Object obj) {
        return new WeakReference<Object>(obj);
      }
    };
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
    System.gc(); // References should disappear now
    assertThat(et.getKnown()).isEmpty();
    try {
      et.getSince(id1);
      fail("Expected ForgottenEventException");
    } catch (ForgottenEventException e) {
      // All good
    }
    assertThat(et.getLastEventId()).isEqualTo(id2);
  }

}
