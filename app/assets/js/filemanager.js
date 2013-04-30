(function() {
  
  var SPREADSHEET_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  var FileTree = Backbone.Model.extend({
    options: {
      startExpanded: true,
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
        }
      }
    }
  });
  
  function catchErrors(f) {
    return function(struct) { try { f.apply(this, arguments); } catch (e) {} };
  }
  
  var NotificationFeed = Backbone.View.extend({
    
    url: '/filestore/notifications',
    
    open: function() {
      var tree = _.bind(function() { return this.model.tree }, this);
      var trigger = _.bind(this.trigger, this);
      var eventHandlers = {
        ping:   function(message) { /*console.log(message);*/ },
        load:   function(struct) { 
          trigger("event:load", struct);
          tree().load(struct); 
        },
        create: catchErrors(function(struct) { tree().add(struct, struct.parentId) }),
        update: catchErrors(function(struct) { tree().update(struct) }),
        'delete': catchErrors(function(struct) { tree().remove(struct.id) })
      };
      
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
          eventHandlers[eventType](data);
        });
      } else {
        // EventSource
        var es = new EventSource(this.url);
        es.addEventListener('ping', function(event) {
          eventHandlers.ping(event.data);
        });
        $.each(['load', 'create', 'update', 'delete'], function(i, n) {
          es.addEventListener(n, function(event) {
            var struct = JSON.parse(event.data);
            eventHandlers[n](struct);
          });
        });
        this.es = es;
      }
    }
  });
  
  var FileTreePane = Backbone.View.extend({
    tagName: "div",
    render: function() {
      var tree = glyphtree(this.$el, this.model.options);
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
      this.model.tree = tree;
    }
  });
  
  var FileUploadView = Backbone.View.extend({
    url: '/upload',
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
    _makeDownloadElement: function() {
      var $link = $('<a class="btn"/>');
      $link.attr('href', '/download' + this.model.path);
      $link.html('<i class="icon-download-alt"></i> Download');
      return $link;
    }
  });
  
  var FolderView = FileOrFolderView.extend({
    initialize: function() { this.render(); }, 
    render: function() {
      var fileUploadView = new FileUploadView({
        model: this.model
      })
      this.$el.append(this._makeBreadCrumbElement());
      this.$el.append(fileUploadView.$el);
    } 
  });
  
  var FileView = FileOrFolderView.extend({
    initialize: function() { this.render(); }, 
    render: function() {
      this.$el.empty();
      this.$el.append(this._makeBreadCrumbElement());
      this.$el.append(this._makeDownloadElement());
      this._loadChartElements();
    },
    _loadChartElements: function() {
      if (this.model.mimeType != SPREADSHEET_MIME_TYPE) {
        return;
      }
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
    render: function() {
      this.$el.empty();
      this.$el.append(this.innerView.$el);
    }
  })
  
  $(function () {
    
    var fileTree = new FileTree();
    var fileTreePane = new FileTreePane({
      model: fileTree
    });
    var notificationFeed = new NotificationFeed({
      model: fileTree
    });
    var mainPane = new MainPane();
    fileTreePane.render();
    $('#sidebar').append(fileTreePane.$el);
    $('#main').append(mainPane.$el);
    notificationFeed.open();
    notificationFeed.on("event:load", function(struct) {
      mainPane.showFolder(_.defaults({}, _.first(struct).attributes));
    });
    fileTreePane.on("folder:select", function(folder) {
      mainPane.showFolder(folder);
    });
    fileTreePane.on("file:select", function(file) {
      mainPane.showFile(file);
    });
    
  });
})();