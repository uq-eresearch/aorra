package service.filestore;

import groovy.lang.Immutable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;

import play.Logger;
import play.api.libs.iteratee.Concurrent.Channel;
import scala.Tuple2;
import service.EventTimeline;
import service.EventTimeline.ForgottenEventException;
import service.InMemoryEventTimeline;

public class EventManagerImpl implements EventManager {

  private final EventTimeline<String, FileStoreEvent> history =
      new InMemoryEventTimeline<FileStoreEvent>();

  private final Set<Channel<Tuple2<String, FileStoreEvent>>> channels =
      Collections.synchronizedSet(
          new HashSet<Channel<Tuple2<String, FileStoreEvent>>>());

  @Override
  public String getLastEventId() {
    return history.getLastEventId();
  }

  @Override
  public Iterable<Tuple2<String,FileStoreEvent>> getSince(final String lastId) {
    final ImmutableList.Builder<Tuple2<String,FileStoreEvent>> b =
        ImmutableList.<Tuple2<String,FileStoreEvent>>builder();
    Map<String, FileStoreEvent> missed;
    try {
      missed = history.getSince(lastId);
    } catch (ForgottenEventException e1) {
      // TODO Handle forgotten history
      return ImmutableList.of();
    }
    for (Map.Entry<String, FileStoreEvent> e : missed.entrySet()) {
      // Push event ID and event
      b.add(new Tuple2<String, FileStoreEvent>(e.getKey(), e.getValue()));
    }
    return b.build();
  }

  @Override
  public void tell(final ChannelMessage<Tuple2<String, FileStoreEvent>> message) {
    switch (message.type) {
    case ADD:
      Logger.debug(this+" - Adding notification channel.");
      performCatchup(message.channel, message.lastId);
      channels.add(message.channel);
      break;
    case REMOVE:
      Logger.debug(this+" - Removing notification channel.");
      channels.remove(message.channel);
      break;
    }
  }

  @Override
  public void tell(final FileStoreEvent event) {
    Logger.debug(this+" - Adding event to history: "+event);
    history.record(event);
    Logger.debug(this+" - Pushing event to channels: "+event);
    for (Channel<Tuple2<String, FileStoreEvent>> channel : channels) {
      // Push event ID and event
      channel.push(new Tuple2<String, FileStoreEvent>(
          history.getLastEventId(), event));
      Logger.debug(this+" - Pushed to channel: "+channel);
    }
  }

  protected void performCatchup(
      final Channel<Tuple2<String, FileStoreEvent>> channel,
      final String lastId) {
    Logger.debug("Catching up from "+lastId);
    if (lastId == null) {
      // TODO: Handle this by sending full load
    } else {
      try {
        final Map<String, FileStoreEvent> missed = history.getSince(lastId);
        for (Map.Entry<String, FileStoreEvent> e : missed.entrySet()) {
          Logger.debug(this+"- Pushing missed event "+e+" to channel: "+channel);
          // Push event ID and event
          channel.push(new Tuple2<String, FileStoreEvent>(
              e.getKey(), e.getValue()));
        }
      } catch (ForgottenEventException e) {
        // TODO: Handle this by sending full load
      }
    }
  }

}