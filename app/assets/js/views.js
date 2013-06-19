define([
        'models',
        'templates',
        'moment',
        'jquery.bootstrap',
        'jquery.iframe-transport',
        'jquery.fileupload'
        ], function(models, templates, moment) {
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
    initialize: function() { this.render(); },
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
      var data = _(this.model.toJSON()).extend({ url: this.model.url() });
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
  })

  var FileOrFolderView = Backbone.View.extend({
    _makeBreadCrumbElement: function() {
      var breadcrumbs = this.model.get('path').split("/");
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

  var FolderView = FileOrFolderView.extend({
    render: function() {
      var fileUploadView = new FileUploadView({
        type: 'folder',
        model: this.model
      })
      var mkdirView = new CreateFolderView({ model: this.model });
      this.$el.append(this._makeBreadCrumbElement());
      this.$el.append(this._makeDeleteElement().addClass('pull-right'));
      this.$el.append(this._makeDownloadElement());
      this.$el.append($('<div/>')
        .append('<h3>Upload files</h3>')
        .append(fileUploadView.$el)
        .append('<h3>Create new folder</h3>')
        .append(mkdirView.$el));
    },
    _makeDownloadElement: function() {
      var $link = $('<a class="btn"/>');
      $link.attr('href', this.model.url()+"/archive");
      $link.html('<i class="icon-download-alt"></i> Download');
      return $link;
    }
  });

  var FileView = FileOrFolderView.extend({
    render: function() {
      var type = typeFromMimeType(this.model.get('mime'));
      this.$el.empty();
      this.$el.append(this._makeBreadCrumbElement());
      var fileInfoView = new FileInfoView({
        model: this.model.info()
      });
      var fileUploadView = new FileUploadView({
        type: 'file',
        model: this.model
      })
      this.$el.append(this._makeDeleteElement().addClass('pull-right'));
      this.$el.append(this._makeDownloadElement());
      this.$el.append($('<div/>')
        .append('<h3>Upload new Version</h3>')
        .append(fileUploadView.$el));
      this.$el.append(fileInfoView.$el);
      switch (type) {
      case 'spreadsheet':
        this.$el.append(this._makeChartElements());
        break;
      case 'image':
        this.$el.append(this._makeImageElement());
        break;
      }
    },
    _makeImageElement: function() {
      return $('<div><br /></div>').append(_.template(
        '<img class="img-polaroid" alt="Image for <%= model.get("path") %>"' +
        ' src="<%= model.url() %>/version/latest" />',
        { model: this.model }));
    },
    _makeChartElements: function() {
      var $wrapper = $('<div/>');
      var format = Modernizr.svg ? 'svg' : 'png';
      var onSuccess = _.bind(function(data) {
        return templates.renderInto($wrapper, 'charts', data);
      }, this);
      $.ajax({
        method: 'GET',
        url: '/charts/?path=' + this.model.get('path')+"&format="+format,
        dataType: 'json',
        success: onSuccess
      });
      return $wrapper;
    },
    _makeDownloadElement: function() {
      var $link = $('<a class="btn"/>');
      $link.attr('href', this.model.url()+"/version/latest");
      $link.html('<i class="icon-download-alt"></i> Download');
      return $link;
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
    showDeleted: function(fof) {
      this.main.show(new DeletedView({ model: fof}));
    }
  });

  return {
    AppLayout: AppLayout,
    FileTree: FileTree
  };
});