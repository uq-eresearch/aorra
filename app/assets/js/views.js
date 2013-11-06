/*jslint nomen: true, white: true, vars: true, eqeq: true, todo: true, unparam: true */
/*global _: false, FileAPI: false, Modernizr: false, define: false, window: false */
define([
        'models',
        'templates',
        'moment',
        'diff_match_patch',
        'glyphtree',
        'jquery.bootstrap',
        'marionette',
        'marked',
        'to-markdown',
        'FileAPI',
        'jquery.bootstrap-wysiwyg',
        'typeahead'
        ], function(models, templates, moment, DiffMatchPatch, glyphtree, $, Backbone, marked, toMarkdown) {
  'use strict';

  var svgOrPng = Modernizr.svg ? 'svg' : 'png';
  
  var formatTimestamp = function($n) {
    var dt = moment($n.text());
    $n.attr('title', $n.text());
    $n.text(dt.format('dddd, D MMMM YYYY @ h:mm:ss a'));
  };

  var typeFromMimeType = function(mimeType) {
    var mimeTypePatterns = [
      { pattern: /^image/, type: 'image' },
      { pattern: /^text/, type: 'document' },
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

  var FileTree = Backbone.View.extend({
    tagName: "div",
    initialize: function() {
      this._hint$el =
        $('<p><i class="fa fa-arrow-up"></i> Click to Expand</p>')
          .addClass('alert alert-info');
    },
    render: function() {
      this.tree().element.detach();
      this.$el.append(this.tree().element);
      this.$el.append(this._hint$el);
    },
    close: function() {
      this.tree().element.detach();
    },
    expandTo: function(node) {
      var n = node;
      var nodes = [];
      while (n != null) {
        nodes.push(n);
        n = n.parent();
      }
      _.each(nodes.reverse(), function(n) {
        if (!n.isLeaf()) { n.expand(); }
      });
      this._hint$el.hide();
    },
    _buildTree: function() {
      var tree = glyphtree($('<div/>'), this.options);
      var selectHandler = _.bind(function(event, node) {
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
      var $hint = this._hint$el;
      var toggleHint = function(e) {
        var isClosed = function(node) { return !node.isExpanded(); };
        $hint.toggle(_.all(tree.nodes(), isClosed));
      };
      var createTooltip = function(e, node) {
        if (node.isLeaf()) { return; }
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
      tree.events.icon.click.push(toggleHint);
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

  var FileUploadView = Backbone.Marionette.ItemView.extend({
    tagName: 'div',
    ui: {
      alerts: '.alerts',
      progress: '.progress',
      upload: 'input[type="file"]'
    },
    initialize: function(options) {
      this._multiple = options.multiple == true;
      if (_.isFunction(this.model.info)) {
        this.listenTo(this.model, 'sync', function() {
          this.model.info().fetch();
        });
      }
      this.render();
    },
    serializeData: function() {
      return {
        multiple: this._multiple
      };
    },
    template: function(data) {
      return templates.render('file_upload', data);
    },
    onRender: function() {
      var $jsWrapper = this.$('.js-fileapi-wrapper');
      var f = _.bind(this.uploadFiles, this);
      FileAPI.event.on(this.ui.upload.get(0), 'change', function(e) {
        f(e);
      });
      $jsWrapper.on('mouseover', 'div', function() {
        // Ensure flash displays below modals
        $jsWrapper.find('div').css('z-index', 1000);
        // Set height to something more sensible
        $jsWrapper.find('div').css('height', $jsWrapper.height()+20+'px');
      });
    },
    uploadFiles: function(event) {
      var triggerMethod = _.bind(this.triggerMethod, this);
      var files = FileAPI.getFiles(event);
      var progress = {
        all: {
          loaded: 0,
          total: 0
        },
        file: {
          loaded: 0,
          total: 0
        }
      };
      var collection = this.model.collection;
      // Replace any existing data
      this._progress = progress;
      var $alerts = this.ui.alerts;
      FileAPI.upload({
        url: this.model.uploadUrl(),
        data: {},
        //headers: { 'x-header': '...' },
        files: {
          files: files
        },

        chunkSize: 0, // or chunk size in bytes, eg: FileAPI.MB*.5 (html5)
        chunkUploadRetry: 0, // number of retries during upload chunks (html5)

        //prepare: function(file, options) {},
        //upload: function(xhr, options) {},
        //fileupload: function(xhr, options) {},
        fileprogress: function(evt) {
          // progress file uploading
          progress.file.loaded = evt.loaded;
          progress.file.total = evt.total;
          triggerMethod('progress:update');
        },
        filecomplete: function(err, xhr, file) {
          var $alert = $('<div/>');
          var render = function(type, name, msg) {
            $alert.html(templates.render('alert_box', {
              type: type,
              message: _.template(
                  "<strong><%= f.name %></strong>: <%= f.msg %>",
                  { name: name, msg: msg },
                  { variable: 'f' })
            }));
          };
          if (err) {
            render('error', file.name, xhr.responseText || 
                "Error uploading. Does the file already exist?");
          } else {
            var response = xhr.responseText;
            file = JSON.parse(response);
            // Update file in its collection
            var existingFile = collection.get(file.id);
            if (existingFile) {
              existingFile.set(file);
            } else {
              collection.add(file);
            }
            render('success', file.name, 'Uploaded successfully.');
          }
          $alerts.append($alert);
        },
        progress: function(evt) {
          progress.all.loaded = evt.loaded;
          progress.all.total = evt.total;
          // Reset file progress if done
          if (progress.file.loaded == progress.file.total) {
            progress.file.loaded = 0;
            progress.file.total = 0;
          }
          triggerMethod('progress:update');
        }
        //complete: function(err, xhr) {}
      });
    },
    onProgressUpdate: function() {
      var p = this._progress;
      var done = 100.0 * (p.all.loaded - p.file.loaded) / p.all.total;
      var inprogress = 100.0 * p.file.loaded / p.all.total;
      this.ui.progress.find('.upload-done').css('width', done+'%');
      this.ui.progress.find('.upload-progress').css('width', inprogress+'%');
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
    }
  });

  var EditingButtonView = FlagButtonView.extend({
    dataDefaults: function() {
      return {
        icon: this.isSet() ? 'flag' : 'flag-o',
        flagType: 'edit',
        title: 'Edit',
        tooltip: 'Let other users know you are making edits to this file.'
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
      return templates.render('create_folder', serialized_model);
    },
    onFormSubmit: function() {
      var fs = this.model.collection;
      var $form = this.$el.find('form');
      $.ajax({
        method: $form.attr('method'),
        url: $form.attr('action')+"?"+$form.serialize(),
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
            $form.find('.messages').append($alert);
            $alert.alert();
          });
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
    triggers: {
      "click .delete": "version:delete"
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
          compareUrl: this.model.url().replace(/^\//, '#') + '/diff',
          canDelete: this.options.canDelete
        });
      }
      return data;
    },
    template: function(serialized_model) {
      return templates.render('version_row', serialized_model);
    },
    onRender: function($e) {
      formatTimestamp(this.ui.timestamp);
    },
    onVersionDelete: function(e) {
      e.model.destroy();
    }
  });

  var FileInfoView = Backbone.Marionette.CompositeView.extend({
    initialize: function(options) {
      this._users = options.users;
      this.collection = this.model.versionList();
      this.render();
      this.model.fetch();
    },
    itemView: VersionView,
    itemViewOptions: function(model, index) {
      var author = model.get("author");
      return {
        canDelete: author && this._users.current().get("email") == author.email
      };
    },
    itemViewContainer: 'tbody',
    emptyView: Backbone.Marionette.ItemView.extend({
      template: function() {
        return '<tr><td><em>Loading version information...</em></td></tr>';
      }
    }),
    template: function(serialized_model) {
      return templates.render('version_table', serialized_model);
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
      return templates.render('permission_row', serialized_model);
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
      return templates.render('permission_table', serialized_model);
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
      this._url = options.url;
      this._label = options.label || 'Download';
    },
    render: function() {
      var $link = $('<a class="btn btn-default" title="Download"/>');
      $link.attr('href', this._url);
      $link.append('<i class="fa fa-download"></i>');
      $link.append('<span class="hidden-phone">'+this._label+'</span>');
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
      return templates.render('breadcrumbs', serialized_model);
    },
    onRender: function() {
      var content = templates.render('link_popup', {
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
      });
      return templates.render('folder_view', serialized_model);
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
          if (event.which == 13) {
            $(event.target).blur();
          }
        });
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
          model: this.model,
          multiple: true
        }));
        this.mkdir.show(new CreateFolderView({ model: this.model }));
        if (this.isAdmin()) {
          this.permissions.show(new GroupPermissionsView({
            model: this.model
          }));
        }
        this.buttons.show(new InlineListView([
          new WatchingButtonView({
            collection: this._users,
            targetId: this.model.id
          }),
          new DownloadButtonView({ url: this.model.url()+"/archive" }),
          new DownloadButtonView({ 
            label: 'Charts', url: this.model.url()+"/charts.zip" }),
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
      var url = _.template(this.model.url() + '/charts?format=<%=format%>', {
        format: svgOrPng
      });
      var onSuccess = function(data) {
        this._charts = data.charts;
        this.render();
      };
      $.get(url, _.bind(onSuccess, this));
    },
    serializeData: function() {
      var showTypes = _.uniq(_.pluck(this._charts, 'type')).length > 1;
      return {
        zip: this.model.url() + '/charts.zip',
        charts: _.map(this._charts, function(c, i) {
          return _(c).extend({
            first: i == 0,
            title: showTypes
              ? c.type + " - " + c.region
              : c.region,
            slug: _.template("<%=v.type%>-<%=v.region%>-<%=v.uniqueId%>",
              {
                type: _.str.slugify(c.type),
                region: _.str.slugify(c.region),
                uniqueId: i
              }, { variable: 'v' }),
            csv: c.url.replace(/\.(png|svg)\?/, ".csv?"),
            docx: c.url.replace(/\.(png|svg)\?/, ".docx?"),
            html: c.url.replace(/\.(png|svg)\?/, ".html?")
          });
        })
      };
    },
    template: function(serialized_model) {
      return templates.render('charts', serialized_model);
    },
    onRender: function() {
      this.$el.find('.commentary').each(function(i, e) {
        var $e = $(e);
        $e.load($e.attr('data-url'));
      });
    }
  });
  
  var ChartSelector = Backbone.Marionette.Layout.extend({
    tagName: 'span',
    ui: {
      toolbarButton: '.btn-insert-chart',
      input: 'input',
      chartList: '.chart-list'
    },
    renderChartList: function() {
      this.ui.chartList.html(templates.render('chart_list', {
        charts: this._charts
      }));
    },
    initTypeahead: function() {
      this.ui.input.typeahead({
        name: 'files',
        valueKey: 'id',
        local: _(this.model.collection.where({ type: 'file' })).map(function(m){
          return _.defaults(m.toJSON(), {
            tokens: [m.id, m.get('name')]
          });
        }),
        template: function(datum) {
          return templates.render('file_typeahead', datum);
        }
      });
    },
    destroyTypeahead: function() {
      this.ui.input.typeahead('destroy');
    },
    onRender: function() {
      var $insertChartContent = this.$el.find('.insert-chart-content').hide();
      this.ui.toolbarButton.popover({
        'html': true,
        'placement': 'bottom',
        'content': function() {
          return $insertChartContent.show();
        }
      });
      // All events on popup content are detached when hiding the popup,
      // so init typeahead when popup shows and destroy it on hide.
      this.ui.toolbarButton.on('shown.bs.popover',
          _.bind(this.initTypeahead, this));
      this.ui.toolbarButton.on('hide.bs.popover',
          _.bind(this.destroyTypeahead, this));
      var $input = this.ui.input;
      var filesAndFolders = this.model.collection;
      var onSuccess = _.bind(function(data) {
        this._charts = data.charts;
        this.renderChartList();
      }, this);
      this.$el.on('click', '.btn-get-charts', function() {
        var fileId = $input.val();
        var file = filesAndFolders.findWhere({type: 'file', id: fileId});
        if (_.isObject(file)) {
          $.get(file.url() + '/charts?format='+svgOrPng, onSuccess);
        }
      });
      this.$el.on('click', '.chart-list a', _.bind(function(e) {
        var chart = _(this._charts).findWhere({url: $(e.target).data('url')});
        if (_.isObject(chart)) {
          this.trigger('chart:selected', chart);
        }
        this.ui.toolbarButton.popover('hide');
        return false;
      }, this));
    },
    serializeData: function() {
      return {
        charts: this._charts
      };
    },
    template: function(serialized_model) {
      return templates.render('chart_selector', serialized_model);
    }
  });

  var MarkdownEditor = Backbone.Marionette.Layout.extend({
    modelEvents: {
      "sync": "fetchData"
    },
    regions: {
      chartSelector: '.chart-selector'
    },
    ui: {
      toolbar: '.html-toolbar',
      html: '.html-pane',
      source: '.source-pane',
      save: 'button.save'
    },
    initialize: function(options) {
      this._content = '';
      this._users = this.options.users;
      this.fetchData();
    },
    fetchData: function() {
      $.get(this.model.downloadUrl(), _.bind(function(data) {
        this._content = data;
      }, this)).done(_.bind(this.render, this));
    },
    serializeData: function() {
      return {
        content: this._content,
        editable: this.editable()
      };
    },
    template: function(obj) {
      return templates.render('markdown_viewer', {
        html: marked(obj.content),
        source: obj.content,
        editable: obj.editable
      });
    },
    flags: function() {
      return this._users.flags()['edit'];
    },
    editable: function() {
      if (this.model.get('accessLevel') != 'RW') {
        return false;
      }
      var flags = this.flags().filter(_.bind(function(f) {
        return f.get('targetId') == this.model.id;
      }, this));
      return flags.length == 1 && _(flags).any(_.bind(function(m) {
        return m.get('userId') == this._users.currentId();
      }, this));
    },
    _watchEditFlags: function() {
      if (this._watchingFlags) { return; }
      this.flags().on('add remove', _.bind(function(f) {
        if (f.get('targetId') == this.model.id) {
          this.render();
        }
      }, this));
      this._watchingFlags = true;
    },
    onRender: function() {
      if (this.editable()) {
        var toggleSave = _.bind(function(content) {
          this.ui.save.prop("disabled", this._content == content);
        }, this);
        var save = _.bind(function() {
          $.ajax(this.model.uploadUrl(), {
            type: 'POST',
            contentType: 'text/x-markdown',
            data: this.ui.source.val()
          });
        }, this);
        this.ui.html.wysiwyg(); // Initialize with Bootstrap WYSIWYG
        this.ui.source
          .on('keyup', _.bind(function(e) {
            var content = $(e.target).val();
            this.ui.html.html(marked(content));
            toggleSave(content);
          }, this));
        var updateMarkdown = _.bind(function(e) {
          var content = this.ui.html.cleanHtml();
          this.ui.source.val(toMarkdown(content));
          toggleSave(this.ui.source.val());
        }, this);
        this.ui.html.on('keyup', updateMarkdown);
        this.ui.toolbar.on('click', updateMarkdown);
        this.ui.save.on('click', save);
        var selector = new ChartSelector({ model: this.model });
        this.chartSelector.show(selector);
        selector.on('chart:selected', _.bind(function(chart) {
          this.ui.html.append(templates.render('chart_with_caption', chart));
          updateMarkdown();
        }, this));
        toggleSave(this._content);
      }
      this._watchEditFlags();
    }
  });

  var NoEditorView = Backbone.Marionette.ItemView.extend({
    template: function() {
      return "";
    }
  });

  var OnlineEditorView = {
    create: function(file, users) {
      if (/\.(markdown|md)$/.test(file.get('name'))) {
        return new MarkdownEditor({ model: file, users: users });
      }
      return new NoEditorView();
    }
  };

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
      return templates.render('file_view', serialized_model);
    },
    regions: {
      breadcrumbs: '.region-breadcrumbs',
      buttons: '.region-buttons',
      display: '.region-display',
      editor: '.region-editor',
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
          if (event.which == 13) {
            $(event.target).blur();
          }
        });
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
        model: this.model.info(),
        users: this._users
      }));
      this.editor.show(OnlineEditorView.create(this.model, this._users));
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
      if (_.isObject(serialized_model.version)) {
        return templates.render('filediff_view', serialized_model);
      }
      return templates.render('loading_page', {});
    },
    regions: {
      breadcrumbs: '.region-breadcrumbs',
      diff: '.region-diff'
    },
    onRender: function() {
      if (!_.isObject(this.version)) { return; }
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
      return templates.render('deleted_page', {});
    }
  });

  var LoadingView = Backbone.Marionette.ItemView.extend({
    template: function() {
      return templates.render('loading_page', {});
    }
  });

  var StartView = Backbone.Marionette.ItemView.extend({
    template: function() {
      return templates.render('start_page', {});
    }
  });

  var UserMenu = Backbone.Marionette.ItemView.extend({
    template: function() {
      return templates.render('user_menu', {});
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

  var NotificationMessageView = Backbone.Marionette.ItemView.extend({
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
      return templates.render('notifications_view', data);
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
      return templates.render('notifications_nav_item', data);
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
      this.main.show(new ChangePasswordView());
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