/*jslint nomen: true, white: true, vars: true, eqeq: true, todo: true */
/*global _: false, window: false */
define(['jquery', 'marionette', 'q', 'models', 'views'], 
    function($, Backbone, Q, models, views) {
  'use strict';

  var module = {};
  
  module.MainController = Backbone.Marionette.Controller.extend({
    initialize: function(options) {
      this._layout = options.layout;
      this._fs = options.filestore;
      this.showLoading();
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
      layout.sidebar.show(layout.getFileTree());
      layout.main.show(new views.LoadingView());
    },
    start: function() {
      var layout = this._layout;
      layout.ui.sidebarTitle.text('Files & Folders');
      layout.sidebar.show(layout.getFileTree());
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
        collection: layout.notificationMessages
      }));
      this._setMainActive();
    },
    showDeleted: function(fof) {
      var layout = this._layout;
      layout.ui.sidebarTitle.text('Files & Folders');
      layout.sidebar.show(layout.getFileTree());
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
        layout.sidebar.show(layout.getFileTree());
        layout.main.show(new views.FolderView({
          model: folder,
          users: layout.users
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
        layout.sidebar.show(layout.getFileTree());
        layout.main.show(new views.FileView({
          model: file,
          users: layout.users
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
        layout.sidebar.show(layout.getFileTree());
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