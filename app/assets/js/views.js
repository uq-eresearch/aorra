/*jslint nomen: true, white: true, vars: true, eqeq: true, todo: true, unparam: true */
/*global _: false, FileAPI: false, Modernizr: false, define: false, window: false */
define([
        'appcore',
        'models',
        'templates',
        'underscore',
        'underscore.string',
        'q',
        'moment',
        'diff_match_patch',
        'glyphtree',
        'jquery.bootstrap',
        'backbone',
        'marionette',
        'webjars!backbone.localStorage.js',
        'backbone.projections',
        'unstyler',
        'htmldiff',
        'cryptojs-md5',
        'spin',
        'jquery.ckeditor',
        'typeahead'
        ], function(App, models, templates, _, _s, Q, moment, DiffMatchPatch,
            glyphtree, $, Backbone, Marionette, LocalStorage,
            BackboneProjections,
            unstyle, htmldiff, CryptoJS, Spinner) {
  'use strict';

  // Clock with ticks once a second, for periodic checks
  var clock = (function() {
    var emitter = _.extend({}, Backbone.Events);
    emitter.on('tick', function() {
      return _.delay(_.bind(emitter.trigger, emitter, 'tick'), 1000);
    });
    emitter.trigger('tick');
    return emitter;
  })();

  var svgOrPng = Modernizr.svg ? 'svg' : 'png';

  var formatTimestamp = function($n, delta) {
    var dt = moment($n.text());
    $n.attr('title', $n.text());
    if (delta) {
      $n.text(dt.fromNow());
    } else {
      $n.text(dt.format('dddd, D MMMM YYYY @ h:mm:ss a'));
    }
  };

  var reverseAppend = function(collectionView, itemView, index){
    var container = collectionView.$(collectionView.itemViewContainer);
    var children = container.children();
    if (children.size() <= index) {
      container.prepend(itemView.el);
    } else {
      children.eq(index).after(itemView.el);
    }
  };

  var typeFromMimeType = function(mimeType) {
    var mimeTypePatterns = [
      { pattern: /^image/, type: 'image' },
      { pattern: /^text/, type: 'document' },
      { pattern: /xhtml\+xml$/, type: 'document' },
      { pattern: /wordprocessingml\.document$/, type: 'document' },
      { pattern: /spreadsheetml\.sheet$/, type: 'spreadsheet' },
      { pattern: /vnd\.ms-excel$/, type: 'spreadsheet' }
    ];
    if (!mimeType) {
      return 'folder';
    }
    var match = _.find(mimeTypePatterns, function(entry) {
      return mimeType.match(entry.pattern);
    });
    if (match) {
      return match.type;
    }
    return 'file';
  };
  
  var SearchBox = Marionette.ItemView.extend({
    ui: {
      searchTerm: '.js-search-term',
      submitButton: '.js-submit'
    },
    onSubmit: function() {
      App.vent.trigger("nav:search", this.ui.searchTerm.val());
    },
    serializeData: function() {
      return {};
    },
    template: function(data) {
      return templates.render('search_box', data);
    },
    onRender: function(data) {
      var submit = _.bind(this.triggerMethod, this, 'submit');
      this.ui.searchTerm.on('keyup', function(evt) {
        if (evt.which == 13) {
          submit();
        }
      });
      this.ui.submitButton.on('click', submit)
    }
  });

  var FileTree = Marionette.Layout.extend({
    tagName: "div",
    regions: {
      search: '.js-search'
    },
    ui: {
      filetree: '.js-filetree',
      hint: '.js-hint'
    },
    initialize: function() {
      this._searchBox = new SearchBox();
    },
    template: function() {
      return templates.render('file_tree', {});
    },
    onBeforeRender: function() {
      this.tree().element.detach();
    },
    onRender: function() {
      this.search.show(this._searchBox);
      this.ui.filetree.append(this.tree().element);
    },
    onBeforeClose: function() {
      this.tree().element.detach();
    },
    _getNode: function(nodeOrId) {
      if (nodeOrId == null)
        return null;
      if (_.isObject(nodeOrId))
        return nodeOrId;
      return this.tree().find(nodeOrId);
    },
    _expandTo: function(n) {
      var nodes = [];
      while (n != null) {
        nodes.push(n);
        n = n.parent();
      }
      _.each(nodes.reverse(), function(n) {
        if (!n.isLeaf()) { n.expand(); }
      });
      this.ui.hint.hide();
    },
    expand: function(nodeOrId) {
      var n = this._getNode(nodeOrId);
      if (n != null) {
        this._expandTo(n);
        return n;
      }
      // Expand if at root
      var firstNode = _.first(this.tree().nodes());
      if (firstNode.name == '/') {
        this._expandTo(firstNode);
        return firstNode;
      }
      this.tree().collapseAll();
    },
    _buildTree: function() {
      var tree = glyphtree($('<div/>'), this.glyphtreeOptions);
      var selectHandler = _.bind(function(event, node) {
        // Emit select event
        if (node.type == 'folder') {
          this.trigger("folder:select", node.id);
        } else {
          this.trigger("file:select", node.id);
        }
      }, this);
      var createTooltip = function(e, node) {
        $(e.currentTarget).tooltip({
          placement: 'bottom',
          trigger: 'manual',
          delay: { show: 0, hide: 100 },
          title: function() {
            return (node.isExpanded() ? 'Collapse' : 'Expand') + ' Folder';
          }
        });
      };
      var hoverHandler = function(e, node) {
        if (node.isLeaf()) { return; }
        createTooltip(e, node);
        $(e.currentTarget).tooltip(e.type == 'mouseenter' ? 'show' : 'hide');
      };
      var hint = _.bind(function() { return this.ui.hint; }, this);
      var toggleHint = function(e) {
        var isClosed = function(node) { return !node.isExpanded(); };
        hint().toggle(_.all(tree.nodes(), isClosed));
      };
      tree.events.label.click = [selectHandler];
      tree.events.icon.click.push(toggleHint);
      tree.events.icon.mouseenter = [hoverHandler];
      tree.events.icon.mouseleave = [hoverHandler];
      return tree;
    },
    tree: function() {
      if (!this._tree) {
        this._tree = this._buildTree();
      }
      return this._tree;
    },
    glyphtreeOptions: {
      startExpanded: false,
      typeResolver: function(struct) {
        if (struct.type == 'folder') {
          return 'folder';
        }
        if (struct.attributes) {
          var mimeType = struct.attributes.mimeType;
          if (mimeType) {
            return typeFromMimeType(mimeType);
          }
        }
        return 'file';
      },
      nodeComparator: function(a, b) {
        var getTypeIndex = function(n) { return n.type == 'folder' ? 0 : 1; };
        if (getTypeIndex(a) == getTypeIndex(b)) {
          if (a.name < b.name) { return -1; }
          if (a.name > b.name) { return 1; }
          return 0;
        }
        return getTypeIndex(a) < getTypeIndex(b) ? -1 : 1;
      },
      types: {
        folder: {
          icon: {
            "default": {
              content: "\uf07b",
              'font-family': "FontAwesome"
            },
            'jquery.ui': '//code.jquery.com/ui/1.10.3/jquery-ui',
            expanded: {
              content: "\uf07c",
              'font-family': "FontAwesome"
            }
          }
        },
        file: {
          icon: {
            leaf: {
              content: "\uf016",
              'font-family': "FontAwesome"
            }
          }
        },
        image: {
          icon: {
            leaf: {
              content: "\uf03e",
              'font-family': "FontAwesome"
            }
          }
        },
        spreadsheet: {
          icon: {
            leaf: {
              content: "\uf0ce",
              'font-family': "FontAwesome",
              'color': '#339933'
            }
          }
        },
        document: {
          icon: {
            leaf: {
              content: "\uf0f6",
              'color': '#333399',
              'font-family': "FontAwesome"
            }
          }
        }
      }
    }
  });

  var FileUploadView = Marionette.ItemView.extend({
    tagName: 'div',
    ui: {
      alerts: '.js-alerts',
      progress: '.js-progress',
      upload: 'input[type="file"]'
    },
    initialize: function(options) {
      this.options = options;
      this._multiple = options.multiple == true;
      if (_.isFunction(this.model.info)) {
        this.listenTo(this.model, 'sync', function() {
          this.model.info().fetch();
        });
      }
    },
    serializeData: function() {
      return {
        multiple: this._multiple,
        exists: this.options.type == 'file'
      };
    },
    template: function(data) {
      return templates.render('file_upload', data);
    },
    onRender: function() {
      this.ui.upload.on('change', _.bind(function(e) {
        this.uploadFiles(e);
      }, this));
    },
    // Thanks to IE behaviour, replacement is the only sure way to clear
    _replaceInput: function() {
      this.ui.upload.replaceWith(this.ui.upload.clone(true));
    },
    initializeProgress: function(files) {
      var ProgressInfo = function(files) {
        var fileInfo = _.reduce(files, function(memo, file) {
          memo[file.name] = {
            'uploaded': 0,
            'total': file.size
          };
          return memo;
        }, {});
        this.update = function(file, uploaded) {
          fileInfo[file.name].uploaded = uploaded;
        };
        this.status = function() {
          return _.reduce(fileInfo, function(h, fInfo) {
            if (fInfo.uploaded >= fInfo.total) {
              h.completed += fInfo.total;
            } else if (fInfo.uploaded < 0) {
              h.failed += fInfo.total;
            } else {
              h.uploading += fInfo.uploaded;
            }
            h.total += fInfo.total
            return h;
          }, {
            completed: 0,
            failed: 0,
            uploading: 0,
            total: 0
          });
        };
      };
      this._progress = new ProgressInfo(files);
      this.ui.progress.show();
      this.refreshProgress();
    },
    updateProgress: function(file, uploaded) {
      this._progress.update(file, uploaded);
      this.refreshProgress();
    },
    refreshProgress: function() {
      var p = this._progress.status();
      var done = 100.0 * p.completed / p.total;
      var inprogress = 100.0 * p.uploading / p.total;
      var error = 100.0 * p.failed / p.total;
      this.ui.progress.find('.upload-done').css('width', done+'%');
      this.ui.progress.find('.upload-progress').css('width', inprogress+'%');
      this.ui.progress.find('.upload-error').css('width', error+'%');
    },
    uploadFiles: function(event) {
      var fs = this.model.collection;
      var files = this.ui.upload.get(0).files;
      if (files.length == 0) {
        return; // Nothing to upload
      }
      this.initializeProgress(files);
      var $alerts = this.ui.alerts;
      var progressHandler = _.bind(function(file) {
        return _.bind(function(evt) {
          if (_.isNumber(evt)) {
            // Direct call
            this.updateProgress(file, evt);
          } else if (evt.loaded) {
            // Event Handler
            this.updateProgress(file, evt.loaded);
          }
        }, this);
      }, this);
      var renderAlert = function(type, name, msg) {
        var $alert = $('<div/>');
        $alert.html(templates.render('alert_box', {
          type: type,
          message: _.template(
              "<strong><%= f.name %></strong>: <%= f.msg %>",
              { name: name, msg: msg },
              { variable: 'f' })
        }));
        return $alert;
      };
      var uploadUrl = this.model.uploadUrl();
      var uploadFileAction = function(file) {
        return function() {
          var reportProgress = progressHandler(file);
          var formData = new FormData();
          formData.append("files[]", file);
          // From: http://stackoverflow.com/a/8758614/701439
          var deferred = Q.defer();
          $.ajax({
            url: uploadUrl,
            type: 'POST',
            xhr: function() {
              var customXHR = $.ajaxSettings.xhr();
              if (customXHR.upload) {
                customXHR.upload.addEventListener('progress',
                    reportProgress, false);
              }
              return customXHR;
            },
            success: function(data) {
              $alerts.append(renderAlert('success', file.name,
                  'Uploaded successfully.'));
              reportProgress(file.size);
              var existing = fs.get(data.id);
              if (existing) {
                existing.set(data);
              } else {
                fs.add([ data ]);
              }
            },
            error: function() {
              $alerts.append(renderAlert('danger', file.name,
                  'Error uploading. Does the file already exist?'));
              reportProgress(-1);
            },
            complete: function() {
              deferred.resolve();
            },
            data: formData,
            // Tell jQuery not to process data or worry about content-type.
            cache: false,
            contentType: false,
            processData: false
          });
          return deferred.promise;
        };
      };
      this._replaceInput();
      return _.reduce(files, function(promise, file) {
        return promise.then(uploadFileAction(file));
      }, Q());
    }
  });

  var FlagButtonView = Marionette.ItemView.extend({
    tagName: 'span',
    className: 'view',
    ui: {
      button: '.btn',
      popover: '[data-toggle="popover"]',
      content: '.counter-button-content'
    },
    initialize: function(options) {
      this.targetId = options.targetId;
      this._uid = this.collection.currentId();
      this._flags = this.collection.flags()[this.dataDefaults().flagType];
      // Re-render when flags change (which will happen when we toggle)
      this.listenTo(this._flags, 'add sync remove', 
        _.bind(this.render, this));
    },
    serializeData: function() {
      var data = this.dataDefaults();
      // Filter to target ID
      var users = _(this._flags.where({ targetId: this.targetId })).chain()
        // Grab matching user object from user collection
        .map(_.bind(function(f) {
          var obj = this.collection.get(f.get('userId'));
          if (obj == null) {
            return null;
          }
          return _(obj.toJSON()).extend({ flagId: f.get('id') });
        }, this))
        // Filter out the missing users
        .compact()
        // Return the value
        .value();
      return _.extend(data, {
        isSet: this.isSet(),
        contentTitle: function() { return data.title+"ing"; },
        users: users,
        count: users.length,
        adminOverride: data.adminOverride &&
          this.collection.current().get('isAdmin')
      });
    },
    template: function(serialized_model) {
      return templates.render('flag_button', serialized_model);
    },
    templateHelpers: {
      activeClass: function() {
        return this.isSet ? 'btn-info' : 'btn-default';
      }
    },
    _getFlagTemplate: function(uid) {
      return {
        userId: uid,
        targetId: this.targetId
      };
    },
    _getFlag: function() {
      if (_.isUndefined(this._flags)) {
        return null;
      }
      return this._flags.findWhere(this._getFlagTemplate(this._uid));
    },
    isSet: function() {
      return this._getFlag() != null;
    },
    _toggleSet: function() {
      var isSet = this.isSet();
      if (this.isSet()) {
        this._getFlag().destroy();
      } else {
        this._flags.create(this._getFlagTemplate(this._uid));
      }
      return !isSet;
    },
    onToggleButton: function() {
      this._toggleSet();
    },
    onRender: function() {
      var content = this.ui.content.html();
      this.ui.button.click(_.bind(function() {
        return this.triggerMethod('toggle:button', {
          collection: this.collection,
          model: this.model,
          view: this
        });
      }, this));
      this.ui.popover.popover({
        html: true,
        content: content
      });
      this.$el.on('click', '.js-delete', _.bind(function(e) {
        var flagId = $(e.target).data('flag-id');
        var flag = this._flags.findWhere({ id: flagId });
        if (flag) {
          flag.destroy();
        }
      }, this));
    }
  });

  var EditingButtonView = FlagButtonView.extend({
    dataDefaults: function() {
      return {
        icon: this.isSet() ? 'pencil-square' : 'pencil-square-o',
        flagType: 'edit',
        title: 'Edit',
        tooltip: 'Let other users know you are making edits to this file.',
        adminOverride: true
      };
    }
  });

  var WatchingButtonView = FlagButtonView.extend({
    dataDefaults: function() {
      return {
        icon: this.isSet() ? 'eye' : 'eye-slash',
        flagType: 'watch',
        title: 'Watch',
        tooltip: 'Receive notifications when new versions are uploaded.'
      };
    }
  });

  var CreateNewView = Marionette.ItemView.extend({
    triggers: {
      'submit form': 'form:submit'
    },
    ui: {
      name: 'input[name="name"]',
      type: 'select[name="type"]',
      messages: '.js-messages'
    },
    template: function(serialized_model) {
      return templates.render('create_new', {});
    },
    onFormSubmit: function() {
      switch (this.ui.type.val()) {
      case 'folder':
        this.createFolder(this.ui.name.val());
        break;
      case 'html':
        this.createHtmlDocument(this.ui.name.val());
        break;
      }
      return false;
    },
    createFolder: function(name) {
      var fs = this.model.collection;
      var $messages = this.ui.messages;
      var $form = this.$el.find('form');
      var attrs = this.ui.name.clone().attr('name', 'mkdir').serialize();
      $.ajax({
        method: 'POST',
        url: this.model.url()+'/folders?'+attrs,
        success: function(folders) {
          // Add each returned folder
          _.each(folders, function(data) {
            fs.add(data);
            var $alert = $(templates.render('alert_box', {
              type: 'info',
              message: _.template(
                '<a href="/#folder/<%=f.id%>"><strong><%=f.name%></strong></a>' +
                ' was successfully created.',
                data,
                { variable: 'f'})
            }));
            $messages.append($alert);
            $alert.alert();
          });
        }
      });
    },
    createHtmlDocument: function(name) {
      this.model.collection.create({
        name: name,
        mime: 'text/html',
        parent: this.model.id
      }, { wait: true });
    }
  });

  var UserAvatar = Marionette.ItemView.extend({
    tagName: 'span',
    initialize: function(options) {
      this.options = options;
    },
    serializeData: function() {
      return this.model.avatar(_(this.options).pick('missing', 'size'));
    },
    template: function(data) {
      return templates.render('user_avatar', data);
    },
    onRender: function(data) {
      this.$el.find('[rel=tooltip]').tooltip();
    }
  });

  var VersionView = Marionette.Layout.extend({
    tagName: 'tr',
    className: 'version-row',
    modelEvents: {
      "change": "render"
    },
    regions: {
      avatar: '.user-avatar'
    },
    triggers: {
      "click .js-filediff": "version:compare",
      "click .js-delete": "version:delete"
    },
    ui: {
      timestamp: '.timestamp'
    },
    initialize: function(options) {
      this.options = options;
    },
    serializeData: function() {
      // Get data to render
      var data = _(this.model.toJSON()).extend({
        downloadUrl: this.model.url()
      });
      if (this.model != this.model.collection.first()) {
        data = _(data).extend({
          canCompare: true,
          canDelete: this.options.canDelete
        });
      }
      return data;
    },
    template: function(serialized_model) {
      return templates.render('version_row', serialized_model);
    },
    avatarView: function() {
      if (!this._avatar) {
        this._avatar = new UserAvatar({
          model: this.model,
          size: 20
        });
      }
      return this._avatar;
    },
    onRender: function($e) {
      formatTimestamp(this.ui.timestamp);
      this.avatar.show(this.avatarView())
    },
    onVersionCompare: function(e) {
      this.trigger("show:diff");
    },
    onVersionDelete: function(e) {
      e.model.destroy();
    }
  });
  
  var CommentView = Marionette.Layout.extend({
    triggers: {
      'click .js-edit': 'comment:edit:start',
      'change textarea.js-message': 'comment:edit:end'
    },
    modelEvents: {
      'change': 'render'
    },
    ui: {
      message: '.js-message',
      modified: '.modified'
    },
    initialize: function(options) {
      this.options = options;
    },
    serializeData: function() {
      var commentData = this.model.toJSON();
      var isCurrent = this.model.collection.target.get('modified') <=
          this.model.get('modified');
      return _(commentData).extend({
        author: this.options.author && this.options.author.avatar({ size: 64 }),
        isCurrent: isCurrent,
        mine: this.model.get("userId") == this.options.currentUser.id
      });
    },
    template: function(serialized_model) {
      return templates.render('comment_view', serialized_model);
    },
    onCommentEditStart: function() {
      var $textarea = $('<textarea/>')
        .addClass('js-message form-control')
        .attr('rows', 5)
        .text(this.model.get('message'));
      this.ui.message.replaceWith($textarea);
      $textarea.focus();
    },
    onCommentEditEnd: function() {
      this.model.save({ message: this.$('textarea.js-message').val() });
    },
    onRender: function() {
      formatTimestamp(this.ui.modified, true);
    }
  });
  
  var CommentsView = Marionette.CompositeView.extend({
    events: {
      "keyup .new-comment textarea": 'textKeyUp'
    },
    triggers: {
      "click .js-submit": 'comment:new'
    },
    ui: {
      newCommentText: '.new-comment textarea'
    },
    itemView: CommentView,
    itemViewContainer: '.comments',
    itemViewOptions: function(model, index) {
      if (!model.get("userId")) {
        return {};
      }
      return {
        author: this._users.findWhere({ id: model.get("userId") }),
        currentUser: this._users.current()
      };
    },
    initialize: function(options) {
      this._users = options.users;
      this.collection.fetch();
    },
    appendHtml: reverseAppend,
    emptyView: Marionette.ItemView.extend({
      template: function() {
        return '<p>No comments yet</p>';
      }
    }),
    onCommentNew: function() {
      var iso8601now = moment().format();
      this.collection.create({
        message: this.ui.newCommentText.val(),
        // Not used by the server, but important locally
        userId: this._users.current().id,
        created: iso8601now,
        modified: iso8601now
      });
      this.ui.newCommentText.val('');
    },
    textKeyUp: function(e) {
      // ctrl + enter
      if (e.ctrlKey && e.which == 13) {
        this.triggerMethod('comment:new')
      }
    },
    template: function(serialized_model) {
      return templates.render('comments_view', serialized_model);
    }
  });

  var VersionsView = Marionette.CompositeView.extend({
    initialize: function(options) {
      var file = options.file;
      this.on("itemview:show:diff", function(view) {
        App.vent.trigger("nav:file:diff", file, view.model);
      });
      this._users = options.users;
      window.versions = this.collection;
      this.render();
      this.collection.fetch();
    },
    itemView: VersionView,
    itemViewOptions: function(model, index) {
      var author = model.get("author");
      return {
        canDelete: author && this._users.current().get("email") == author.email
      };
    },
    itemViewContainer: 'tbody',
    appendHtml: reverseAppend,
    emptyView: Marionette.ItemView.extend({
      template: function() {
        return '<tr><td><em>Loading version information...</em></td></tr>';
      }
    }),
    template: function(serialized_model) {
      return templates.render('version_table', serialized_model);
    }
  });

  var GroupPermissionView = Marionette.ItemView.extend({
    tagName: 'tr',
    events: {
      "change select": 'updateModel'
    },
    modelEvents: {
      "change": "render"
    },
    onRender: function() {
      // Select correct values
      this.$el.find('select').each(function(i, v) {
        var value = $(v).data('value');
        $(v).find('option[value="'+value+'"]').prop('selected', true);
      });
    },
    template: function(serialized_model) {
      return templates.render('permission_row', serialized_model);
    },
    updateModel: function() {
      var newAccess = this.$el.find('select').val();
      this.model.set('access', newAccess);
      this.model.save();
    }
  });

  var GroupPermissionsView = Marionette.CompositeView.extend({
    initialize: function() {
      this.collection = this.model.permissions();
      this.render();
      this.collection.fetch();
    },
    itemView: GroupPermissionView,
    itemViewContainer: 'tbody',
    template: function(serialized_model) {
      return templates.render('permission_table', serialized_model);
    }
  });

  var DeleteButtonView = Marionette.ItemView.extend({
    tagName: 'span',
    events: {
      'click .delete-button': 'showModal',
      'submit form': 'formSubmit'
    },
    ui: {
      popup: '.modal'
    },
    serializeData: function() {
      return { action: this.model.url() };
    },
    template: function(serialized_model) {
      return templates.render('delete_button', serialized_model);
    },
    onRender: function() {
      this.ui.popup.modal({ show: false });
      // For some reason delegation needs to be set up again
      this.undelegateEvents();
      this.delegateEvents(this.events);
    },
    showModal: function() {
      this.ui.popup.modal('show');
      return false;
    },
    hideModal: function() {
      this.ui.popup.modal('hide');
      return false;
    },
    formSubmit: function(e) {
      this.model.destroy({wait: true});
      this.hideModal();
      return false;
    }
  });

  var DownloadButtonView = Backbone.View.extend({
    tagName: 'span',
    initialize: function(options) {
      // { "label": "url" } => { label: "label", url: "url" }
      this.formats = _(options.formats).map(function(v, k) {
        return { label: k, url: v};
      });
    },
    render: function() {
      var html = (this.formats.length == 1) ?
          templates.render("download_button_single", this.formats[0]) :
          templates.render("download_button_multiple", {formats: this.formats});
      this.$el.html(html);
    }
  });

  var MoveButton = Marionette.ItemView.extend({
    ui: {
      button: '.js-open'
    },
    template: function(data) {
      return templates.render('move_button', {});
    },
    onDestinationList: function() {
      var $destinationSelect = this.$('select.js-folders');
      $destinationSelect.empty();
      var filestore = this.model.collection;
      var folders = filestore.where({type: 'folder'});
      // Filter out current parent
      folders = _(folders).reject(_.bind(function(m) {
        return m == this.model ||
          m.id == this.model.get('parent') ||
          _(m.ancestors()).contains(this.model);
      }, this));
      if (this.model.get('type') == 'folder') {
        var thisId = this.model.id;
        // Filter out self
        folders = _(folders).reject(function(m) {
          return m.id == thisId;
        })
      }
      _(folders).chain()
        .sortBy(function(v) { return v.get('path'); })
        .each(function(folder) {
          var $option = $('<option/>')
            .attr('value', folder.id)
            .text(folder.get('path'));
          $destinationSelect.append($option);
        });
    },
    onDestinationSelected: function() {
      var $modal = this.$('.modal');
      var $destinationSelect = this.$('select.js-folders');
      var folderId = $destinationSelect.val();
      Q(this.model.save({ parent: folderId })).then(_.bind(function() {
        // Once the modal is closed, reshow file in new location
        $modal.one('hidden.bs.modal', _.bind(function() {
          App.vent.trigger('nav:'+this.model.get('type')+':show', this.model);
        }, this));
        $modal.modal('hide');
      }, this)).fail(_.bind(function() {
        this.$('.js-select').button('reset');
      }, this));
    },
    onRender: function() {
      var $modal = this.$('.modal').modal({ show: false });
      $modal.on('show.bs.modal', _.bind(function (e) {
        this.triggerMethod('destination:list');
      }, this));
      this.ui.button.on('click', function() {
        $modal.modal('show');
      });
      this.$('.js-select').on('click', _.bind(function (e) {
        $(e.target).button('loading');
        this.triggerMethod('destination:selected');
      }, this));
    }
  });

  var BreadcrumbItemView = Marionette.ItemView.extend({
    tagName: 'li',
    triggers: {
      'click .js-folder': 'folder:show'
    },
    initialize: function(options) {
      this.options = options;
    },
    onFolderShow: function() {
      App.vent.trigger('nav:folder:show', this.model);
    },
    _isLast: function() {
      return this.options.isLast;
    },
    serializeData: function() {
      return {
        name: this.model.get('path') == '/' ? '/' : this.model.get('name'),
        url: this.model.url(),
        isLast: this._isLast()
      };
    },
    template: function(serialized_model) {
      if (serialized_model.isLast) {
        return serialized_model.name;
      }
      return _.template(
          '<a class="js-folder" href="<%=url%>"><%=name%></a>',
          serialized_model);
    },
    onRender: function() {
      if (this._isLast()) {
        this.$el.addClass('active');
      }
    }
  });

  var BreadcrumbView = Marionette.CollectionView.extend({
    tagName: 'ol',
    className: 'breadcrumb',
    modelEvents: {
      'change:parent': 'repopulate'
    },
    itemView: BreadcrumbItemView,
    itemViewOptions: function(model, index) {
      if (this.collection.size() - 1 == index) {
        return { isLast: true };
      }
    },
    initialize: function() {
      this.collection = new models.FileStore();
      this.repopulate();
    },
    repopulate: function() {
      this._populateCollection(this.collection);
    },
    _populateCollection: function(breadcrumbCollection) {
      var folders = this.model.ancestors().concat([this.model]);
      breadcrumbCollection.reset(folders);
      return breadcrumbCollection;
    }
  });

  var FileOrFolderView = Marionette.Layout.extend({
    _inlineList: function() {
      var $list = $('<ul class="list-inline"/>');
      _.each(arguments, function(arg) {
        $list.append($('<li/>').append(arg));
      });
      return $list;
    },
    _makeHeading: function() {
      return $('<h2/>').text(this.model.get('name'));
    }
  });

  /*
   * Simple container view which can contain a number of other views or DOM
   * elements, and render each one in an inline list.
   */
  var ButtonToolbarView = Backbone.View.extend({
    className: 'btn-toolbar',
    els: [],
    container: new Backbone.ChildViewContainer(),
    initialize: function(viewsAndEls) {
      var isView = function(v) { return v instanceof Backbone.View; };
      _.chain(viewsAndEls)
        .filter(isView)
        .each(_.bind(this.container.add, this.container));
      this.els = _.compact(_.map(viewsAndEls, function(viewOrEl) {
        return isView(viewOrEl) ? viewOrEl.$el : viewOrEl;
      }));
      this.render();
    },
    render: function() {
      this.$el.empty().append(_.map(this.els, function(el) {
        return $('<div class="btn-group"/>').append(el);
      }));
      this.container.apply('render');
    }
  });

  var FileOrFolderNameView = Marionette.ItemView.extend({
    modelEvents: {
      "sync": "render"
    },
    triggers: {
      'click span.name': 'focus:name',
      'blur input.name': 'blur:name'
    },
    ui: {
      nameSpan: 'span.name',
      nameField: 'input.name'
    },
    initialize: function(options) {
      this.options = options;
    },
    onFocusName: function() {
      if (this.options.isAdmin) {
        this._isEditing = true;
        this.render();
        this.ui.nameField.focus();
      }
    },
    onBlurName: function() {
      this.model.save({ name: this.ui.nameField.val() })
        .error(_.bind(this.model.fetch, this.model));
      this._isEditing = false;
      this.render();
    },
    serializeData: function() {
      return _(this.model.toJSON()).extend({
        canRename: this.options.isAdmin,
        isEditing: this._isEditing
      });
    },
    template: function(data) {
      return templates.render('fof_name_view', data);
    },
    onRender: function() {
      this.ui.nameField.keyup(function(event) {
        // on Enter key
        if (event.which == 13) {
          $(event.target).blur();
        }
      });
    }
  });

  var FolderView = FileOrFolderView.extend({
    initialize: function(options) {
      this._users = options.users;
    },
    serializeData: function() {
      return _(this.model.toJSON()).extend({
        canRename: this.isAdmin(),
        url: this.model.url()
      });
    },
    template: function(serialized_model) {
      _(serialized_model).extend({
        hasWrite: serialized_model.accessLevel == 'RW'
      });
      return templates.render('folder_view', serialized_model);
    },
    regions: {
      breadcrumbs: '.region-breadcrumbs',
      buttons: '.region-buttons',
      name: '.region-name',
      upload: '.region-upload',
      create: '.region-create',
      permissions: '.region-permissions'
    },
    onRender: function() {
      this.breadcrumbs.show(new BreadcrumbView({ model: this.model }));
      this.name.show(new FileOrFolderNameView({
        model: this.model,
        isAdmin: this.isAdmin()
      }));
      if (this.model.get('accessLevel') == 'RW') {
        this.upload.show(new FileUploadView({
          type: 'folder',
          model: this.model,
          multiple: true
        }));
        this.create.show(new CreateNewView({ model: this.model }));
        if (this.isAdmin()) {
          this.permissions.show(new GroupPermissionsView({
            model: this.model
          }));
        }
        this.buttons.show(new ButtonToolbarView([
          new WatchingButtonView({
            collection: this._users,
            targetId: this.model.id
          }),
          new DownloadButtonView({
            formats: {
              'All files': this.model.url()+"/archive",
              'Generated charts': this.model.url()+"/charts.zip"
            }
          }),
          this.isAdmin() && this.model.get('path') != '/' ?
              new MoveButton({ model: this.model }) : null,
          this.isAdmin() ? new DeleteButtonView({ model: this.model }) : null
        ]));
      } else {
        this.buttons.show(new ButtonToolbarView([
          new DownloadButtonView({
            formats: {
              'All files': this.model.url()+"/archive",
              'Generated charts': this.model.url()+"/charts.zip"
            }
          })
        ]));
      }
      this.delegateEvents();
    },
    isAdmin: _.memoize(function() {
      return this._users.current().get('isAdmin');
    })
  });

  var ImageElementView = Marionette.ItemView.extend({
    modelEvents: {
      'change sync': 'reloadImage'
    },
    _imageUrl: function() {
      // Download URL with content-based cache-breaking query
      return this.model.downloadUrl() + "?" + this.model.get('sha512');
    },
    reloadImage: function() {
      this.$('img').attr('src', this._imageUrl());
    },
    serializeData: function() {
      return {
        path: this.model.get('path'),
        url: this._imageUrl()
      };
    },
    template: function(serialized_model) {
      return templates.render('img_view', serialized_model);
    }
  });
  
  var ExternalRefButton = Marionette.ItemView.extend({
    triggers: {
      'click .js-update': 'update'
    },
    ui: {
      'button': '.js-update',
      'msg': '.js-msg'
    },
    initialize: function() {
      this._hasRefs = false;
      // Check if external references exist
      $.ajax({
        type: 'HEAD',
        url: this.model.url()+"/spreadsheet-external-references",
        success: _.bind(function() {
          // Show the button
          this._hasRefs = true;
          this.render();
        }, this)
      });
    },
    onUpdate: function() {
      this.ui.msg.text('');
      this.ui.button.button('loading');
      $.ajax({
        type: 'POST',
        url: this.model.url()+"/spreadsheet-external-references/update",
        statusCode: {
          200: _.bind(function() {
            this.ui.msg.text('No updated required.');
          }, this),
          201: _.bind(function() {
            this.ui.msg.text('Successfully updated.');
          }, this)
        }
      }).done(_.bind(function(data, textStatus, jqXHR) {
        this.ui.button.button('reset');
      }, this));
    },
    serializeData: function() {
      return { hidden: !this._hasRefs };
    },
    template: function(serialized_model) {
      if (serialized_model.hidden) {
        return '';
      } else {
        return templates.render('external_refs', serialized_model);
      }
    },
    onRender: function() {
      this.ui.button.on('click',
          _.bind(this.triggerMethod, this, 'update'));
    }
  });

  var ChartElementView = Marionette.Layout.extend({
    regions: {
      'externals': '.region-external-refs'
    },
    initialize: function() {
      this._externalRefButton = new ExternalRefButton({ model: this.model });
    },
    onShow: function() {
      this._charts = new models.Charts(this.model);
      this.listenTo(this._charts, 'change', _(this.render).bind(this));
    },
    onClose: function() {
      this._charts.stopListening();
      this.stopListening(this._charts);
      this._charts = null;
    },
    serializeData: function() {
      var makeChartAttrs = function(region) {
        return function(c, i) {
          return _(c).extend({
            title: c.title,
            slug: _.template("<%=v.type%>-<%=v.region%>-<%=v.uniqueId%>",
              {
                type: _s.slugify(c.type),
                region: _s.slugify(c.region),
                uniqueId: i
              }, { variable: 'v' }),
            url: c[svgOrPng].replace(/(#[0-9]+)?$/, "#"+(new Date()).getTime()) // Force recheck of URL
          });
        };
      };
      var l = this._charts ? this._charts.list : [];
      return {
        regions: _.chain(l).groupBy('region')
          .map(function(charts, region) {
            var common = { title: region, slug: _s.slugify(region) };
            if (charts.length == 1) {
              return _(common).extend({
                chart: makeChartAttrs(region)(charts[0], 0)
              });
            }
            return _(common).extend({
              charts: _(charts).chain()
                .map(makeChartAttrs(region))
                .sortBy('title')
                .map(function(c, i) { return _(c).extend({ first: i == 0 }); })
                .value()
            });
          }).map(function(region, i) {
            return _(region).extend({
              first: i == 0
            });
          }).value()
      };
    },
    template: function(serialized_model) {
      return templates.render('charts', serialized_model);
    },
    onRender: function() {
      this.$('img').each(function(i, imgEl) {
        var parent = $(imgEl).parents('.js-img-container').get(0);
        $(parent).css('min-width', '100%');
        $(parent).css('min-height', '200px');
        var spinner = new Spinner({}).spin(parent);
        $(imgEl).on('load', function() {
          spinner.stop();
        });
      });
      if (this.model.get('accessLevel') == 'RW') {
        this.externals.show(this._externalRefButton);
      }
    }
  });
  
  var FileIdAutocomplete = Backbone.View.extend({
    tagName: 'input',
    getCharts: function(datum) {
      var $input = this.ui.input;
      var filesAndFolders = this.model.collection;
      var onSuccess = _.bind(function(data) {
        this._charts = data.charts;
        this.renderChartList();
      }, this);
      var fileId = $input.val();
      var file = filesAndFolders.findWhere({type: 'file', id: fileId});
      if (_.isObject(file)) {
        $.get(file.url() + '/charts?format='+svgOrPng, onSuccess);
      }
    },
    render: function() {
      var files = this.model.collection.where({ type: 'file' });
      var initFileTypeAhead = _.bind(function() {
        this.$el.typeahead('destroy');
        this.$el.typeahead({
          // LocalStorage is triggered by 'name', and cannot be active for
          // list updates to show.
          //name: 'files',
          valueKey: 'id',
          local: _(files).map(function(m){
            return _.defaults(m.toJSON(), {
              tokens: [m.id].concat(m.get('name').split(/[\W_]+/))
            });
          }),
          template: function(datum) {
            return templates.render('file_typeahead', datum);
          }
        });
      }, this);
      initFileTypeAhead();
      this.listenTo(this.model.collection,
          'add change remove', initFileTypeAhead);
      // Compensate for typeahead DOM changes
      $('.twitter-typeahead, .tt-dropdown-menu').css('min-width', '100%');
      this.$el.siblings('.tt-hint')
        .css('height', 'auto')
        .css('width', 'auto');
      // Force bootstrap styles
      this.$el.parent().addClass('nested-bootstrap');
      // Connect events to select file
      var emitEvent = _.bind(function() {
        var fileId = this.$el.val();
        this.trigger('file:selected', _(files).findWhere({id: fileId}));
      }, this);
      this.$el.on('typeahead:selected', emitEvent);
      this.$el.on('typeahead:autocompleted', emitEvent);
      this.on('content:set', _.bind(function(fileId) {
        this.$el.typeahead('setQuery', fileId);
        emitEvent();
      }, this));
    }
  });
  
  var HtmlEditor = Marionette.Layout.extend({
    modelEvents: {
      "sync": "updatedOnServer"
    },
    regions: {
      chartSelector: '.chart-selector'
    },
    ui: {
      diff: '.diff-pane',
      html: '.html-pane',
      save: 'button.js-save',
      reset: 'button.js-reset',
      outofdate: '.label.js-outofdate'
    },
    initialize: function(options) {
      var setupPromises = [
        this._updateServerContentCache(),
        this._setupWorkingStorage()
      ];
      Q.all(setupPromises).then(_.bind(function() {
        // Initialize working copy if none exists
        if (this._getWorkingCopy() == null || 
            !this._getWorkingCopy().isModified()) {
          this._setWorkingCopy(this._serverContentCache);
        }
        this._contentLoaded = true;
        this.render();
        this.triggerMethod('html:loaded');
      }, this)).done();
      // Keep user cache
      this._users = this.options.users;
    },
    updatedOnServer: function() {
      // Update cache and then trigger UI update
      this._updateServerContentCache()
        .then(_.bind(this.triggerMethod, this, 'html:loaded'))
        .then(_.bind(this.triggerMethod, this, 'html:update'));
    },
    _updateServerContentCache: function() {
      var versions = this.model.versions();
      return Q(versions.fetch()).then(function() {
          return versions.last().content();
        }).then(_.bind(function(content) {
          this._serverContentCache = content;
          return content;
        }, this));
    },
    _setupWorkingStorage: function() {
      this.localStorage = new (Backbone.Collection.extend({
        localStorage: new LocalStorage("HtmlEditor-"+this.model.id)
      }));
      // When working copy data changes, trigger update to other elements
      this.localStorage.on('change',
          _.bind(this.triggerMethod, this, 'html:update'));
      return Q(this.localStorage.fetch());
    },
    _getWorkingCopy: function() {
      var mine = this.localStorage.get('mine');
      if (!mine) {
        return null;
      }
      var content = mine.get('content');
      content.isModified = _.bind(function() {
        return CryptoJS.MD5(content.data).toString() != content.md5 ||
          content.versionId != this._serverContentCache.versionId;
      }, this);
      return content;
    },
    _setWorkingCopy: function(content) {
      var mine = this.localStorage.get('mine');
      if (mine) {
        mine.set('content', content);
      } else {
        this.localStorage.add({
          id: 'mine',
          content: content
        });
      }
      this.localStorage.get('mine').save();
      this.triggerMethod('html:loaded');
    },
    _updateWorkingCopy: function(data) {
      var mine = this.localStorage.get('mine');
      if (mine) {
        mine.set('content',
            _({ data: data }).defaults(mine.get('content')));
        mine.save();
      }
    },
    _resetWorkingCopy: function() {
      this._setWorkingCopy(this._serverContentCache);
    },
    serializeData: function() {
      return { editable: this.editable() };
    },
    template: function(obj) {
      return templates.render('html_editor', {
        editable: obj.editable
      });
    },
    _flags: function() {
      return this._users.flags()['edit'];
    },
    editable: function() {
      if (this.model.get('accessLevel') != 'RW') {
        return false;
      }
      var flags = this._flags().filter(_.bind(function(f) {
        return f.get('targetId') == this.model.id;
      }, this));
      return flags.length == 1 && _(flags).any(_.bind(function(m) {
        return m.get('userId') == this._users.currentId();
      }, this));
    },
    _watchEditFlags: function() {
      if (this._watchingFlags) { return; }
      this._flags().on('add remove', _.bind(function(f) {
        if (f.get('targetId') == this.model.id) {
          this.render();
        }
      }, this));
      this._watchingFlags = true;
    },
    onHtmlLoaded: function() {
      if (this._contentLoaded) {
        if (this.ui.html.editor) {
          // Editor
          this._setEditorContent(this._getWorkingCopy().data);
        } else {
          // Viewer
          this.ui.html.html(this._serverContentCache.data);
        }
        this.toggleOutOfDate();
      }
    },
    onHtmlUpdate: function() {
      if (this._contentLoaded) {
        this.updateDiff();
        this.toggleSave();
      }
    },
    updateDiff: function() {
      var workingCopy = this._getWorkingCopy();
      if (workingCopy) {
        this.ui.diff.html(htmldiff(
            this._serverContentCache.data, workingCopy.data));
      }
    },
    _setEditorContent: function(html, callback) {
      return this.ui.html.editor.setData(html, callback);
    },
    toggleOutOfDate: function() {
      var workingCopy = this._getWorkingCopy();
      if (workingCopy && 
          workingCopy.versionId != this._serverContentCache.versionId) {
        this.ui.outofdate.show();
      } else {
        this.ui.outofdate.hide();
      }
    },
    toggleSave: function() {
      var workingCopy = this._getWorkingCopy();
      if (workingCopy) {
        this.ui.save.prop("disabled", !workingCopy.isModified());
        this.ui.reset.prop("disabled", !workingCopy.isModified())
      }
    },
    getCharts: function(file, callback) {
      if (_.isObject(file)) {
        $.get(file.url() + '/charts?format='+svgOrPng, callback);
      }
    },
    _setupCKEditor: function() {
      this.ui.html.ckeditor({
        extraPlugins: 'aorrafigure,justify'
      }); // Initialize with CKEditor
      var fileIdInit = _.bind(function(e) {
        var elementObj = e.data;
        var $element = $('#'+elementObj.domId);
        this._fileIdAutocomplete = new FileIdAutocomplete({
          el: $element.find('input').get(0),
          model: this.model
        });
        this._fileIdAutocomplete.on('file:selected', _.bind(function(file) {
          if (_.isUndefined(file)) return;
          var fileType = typeFromMimeType(file.get('mime'));
          switch (fileType) {
          case 'spreadsheet':
            this.trigger('chartFile:selected', file)
            break;
          case 'image':
            this.trigger('imageFile:selected', file)
            break;
          }
        }, this));
        this._fileIdAutocomplete.render();
      }, this);
      var chartUrlInit = _.bind(function(e) {
        var elementObj = e.data;
        this.on('chartFile:selected', function(file) {
          var populateSelect = function(charts) {
            elementObj.clear();
            _(charts).each(function(chart) {
              elementObj.add(chart.type+" - "+chart.region, chart.url);
              if (elementObj.initialImageUrl == chart.url) {
                elementObj.setValue(chart.url, false);
              }
            });
          }
          this.getCharts(file, function(data) {
            populateSelect(data.charts);
          });
        });
        this.on('imageFile:selected', function(file) {
          elementObj.clear();
          elementObj.add(file.get('name'), file.downloadUrl());
          elementObj.setValue(file.downloadUrl(), false);
        });
      }, this);
      this.ui.html.ckeditor().editor.on(
          'aorrafigure_fileId:loaded', fileIdInit);
      this.ui.html.ckeditor().editor.on(
          'aorrafigure_fileId:set',
          _.bind(function(e) {
            // There was an existing value, so propagate it through as if
            // selected manually.
            this._fileIdAutocomplete.trigger("content:set", e.data);
          }, this));
      this.ui.html.ckeditor().editor.on(
          'aorrafigure_imageUrl:loaded', chartUrlInit);
      this.ui.html.ckeditor().editor.on(
          'change', _.debounce(_.bind(function(evt) {
            this._updateWorkingCopy(evt.editor.getData());
          }, this), 500));
      this.triggerMethod('html:loaded');
    },
    onRender: function() {
      var file = this.model;
      if (this.editable()) {
        var save = _.bind(function() {
          this.ui.html.ckeditor().editor.setReadOnly(true);
          $.ajax(this.model.uploadUrl(), {
            type: 'POST',
            contentType: 'text/html',
            data: this._getWorkingCopy().data,
            success: _.bind(function() {
              var afterUpdate = _.bind(function() {
                this._resetWorkingCopy();
                this.ui.html.ckeditor().editor.setReadOnly(false);
              }, this);
              this._updateServerContentCache().then(afterUpdate).done();
            }, this),
            failure: _.bind(function() {
              this.ui.html.ckeditor().editor.setReadOnly(false);
            }, this)
          });
        }, this);
        this._setupCKEditor();
        this.ui.save.on('click', save);
        this.ui.reset.on('click', _.bind(this._resetWorkingCopy, this));
        this.triggerMethod('html:update');
      } else {
        this.triggerMethod('html:loaded');
        this.triggerMethod('html:update');
      }
      this._watchEditFlags();
    }
  });

  var YamlEditorView = Marionette.ItemView.extend({
    modelEvents: {
      "sync": "updatedOnServer"
    },
    initialize: function() {
      this._isInfographic = false;
      this.updatedOnServer();
    },
    updatedOnServer: function() {
      var versions = this.model.versions();
      var contentPromise = Q(versions.fetch()).then(function() {
        return versions.last().content();
      });
      var dataPromise = Q($.get('/file/'+this.model.id+'/infographic/data.json'));
      return Q.all(contentPromise).then(_.bind(function(content) {
          this._serverContent = content.data;
          this.render();
          return content;
        }, this)).then(function() {
          return dataPromise;
        }).done(_.bind(function() {
          this._isInfographic = true;
          this.render();
        }, this), _.bind(function() {
          this._isInfographic = false;
        }, this));
    },
    serializeData: function() {
      return {
        id: this.model.id,
        content: this._serverContent,
        infographic: this._isInfographic
      };
    },
    template: function(serialized_model) {
      return templates.render('yaml_editor', serialized_model);
    }
  });

  var NoEditorView = Marionette.ItemView.extend({
    template: function() {
      return templates.render('no_editor', {});
    }
  });

  var OnlineEditorView = {
    create: function(file, users) {
      var type = typeFromMimeType(file.get('mime'));
      switch (type) {
      case 'document':
        if (/html/.test(file.get('mime'))) {
          return new HtmlEditor({ model: file, users: users });
        }
        break;
      case 'spreadsheet':
        return new ChartElementView({ model: file });
      case 'image':
        return new ImageElementView({ model: file });
      case 'file':
        if (/yaml/.test(file.get('mime')) ||
            /.yaml$/.test(file.get('name'))) {
          return new YamlEditorView({ model: file, users: users });
        }
      }
      return new NoEditorView();
    }
  };
  
  var FileInfoView = Marionette.ItemView.extend({
    modelEvents: {
      "sync": "render"
    },
    initialize: function() {
      // Function to detect changes in "x somethings ago" display
      var changeMonitor = (function(view) {
        var check = function() {
          var fromNow = view._dateTime().fromNow();
          if (fromNow != check.last) {
            check.last = fromNow;
            view.render();
          }
        };
        check.last = view._dateTime().fromNow();
        return check;
      })(this);
      // Run on clock tick
      this.listenTo(clock, 'tick', changeMonitor);
    },
    serializeData: function() {
      var dt = this._dateTime();
      return _(this.model.toJSON()).extend({
        prettyModification: function() {
          return dt.format('dddd, D MMMM YYYY @ h:mm:ss a');
        },
        sinceModification: function() {
          return dt.fromNow();
        }
      });
    },
    _dateTime: function() {
      return moment(this.model.get('modified'));
    },
    template: function(data) {
      return templates.render('file_info_view', data);
    }
  });

  var FileView = FileOrFolderView.extend({
    initialize: function(options) {
      this._users = options.users;
      // Cache views at the instance level
      this.getNameView = _.memoize(this.getNameView);
      this.getContentView = _.memoize(this.getContentView);
      this.getVersionsView = _.memoize(this.getVersionsView);
      this.getCommentsView = _.memoize(this.getCommentsView);
    },
    serializeData: function() {
      return _(this.model.toJSON()).extend({
        url: this.model.url()
      });
    },
    template: function(serialized_model) {
      _(serialized_model).extend({
        hasWrite: serialized_model.accessLevel == 'RW'
      });
      return templates.render('file_view', serialized_model);
    },
    regions: {
      breadcrumbs: '.region-breadcrumbs',
      buttons: '.region-buttons',
      display: '.region-display',
      info: '.region-info',
      name: '.region-name',
      upload: '.region-upload'
    },
    triggers: {
      'click .show-content a': 'display:content',
      'click .show-versions a': 'display:versions',
      'click .show-comments a': 'display:comments'
    },
    ui: {
      displayToggle: '.display-toggle'
    },
    getNameView: function() {
      return new FileOrFolderNameView({
        model: this.model,
        isAdmin: this.isAdmin()
      });
    },
    getContentView: function() {
      return OnlineEditorView.create(this.model, this._users);
    },
    getVersionsView: function() {
      return new VersionsView({
        collection: this.model.versions(),
        file: this.model,
        users: this._users
      });
    },
    getCommentsView: function() {
      return new CommentsView({
        collection: this.model.comments(),
        file: this.model,
        users: this._users
      });
    },
    onDisplayContent: function() {
      this.display.show(this.getContentView());
      this.upload.show(new FileUploadView({
        type: 'file',
        model: this.model
      }));
      this.ui.displayToggle.find('li').removeClass('active');
      this.ui.displayToggle.find('li.show-content').addClass('active');
    },
    onDisplayVersions: function() {
      this.display.show(this.getVersionsView());
      this.upload.show(new FileUploadView({
        type: 'file',
        model: this.model
      }));
      this.ui.displayToggle.find('li').removeClass('active');
      this.ui.displayToggle.find('li.show-versions').addClass('active');
    },
    onDisplayComments: function() {
      this.display.show(this.getCommentsView());
      this.upload.close();
      this.ui.displayToggle.find('li').removeClass('active');
      this.ui.displayToggle.find('li.show-comments').addClass('active');
    },
    downloadFormats: function() {
      var formats = {
        "Original": this.model.downloadUrl()
      };
      if (this.model.get('mime') == 'text/html') {
        formats['HTML Zip'] = this.model.url()+"/htmlzip";
        formats['PDF'] = this.model.url()+"/pdf";
      } else if (typeFromMimeType(this.model.get('mime')) == 'spreadsheet') {
        formats['Charts'] = this.model.url()+'/charts.zip';
      }
      return formats;
    },
    onRender: function() {
      this.breadcrumbs.show(new BreadcrumbView({ model: this.model }));
      this.name.show(this.getNameView());
      this.info.show(new FileInfoView({
        model: this.model
      }));
      if (this.model.get('accessLevel') == 'RW') {
        this.buttons.show(new ButtonToolbarView([
          new WatchingButtonView({
            collection: this._users,
            targetId: this.model.id
          }),
          new EditingButtonView({
            collection: this._users,
            targetId: this.model.id
          }),
          new DownloadButtonView({
            formats: this.downloadFormats()
          }),
          this.isAdmin() ? new MoveButton({ model: this.model }) : null,
          this.isAdmin() ? new DeleteButtonView({ model: this.model }) : null
        ]));
      } else {
        this.buttons.show(new ButtonToolbarView([
          new WatchingButtonView({
            collection: this._users,
            targetId: this.model.id
          }),
          new DownloadButtonView({
            formats: this.downloadFormats()
          })
        ]));
      }
      this.triggerMethod('display:content');
      this.delegateEvents();
    },
    isAdmin: _.memoize(function() {
      return this._users.current().get('isAdmin');
    })
  });

  var DiffView = Backbone.View.extend({
    initialize: function(version1, version2) {
      this._version1 = version1;
      this._version2 = version2;
    },
    _doDiff: function(version1, version2, callback) {
      var scrollToFunc = function(eOrSel) {
        var margin = 100;
        return function() {
          $('html, body').animate({
            scrollTop: $(eOrSel).offset().top - margin
          });
        };
      };
      $.when(version1.textSummary(), version2.textSummary()).done(
          function(text1, text2) {
        var dmp = new DiffMatchPatch();
        // Perform diff calculation and cleanup
        var diff = dmp.diff_main(text1, text2);
        dmp.diff_cleanupSemantic(diff);
        // Add IDs for all fragments of the diff
        _.each(diff, function(v) { v.push(_.uniqueId('diff-fragment-')); });
        var $txt = $('<div/>');
        var $summary = $('<div/>');
        var $docFragments = _.map(diff, function(v) {
          var $el;
          switch (v[0]) {
          case -1:
            $el = $('<del class="clickable red-text"/>');
            $el.click(scrollToFunc($summary));
            break;
          case 1:
            $el = $('<ins class="clickable green-text"/>');
            $el.click(scrollToFunc($summary));
            break;
          default:
            $el = $('<span/>');
          }
          $el.attr('id', v[2]);
          $el.text(v[1]);
          return $el;
        });
        var stats = _.reduce(diff,
            function(h, v) {
              switch (v[0]) {
                case 1:  h.additions += 1; break;
                case -1: h.deletions += 1; break;
              }
              return h;
            },
            { additions: 0, deletions: 0 });
        $summary.append($('<p/>').text(_.template(
            "<%=additions%> additions & <%=deletions%> deletions", stats)));
        var makeBoxes = function(v) {
          var $icon = $('<i class="clickable"/>')
            .click(scrollToFunc('#'+v[2]))
            .attr('data-content', v[1]);
          if (v[0] == 1) {
            $icon.addClass('fa fa-plus-square green-text');
            $icon.attr('title', 'Added text');
          } else {
            $icon.addClass('fa fa-minus-square red-text');
            $icon.attr('title', 'Deleted text');
          }
          $icon.popover({ placement: 'bottom', trigger: 'hover'});
          return $('<li/>').append($icon);
        };
        // Make box elements
        var $boxes = _(diff).chain()
          .reject(function(v) { return v[0] == 0; })
          .map(makeBoxes)
          .value();
        // Add box elements to summary
        $summary.append(
          $('<ul class="list-inline"/>').append($boxes)
        );
        // Build the DOM hierarchy
        $txt
          .append($summary)
          .append($('<hr/>'))
          .append($('<p/>')
            .append($docFragments)
            .css('white-space', 'pre-wrap'));
        callback($txt);
      });
    },
    render: function() {
      var $el = this.$el;
      this._doDiff(this._version1, this._version2, function($e) {
        $el.empty().append($e);
      });
    }
  });

  var FileDiffView = FileOrFolderView.extend({
    triggers: {
      'click .js-back': 'show:file'
    },
    initialize: function(attrs) {
      var versionName = attrs.versionName;
      var versions = this.model.versions();
      versions.fetch().done(_.bind(function() {
        this.version = versions.findWhere({ name: versionName });
        this.render();
      }, this));
    },
    serializeData: function() {
      var model = this.model;
      var version = this.version;
      var versionList = model.versions();
      var idxOf = _.bind(versionList.indexOf, versionList);
      // Only show earlier versions
      var otherVersions = _.invoke(
        versionList.filter(function(m) {
          return idxOf(version) > idxOf(m);
        }), 'toJSON');
      return {
        backUrl: model.url(),
        version: version ? version.toJSON() : null,
        otherVersions: otherVersions
      };
    },
    template: function(serialized_model) {
      if (_.isObject(serialized_model.version)) {
        return templates.render('filediff_view', serialized_model);
      }
      return templates.render('loading_page', {});
    },
    regions: {
      breadcrumbs: '.region-breadcrumbs',
      diff: '.region-diff'
    },
    onShowFile: function() {
      App.vent.trigger('nav:file:show', this.model);
    },
    onRender: function() {
      if (!_.isObject(this.version)) { return; }
      this.breadcrumbs.show(new BreadcrumbView({ model: this.model }));
      var onSelectChange = _.bind(function(e) {
        var versionList = this.model.versions();
        var otherVersion = versionList.get($(e.target).val());
        this.diff.show(new DiffView(otherVersion, this.version));
      }, this);
      var $select = this.$el.find('select');
      $select.change(onSelectChange);
      $select.trigger('change');
    }
  });

  var DeletedView = Marionette.ItemView.extend({
    template: function() {
      return templates.render('deleted_page', {});
    }
  });

  var LoadingView = Marionette.ItemView.extend({
    template: function() {
      return templates.render('loading_page', {});
    }
  });

  var StartView = Marionette.ItemView.extend({
    template: function() {
      return templates.render('start_page', {});
    }
  });

  var UserMenu = Marionette.ItemView.extend({
    triggers: {
      'click .js-notification-list': 'notification:list',
      'click .js-change-password': 'password:change',
      'click .js-manage-groups': 'manage:groups',
      'click .js-back': 'back'
    },
    initialize: function(options) {
      this.options = options;
    },
    onBack: function() {
      App.vent.trigger('nav:start');
    },
    onNotificationList: function() {
      App.vent.trigger('nav:notification:list');
    },
    onPasswordChange: function() {
      App.vent.trigger('nav:password:change');
    },
    onManageGroups: function() {
      App.vent.trigger('nav:manage:groups');
    },
    serializeData: function() {
      return {
        isAdmin: this.options.currentUser.get('isAdmin')
      };
    },
    template: function(data) {
      return templates.render('user_menu', data);
    }
  });

  var ChangePasswordView = Marionette.ItemView.extend({
    initialize: function() { this.render(); },
    events: {
      'keyup #newPassword,#repeatPassword': 'typePassword',
      'click button[type="submit"]': 'submitForm'
    },
    template: function() {
      return templates.render('change_password', {});
    },
    typePassword: function() {
      var minlength = 8;
      var $submit = this.$el.find('form button[type="submit"]');
      var $passwords = this.$el.find('#newPassword, #repeatPassword');
      var valid = (function() {
        var values = $.map($passwords, function(v) { return $(v).val(); });
        return _.any(values, function(v) { return v.length >= minlength; }) &&
          _.uniq(values).length == 1;
      }());
      $passwords.parents('.form-group')
        .removeClass('has-success has-error')
        .addClass( valid ? 'has-success' : 'has-error' );
      $submit.prop('disabled', !valid);
    },
    submitForm: function() {
      var $form = this.$el.find('form');
      var showAlert = function(data) {
        $form.find('.outcome').html(templates.render('alert_box', data));
      };
      $.ajax({
        url: $form.attr('action'),
        type: $form.attr('method'),
        data: $form.serialize(),
        success: function() {
          return showAlert({
            type: 'success',
            icon: 'smile',
            message: 'Password changed successfully.'
          });
        },
        error: function(jqXHR, textStatus) {
          return showAlert({
            type: 'danger',
            icon: 'frown',
            message: 'Unable to change password: '+jqXHR.responseText
          });
        }
      });
      return false;
    }
  });

  var NotificationMessageView = Marionette.ItemView.extend({
    tagName: 'li',
    className: 'media',
    triggers: {
      'click .unread': 'notification:read',
      'click .delete': 'notification:delete'
    },
    template: function(data) {
      return templates.render('notification_message', data);
    },
    onRender: function($e) {
      formatTimestamp(this.$('.timestamp'));
    },
    onNotificationRead: function() {
      this.model.set('read', true);
      this.model.save();
      return false;
    },
    onNotificationDelete: function() {
      this.model.destroy();
      return false;
    }
  });

  var NotificationsView = Marionette.CompositeView.extend({
    collectionEvents: {
      'sync': 'render'
    },
    itemView: NotificationMessageView,
    emptyView: Marionette.ItemView.extend({
      template: function() {
        return '<div class="media-body">No messages.</div>';
      }
    }),
    itemViewContainer: '.notifications',
    template: function(data) {
      return templates.render('notifications_view', data);
    }
  });

  var NotificationsNavView = Marionette.ItemView.extend({
    tagName: 'a',
    collectionEvents: {
      'sync': 'render',
      'remove': 'render'
    },
    triggers: {
      'click': 'notification:list'
    },
    attributes: {
      "data-placement": "bottom",
      href: '/notifications',
      rel: "tooltip",
      title: "Notification messages"
    },
    serializeData: function() {
      return {
        hasUnread: this.collection.findWhere({read: false}) != null,
        messageCount: this.collection.size()
      };
    },
    template: function(data) {
      return templates.render('notifications_nav_item', data);
    },
    onNotificationList: function() {
      App.vent.trigger('nav:notification:list')
    },
    onRender: function() {
      this.$el.tooltip();
    }
  });

  var SearchResult = Marionette.ItemView.extend({
    tagName: 'a',
    className: 'list-group-item',
    attributes: {
      href: '#'
    },
    triggers: {
      'click': 'file:select'
    },
    initialize: function(options) {
      this.options = options;
    },
    onFileSelect: function() {
      var fileOrFolder = this.options.fileOrFolder;
      App.vent.trigger('nav:'+fileOrFolder.get('type')+':show', fileOrFolder);
    },
    serializeData: function() {
      var searchTerm = this.options.searchTerm;
      var fof = this.options.fileOrFolder;
      var data = fof ? fof.toJSON() : {};
      var highlightedExcerpt = this.model.get('excerpt')
        .replace(searchTerm, '<strong>'+searchTerm+'</strong>')
      return _(data).chain().extend(this.model.toJSON()).extend({
        excerpt: highlightedExcerpt
      }).value();
    },
    template: function(data) {
      return templates.render('search_result', data);
    }
  });
  
  var SearchView = Marionette.CollectionView.extend({
    className: "list-group",
    itemView: SearchResult,
    emptyView: Marionette.ItemView.extend({
      template: function() {
        return "<p>No files found.</p>";
      }
    }),
    itemViewOptions: function(m) {
      return {
        fileOrFolder: this.options.filestore.get(m.id),
        searchTerm: this.options.searchTerm
      };
    },
    initialize: function(options) {
      this.options = options;
    }
  });

  var GroupMembership = Marionette.CompositeView.extend({
    triggers: {
      'click .js-delete': 'delete'
    },
    tagName: 'li',
    className: 'list-group-item',
    initialize: function(options) {
      this.options = options;
    },
    template: function(data) {
      return templates.render('group_membership', data);
    },
    onDelete: function() {
      var group = this.options.group;
      group.save(
        { members: _.without(group.get('members'), this.model.id) },
        { wait: true });
    }
  });
  
  var GroupView = Marionette.CompositeView.extend({
    triggers: {
      'click .js-add-user-button': 'add:request',
      'click .js-delete': 'delete:request',
      'keyup .js-confirm-delete-name': 'delete:typing',
      'click .js-confirm-delete': 'delete:confirm'
    },
    ui: {
      addUserControls: '.js-add-user-controls',
      addUserSelect: '.js-add-users-select',
      confirmDelete: '.js-confirm-delete',
      confirmDeleteModal: '.js-confirm-delete-modal',
      confirmDeleteName: '.js-confirm-delete-name'
    },
    itemView: GroupMembership,
    itemViewContainer: '.js-memberships',
    itemViewOptions: function(m) {
      return {
        group: this.model
      };
    },
    initialize: function(options) {
      this.options = options;
    },
    template: function(data) {
      return templates.render('group', data);
    },
    onAddRequest: function() {
      var $typeahead = this.ui.addUserSelect.typeahead({
        // LocalStorage is triggered by 'name', and cannot be active for
        // list updates to show.
        //name: 'files',
        valueKey: 'id',
        local: this.options.users.map(function(m){
          return _.defaults(m.toJSON(), {
            tokens: [m.get('name'), m.get('email')]
          });
        }),
        template: function(datum) {
          return templates.render('user_typeahead', datum);
        }
      });
      $typeahead.on('typeahead:selected', _.bind(function(e) {
        var userId = $(e.target).val();
        this.ui.addUserControls.find('.twitter-typeahead').typeahead('destroy');
        this.model.save(
          { members: _.union(this.model.get('members'), [userId]) },
          { wait: true });
        this.ui.addUserControls.hide();
      }, this));
      $typeahead.typeahead('setQuery', '');
      this.ui.addUserControls.find('.twitter-typeahead').css('width', '100%');
      this.ui.addUserControls.show();
      $typeahead.focus();
    },
    onDeleteRequest: function() {
      this.ui.confirmDeleteModal.modal();
      this.ui.confirmDeleteName
        .attr('placeholder', this.model.get("name"))
        .val('');
      this.ui.confirmDelete.prop('disabled', true);
    },
    onDeleteTyping: function() {
      var match = this.ui.confirmDeleteName.val() == this.model.get("name");
      this.ui.confirmDelete.prop('disabled', !match);
    },
    onDeleteConfirm: function() {
      this.ui.confirmDelete.button('loading');
      this.ui.confirmDeleteModal.modal('hide');
      this.model.destroy({ wait: true });
    }
  });
  
  var ManageGroupsView = Marionette.CompositeView.extend({
    triggers: {
      'submit .js-create-form': 'group:create'
    },
    ui: {
      createForm: '.js-create-form'
    },
    itemView: GroupView,
    itemViewContainer: '.js-groups',
    itemViewOptions: function(m) {
      return {
        collection: m.members(),
        users: this.options.users
      };
    },
    initialize: function(options) {
      this.options = options;
    },
    template: function(data) {
      return templates.render('manage_groups', data);
    },
    onGroupCreate: function() {
      var props = {
        name: this.ui.createForm.find('[name="name"]').val()
      };
      this.collection.create(props, { wait: true });
    }
  });
  
  var SettingsView = Marionette.Layout.extend({
    template: function(data) {
      return templates.render('settings', {});
    }
  });

  var AppLayout = Marionette.Layout.extend({
    regions: {
      main: "#main",
      sidebar: "#sidebar .panel-body"
    },
    ui: {
      main: "#main",
      sidebar: "#sidebar",
      sidebarTitle: "#sidebar .title"
    },
    initialize: function(options) {
      this.addRegions({
        notifications: new Marionette.Region({
          el: '#notifications-nav-item'
        }),
        currentUserAvatar: new Marionette.Region({
          el: '#current-user-avatar'
        })
      });
    },
    onRender: function() {
      var $sidebar = this.ui.sidebar;
      var $main = this.ui.main;
      var $sidebarPanel = $sidebar.find('.panel');
      $sidebar.on('click', '.panel-heading', function(e) {
        $sidebar.toggleClass('col-md-4 col-md-1');
        if ($sidebarPanel.hasClass('collapsed')) {
          $main.toggleClass('col-md-8 col-md-11');
          $sidebarPanel.removeClass('collapsed');
        } else {
          $main.toggleClass('col-md-8 col-md-11');
          $sidebarPanel.addClass('collapsed');
        }
      });
    },
    template: function(data) {
      return templates.render('main_layout', data);
    }
  });
  

  return {
    AppLayout: AppLayout,
    ChangePasswordView: ChangePasswordView,
    DeletedView: DeletedView,
    LoadingView: LoadingView,
    FileView: FileView,
    FileDiffView: FileDiffView,
    FileTree: FileTree,
    FolderView: FolderView,
    ManageGroupsView: ManageGroupsView,
    NotificationsNavView: NotificationsNavView,
    NotificationsView: NotificationsView,
    SearchView: SearchView,
    SettingsView: SettingsView,
    StartView: StartView,
    UserAvatar: UserAvatar,
    UserMenu: UserMenu
  };
});