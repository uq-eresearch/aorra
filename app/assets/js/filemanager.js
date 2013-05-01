(function() {
  
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
      selectHandler = _.bind(function(event, node) {
        var m = _.defaults({}, node.attributes);
        // Emit select event
        if (node.type == 'folder') {
          this.trigger("folder:select", m);
        } else {
          this.trigger("file:select", m);
        }
        // Show the active node on the tree
        $('.label.label-info').removeClass('label label-info');
        $(node.element()).children('.glyphtree-node-label')
          .addClass('label label-info');
      }, this);
      tree.events.label.click = [selectHandler];
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
  
  var NotificationFeed = function(config) {
      var obj = _.extend({}, config)
      _.extend(obj, Backbone.Events);
      _.extend(obj, {
      url: '/notifications',
      open: function() {
        var trigger = _.bind(this.trigger, this);
        // Are we using a modern browser, or are we using IE?
        if (typeof(window.EventSource) == 'undefined') {
          // HTML iframe
          function connect_htmlfile(url, callback) {
            var ifrDiv = document.createElement("div");
            var iframe = document.createElement("iframe");
            ifrDiv.setAttribute("style", "display: none");
            document.body.appendChild(ifrDiv);
            ifrDiv.appendChild(iframe);
            iframe.src = url;
            iframe.onload = function() { iframe.currentWindow.location.reload(); };
            // From: http://davidwalsh.name/window-iframe
            function eventHandler(w, eventName, handler) {
              var eMethod = w.addEventListener ? "addEventListener" : "attachEvent";
              var eName = w.addEventListener ? eventName : "on" + eventName;
              return w[eMethod](eName, handler, false);
            }
            // Receive messages from iframe
            eventHandler(window, 'message', function(e) {
              var msg = JSON.parse(e.data);
              callback(msg.type, msg.data);
            });
          }
          connect_htmlfile(this.url, function(eventType, data) {
            trigger('event:'+eventType, data);
          });
        } else {
          // EventSource
          var es = new EventSource(this.url);
          es.addEventListener('ping', function(event) {
            trigger('event:ping', event.data);
          });
          _.each(['load', 'create', 'update', 'delete'], function(n) {
            es.addEventListener(n, function(event) {
              var struct = JSON.parse(event.data);
              trigger('event:'+n, struct);
            });
          });
          this.es = es;
        }
      }
    });
    
    function catchErrors(f) {
      return function(struct) { try { f.apply(this, arguments); } catch (e) {} };
    }
    
    var tree = _.bind(obj.getTree, this);
    // Event handlers
    obj.on("event:load", function(struct) { tree().load(struct); });
    obj.on("event:create",
      catchErrors(function(struct) { tree().add(struct, struct.parentId) }));
    obj.on("event:update",
      catchErrors(function(struct) { tree().update(struct) }));
    obj.on("event:delete",
      catchErrors(function(struct) { tree().remove(struct.id) }));
    
    return obj;
  };
    
  var FileUploadView = Backbone.View.extend({
    url: '/filestore',
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
        url: this.url+this.model.path,
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
    url: '/filestore',
    tagName: 'form',
    initialize: function() { this.render(); }, 
    render: function() {
      var parentFolder = this.model.path;
      var $input = $('<input type="text" name="folder"/>')
      $input.addClass('span3');
      $input.attr('placeholder', 'folder name');
      this.$el.addClass('form-inline');
      this.$el.attr('method', 'PUT');
      this.$el.append($input);
      this.$el.append(' <button class="btn" type="submit">Create</mkdir>');
      this.$el.submit(function(e) {
        var form = e.target;
        var path = parentFolder
        // Handle possibility parent has a trailing slash
        if (!_.str.endsWith(path, "/")) path += "/"
        path += $(form).find('[name="folder"]').val();
        $.ajax({
          method: $(form).attr('method'),
          url: '/filestore' + path,
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
  
  var FileOrFolderView = Backbone.View.extend({
    _makeBreadCrumbElement: function() {
      var breadcrumbs = this.model.path.split("/");
      var $breadcrumbs = $('<ul/>');
      $breadcrumbs.addClass('breadcrumb');
      $breadcrumbs.append(_.map(_.initial(breadcrumbs), function(v) {
        var $li = $('<li/>')
        $li.append($('<span>'+_.escape(v)+'</span>'))
        $li.append($('<span class="divider"> / </span>'))
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
      $form.attr('action', '/filestore' + this.model.path)
      var $btn = $('<button type="submit" class="btn btn-danger"/>');
      $btn.html('<i class="icon-remove"></i> Delete');
      $form.append($btn);
      $form.submit(function(e) {
        $.ajax({
          method: $form.attr('method'),
          url: $form.attr('action'),
          success: function() {
            // need to implement sensible handling for deletion
          }
        });
        return false;
      });
      return $form;
    },
    _makeDownloadElement: function() {
      var $link = $('<a class="btn"/>');
      $link.attr('href', '/filestore' + this.model.path);
      $link.html('<i class="icon-download-alt"></i> Download');
      return $link;
    }
  });
  
  var FolderView = FileOrFolderView.extend({
    initialize: function() { this.render(); }, 
    render: function() {
      var fileUploadView = new FileUploadView({ model: this.model })
      var mkdirView = new CreateFolderView({ model: this.model });
      this.$el.append(this._makeBreadCrumbElement());
      this.$el.append(this._makeDeleteElement().addClass('pull-right'));
      this.$el.append($('<div/>')
        .append('<h3>Upload files</h3>')
        .append(fileUploadView.$el)
        .append('<h3>Create new folder</h3>')
        .append(mkdirView.$el));
    } 
  });
  
  var FileView = FileOrFolderView.extend({
    initialize: function() { this.render(); }, 
    render: function() {
      var type = typeFromMimeType(this.model.mimeType);
      this.$el.empty();
      this.$el.append(this._makeBreadCrumbElement());
      this.$el.append(this._makeDeleteElement().addClass('pull-right'));
      this.$el.append(this._makeDownloadElement());
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
        '<img class="img-polaroid" alt="Image for <%= model.path %>"' + 
        ' src="/filestore<%= model.path %>" />', 
        this));
    },
    _loadChartElements: function() {
      var createChartElement = _.bind(function(chart) {
        var $wrapper = $('<div/>')
          .append('<h3>'+chart.region+'</h3>')
          .append(_.template(
            '<img src="/chart<%= model.path %>?region=<%=chart.region %>"'+
            ' alt="Chart for <%= chart.region %>" />'
          , { model: this.model, chart: chart }));
        return $wrapper;
      }, this);
      var onSuccess = _.bind(function(data) {
        this.$el.append(_.map(data.charts, createChartElement));
      }, this);
      $.ajax({
        method: 'GET',
        url: '/chart' + this.model.path,
        dataType: 'json',
        success: onSuccess
      });
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
      $message.text("This location has been deleted.");
      $heading.append($symbol, '<br />', $message);
      this.$el.append($heading)
    }
  });
  
  var MainPane = Backbone.View.extend({
    tagName: "div",
    showFolder: function(folder) {
      this.innerView = new FolderView({ model: folder });
      this.render();
    },
    showFile: function(file) {
      this.innerView = new FileView({ model: file });
      this.render();
    },
    showDeleted: function() {
      this.innerView = new DeletedView();
      this.render();
    },
    render: function() {
      this.$el.empty();
      this.$el.append(this.innerView.$el);
    }
  })
  
  $(function () {
    
    var fileTree = new FileTree();
    var notificationFeed = new NotificationFeed({
      getTree: function() { return fileTree.tree }
    });
    var mainPane = new MainPane();
    fileTree.render();
    $('#sidebar').append(fileTree.$el);
    $('#main').append(mainPane.$el);
    notificationFeed.open();
    notificationFeed.on("event:load", function(struct) {
      mainPane.showFolder(_.defaults({}, _.first(struct).attributes));
    });
    fileTree.on("folder:select", function(folder) {
      mainPane.showFolder(folder);
    });
    fileTree.on("file:select", function(file) {
      mainPane.showFile(file);
    });
    notificationFeed.on("event:delete", function(struct) {
      // Handle being on the deleted page already
      if (_.isUndefined(mainPane.innerView.model)) return;
      // If the current path has been deleted, then hide it.
      if (mainPane.innerView.model.path == struct.attributes.path) {
        mainPane.showDeleted();
      }
    });
    
  });
})();