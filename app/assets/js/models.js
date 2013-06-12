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
  
  var FileOrFolder = Backbone.Model.extend({}, {
    _getNodeAttrs: function(node) {
      return _({
        id: node.id,
        name: node.name,
        type: node.type
      }).extend(node.attributes);
    }
  });

  var Folder = FileOrFolder.extend({
    urlRoot: '/folder',
    uploadUrl: function() {
      return this.url()+'/files';
    }
  }, {
    fromNode: function(node) {
      return new Folder(this._getNodeAttrs(node));
    }
  });
  
  var VersionInfo = Backbone.Model.extend({
    url: function() {
      return this.collection.url() + '/' + this.get('name');
    }
  });
  var VersionList = Backbone.Collection.extend({
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
    initialize: function() {
      this.versionList = new VersionList({});
      this.set('versions', this.versionList);
    },
    parse: function(response) {
      this.versionList.reset(response.versions);
      response.versions = this.versionList;
      response.dummy = new Date(); // ensure change triggers
      return response;
    },
    url: function() {
      return this.file.url() + "/info";
    }
  }, {
    fromFile: function(file) {
      var instance = new FileInfo({});
      instance.file = file;
      instance.versionList.file = file;
      instance.fetch();
      return instance;
    }
  });
  
  var File = FileOrFolder.extend({
    urlRoot: '/file',
    uploadUrl: function() {
      return this.url()+'/version/new';
    }
  }, {
    fromNode: function(node) {
      var instance = new File(this._getNodeAttrs(node));
      instance.set("info", FileInfo.fromFile(instance));
      window.fileModel = instance;
      return instance;
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