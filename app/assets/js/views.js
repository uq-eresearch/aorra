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
      { pattern: /spreadsheetml.sheet$/, type: 'spreadsheet' }
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
      var tree = glyphtree(this.$el, this.options);
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
      this._tree = tree;
    },
    tree: function() {
      if (!this._tree) {
        this.render();
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

  var CreateFolderView = Backbone.View.extend({
    tagName: 'form',
    render: function() {
      var parentFolder = this.model.get('path');
      var $input = $('<input type="text" name="mkdir"/>')
      $input.addClass('span3');
      $input.attr('placeholder', 'folder name');
      this.$el.addClass('form-inline');
      this.$el.attr('method', 'POST');
      this.$el.attr('action', this.model.url()+"/folders");
      this.$el.append($input);
      this.$el.append(' <button class="btn" type="submit">Create</mkdir>');
      this.$el.submit(function(e) {
        var form = e.target;
        var path = $(form).find('input').val();
        $.ajax({
          method: $(form).attr('method'),
          url: $(form).attr('action')+"?"+$(form).serialize(),
          success: function() {
            var $alert = $(
              '<div class="alert alert-info">' +
              '<button type="button" class="close" data-dismiss="alert">&times;</button>' +
              '<strong>' + path + '</strong> was successfully created.' +
              '</div>');
            $input.before($alert);
            $alert.alert();
          }
        });
        return false;
      });
    }
  });

  var VersionView = Backbone.Marionette.ItemView.extend({
    tagName: 'tr',
    modelEvents: {
      "change": "render"
    },
    render: function() {
      // Get data to render
      var data = _(this.model.toJSON()).extend({
        downloadUrl: this.model.url()
      });
      if (this.model != this.model.collection.last()) {
        data = _(data).extend({
          compareUrl: this.model.url().replace(/^\//, '#') + '/diff'
        });
      }
      // Render asynchronously into row
      return templates.renderInto(this.$el, 'version_row', data, function($e) {
        $.each($e.find('.timestamp'), function(i, n) {
          var $n = $(n);
          var dt = moment($n.text());
          $n.attr('title', $n.text());
          $n.text(dt.format('dddd, D MMMM YYYY @ h:mm:ss a'));
        });
      });
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

  var DeleteButtonView = Backbone.View.extend({
    events: {
      'click .delete-button': 'showModal',
      'submit form': 'formSubmit'
    },
    render: function() {
      return templates.renderInto(
          this.$el,
          'delete_button',
          { action: this.model.url() },
          _.bind(function($container) {
            this.getModal().modal({ show: false });
          },this));
    },
    getModal: function() {
      return this.$el.find('.modal');
    },
    showModal: function() {
      this.getModal().modal('show');
      return false;
    },
    hideModal: function() {
      this.getModal().modal('hide');
      return false;
    },
    formSubmit: function(e) {
      this.model.destroy({wait: true});
      this.hideModal();
      return false;
    }
  });
  
  var BreadcrumbView = Backbone.View.extend({
    render: function() {
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
      var context = {
        parents: _.initial(breadcrumbs),
        current: _.last(breadcrumbs)
      };
      return templates.renderInto(this.$el, 'breadcrumbs', context);
    }
  });

  var FileOrFolderView = Backbone.Marionette.Layout.extend({
    _inlineList: function() {
      var $list = $('<ul class="inline"/>');
      _.each(arguments, function(arg) {
        $list.append($('<li/>').append(arg));
      });
      return $list;
    },
    _makeHeading: function() {
      return $('<h2/>').text(this.model.get('name'));
    },
    _makeBreadCrumbElement: function() {
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
      var context = {
        parents: _.initial(breadcrumbs),
        current: _.last(breadcrumbs)
      };
      return templates.renderInto($('<div/>'), 'breadcrumbs', context);
    },
    _makeDeleteElement: function() {
      var deleteButton = new DeleteButtonView({ model: this.model });
      _.defer(function() { deleteButton.render() });
      this.deleteButton = deleteButton;
      return deleteButton.$el;
    }
  });
  
  /*
   * Simple container view which can contain a number of other views or DOM
   * elements, and render each one in an inline list.
   */
  var InlineListView = Backbone.View.extend({
    tagName: 'ul',
    className: 'inline',
    els: [],
    container: new Backbone.ChildViewContainer(),
    initialize: function(viewsAndEls) {
      var isView = function(v) { return v instanceof Backbone.View; };
      _.chain(viewsAndEls)
        .filter(isView)
        .each(_.bind(this.container.add, this.container));
      this.els = _.map(viewsAndEls, function(viewOrEl) {
        return isView(viewOrEl) ? viewOrEl.el : viewOrEl;
      });
      this.render();
    },
    render: function() {
      this.$el.empty().append(_.map(this.els, function(el) {
        return $('<li style="text-align: right; clear: left"/>').append(el);
      }));
      this.container.apply('render');
    }
  });

  var FolderView = FileOrFolderView.extend({
    serializeData: function() {
      return _(this.model.toJSON()).extend({ url: this.model.url() });
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
    onRender: function() {
      this.breadcrumbs.show(new BreadcrumbView({ model: this.model }));
      if (this.model.get('accessLevel') == 'RW') {
        this.upload.show(new FileUploadView({
          type: 'folder',
          model: this.model
        }));
        this.mkdir.show(new CreateFolderView({ model: this.model }));
        this.permissions.show(new GroupPermissionsView({
          model: this.model
        }));
        this.buttons.show(new InlineListView([
          this._makeDownloadElement(),
          new DeleteButtonView({ model: this.model })
        ]));
      } else {
        this.buttons.show(new InlineListView([
          this._makeDownloadElement()
        ]));
      }
    },
    _makeDownloadElement: function() {
      var $link = $('<a class="btn"/>');
      $link.attr('href', this.model.url()+"/archive");
      $link.html('<i class="icon-download-alt"></i> Download');
      return $link;
    }
  });
  
  var ImageElementView = Backbone.View.extend({
    render: function() {
      this.$el.append(_.template(
          '<img class="img-polaroid" alt="Image for <%= model.get("path") %>"' +
          ' src="<%= model.url() %>/version/latest" />',
          { model: this.model }));
    }
  });
  
  var ChartElementView = Backbone.View.extend({
    render: function() {
      var format = Modernizr.svg ? 'svg' : 'png';
      var onSuccess = _.bind(function(data) {
        return templates.renderInto(this.$el, 'charts', data);
      }, this);
      $.ajax({
        method: 'GET',
        url: '/charts/?path=' + this.model.get('path')+"&format="+format,
        dataType: 'json',
        success: onSuccess
      });
    }
  });
  
  var FlagButtonView = Backbone.Marionette.ItemView.extend({
    tagName: 'span',
    ui: {
      button: '.btn',
      popover: '[data-toggle="popover"]'
    },
    initialize: function() {
      this._flag = false;
    },
    serializeData: function() {
      return _.extend(this.dataDefaults(), {
        isSet: this.isSet(),
        count: 0
      });
    },
    template: function(serialized_model) {
      return templates.renderSync('flag_button', serialized_model);
    },
    templateHelpers: {
      activeClass: function() { 
        return this.isSet ? 'active' : '';
      }
    },
    isSet: function() {
      return this._flag;
    },
    _toggleSet: function() {
      return this._flag = this._flag != true;
    },
    onToggleButton: function() {
      this._toggleSet();
      this.render();
    },
    onRender: function() {
      this.ui.button.click(_.bind(function() {
        return this.triggerMethod('toggle:button', {
          collection: this.collection,
          model: this.model,
          view: this
        });
      }, this));
      this.ui.popover.popover();
    }
  });
  
  var EditingButtonView = FlagButtonView.extend({
    dataDefaults: function() {
      return {
        icon: this.isSet() ? 'flag' : 'flag-alt',
        title: 'Edit',
        tooltip: 'Let other users know you are making edits to this file.'
      };
    }
  });
  
  var WatchingButtonView = FlagButtonView.extend({
    dataDefaults: function() {
      return {
        icon: this.isSet() ? 'eye-open' : 'eye-close',
        title: 'Watch',
        tooltip: 'Receive email notifications when new versions are uploaded.'
      };
    }
  });

  var FileView = FileOrFolderView.extend({
    serializeData: function() {
      return _(this.model.toJSON()).extend({ url: this.model.url() });
    },
    template: function(serialized_model) {
      _(serialized_model).extend({
        hasWrite: serialized_model.accessLevel == 'RW'
      })
      return templates.renderSync('file_view', serialized_model); 
    },
    regions: {
      breadcrumbs: '.region-breadcrumbs',
      buttons: '.region-buttons',
      display: '.region-display',
      info:   '.region-info',
      upload: '.region-upload'
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
          new WatchingButtonView({ model: this.model.info() }),
          new EditingButtonView({ model: this.model.info() }),
          this._makeDownloadElement(),
          new DeleteButtonView({ model: this.model })
        ]));
      } else {
        this.buttons.show(new InlineListView([
          this._makeDownloadElement()
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
    },
    _makeDownloadElement: function() {
      var $link = $('<a class="btn"/>');
      $link.attr('href', this.model.url()+"/version/latest");
      $link.html('<i class="icon-download-alt"></i> Download');
      return $link;
    }
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
        $summary.append(
          $('<ul class="inline"/>').html(
            _(diff).reject(function(v) {
              return v[0] == 0;
            }).map(makeBoxes))
        );
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
      var otherVersions = 
        versionList.filter(function(m) {
          return idxOf(version) < idxOf(m);
        }).map(function(m) {
          return m.toJSON();
        });
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
        return '';
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

  var DeletedView = Backbone.View.extend({
    initialize: function() { this.render(); },
    render: function() {
      return templates.renderInto(this.$el, 'deleted_page', {});
    }
  });

  var LoadingView = Backbone.View.extend({
    initialize: function() { this.render(); },
    render: function() {
      return templates.renderInto(this.$el, 'loading_page', {});
    }
  });

  var StartView = Backbone.View.extend({
    initialize: function() { this.render(); },
    render: function() {
      return templates.renderInto(this.$el, 'start_page', {});
    }
  });

  var AppLayout = Backbone.Marionette.Layout.extend({
    template: "#main-layout",
    regions: {
      main: "#main",
      sidebar: "#sidebar"
    },
    initialize: function() {
      this.showLoading();
    },
    showLoading: function() {
      this.main.show(new LoadingView());
    },
    showStart: function() {
      this.main.show(new StartView());
    },
    showFolder: function(folder) {
      this.main.show(new FolderView({ model: folder }));
    },
    showFile: function(file) {
      this.main.show(new FileView({ model: file }));
    },
    showFileDiff: function(file, versionName) {
      this.main.show(new FileDiffView({
        model: file,
        versionName: versionName
      }));
    },
    showDeleted: function(fof) {
      this.main.show(new DeletedView({ model: fof}));
    }
  });

  return {
    AppLayout: AppLayout,
    FileTree: FileTree
  };
});