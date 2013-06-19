require(['models', 'views'], function(models, views) {
  'use strict';
  var NotificationFeed = function(config) {
    var obj = _.extend({}, config)
    _.extend(obj, Backbone.Events);
    _.extend(obj, {
      url: function() {
        return '/notifications?from='+obj.lastEventId
      },
      updateLastId: function(id) {
        obj.lastEventId = id;
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
                    trigger('event:outofdate', v.id);
                    return false;
                  } else {
                    updateLastId(v.id);
                    trigger('event:'+v.type, v.data);
                    return true;
                  }
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
              trigger('event:outofdate', event.data);
              es.close();
            });
            _.each(['create', 'update', 'delete', 'ping'], function(n) {
              es.addEventListener(n, function(event) {
                trigger('event:'+n, event.data);
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

  $(function () {

    function catchErrors(f) {
      return function(struct) { try { f.apply(this, arguments); } catch (e) {} };
    }

    var fs = new models.FileStore();
    var fileTree = new views.FileTree();
    var notificationFeed = new NotificationFeed({
      lastEventId: window.lastEventID
    });
    // Event handlers
    notificationFeed.on("event:create",
      catchErrors(function(id) {
        fs.fetch();
      }));
    notificationFeed.on("event:update",
      catchErrors(function(id) {
        fs.get(id).fetch();
      }));
    notificationFeed.on("event:delete",
      catchErrors(function(id) {
        fs.remove(fs.get(id))
      }));

    window.fs = fs;
    var startRouting = function() {
      // If we're using IE8 heavily, then push state is just trouble
      if (window.location.pathname != '/') {
        window.location.href = "/#"+window.location.pathname.replace(/^\//,'');
      }
      Backbone.history.start({ pushState: false });
    };

    var layout = new views.AppLayout();
    window.layout = layout;
    layout.render();
    $('#content').append(layout.$el);

    fileTree.render();
    layout.sidebar.show(fileTree);

    fs.on('sync', function() {
      try {
        // Start router (as now we can load existing nodes)
        startRouting();
      } catch (e) {}
    });
    fs.on('reset', function() {
      fileTree.tree().load([]);
      fs.each(function(m) {
        fileTree.tree().add(m.asNodeStruct(), m.get('parent'));
      });
    });
    fs.on('add', function(m) {
      fileTree.tree().add(m.asNodeStruct(), m.get('parent'));
    });
    fs.on('remove', function(m) {
      fileTree.tree().remove(m.get('id'));
    });

    var Router = Backbone.Router.extend({
      routes: {
        "": "showStart",
        "file/:id": "showFile",
        "folder/:id": "showFolder"
      },
      showStart: function() {
        layout.showStart();
        this._setSidebarActive();
      },
      showFolder: function(id) {
        var node = fileTree.tree().find(id);
        if (node == null) {
          layout.showDeleted();
        } else {
          layout.showFolder(fs.get(node.id));
          this._highlightNode(node);
        }
        this._setMainActive();
      },
      showFile: function(id) {
        var node = fileTree.tree().find(id);
        if (node == null) {
          layout.showDeleted();
        } else {
          layout.showFile(fs.get(node.id));
          this._highlightNode(node);
        }
        this._setMainActive();
      },
      _setMainActive: function() {
        layout.main.$el.addClass('active');
        layout.sidebar.$el.removeClass('active');
        $('#nav-back').removeClass('hidden');
      },
      _setSidebarActive: function() {
        layout.sidebar.$el.addClass('active');
        layout.main.$el.removeClass('active');
        $('#nav-back').addClass('hidden');
      },
      _highlightNode: function(node) {
        // Show the active node on the tree
        $('.label.label-info').removeClass('label label-info');
        $(node.element()).children('.glyphtree-node-label')
          .addClass('label label-info');
      }
    })

    var router = new Router();

    fileTree.on("folder:select", function(folderId) {
      router.navigate("folder/"+folderId, {trigger: true});
    });
    fileTree.on("file:select", function(fileId) {
      router.navigate("file/"+fileId, {trigger: true});
    });
    fs.on("remove", function(m) {
      // Handle being on the deleted page already
      if (_.isUndefined(layout.main.currentView.model)) return;
      // If the current path has been deleted, then hide it.
      if (m.id == layout.main.currentView.model.id) {
        layout.showDeleted();
      }
    });

    if (_.isUndefined(window.filestoreJSON)) {
      fs.fetch();
    } else {
      fs.reset(window.filestoreJSON);
      startRouting();
    }

    // If our data is out-of-date, refresh and reopen event feed.
    notificationFeed.on("event:outofdate", function(id) {
      fs.reset();
      fs.fetch().done(function() {
        notificationFeed.updateLastId(id);
        notificationFeed.trigger('recheck');
      });
    });

    // Open feed
    notificationFeed.open();
  });
});