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
                    trigger('outofdate', v.id);
                    return false;
                  } else {
                    updateLastId(v.id);
                    trigger(v.type, v.data);
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
              trigger('outofdate', event.data);
              es.close();
            });
            es.addEventListener('ping', function(event) {
              trigger('ping', event.data);
            });
            _.each(['folder', 'file'], function(t) {
              _.each(['create', 'update', 'delete'], function(n) {
                var eventName = t+":"+n;
                es.addEventListener(eventName, function(event) {
                  trigger(eventName, event.data);
                });
              });  
            })
            
            this.es = es;
          });
        }
        triggerRecheck();
      }
    });

    return obj;
  };

  $(function () {

    var fs = new models.FileStore();
    var fileTree = new views.FileTree();
    var notificationFeed = new NotificationFeed({
      lastEventId: window.lastEventID
    });
    // Event handlers
    notificationFeed.on("folder:create", function(id) {
        // Create a stand-alone folder
        var folder = new models.Folder({ id: id });
        // Get the data for it
        folder.fetch().done(function() {
          // It exists, so add it to the collection
          fs.add([folder.toJSON()]);
        });
      });
    notificationFeed.on("file:create",
      function(id) {
        var file = new models.File({ id: id })
        file.fetch().done(function() {
          fs.add([file.toJSON()]);
        });
      });
    notificationFeed.on("folder:update file:update",
      function(id) {
        fs.get(id).fetch();
      });
    notificationFeed.on("folder:delete file:delete",
      function(id) {
        fs.remove(fs.get(id));
      });

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
      // Handle being on the deleted page already
      if (_.isUndefined(layout.main.currentView.model)) return;
      // If the current path has been deleted, then hide it.
      if (m.id == layout.main.currentView.model.id) {
        layout.showDeleted(m);
      }
    });

    var Router = Backbone.Router.extend({
      routes: {
        "": "showStart",
        "file/:id": "showFile",
        "folder/:id": "showFolder",
        "file/:id/version/:version/diff": "showFileDiff"
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
        }
        this._setMainActive();
      },
      showFile: function(id) {
        var node = fileTree.tree().find(id);
        if (node == null) {
          layout.showDeleted();
        } else {
          layout.showFile(fs.get(node.id));
        }
        this._setMainActive();
      },
      showFileDiff: function(id, version) {
        var node = fileTree.tree().find(id);
        if (node == null) {
          layout.showDeleted();
        } else {
          layout.showFileDiff(fs.get(node.id), version);
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
      }
    })

    var router = new Router();

    fileTree.on("folder:select", function(folderId) {
      router.navigate("folder/"+folderId, {trigger: true});
    });
    fileTree.on("file:select", function(fileId) {
      router.navigate("file/"+fileId, {trigger: true});
    });

    if (_.isUndefined(window.filestoreJSON)) {
      fs.fetch();
    } else {
      fs.reset(window.filestoreJSON);
      startRouting();
    }

    // If our data is out-of-date, refresh and reopen event feed.
    notificationFeed.on("outofdate", function(id) {
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