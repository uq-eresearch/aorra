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
    showLoading: function() {
      this._layout.showLoading();
    },
    start: function() {
      this._layout.showStart();
      this._setSidebarActive();
      this.trigger('start');
    },
    changePassword: function() {
      this._layout.changePassword();
      this._setMainActive();
    },
    showNotifications: function() {
      this._layout.showNotifications();
      this._setMainActive();
    },
    showFolder: function(id) {
      var layout = this._layout;
      var folder = this._fs.get(id);
      if (folder == null) {
        layout.showDeleted();
        this.trigger('start');
      } else {
        layout.showFolder(folder);
        this.trigger('showFolder', folder);
      }
      this._setMainActive();
    },
    showFile: function(id) {
      var layout = this._layout;
      var file = this._fs.get(id);
      if (file == null) {
        layout.showDeleted();
        this.trigger('start');
      } else {
        layout.showFile(file);
        this.trigger('showFile', file);
      }
      this._setMainActive();
    },
    showFileDiff: function(id, version) {
      var layout = this._layout;
      var file = this._fs.get(id);
      if (file == null) {
        layout.showDeleted();
        this.trigger('start');
      } else {
        layout.showFileDiff(file, version);
        this.trigger('showFileDiff', file);
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
  
  return module;
});