package service;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

import java.util.Calendar;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.ImmutableSortedMap;

public class InMemoryEventTimeline<E> implements EventTimeline<String, E> {

  private static final String IDENTIFIER_FORMAT =
      "%0"+Long.toHexString(Long.MAX_VALUE).length()+"x"+
      "%0"+Integer.toHexString(Integer.MAX_VALUE).length()+"x";

  private final AtomicInteger counter = new AtomicInteger();
  private final ConcurrentNavigableMap<String, Reference<E>> timeline =
      new ConcurrentSkipListMap<String, Reference<E>>();

  // Initial lost event ID is lexigraphically less than any valid ID
  private String lastLostEventId = getIdentifier(getMillis(), 0);

  @Override
  public String getLastEventId() {
    if (timeline.isEmpty())
      return lastLostEventId;
    return timeline.lastKey();
  }

  @Override
  public NavigableMap<String, E> getKnown() {
    try {
      return resolveRefs(timeline);
    } catch (ForgottenEventException e) {
      // GC may have run, so cleanup and try again
      cleanup();
      return getKnown();
    }
  }

  @Override
  public NavigableMap<String, E> getSince(String id)
      throws ForgottenEventException {
    if (id.compareTo(lastLostEventId) < 0) {
      throw new ForgottenEventException(id + " is before known history.");
    }
    return resolveRefs(timeline.tailMap(id,false));
  }

  @Override
  public synchronized String record(E event) {
    cleanup();
    final String id = getNewIdentifier();
    timeline.put(id, getReference(event));
    return id;
  }

  protected NavigableMap<String, E> resolveRefs(
      final NavigableMap<String, Reference<E>> m)
      throws service.EventTimeline.ForgottenEventException {
    final ImmutableSortedMap.Builder<String, E> b = ImmutableSortedMap
        .<String, E> naturalOrder();
    for (final Map.Entry<String, Reference<E>> e : m.entrySet()) {
      final E v = e.getValue().get();
      if (v == null)
        throw new ForgottenEventException(e.getKey() + " has been forgotten.");
      b.put(e.getKey(), v);
    }
    return b.build();
  }

  protected void cleanup() {
    for (String k : timeline.keySet()) {
      if (timeline.get(k).get() != null) return;
      timeline.remove(k);
      lastLostEventId = k;
    }
  }

  /*
   * This method can be overridden for testing purposes.
   */
  protected Reference<E> getReference(E referent) {
    return new SoftReference<E>(referent);
  }

  private String getNewIdentifier() {
    return getIdentifier(getMillis(), counter.addAndGet(1));
  }

  private static String getIdentifier(final long ms, final int c) {
    return String.format(IDENTIFIER_FORMAT, ms, c);
  }

  private static Long getMillis() {
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis();
  }

}
