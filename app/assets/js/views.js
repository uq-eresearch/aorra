define([
        'models',
        'templates',
        'jquery.bootstrap',
        'jquery.iframe-transport',
        'jquery.fileupload'
        ], function(models, templates) {
  'use strict';
  var typeFromMimeType = function(mimeType) {
    var mimeTypePatterns = [
      { pattern: /^image/, type: 'image' },
      { pattern: /^text/, type: 'document' },
      { pattern: /wordprocessingml.document$/, type: 'document' },
      { pattern: /spreadsheetml.sheet$/, type: 'spreadsheet' }
    ];
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
          this.trigger("folder:select", models.Folder.fromNode(node));
        } else {
          this.trigger("file:select", models.File.fromNode(node));
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
      this.tree = tree;
    },
    options: {
      startExpanded: true,
      typeResolver: function(struct) {
        if (struct.type == 'folder') {
          return 'folder';
        }
        if (struct['attributes'] && struct['attributes']['mimeType']) {
          return typeFromMimeType(struct.attributes.mimeType);
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
    initialize: function() { this.render(); },
    render: function() {
      this.$el.empty();
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
      $input.fileupload({
        url: this.model.url()+"/files",
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
        done: function (e, data) {
          // No need to do anything
        }
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
      var data = _(this.model.toJSON()).extend({ url: this.model.url() });
      return templates.renderInto(this.$el, 'version_row', data);
    }
  });
  
  var VersionListView = Backbone.Marionette.CollectionView.extend({
    tagName: 'tbody',
    itemView: VersionView
  });
  
  var FileInfoView = Backbone.View.extend({
    initialize: function() {
      this.vlv = new VersionListView({
        collection: this.model.get('versions')
      });
      this.model.on('change', _.bind(this.render, this));
    },
    render: function(obj) {
      return templates.renderInto(
          this.$el, 
          'version_table', 
          {}, 
          _.bind(function($container) {
            $container.find('tbody').replaceWith(this.vlv.$el);
          }, this));
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
      var $form = $(e.target);
      $.ajax({
        method: $form.attr('method'),
        url: $form.attr('action'),
        success: function() {
          // need to implement sensible handling for deletion
        }
      });
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
    initialize: function() { this.render(); },
    render: function() {
      var fileUploadView = new FileUploadView({ model: this.model })
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
    initialize: function() { this.render(); },
    render: function() {
      var type = typeFromMimeType(this.model.get('mimeType'));
      this.$el.empty();
      this.$el.append(this._makeBreadCrumbElement());
      var fileInfoView = new FileInfoView({
        model: this.model.get('info')
      });
      this.$el.append(this._makeDeleteElement().addClass('pull-right'));
      this.$el.append(this._makeDownloadElement());
      this.$el.append(fileInfoView.$el);
      switch (type) {
      case 'spreadsheet':
        this._loadChartElements();
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
    _loadChartElements: function() {
      var format = Modernizr.svg ? 'svg' : 'png';
      var createChartElement = _.bind(function(chart) {
        var $wrapper = $('<div/>')
          .append('<h3>'+chart.region+'</h3>')
          .append(_.template(
            '<img src="<%= chart.url %>"'+
            ' alt="Chart for <%= chart.region %>" />'
          , { model: this.model.toJSON(), chart: chart }));
        return $wrapper;
      }, this);
      var onSuccess = _.bind(function(data) {
        this.$el.append(_.map(data.charts, createChartElement));
      }, this);
      $.ajax({
        method: 'GET',
        url: '/charts/?path=' + this.model.get('path')+"&format="+format,
        dataType: 'json',
        success: onSuccess
      });
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
      var $heading = $('<h1/>')
        .css('text-align', 'center');
      var $symbol = $('<span/>')
        .css('font-size', '5em')
        .addClass("muted")
        .html('<i class="icon-remove-circle"></i>');
      var $message = $('<small/>');
      $message.text('This location no longer exists.');
      $heading.append($symbol, '<br />', $message);
      this.$el.append($heading)
    }
  });

  var LoadingView = Backbone.View.extend({
    initialize: function() { this.render(); },
    render: function() {
      var $heading = $('<h1/>')
        .css('text-align', 'center');
      var $symbol = $('<span/>')
        .css('font-size', '5em')
        .addClass("muted")
        .html(
          '<i class="icon-refresh icon-spin"></i>'
          );
      var $message = $('<small/>');
      $message.html('Please wait - data loading&hellip;');
      $heading.append($symbol, '<br />', $message);
      this.$el.append($heading)
    }
  });

  var StartView = Backbone.View.extend({
    initialize: function() { this.render(); },
    render: function() {
      var $heading = $('<h1/>')
        .css('text-align', 'center');
      var $symbol = $('<span/>')
        .css('font-size', '5em')
        .addClass("muted")
        .html(
          '<i class="icon-circle-arrow-left"></i>'
          );
      var $message = $('<small/>');
      $message.text('Select a file or folder by clicking its name.');
      $heading.append($symbol, '<br />', $message);
      this.$el.append($heading)
    }
  });

  var MainPane = Backbone.View.extend({
    tagName: "div",
    initialize: function() {
      this.innerView = new LoadingView();
      this.render();
    },
    showStart: function() {
      this.innerView = new StartView();
      this.render();
    },
    showFolder: function(folder) {
      this.innerView = new FolderView({ model: folder });
      this.render();
    },
    showFile: function(file) {
      this.innerView = new FileView({ model: file });
      this.render();
    },
    showDeleted: function(fof) {
      this.innerView = new DeletedView({ model: fof});
      this.render();
    },
    render: function() {
      this.$el.empty();
      this.$el.append(this.innerView.$el);
    }
  });
  
  return {
    MainPane: MainPane,
    FileTree: FileTree
  };
});