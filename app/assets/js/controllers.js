/*jslint nomen: true, white: true, vars: true, eqeq: true, todo: true */
/*global _: false, window: false */
define(['jquery', 'marionette', 'q', 'models', 'views'], 
    function($, Marionette, Q, models, views) {
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
      this.showLoading();
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
          fileTree.tree().update(m.asNodeStruct(), m.get('parent'));
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
      layout.ui.sidebarTitle.text('User Menu');
      layout.sidebar.show(new views.UserMenu());
      layout.main.show(new views.ChangePasswordView());
      this._setMainActive();
    },
    showNotifications: function() {
      var layout = this._layout;
      layout.ui.sidebarTitle.text('User Menu');
      layout.sidebar.show(new views.UserMenu());
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