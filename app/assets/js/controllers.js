/*jslint nomen: true, white: true, vars: true, eqeq: true, todo: true */
/*global _: false, window: false */
define(['jquery', 'marionette', 'q', 'appcore', 'models', 'views'], 
    function($, Marionette, Q, App, models, views) {
  'use strict';

  var module = {};
  
  module.MainController = Marionette.Controller.extend({
    initialize: function(options) {
      var layout = options.layout;
      this._layout = layout;
      this._fs = options.filestore;
      this._users = options.users;
      this._notificationMessages = options.notifications;
      layout.notifications.show(new views.NotificationsNavView({
        collection: this._notificationMessages
      }));
      this._notificationMessages.fetch();
      this._users.once('sync reset', function() {
        layout.currentUserAvatar.show(new views.UserAvatar({
          model: this._users.current(),
          size: 20
        }));
      }, this);
      this._setupVentListeners();
      this.showLoading();
    },
    _setupVentListeners: function() {
      var c = this;
      c.listenTo(App.vent, "nav:start", function() {
        c.start();
      });
      c.listenTo(App.vent, "nav:folder:show", function(folder) {
        c.showFolder(folder.id);
      });
      c.listenTo(App.vent, "nav:file:show", function(file) {
        c.showFile(file.id);
      });
      c.listenTo(App.vent, "nav:file:diff", function(file, version) {
        c.showFileDiff(file.id, version.get('name'));
      });
      c.listenTo(App.vent, "nav:notification:list", function() {
        c.showNotifications();
      });
      c.listenTo(App.vent, "nav:password:change", function() {
        c.changePassword();
      });
      c.listenTo(App.vent, "nav:manage:groups", function() {
        c.manageGroups();
      });
      c.listenTo(App.vent, "nav:search", function(searchTerm) {
        c.search(searchTerm);
      });
    },
    _buildFileTree: function() {
      var fileTree = new views.FileTree();
      // Controller hooks
      fileTree.on("folder:select", _.bind(this.showFolder, this));
      fileTree.on("file:select", _.bind(this.showFile, this));
      fileTree.listenTo(this, 'start deleted', function() { 
        fileTree.expand();
      });
      fileTree.listenTo(this, 
          'showFolder showFile showFileDiff', function(fof) {
        fileTree.expand(fof.id);
      });
      // Filestore hooks
      (function(fs) {
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
          var node = fileTree.tree().find(m.id);
          if (node.parent() && node.parent().id != m.get('parent')) {
            node.remove();
            fileTree.tree().add(m.asNodeStruct(), m.get('parent'));
            _(m.decendants()).each(function(m) {
              fileTree.tree().add(m.asNodeStruct(), m.get('parent'));
            });
          } else {
            fileTree.tree().update(m.asNodeStruct());
          }
        });
        fileTree.listenTo(fs, 'remove', function(m) {
          fileTree.tree().remove(m.get('id'));
        });
      })(this._fs);
      return fileTree;
    },
    getFileTree: function() {
      if (_.isUndefined(this._fileTree)) {
        this._fileTree = this._buildFileTree();
      }
      return this._fileTree;
    },
    getUserMenu: function() {
      return new views.UserMenu({
        currentUser: this._users.current()
      });
    },
    isShowingFileOrFolder: function(id) {
      var layout = this._layout;
      // Handle being on the deleted page already
      if (_.isUndefined(layout.main.currentView.model)) { return false; }
      // If the current path has been deleted, then hide it.
      return id == layout.main.currentView.model.id;
    },
    showLoading: function() {
      var layout = this._layout;
      layout.ui.sidebarTitle.text('Files & Folders');
      layout.sidebar.show(this.getFileTree());
      layout.main.show(new views.LoadingView());
    },
    start: function() {
      var layout = this._layout;
      layout.ui.sidebarTitle.text('Files & Folders');
      layout.sidebar.show(this.getFileTree());
      layout.main.show(new views.StartView());
      this._setSidebarActive();
      this.trigger('start');
    },
    changePassword: function() {
      var layout = this._layout;
      layout.ui.sidebarTitle.text('Messages & Settings');
      layout.sidebar.show(this.getUserMenu());
      layout.main.show(new views.ChangePasswordView());
      this._setMainActive();
    },
    manageGroups: function() {
      var layout = this._layout;
      layout.ui.sidebarTitle.text('Messages & Settings');
      layout.sidebar.show(this.getUserMenu());
      layout.main.show(new views.ManageGroupsView({
        collection: this._users.groups(),
        users: this._users
      }));
      this._users.groups().fetch();
      this._setMainActive();
    },
    showNotifications: function() {
      var layout = this._layout;
      layout.ui.sidebarTitle.text('Messages & Settings');
      layout.sidebar.show(this.getUserMenu());
      layout.main.show(new views.NotificationsView({
        collection: this._notificationMessages
      }));
      this._setMainActive();
    },
    showDeleted: function(fof) {
      var layout = this._layout;
      layout.ui.sidebarTitle.text('Files & Folders');
      layout.sidebar.show(this.getFileTree());
      layout.main.show(new views.DeletedView({ model: fof }));
      this.trigger('deleted');
    },
    showFolder: function(id) {
      var layout = this._layout;
      var folder = this._fs.get(id);
      if (folder == null) {
        this.showDeleted();
      } else {
        layout.ui.sidebarTitle.text('Files & Folders');
        layout.sidebar.show(this.getFileTree());
        layout.main.show(new views.FolderView({
          model: folder,
          users: this._users
        }));
        this.trigger('showFolder', folder);
      }
      this._setMainActive();
    },
    showFile: function(id) {
      var layout = this._layout;
      var file = this._fs.get(id);
      if (file == null) {
        this.showDeleted();
      } else {
        layout.ui.sidebarTitle.text('Files & Folders');
        layout.sidebar.show(this.getFileTree());
        layout.main.show(new views.FileView({
          model: file,
          users: this._users
        }));
        this.trigger('showFile', file);
      }
      this._setMainActive();
    },
    showFileDiff: function(id, version) {
      var layout = this._layout;
      var file = this._fs.get(id);
      if (file == null) {
        this.showDeleted();
      } else {
        layout.ui.sidebarTitle.text('Files & Folders');
        layout.sidebar.show(this.getFileTree());
        layout.main.show(new views.FileDiffView({
          model: file,
          versionName: version
        }));
        this.trigger('showFileDiff', file);
      }
      this._setMainActive();
    },
    search: function(searchTerm) {
      var layout = this._layout;
      this.showLoading();
      var searchResults = new (Backbone.Collection.extend({
        url: '/search?q='+searchTerm
      }));
      searchResults.fetch().then(_.bind(function() {
        layout.main.show(new views.SearchView({
          collection: searchResults,
          filestore: this._fs,
          searchTerm: searchTerm
        }));
        this._setMainActive();
      }, this));
    },
    _setMainActive: function() {
      var layout = this._layout;
      layout.ui.main.addClass('active');
      layout.ui.sidebar.removeClass('active');
      $('#nav-back').removeClass('hidden');
    },
    _setSidebarActive: function() {
      var layout = this._layout;
      layout.ui.sidebar.addClass('active');
      layout.ui.main.removeClass('active');
      $('#nav-back').addClass('hidden');
    }
  });
  
  return module;
});