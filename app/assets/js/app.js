/*jslint nomen: true, white: true, vars: true, eqeq: true, todo: true */
/*global _: false, window: false */
require(['jquery', 'marionette', 'q', 'events', 'models', 'views', 'controllers'], 
    function($, Backbone, Q, EventFeed, models, views, controllers) {
  'use strict';
  
  // Once we switch to pushState, this will just start the history
  var startRouting = function() {
    // If we're using IE8 heavily, then push state is just trouble
    if (window.location.pathname != '/') {
      window.location.href = "/#"+window.location.pathname.replace(/^\//,'');
    }
    Backbone.history.start({ pushState: false });
  };
  
  var App = new Backbone.Marionette.Application();
  
  var bubbleAllToAppFunc = function(prefix) {
    return function(eventName) {
      var args = [prefix + ':' + eventName].concat(_.rest(arguments));
      App.vent.trigger.apply(App.vent, args);
    };
  };
  
  App.vent.on('all', _.bind(console.log, console));

  // Start routing when all data is loaded
  App.vent.once('data:preloaded', startRouting);
  
  // Set up data:preloaded to be triggered
  App.addInitializer(function(options) {
    // Monitor each of the listed precursor events
    var loadPrecursorEvents = ['filestore', 'users'];
    var promises = _(loadPrecursorEvents).map(function(e) {
      var d = Q.defer();
      App.vent.on('data:preloaded:'+e, d.resolve);
      return d.promise;
    });
    // Once all events have been seen once, trigger the overall preloaded event
    Q.all(promises).done(function() {
      App.vent.trigger('data:preloaded');
    });
  });
  
  App.addInitializer(function(options) {
    var eventFeed = new EventFeed({
      lastEventId: window.lastEventID
    });
    // Bubble all events up
    eventFeed.on("all", bubbleAllToAppFunc('feed'));
    // If our data is out-of-date, refresh and reopen event feed.
    eventFeed.on("outofdate", function(id) {
      eventFeed.listenToOnce(App.vent, "data:refreshed", function() {
        eventFeed.reopen(id);
      });
    });
    // Open feed on application start
    eventFeed.listenToOnce(App, 'start', function() {
      eventFeed.open();
    });
  });
  
  App.addInitializer(function(options) {
    var users = new models.Users({
      currentId: options.currentUserID
    });
    var fs = new models.FileStore();
    var notifications = new models.Notifications();

    // Event handlers - fs & eventFeed
    App.vent.on("feed:folder:create", function(id) {
      if (fs.get(id) == null) {
        // Create a stand-alone folder
        var folder = new models.Folder({ id: id });
        // Get the data for it
        folder.fetch().done(function() {
          // It exists, so add it to the collection
          fs.add([folder.toJSON()]);
        });
      }
    });
    App.vent.on("feed:file:create", function(id) {
      if (fs.get(id) == null) {
        var file = new models.File({ id: id });
        file.fetch().done(function() {
          fs.add([file.toJSON()]);
        });
      }
    });
    App.vent.on("feed:folder:update feed:file:update", function(id) {
      var fof = fs.get(id);
      if (fof) { fof.fetch(); }
    });
    App.vent.on("feed:folder:delete feed:file:delete", function(id) {
      fs.remove(fs.get(id));
    });
    // If our data is out-of-date, refresh and reopen event feed.
    App.vent.on("feed:outofdate", function(id) {
      fs.refresh().done(function() {
        App.vent.trigger('data:refreshed');
      });
    });

    // Event handlers - users & eventFeed
    // Rather brute-force, but the flag will turn up
    App.vent.on("feed:flag:create", function(id) {
      _.each(users.flags(), function(c) {
        if (c.get(id)) { return; } // Already exists
        c.add({ id: id });
        c.get(id).fetch().error(function() {
          c.remove(id);
        });
      });
    });
    // We can delete from all without error
    App.vent.on("feed:flag:delete", function(id) {
      _.each(users.flags(), function(c) { c.remove(id); });
    });

    // Update notifications based on events - notifications & eventFeed
    App.vent.on("feed:notification:create", function() {
      // TODO: Make this more efficient
      notifications.fetch();
    });
    App.vent.on("feed:notification:update", function(id) {
      var n = notifications.get(id);
      if (n) { n.fetch(); }
    });
    App.vent.on("feed:notification:delete", function(id) {
      notifications.remove(notifications.get(id));
    });

    // This is probably fine as a layout now
    var layout = new views.AppLayout({
      el: options.rootSelector
    });
    layout.render();
    
    var mainController = new controllers.MainController({
      filestore: fs,
      layout: layout,
      notifications: notifications,
      users: users
    });

    var fileTree = mainController.getFileTree();
    
    // These functions only use fileTree & fs
    fileTree.listenTo(fs, 'reset', function() {
      fileTree.tree().load([]);
      fs.each(function(m) {
        fileTree.tree().add(m.asNodeStruct(), m.get('parent'));
      });
    });
    fileTree.listenTo(fs, 'add', function(m) {
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
    fileTree.listenTo(fs, 'change', function(m) {
      fileTree.tree().update(m.asNodeStruct(), m.get('parent'));
    });
    fileTree.listenTo(fs, 'remove', function(m) {
      fileTree.tree().remove(m.get('id'));
    });
    
    var setupFileTreeEvents = function(c) {
      fileTree.on("folder:select", _.bind(c.showFolder, c));
      fileTree.on("file:select", _.bind(c.showFile, c));
      fileTree.listenTo(c, 'start deleted', function() { fileTree.expand(); });
      fileTree.listenTo(c, 'showFolder showFile showFileDiff', function(fof) {
        fileTree.expand(fof.id);
      });
    };
    setupFileTreeEvents(mainController);
    
    // This function uses fileTree & layout
    mainController.listenTo(fs, 'remove', function(m) {
      // If the current path has been deleted, then hide it.
      if (mainController.isShowingFileOrFolder(m.id)) {
        mainController.showDeleted(m);
      }
    });
    
    // Router really acting like a controller here
    var router = new Backbone.Marionette.AppRouter({
      appRoutes: {
        "": "start",
        "change-password": "changePassword",
        "file/:id": "showFile",
        "folder/:id": "showFolder",
        "file/:id/version/:version/diff": "showFileDiff",
        "notifications": "showNotifications"
      },
      controller: mainController
    });
    // Router listens to main controller actions to change URL
    router.listenTo(mainController, "start deleted", function() {
      router.navigate("");
    });
    router.listenTo(mainController, "showFolder", function(folder) {
      router.navigate("folder/"+folder.id);
    });
    router.listenTo(mainController, "showFile", function(file) {
      router.navigate("file/"+file.id);
    });
    
    fs.preload(options.filesAndFolders).done(function(fs) {
      App.vent.trigger('data:preloaded:filestore', fs);
    });

    users.preload(options.users).done(function(users) {
      App.vent.trigger('data:preloaded:users', users);
    });

  });
  
  $(function() {
    App.start({
      rootSelector: '#content',
      currentUserID: $('#current-user').data('id'),
      users: window.usersJSON,
      filesAndFolders: window.filestoreJSON
    });
  });
});