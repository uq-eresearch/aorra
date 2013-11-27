/*jslint nomen: true, white: true, vars: true, eqeq: true, todo: true, unparam: true */
/*global _: false, $: false, Backbone: false, define: false, window: false */
define(['backbone'], function(Backbone) {
  var EventFeed = function(config) {
    var obj = _.extend({}, config);
    _.extend(obj, Backbone.Events);
    _.extend(obj, {
      url: function() {
        return '/events?from='+obj.lastEventId;
      },
      updateLastId: function(id) {
        obj.lastEventId = id;
      },
      reopen: function(lastEventID) {
        this.updateLastId(lastEventID);
        this.trigger('recheck');
      },
      open: function() {
        var trigger = _.bind(this.trigger, this);
        var triggerRecheck = function() {
          trigger('recheck');
        };
        // Are we using a modern browser, or are we using IE?
        if (_.isUndefined(window.EventSource)) {
          var poll = _.bind(function(callback) {
            var updateLastId = _.bind(this.updateLastId, this);
            $.ajax({
              url: this.url(),
              dataType: 'json',
              success: function(data) {
                var canContinue = _(data).all(function(v) {
                  if (v.type == 'outofdate') {
                    trigger('outofdate', v.id);
                    return false;
                  }
                  updateLastId(v.id);
                  trigger(v.type, v.data);
                  return true;
                });
                if (canContinue) {
                  callback();
                }
              },
              error: callback
            });
          }, this);
          this.on('recheck', function() {
            poll(function() {
              _.delay(triggerRecheck, 5000);
            });
          });
        } else {
          this.on('recheck', function() {
            // EventSource
            var es = new EventSource(this.url());
            es.addEventListener('outofdate', function(event) {
              trigger('outofdate', event.data);
              es.close();
            });
            es.addEventListener('ping', function(event) {
              trigger('ping', event.data);
            });
            _.each(['folder', 'file', 'flag', 'notification'], function(t) {
              _.each(['create', 'update', 'delete'], function(n) {
                var eventName = t+":"+n;
                es.addEventListener(eventName, function(event) {
                  // Ensure that notifications always follow the UI events
                  // that trigger them.
                  _.delay(function() {
                    trigger(eventName, event.data);
                  }, 100);
                });
              });
            });
            this.es = es;
          });
        }
        triggerRecheck();
      }
    });

    return obj;
  };

  return EventFeed;
});