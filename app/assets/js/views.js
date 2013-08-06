define([
        'models',
        'templates',
        'moment',
        'diff_match_patch',
        'glyphtree',
        'jquery.bootstrap',
        'marionette',
        'jquery.iframe-transport',
        'jquery.fileupload'
        ], function(models, templates, moment, diff_match_patch, glyphtree, $, Backbone) {
  'use strict';
  var typeFromMimeType = function(mimeType) {
    var mimeTypePatterns = [
      { pattern: /^image/, type: 'image' },
      { pattern: /^text/, type: 'document' },
      { pattern: /wordprocessingml.document$/, type: 'document' },
      { pattern: /spreadsheetml.sheet$/, type: 'spreadsheet' },
      { pattern: /vnd.ms-excel$/, type: 'spreadsheet' }
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

  var FileTree = Backbone.View.extend({
    tagName: "div",
    render: function() {
      this.tree().element.detach();
      this.$el.append(this.tree().element);
    },
    close: function() {
      this.tree().element.detach();
    },
    _buildTree: function() {
      var tree = glyphtree($('<div/>'), this.options);
      var selectHandler = _.bind(function(event, node) {
        var m = _.defaults({}, node.attributes);
        // Emit select event
        if (node.type == 'folder') {
          this.trigger("folder:select", node.id);
        } else {
          this.trigger("file:select", node.id);
        }
      }, this);
      var hoverHandler = function(e) {
        $(e.currentTarget).tooltip(e.type == 'mouseenter' ? 'show' : 'hide');
      };
      var setTooltipText = function(e) {
        $(e.currentTarget).tooltip('show');
      };
      var createTooltip = function(e, node) {
        if (node.isLeaf()) return;
        $(e.currentTarget).tooltip({
          placement: 'bottom',
          trigger: 'manual',
          delay: { show: 0, hide: 100 },
          title: function() {
            return (node.isExpanded() ? 'Collapse' : 'Expand') + ' Folder';
          }
        });
      };
      tree.events.label.click = [selectHandler];
      tree.events.icon.click.push(setTooltipText);
      tree.events.icon.mouseenter = [createTooltip, hoverHandler];
      tree.events.icon.mouseleave = [hoverHandler];
      return tree;
    },
    tree: function() {
      if (!this._tree) {
        this._tree = this._buildTree();
      }
      return this._tree;
    },
    options: {
      startExpanded: true,
      typeResolver: function(struct) {
        if (struct.type == 'folder') {
          return 'folder';
        }
        if (struct['attributes']) {
          var mimeType = struct['attributes']['mimeType'];
          if (mimeType) {
            return typeFromMimeType(mimeType);
          }
        }
        return 'file';
      },
      types: {
        folder: {
          icon: {
            "default": {
              content: "\uf07b",
              'font-family': "FontAwesome"
            },
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

  var FileUploadView = Backbone.View.extend({
    tagName: 'div',
    initialize: function() {
      if (_.isFunction(this.model.info)) {
        this.listenTo(this.model, 'sync', function() {
          this.model.info().fetch();
        });
      }
      this.render();
    },
    render: function() {
      this.$el.empty();
      var $alerts = $('<div/>');
      var $progressbar = $('<div class="progress"></div>')
          .append('<div class="bar"></div>');
      var setProgress = function(current, total) {
        var progress = parseInt(1.0 * current / total * 100, 10);
        $progressbar.find('.bar').css('width', progress+'%');
      };
      setProgress(0, 1);
      var $input = $('<input type="file" name="files[]" multiple />');
      this.$el.append($input);
      $progressbar.hide();
      this.$el.append($progressbar);
      this.$el.append($alerts);
      $input.fileupload({
        url: this.model.uploadUrl(),
        type: 'POST',
        limitMultiFileUploads:
          (this.model.urlRoot == '/file' ? 1 : undefined),
        dataType: 'json',
        sequentialUploads: true,
        add: _.bind(function (e, data) {
          $progressbar.show();
          data.submit();
        }, this),
        progressall: function (e, data) {
          setProgress(data.loaded, data.total);
          if (data.loaded == data.total) {
            $progressbar.hide();
            setProgress(0, 1);
          }
        },
        done: _.bind(function (e, data) {
          _.each(data.result.files, function(file) {
            var $alert = $('<div/>');
            var render = function(type, msg) {
              templates.renderInto($alert, 'alert_box', {
                type: type,
                message: _.template(
                    "<strong><%= f.name %></strong>: <%= f.msg %>",
                    { name: file.name, msg: msg },
                    { variable: 'f' })
              });
            };
            if (_.isUndefined(file['error'])) {
              render('success', 'Uploaded successfully.');
            } else {
              render('error', file.error);
            }
            $alerts.append($alert);
          });
          if (_.isFunction(this.model.info)) {
            this.model.info().fetch();
          }
        }, this)
      });
    }
  });

  var CreateFolderView = Backbone.Marionette.ItemView.extend({
    triggers: {
      'submit form': 'form:submit'
    },
    serializeData: function() {
      return {
        action: this.model.url() + "/folders"
      };
    },
    template: function(serialized_model) {
      return templates.renderSync('create_folder', serialized_model);
    },
    onFormSubmit: function() {
      var $form = this.$el.find('form');
      var path = $form.find('input').val();
      $.ajax({
        method: $form.attr('method'),
        url: $form.attr('action')+"?"+$form.serialize(),
        success: function() {
          var $alert = $(templates.renderSync('alert_box', {
            type: 'info',
            message: '<strong>' + path + '</strong> was successfully created.'
          }));
          $form.find('.messages').append($alert);
          $alert.alert();
        }
      });
      return false;
    }
  });

  var VersionView = Backbone.Marionette.ItemView.extend({
    tagName: 'tr',
    modelEvents: {
      "change": "render"
    },
    ui: {
      timestamp: '.timestamp'
    },
    serializeData: function() {
      // Get data to render
      var data = _(this.model.toJSON()).extend({
        downloadUrl: this.model.url()
      });
      if (this.model != this.model.collection.last()) {
        data = _(data).extend({
          compareUrl: this.model.url().replace(/^\//, '#') + '/diff'
        });
      }
      return data;
    },
    template: function(serialized_model) {
      return templates.renderSync('version_row', serialized_model);
    },
    onRender: function($e) {
      var $n = this.ui.timestamp;
      var dt = moment($n.text());
      $n.attr('title', $n.text());
      $n.text(dt.format('dddd, D MMMM YYYY @ h:mm:ss a'));
    }
  });

  var FileInfoView = Backbone.Marionette.CompositeView.extend({
    initialize: function() {
      this.collection = this.model.versionList();
      this.render();
      this.model.fetch();
    },
    itemView: VersionView,
    itemViewContainer: 'tbody',
    onCompositeCollectionRendered: function() {
      if (this.collection.isEmpty())
        this.$el.hide();
      else
        this.$el.show();
    },
    template: function(serialized_model) {
      return templates.renderSync('version_table', serialized_model);
    }
  });

  var GroupPermissionView = Backbone.Marionette.ItemView.extend({
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
      return templates.renderSync('permission_row', serialized_model);
    },
    updateModel: function() {
      var newAccess = this.$el.find('select').val();
      this.model.set('access', newAccess);
      this.model.save();
    }
  });

  var GroupPermissionsView = Backbone.Marionette.CompositeView.extend({
    initialize: function() {
      this.collection = this.model.permissions();
      this.render();
      this.collection.fetch();
    },
    itemView: GroupPermissionView,
    itemViewContainer: 'tbody',
    template: function(serialized_model) {
      return templates.renderSync('permission_table', serialized_model);
    }
  });

  var DeleteButtonView = Backbone.Marionette.ItemView.extend({
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
      return templates.renderSync('delete_button', serialized_model);
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
      this._url = options.url;
    },
    render: function() {
      var $link = $('<a class="btn btn-default" title="Download"/>');
      $link.attr('href', this._url);
      $link.append('<i class="icon-download-alt"></i>');
      $link.append('<span class="hidden-phone">Download</span>');
      this.$el.html($link);
    }
  });

  var BreadcrumbView = Backbone.Marionette.ItemView.extend({
    ui: {
      share: '.share-link',
      shareContent: '.share-link-content',
      popover: '.popover-content'
    },
    serializeData: function() {
      var collection = this.model.collection;
      var path = [];
      var m = this.model;
      while (m != null) {
        path.unshift(m);
        m = collection.get(m.get('parent'));
      }
      var breadcrumbs = _.map(path, function(m) {
        return _.extend(m.asNodeStruct(), { url: m.displayUrl() });
      });
      return {
        parents: _.initial(breadcrumbs),
        current: _.last(breadcrumbs)
      };
    },
    template: function(serialized_model) {
      return templates.renderSync('breadcrumbs', serialized_model);
    },
    onRender: function() {
      var content = templates.renderSync('link_popup', {
        url: window.location.protocol+"//"+window.location.host+this.model.url()
      });
      this.ui.share.popover({
        html: true,
        content: content
      });
      this.ui.share.on('click', _.bind(function() {
        var $share = this.ui.share;
        var $input = this.$el.find('input');
        $input.select();
        $input.on('blur', function() { $share.popover('hide'); });
      }, this));
    }
  });

  var FileOrFolderView = Backbone.Marionette.Layout.extend({
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
  var InlineListView = Backbone.View.extend({
    tagName: 'ul',
    className: 'list-inline',
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
        return $('<li/>').append(el);
      }));
      this.container.apply('render');
    }
  });

  var FolderView = FileOrFolderView.extend({
    modelEvents: {
      "sync": "render"
    },
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
      })
      return templates.renderSync('folder_view', serialized_model);
    },
    regions: {
      breadcrumbs: '.region-breadcrumbs',
      buttons: '.region-buttons',
      upload: '.region-upload',
      mkdir: '.region-mkdir',
      permissions: '.region-permissions'
    },
    triggers: {
      'click span.name': 'focus:name',
      'blur input.name': 'blur:name'
    },
    ui: {
      nameSpan: 'span.name',
      nameField: 'input.name'
    },
    onFocusName: function() {
      if (this.isAdmin()) {
        this.ui.nameSpan.hide();
        this.ui.nameField.show();
        this.ui.nameField.keyup(function(event) {
          // on Enter key
          if (event.which == 13)
            $(event.target).blur();
        })
        this.ui.nameField.focus();
      }
    },
    onBlurName: function() {
      if (this.isAdmin()) {
        this.model.set("name", this.ui.nameField.val());
        this.ui.nameSpan.show();
        this.ui.nameField.hide();
        this.model.save().error(_.bind(this.model.fetch, this.model));
      }
    },
    onRender: function() {
      this.breadcrumbs.show(new BreadcrumbView({ model: this.model }));
      if (this.model.get('accessLevel') == 'RW') {
        this.upload.show(new FileUploadView({
          type: 'folder',
          model: this.model
        }));
        this.mkdir.show(new CreateFolderView({ model: this.model }));
        if (this.isAdmin()) {
          this.permissions.show(new GroupPermissionsView({
            model: this.model
          }));
        }
        this.buttons.show(new InlineListView([
          new DownloadButtonView({ url: this.model.url()+"/archive" }),
          this.isAdmin() ? new DeleteButtonView({ model: this.model }) : null
        ]));
      } else {
        this.buttons.show(new InlineListView([
          new DownloadButtonView({ url: this.model.url()+"/archive" })
        ]));
      }
      this.delegateEvents();
    },
    isAdmin: _.memoize(function() {
      return this._users.current().get('isAdmin');
    })
  });

  var ImageElementView = Backbone.View.extend({
    render: function() {
      this.$el.append(_.template(
          '<img class="img-thumbnail" alt="Image for <%= model.get("path") %>"' +
          ' src="<%= model.url() %>/version/latest" />',
          { model: this.model }));
    }
  });

  var ChartElementView = Backbone.Marionette.ItemView.extend({
    initialize: function() {
      var url = _.template('/charts/?path=<%=path%>&format=<%=format%>', {
        format: Modernizr.svg ? 'svg' : 'png',
        path: this.model.get('path')
      });
      var onSuccess = function(data) {
        this._charts = data.charts;
        this.render();
      };
      $.get(url, _.bind(onSuccess, this));
    },
    serializeData: function() {
      return {
        charts: _.map(this._charts, function(c, i) {
          return _(c).extend({
            first: i == 0,
            slug: _.str.slugify(c.region)
          });
        })
      };
    },
    template: function(serialized_model) {
      return templates.renderSync('charts', serialized_model);
    }
  });

  var FlagButtonView = Backbone.Marionette.ItemView.extend({
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
        // Get associated user ID
        .invoke('get', 'userId')
        // Grab matching user object from user collection
        .map(_.bind(function(v) {
          var obj = this.collection.get(v);
          return obj != null ? obj.toJSON() : null;
        }, this))
        // Filter out the missing users
        .compact()
        // Return the value
        .value();
      return _.extend(data, {
        isSet: this.isSet(),
        contentTitle: function() { return data.title+"ing"; },
        users: users,
        count: users.length
      });
    },
    template: function(serialized_model) {
      return templates.renderSync('flag_button', serialized_model);
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
      if (_.isUndefined(this._flags))
        return null;
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
    }
  });

  var EditingButtonView = FlagButtonView.extend({
    dataDefaults: function() {
      return {
        icon: this.isSet() ? 'flag' : 'flag-alt',
        flagType: 'edit',
        title: 'Edit',
        tooltip: 'Let other users know you are making edits to this file.'
      };
    }
  });

  var WatchingButtonView = FlagButtonView.extend({
    dataDefaults: function() {
      return {
        icon: this.isSet() ? 'eye-open' : 'eye-close',
        flagType: 'watch',
        title: 'Watch',
        tooltip: 'Receive email notifications when new versions are uploaded.'
      };
    }
  });

  var FileView = FileOrFolderView.extend({
    modelEvents: {
      "sync": "render"
    },
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
      return templates.renderSync('file_view', serialized_model);
    },
    regions: {
      breadcrumbs: '.region-breadcrumbs',
      buttons: '.region-buttons',
      display: '.region-display',
      info:   '.region-info',
      upload: '.region-upload'
    },
    triggers: {
      'click span.name': 'focus:name',
      'blur input.name': 'blur:name'
    },
    ui: {
      nameSpan: 'span.name',
      nameField: 'input.name'
    },
    onFocusName: function() {
      if (this.isAdmin()) {
        this.ui.nameSpan.hide();
        this.ui.nameField.show();
        this.ui.nameField.keyup(function(event) {
          // on Enter key
          if (event.which == 13)
            $(event.target).blur();
        })
        this.ui.nameField.focus();
      }
    },
    onBlurName: function() {
      if (this.isAdmin()) {
        this.model.set("name", this.ui.nameField.val());
        this.ui.nameSpan.show();
        this.ui.nameField.hide();
        this.model.save().error(_.bind(this.model.fetch, this.model));
      }
    },
    onRender: function() {
      var type = typeFromMimeType(this.model.get('mime'));
      this.breadcrumbs.show(new BreadcrumbView({ model: this.model }));
      this.info.show(new FileInfoView({
        model: this.model.info()
      }));
      if (this.model.get('accessLevel') == 'RW') {
        this.upload.show(new FileUploadView({
          type: 'file',
          model: this.model
        }));
        this.buttons.show(new InlineListView([
          new WatchingButtonView({
            collection: this._users,
            targetId: this.model.id
          }),
          new EditingButtonView({
            collection: this._users,
            targetId: this.model.id
          }),
          new DownloadButtonView({ url: this.model.url()+"/version/latest" }),
          this.isAdmin() ? new DeleteButtonView({ model: this.model }) : null
        ]));
      } else {
        this.buttons.show(new InlineListView([
          new WatchingButtonView({
            collection: this._users,
            targetId: this.model.id
          }),
          new DownloadButtonView({ url: this.model.url()+"/version/latest" })
        ]));
      }
      switch (type) {
      case 'spreadsheet':
        this.display.show(new ChartElementView({ model: this.model }));
        break;
      case 'image':
        this.display.show(new ImageElementView({ model: this.model }));
        break;
      }
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
        var dmp = new diff_match_patch;
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
            $el = $('<ins class="clickable green-text"/>')
            $el.click(scrollToFunc($summary));
            break;
          default:
            $el = $('<span/>')
          }
          $el.attr('id', v[2]);
          $el.text(v[1]);
          return $el;
        });
        var stats = _.reduce(diff,
            function(h, v) {
              switch (v[0]) {
                case 1:  h['additions']++; break;
                case -1: h['deletions']++; break;
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
            $icon.addClass('icon-plus-sign-alt green-text');
            $icon.attr('title', 'Added text');
          } else {
            $icon.addClass('icon-minus-sign-alt red-text');
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
    initialize: function(attrs) {
      var versionName = attrs.versionName;
      var info = this.model.info();
      info.fetch().done(_.bind(function() {
        this.version = info.versionList().findWhere({ name: versionName });
        this.render();
      }, this));
    },
    serializeData: function() {
      var model = this.model;
      var version = this.version;
      var versionList = model.info().versionList();
      var idxOf = _.bind(versionList.indexOf, versionList);
      // Only show earlier versions
      var otherVersions = _.invoke(
        versionList.filter(function(m) {
          return idxOf(version) < idxOf(m);
        }), 'toJSON');
      return {
        backUrl: model.displayUrl(),
        version: version ? version.toJSON() : null,
        otherVersions: otherVersions
      };
    },
    template: function(serialized_model) {
      if (_.isObject(serialized_model.version))
        return templates.renderSync('filediff_view', serialized_model);
      else
        return templates.renderSync('loading_page', {});
    },
    regions: {
      breadcrumbs: '.region-breadcrumbs',
      diff: '.region-diff'
    },
    onRender: function() {
      if (!_.isObject(this.version)) return;
      this.breadcrumbs.show(new BreadcrumbView({ model: this.model }));
      var onSelectChange = _.bind(function(e) {
        var versionList = this.model.info().versionList();
        var otherVersion = versionList.findWhere({ name: $(e.target).val() });
        this.diff.show(new DiffView(otherVersion, this.version));
      }, this);
      var $select = this.$el.find('select');
      $select.change(onSelectChange);
      $select.trigger('change');
    }
  });

  var DeletedView = Backbone.Marionette.ItemView.extend({
    template: function() {
      return templates.renderSync('deleted_page', {});
    }
  });

  var LoadingView = Backbone.Marionette.ItemView.extend({
    template: function() {
      return templates.renderSync('loading_page', {});
    }
  });

  var StartView = Backbone.Marionette.ItemView.extend({
    template: function() {
      return templates.renderSync('start_page', {});
    }
  });
  
  var UserMenu = Backbone.Marionette.ItemView.extend({
    template: function() {
      return templates.renderSync('user_menu', {});
    },
    onRender: function() {
      this.$el.find("[href='"+Backbone.history.location.hash+"']")
        .parent('li').addClass('active');
    }
  });
  
  var ChangePasswordView = Backbone.Marionette.ItemView.extend({
    initialize: function() { this.render(); },
    events: {
      'keyup #newPassword,#repeatPassword': 'typePassword',
      'click button[type="submit"]': 'submitForm'
    },
    template: function() {
      return templates.renderSync('change_password', {});
    },
    typePassword: function() {
      var minlength = 8;
      var $submit = this.$el.find('form button[type="submit"]');
      var $passwords = this.$el.find('#newPassword, #repeatPassword');
      var valid = (function() {
        var values = $.map($passwords, function(v) { return $(v).val() });
        return _.any(values, function(v) { return v.length >= minlength }) &&
          _.uniq(values).length == 1;
      })();
      $passwords.parents('.form-group')
        .removeClass('has-success has-error')
        .addClass( valid ? 'has-success' : 'has-error' );
      $submit.prop('disabled', !valid);
    },
    submitForm: function() {
      var $form = this.$el.find('form');
      var showAlert = function(data) {
        $form.find('.outcome').html(templates.renderSync('alert_box', data));
      }
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

  var NotificationMessageView = Backbone.Marionette.ItemView.extend({
    tagName: 'li',
    className: 'media',
    triggers: {
      'click .unread': 'notification:read',
      'click .delete': 'notification:delete'
    },
    template: function(data) {
      return templates.renderSync('notification_message', data);
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

  var NotificationsView = Backbone.Marionette.CompositeView.extend({
    collectionEvents: {
      'sync': 'render'
    },
    itemView: NotificationMessageView,
    emptyView: Backbone.Marionette.ItemView.extend({
      template: function() {
        return '<div class="media-body">No messages.</div>';
      }
    }),
    itemViewContainer: '.notifications',
    template: function(data) {
      return templates.renderSync('notifications_view', data);
    }
  });

  var NotificationsNavView = Backbone.Marionette.ItemView.extend({
    tagName: 'a',
    collectionEvents: {
      'sync': 'render',
      'remove': 'render'
    },
    attributes: {
      "data-placement": "bottom",
      href: '#notifications',
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
      return templates.renderSync('notifications_nav_item', data);
    },
    onRender: function() {
      this.$el.tooltip();
    }
  });

  var AppLayout = Backbone.Marionette.Layout.extend({
    template: "#main-layout",
    regions: {
      main: "#main",
      sidebar: "#sidebar"
    },
    initialize: function(options) {
      this.users = options.users;
      this.notificationMessages = options.notifications;
      this.addRegions({
        notifications: new Backbone.Marionette.Region({
          el: '#notifications-nav-item'
        })
      });
      this.notifications.show(new NotificationsNavView({
        collection: this.notificationMessages
      }));
      this.notificationMessages.fetch();
    },
    getFileTree: function() {
      if (_.isUndefined(this._fileTree)) {
        this._fileTree = new FileTree();
      }
      return this._fileTree;
    },
    changePassword: function() {
      this.sidebar.show(new UserMenu());
      this.main.show(new ChangePasswordView())
    },
    showNotifications: function() {
      this.sidebar.show(new UserMenu());
      this.main.show(new NotificationsView({
        collection: this.notificationMessages
      }));
    },
    showLoading: function() {
      this.main.show(this.getFileTree());
      this.main.show(new LoadingView());
    },
    showStart: function() {
      this.sidebar.show(this.getFileTree());
      this.main.show(new StartView());
    },
    showFolder: function(folder) {
      this.sidebar.show(this.getFileTree());
      this.main.show(new FolderView({ model: folder, users: this.users }));
    },
    showFile: function(file) {
      this.sidebar.show(this.getFileTree());
      this.main.show(new FileView({ model: file, users: this.users }));
    },
    showFileDiff: function(file, versionName) {
      this.sidebar.show(this.getFileTree());
      this.main.show(new FileDiffView({
        model: file,
        versionName: versionName
      }));
    },
    showDeleted: function(fof) {
      this.sidebar.show(this.getFileTree());
      this.main.show(new DeletedView({ model: fof }));
    }
  });

  return {
    AppLayout: AppLayout,
    FileTree: FileTree
  };
});