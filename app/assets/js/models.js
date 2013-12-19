/*jslint nomen: true, white: true, vars: true, eqeq: true, todo: true, unparam: true */
/*global _: false, define: false, window: false */
define(
    ['backbone', 'backbone.localstorage', 'backbone.projections', 'q', 'cryptojs-md5'], 
    function(Backbone, LocalStorage, Projections, Q, CryptoJS) {
  'use strict';

  var Avatar = function(options) {
    this.name = options.name;
    this.email = options.email;
    this.image = function() {
      var email = options.email,
          emailHash = CryptoJS.MD5(email.toLowerCase()),
          missing = options.missing || 'identicon',
          size = options.size || 50;
      return "//www.gravatar.com/avatar/"+emailHash+"?d="+missing+"&s="+size;
    }
  };

  var ExtendedCollection = Backbone.Collection.extend({
    // Produce a promise that a collection has been preloaded
    preload: function(initialData) {
      if (_.isUndefined(initialData)) {
        return Q(this.fetch()).thenResolve(this);
      }
      this.reset(initialData);
      return Q(this);
    },
    // Produce a promise that a collection has been reset to empty and fetched
    refresh: function(initialData) {
      this.reset();
      return Q(this.fetch());
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
    },
    ancestors: function() {
      var collection = this.collection;
      var parentModel = function(m) { return collection.get(m.get('parent')); };
      var ancestors = [];
      var m = parentModel(this);
      while (m != null) {
        ancestors.unshift(m);
        m = parentModel(m);
      }
      return ancestors;
    },
    decendants: function() {
      var decendants = this.collection.where({ parent: this.id });
      var flatMap = _.compose(_.flatten, _.map);
      return decendants.concat(flatMap(decendants, function(m) {
        return m.decendants();
      }));
    }
  });

  var GroupPermission = Backbone.Model.extend({});

  var GroupPermissions = Backbone.Collection.extend({
    model: GroupPermission,
    initialize: function(attributes, options) {
      this.folder = options.folder;
    },
    url: function() {
      return this.folder.url() + "/permissions";
    }
  });

  var Folder = FileOrFolder.extend({
    initialize: function() {
      // Sync permissions when the folder changes
      var fetchPermissions = _.bind(function() {
        this.permissions().fetch();
      }, this);
      this.on('sync', fetchPermissions);
      // When the folder is deleted, so are the permissions.
      this.on('remove', function() { this.off('sync', fetchPermissions); });
    },
    urlRoot: '/folder',
    uploadUrl: function() {
      return this.url()+'/files';
    },
    permissions: function() {
      if (_.isUndefined(this._permissionsCollection)) {
        this._permissionsCollection = new GroupPermissions([], {folder: this});
      }
      return this._permissionsCollection;
    }
  });

  var VersionInfo = Backbone.Model.extend({
    // Return a promise of a text summary
    textSummary: function() {
      var deferred = $.Deferred();
      if (_.isUndefined(this._textSummary)) {
        // Get fresh data from server
        $.get(this.url()+"/text-summary", _.bind(function(data) {
          this._textSummary = data;
          deferred.resolve(data);
        }, this));
      } else {
        // Use cached result
        _.defer(_.bind(function() {
          deferred.resolve(this._textSummary);
        }, this));
      }
      return deferred.promise();
    },
    avatar: function(options) {
      return new Avatar(_(options).defaults({
        name: this.get('author').name,
        email: this.get('author').email
      }));
    },
    content: function() {
      var fileId = this.collection.file.id;
      var versionId = this.id;
      return Q($.get(this.url())).then(function(data) {
        return {
          fileId: fileId,
          versionId: versionId,
          data: data,
          md5: CryptoJS.MD5(data).toString()
        };
      });
    }
  });
  
  var VersionList = Backbone.Collection.extend({
    initialize: function(attributes, options) {
      this.file = options.file;
      // When a version is deleted, it changes the file
      this.listenTo(this.file, 'sync', _.bind(function() {
        this.fetch();
      }, this));
    },
    model: VersionInfo,
    url: function() {
      return this.file.url() + '/versions';
    }
  });
  
  var Comment = Backbone.Model.extend({});

  var Comments = Backbone.Collection.extend({
    model: Comment,
    initialize: function(models, options) {
      this.target = options.target;
    }
  }, {
    forFile: function(file) {
      var CommentsForThisFile = Comments.extend({
        url: file.url() + '/comments'
      });
      return new CommentsForThisFile([], { target: file });
    }
  });

  var File = FileOrFolder.extend({
    urlRoot: '/file',
    downloadUrl: function() {
      return this.url()+'/versions/latest';
    },
    uploadUrl: function() {
      return this.url()+'/versions/new';
    },
    versions: function() {
      if (_.isUndefined(this._versionCollection)) {
        this._versionCollection = new VersionList([], { file: this });
      }
      return this._versionCollection;
    },
    comments: function() {
      if (_.isUndefined(this._commentCollection)) {
        this._commentCollection = Comments.forFile(this);
      }
      return this._commentCollection;
    }
  });

  var FileStore = ExtendedCollection.extend({
    url: '/filestore',
    model: function(attrs, options) {
      if (attrs.type == 'folder') {
        return new Folder(attrs, options);
      }
      return new File(attrs, options);
    }
  });

  var Flag = Backbone.Model.extend({});

  var EditFlags = Backbone.Collection.extend({
    model: Flag,
    url: '/flags/edit'
  });

  var WatchFlags = Backbone.Collection.extend({
    model: Flag,
    url: '/flags/watch'
  });

  var Notification = Backbone.Model.extend({});

  var Notifications = Backbone.Collection.extend({
    model: Notification,
    url: '/user/notifications',
    // Reverse sort
    comparator: function(v1, v2) {
      var d1 = v1.get('timestamp'), d2 = v2.get('timestamp');
      if (d1 == d2) {
        return 0;
      }
      return d1 > d2 ? -1 : 1;
    }
  });
  
  var Group = Backbone.Model.extend({
    members: function() {
      var members = this.get('members');
      return new Projections.Filtered(this.collection.options.users, {
        filter: function(user) {
          return _(members).contains(user.id);
        }
      });
    }
  });

  var Groups = ExtendedCollection.extend({
    model: Group,
    url: '/groups',
    initialize: function(options) {
      this.options = options;
    }
  });

  var User = Backbone.Model.extend({
    avatar: function(options) {
      return new Avatar(_(options).defaults({
        name: this.get('name'),
        email: this.get('email')
      }));
    }
  });

  var Users = ExtendedCollection.extend({
    model: User,
    url: '/user',
    initialize: function(options) {
      this.options = options;
      // Build flag collections
      this._flags = {
        edit: new EditFlags(),
        watch: new WatchFlags()
      };
      _.each(this._flags, function(c) { return c.fetch(); });
      this.groups = _.memoize(_.bind(this.groups, this));
    },
    currentId: function() {
      return this.options.currentId;
    },
    current: function() {
      return this.get(this.currentId());
    },
    flags: function() {
      return this._flags;
    },
    groups: function() {
      return new Groups({
        users: this
      });
    }
  });

  return {
    File: File,
    FileOrFolder: FileOrFolder,
    FileStore: FileStore,
    Folder: Folder,
    Notifications: Notifications,
    VersionInfo: VersionInfo,
    VersionList: VersionList,
    Users: Users
  };
});
