require(['models', 'views'], function(models, views) {
  'use strict';
  var NotificationFeed = function(config) {
    var obj = _.extend({}, config)
    _.extend(obj, Backbone.Events);
    _.extend(obj, {
      url: '/notifications',
      _feedFromEventID: function(eventId) {
        return this.url+"?from="+eventId;
      },
      open: function() {
        var trigger = _.bind(this.trigger, this);
        // Are we using a modern browser, or are we using IE?
        if (typeof(window.EventSource) == 'undefined') {
          // HTML iframe
          var connect_htmlfile = function (url, callback) {
            var ifrDiv = document.createElement("div");
            var iframe = document.createElement("iframe");
            ifrDiv.setAttribute("style", "display: none");
            document.body.appendChild(ifrDiv);
            ifrDiv.appendChild(iframe);
            iframe.src = url;
            iframe.onload = function() {
              try {
                iframe.currentWindow.location.reload();
              } catch (e) {}
            };
            // From: http://davidwalsh.name/window-iframe
            function eventHandler(w, eventName, handler) {
              var eMethod = w.addEventListener ? "addEventListener" : "attachEvent";
              var eName = w.addEventListener ? eventName : "on" + eventName;
              return w[eMethod](eName, handler, false);
            }
            // Receive messages from iframe
            eventHandler(window, 'message', function(e) {
              var msg = JSON.parse(e.data);
              callback(msg.type, msg.data);
            });
          }
          connect_htmlfile(this.url, function(eventType, data) {
            trigger('event:'+eventType, data);
          });
        } else {
          // EventSource
          var es = new EventSource(this._feedFromEventID(window.lastEventID));
          es.addEventListener('ping', function(event) {
            trigger('event:ping', event.data);
          });
          _.each(['create', 'update', 'delete'], function(n) {
            es.addEventListener(n, function(event) {
              var struct = JSON.parse(event.data);
              trigger('event:'+n, struct);
            });
          });
          this.es = es;
        }
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
    var notificationFeed = new NotificationFeed({});
    // Event handlers
    notificationFeed.on("event:create",
      catchErrors(function(struct) { 
        fs.fetch();
      }));
    notificationFeed.on("event:update",
      catchErrors(function(struct) { 
        fs.get(struct.id).fetch();
      }));
    notificationFeed.on("event:delete",
      catchErrors(function(struct) { 
        fs.remove(fs.get(struct.id)) 
      }));
    
    window.fs = fs;
    var startRouting = function() {
      Backbone.history.start({ pushState: true, hashChange: false });
    };
    
    var mainPane = new views.MainPane();
    fileTree.render();
    $('#sidebar').append(fileTree.$el);
    $('#main').append(mainPane.$el);

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
        mainPane.showStart();
        this._setSidebarActive();
      },
      showFolder: function(id) {
        var node = fileTree.tree().find(id);
        if (node == null) {
          mainPane.showDeleted()
        } else {
          mainPane.showFolder(fs.get(node.id));
          this._highlightNode(node);
        }
        this._setMainActive();
      },
      showFile: function(id) {
        var node = fileTree.tree().find(id);
        if (node == null) {
          mainPane.showDeleted()
        } else {
          mainPane.showFile(fs.get(node.id));
          this._highlightNode(node);
        }
        this._setMainActive();
      },
      _setMainActive: function() {
        $('#main').addClass('active');
        $('#sidebar').removeClass('active');
        $('#nav-back').removeClass('hidden');
      },
      _setSidebarActive: function() {
        $('#sidebar').addClass('active');
        $('#main').removeClass('active');
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
      if (_.isUndefined(mainPane.innerView.model)) return;
      // If the current path has been deleted, then hide it.
      if (m.id == mainPane.innerView.model.id) {
        mainPane.showDeleted();
      }
    });
    
    if (_.isUndefined(window.filestoreJSON)) {
      fs.fetch();
    } else {
      fs.reset(window.filestoreJSON);
      startRouting();
    }
    notificationFeed.open();

  });
});