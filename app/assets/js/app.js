require(['models', 'views'], function(models, views) {
  'use strict';
  var NotificationFeed = function(config) {
      var obj = _.extend({}, config)
      _.extend(obj, Backbone.Events);
      _.extend(obj, {
      url: '/notifications',
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
          var es = new EventSource(this.url);
          es.addEventListener('ping', function(event) {
            trigger('event:ping', event.data);
          });
          _.each(['load', 'create', 'update', 'delete'], function(n) {
            es.addEventListener(n, function(event) {
              var struct = JSON.parse(event.data);
              trigger('event:'+n, struct);
            });
          });
          this.es = es;
        }
      }
    });

    function catchErrors(f) {
      return function(struct) { try { f.apply(this, arguments); } catch (e) {} };
    }

    var tree = _.bind(obj.getTree, this);
    // Event handlers
    obj.on("event:load", function(struct) { tree().load(struct); });
    obj.on("event:create",
      catchErrors(function(struct) { tree().add(struct, struct.parentId) }));
    obj.on("event:update",
      catchErrors(function(struct) { tree().update(struct) }));
    obj.on("event:delete",
      catchErrors(function(struct) { tree().remove(struct.id) }));

    return obj;
  };

  $(function () {

    var fileTree = new views.FileTree();
    var notificationFeed = new NotificationFeed({
      getTree: function() { return fileTree.tree }
    });
    var mainPane = new views.MainPane();
    fileTree.render();
    $('#sidebar').append(fileTree.$el);
    $('#main').append(mainPane.$el);
    notificationFeed.once("event:load", function(struct) {
      // Start router (as now we can load existing nodes)
      Backbone.history.start({ pushState: true, hashChange: false });
    });
    notificationFeed.open();

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
        var node = fileTree.tree.find(id);
        if (node == null) {
          mainPane.showDeleted()
        } else {
          mainPane.showFolder(models.Folder.fromNode(node));
        }
        this._highlightNode(node);
        this._setMainActive();
      },
      showFile: function(id) {
        var node = fileTree.tree.find(id);
        if (node == null) {
          mainPane.showDeleted()
        } else {
          mainPane.showFile(models.File.fromNode(node));
        }
        this._highlightNode(node);
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

    fileTree.on("folder:select", function(folder) {
      router.navigate("folder/"+folder.id, {trigger: true});
    });
    fileTree.on("file:select", function(file) {
      router.navigate("file/"+file.id, {trigger: true});
    });
    notificationFeed.on("event:delete", function(struct) {
      // Handle being on the deleted page already
      if (_.isUndefined(mainPane.innerView.model)) return;
      // If the current path has been deleted, then hide it.
      if (fileTree.tree.find(mainPane.innerView.model.id) == null) {
        mainPane.showDeleted();
      }
    });

  });
});