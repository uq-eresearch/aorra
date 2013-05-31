define([
        'models', 
        'tmpl/breadcrumbs',
        'jquery.bootstrap',
        'jquery.iframe-transport',
        'jquery.fileupload'
        ], function(models) {
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
    serializeData: function() {
      return _(this.model.toJSON()).extend({ url: this.model.url() });
    },
    template: function(serialized_model) {
      return _.template(
          '<td><%= v.timestamp %></td>'+
          '<td><a href="mailto:<%= v.author.email %>"><%= v.author.name %></a></td>'+
          '<td><a class="btn btn-small" href="<%= v.url %>">'+
            '<i class="icon-download-alt"></i> Download'+
          '</a></td>',
          serialized_model,
          { variable: 'v'});
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
      var $table = $('<table><thead><tr></tr></thead></table>');
      $table.addClass('table');
      $table.find('thead tr').append(_.map(
        ['Uploaded At', 'Author', ''],
        function(v) {
          return $('<th/>').text(v);
        }
      ));
      $table.append(this.vlv.$el);
      this.$el.append('<h3>Versions</h3>');
      this.$el.append($table);
    }
  });

  var FileOrFolderView = Backbone.View.extend({
    _makeBackElement: function() {
      var $wrapper = $('<div class="span12 visible-phone" />');
      var $back = $('<button class="btn"/>');
      $back.addClass('visible-phone');
      $back.html('<i class="icon-reply"></i> Back');
      $back.click(function() { window.history.back(); });
      $wrapper.append($back);
      return $wrapper;
    },
    _makeBreadCrumbElement: function() {
      var breadcrumbs = this.model.get('path').split("/");
      var $breadcrumbs = $('<ul/>');
      $breadcrumbs.addClass('breadcrumb');
      $breadcrumbs.append(_.map(_.initial(breadcrumbs), function(v) {
        var $li = $('<li/>');
        $li.append($('<span>'+_.escape(v)+'</span>'));
        $li.append($('<span class="divider"> / </span>'));
        return $li;
      }));
      var $active = $('<li class="active"/>')
      $active.append($('<span>'+_.escape(_(breadcrumbs).last())+'</span>'));
      $breadcrumbs.append($active);
      return $breadcrumbs;
    },
    _makeDeleteElement: function() {
      var $form = $('<form>');
      $form.attr('method', 'DELETE');
      $form.attr('action', this.model.url());
      var $btn = $('<button class="btn btn-danger"/>');
      $btn.html('<i class="icon-remove"></i> Delete');
      var $modal = $(
        '<div class="modal hide fade" tabindex="-1" role="dialog" aria-hidden="true">' +
        '  <div class="modal-header">' +
        '    <button type="button" class="close" data-dismiss="modal" aria-hidden="true">×</button>' +
        '    <h3>Are you sure? There is no undo!</h3>' +
        '  </div>' +
        '  <div class="modal-body">' +
        '    <p>Please confirm you realy want to delete this item.</p>' +
        '  </div>' +
        '  <div class="modal-footer">' +
        '    <button type="submit" class="btn btn-danger">Yes, I want it gone forever!</button>' +
        '    <button class="btn" data-dismiss="modal" aria-hidden="true">No, leave it.</button>' +
        '  </div>' +
        '</div>'
        ).modal({ show: false });
      $btn.click(function() { $modal.modal('show'); return false; });
      $form.append($btn);
      $form.append($modal);
      $form.submit(function(e) {
        $.ajax({
          method: $form.attr('method'),
          url: $form.attr('action'),
          success: function() {
            // need to implement sensible handling for deletion
          }
        });
        $modal.modal('hide');
        return false;
      });
      return $form;
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