define(function() {
  'use strict';

  var FileStore = Backbone.Collection.extend({
    url: '/filestore',
    model: function(attrs, options) {
      if (attrs.type == 'folder') {
        return new Folder(attrs, options);
      } else {
        return new File(attrs, options);
      }
    }
  });

  var FileOrFolder = Backbone.Model.extend({
    asNodeStruct: function() {
      return {
        id: this.id,
        // Root notes should show their path
        name: this.get('parent') ? this.get('name') : this.get('path'),
        type: this.get('type'),
        attributes: {
          mimeType: this.get('mime')
        }
      };
    }
  });

  var Folder = FileOrFolder.extend({
    urlRoot: '/folder',
    uploadUrl: function() {
      return this.url()+'/files';
    }
  });

  var VersionInfo = Backbone.Model.extend({
    url: function() {
      return this.collection.url() + '/' + this.get('name');
    }
  });
  var VersionList = Backbone.Collection.extend({
    initialize: function(attributes, options) {
      this.file = options.file;
    },
    // Reverse sort by time
    comparator: function(a,b) {
      if (a.get('timestamp') < b.get('timestamp')) return 1;
      if (a.get('timestamp') > b.get('timestamp')) return -1;
      return 0;
    },
    model: VersionInfo,
    url: function() {
      return this.file.url() + '/version';
    }
  });

  var FileInfo = Backbone.Model.extend({
    initialize: function(attributes, options) {
      this.file = options.file;
      this.set('versions', this.versionList);
    },
    parse: function(response) {
      this.versionList().reset(response.versions);
      response.versions = this.versionList();
      response.dummy = new Date(); // ensure change triggers
      return response;
    },
    versionList: function() {
      if (!this._versionList) {
        this._versionList = new VersionList({}, {file: this.file});
      }
      return this._versionList;
    },
    url: function() {
      return this.file.url() + "/info";
    }
  });

  var File = FileOrFolder.extend({
    urlRoot: '/file',
    uploadUrl: function() {
      return this.url()+'/version/new';
    },
    info: function() {
      if (!this.infoModel) {
        this._infoModel = new FileInfo({}, {file: this});
      }
      return this._infoModel;
    }
  });

  return {
    File: File,
    FileInfo: FileInfo,
    FileOrFolder: FileOrFolder,
    FileStore: FileStore,
    Folder: Folder,
    VersionInfo: VersionInfo,
    VersionList: VersionList
  };
})