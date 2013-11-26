/*jslint nomen: true, white: true, vars: true, eqeq: true, todo: true */
/*global _: false, $: false, Backbone: false, EventSource: false, window: false */
require(['events', 'models', 'views'], function(EventFeed, models, views) {
  'use strict';

  var App = new Backbone.Marionette.Application();

  App.addInitializer(function(options) {
    var users = new models.Users({
      currentId: options.currentUserID
    });
    var fs = new models.FileStore();
    var notifications = new models.Notifications();
    var eventFeed = new EventFeed({
      lastEventId: window.lastEventID
    });

    // Event handlers - users, fs & eventFeed
    eventFeed.on("folder:create", function(id) {
        // Create a stand-alone folder
        var folder = new models.Folder({ id: id });
        // Get the data for it
        folder.fetch().done(function() {
          // It exists, so add it to the collection
          fs.add([folder.toJSON()]);
        });
      });
    eventFeed.on("file:create",
      function(id) {
        var file = new models.File({ id: id });
        file.fetch().done(function() {
          fs.add([file.toJSON()]);
        });
      });
    eventFeed.on("folder:update file:update", function(id) {
      var fof = fs.get(id);
      if (fof) { fof.fetch(); }
    });
    // Rather brute-force, but the flag will turn up
    eventFeed.on("flag:create",
      function(id) {
        _.each(users.flags(), function(c) {
          if (c.get(id)) { return; } // Already exists
          c.add({ id: id });
          c.get(id).fetch().error(function() {
            c.remove(id);
          });
        });
      });
    // We can delete from all without error
    eventFeed.on("flag:delete",
      function(id) {
        _.each(users.flags(), function(c) { c.remove(id); });
      });
    eventFeed.on("folder:delete file:delete",
      function(id) {
        fs.remove(fs.get(id));
      });

    // Update notifications based on events - notifications & eventFeed
    eventFeed.on("notification:create",
      function() {
        // TODO: Make this more efficient
        notifications.fetch();
      });
    eventFeed.on("notification:update",
      function(id) {
        var n = notifications.get(id);
        if (n) { n.fetch(); }
      });
    eventFeed.on("notification:delete",
      function(id) {
        notifications.remove(notifications.get(id));
      });

    // Once we switch to pushState, this will just start the history
    var startRouting = function() {
      // If we're using IE8 heavily, then push state is just trouble
      if (window.location.pathname != '/') {
        window.location.href = "/#"+window.location.pathname.replace(/^\//,'');
      }
      Backbone.history.start({ pushState: false });
    };

    // This probably shouldn't be a layout
    var layout = new views.AppLayout({
      el: '#content',
      notifications: notifications,
      users: users
    });
    layout.render();
    $('#content').append(layout.$el);
    layout.showLoading();

    
    var fileTree = layout.getFileTree();
    
    
    // These functions only use fileTree & fs
    fs.on('reset', function() {
      fileTree.tree().load([]);
      fs.each(function(m) {
        fileTree.tree().add(m.asNodeStruct(), m.get('parent'));
      });
    });
    fs.on('add', function(m) {
      // Retry failed adding, as sometimes events arrive out-of-order
      var f = function() {
        try {
          fileTree.tree().add(m.asNodeStruct(), m.get('parent'));
        } catch (e) {
          // Try again
          _.delay(f, 1000);
          //console.log(m.id+": "+e.message);
        }
      };
      f();
    });
    fs.on('change', function(m) {
      fileTree.tree().update(m.asNodeStruct(), m.get('parent'));
    });
    
    // This function uses fileTree, fs & layout
    fs.on('remove', function(m) {
      fileTree.tree().remove(m.get('id'));
      // Handle being on the deleted page already
      if (_.isUndefined(layout.main.currentView.model)) { return; }
      // If the current path has been deleted, then hide it.
      if (m.id == layout.main.currentView.model.id) {
        layout.showDeleted(m);
      }
    });

    // Router really acting like a controller here
    var Router = Backbone.Router.extend({
      routes: {
        "": "start",
        "change-password": "changePassword",
        "file/:id": "showFile",
        "folder/:id": "showFolder",
        "file/:id/version/:version/diff": "showFileDiff",
        "notifications": "showNotifications"
      },
      start: function() {
        layout.showStart();
        this._setSidebarActive();
        // Expand if at root
        var firstNode = _.first(fileTree.tree().nodes());
        if (firstNode.name == '/') {
          fileTree.expandTo(firstNode);
        }
      },
      changePassword: function() {
        layout.changePassword();
        this._setMainActive();
      },
      showNotifications: function() {
        layout.showNotifications();
        this._setMainActive();
      },
      showFolder: function(id) {
        var node = fileTree.tree().find(id);
        if (node == null) {
          layout.showDeleted();
        } else {
          layout.showFolder(fs.get(node.id));
          fileTree.expandTo(node);
        }
        this._setMainActive();
      },
      showFile: function(id) {
        var node = fileTree.tree().find(id);
        if (node == null) {
          layout.showDeleted();
        } else {
          layout.showFile(fs.get(node.id));
          fileTree.expandTo(node);
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
        $('#main').addClass('active');
        $('#sidebar').removeClass('active');
        $('#nav-back').removeClass('hidden');
      },
      _setSidebarActive: function() {
        $('#sidebar').addClass('active');
        $('#main').removeClass('active');
        $('#nav-back').addClass('hidden');
      }
    });

    var router = new Router();

    // Functions using router & fileTree
    fileTree.on("folder:select", function(folderId) {
      router.navigate("folder/"+folderId, {trigger: true});
    });
    fileTree.on("file:select", function(fileId) {
      router.navigate("file/"+fileId, {trigger: true});
    });

    var initFilestore = function() {
      if (_.isUndefined(options.filesAndFolders)) {
        return fs.fetch();
      }
      fs.reset(options.filesAndFolders);
      return $.Deferred().resolve();
    };

    // Users collection
    var initUsers = function() {
      if (_.isUndefined(options.users)) {
        return users.fetch();
      }
      users.reset(options.users);
      return $.Deferred().resolve();
    };

    // Wait to start routing
    $.when(initFilestore(), initUsers()).done(function() {
      startRouting();
    });

    // If our data is out-of-date, refresh and reopen event feed.
    eventFeed.on("outofdate", function(id) {
      fs.reset();
      fs.fetch().done(function() {
        eventFeed.updateLastId(id);
        eventFeed.trigger('recheck');
      });
    });

    // Open feed
    eventFeed.open();
  });
  
  
  $(function() {
    App.start({
      currentUserID: $('#current-user').data('id'),
      users: window.usersJSON,
      filesAndFolders: window.filestoreJSON
    });
  });
});