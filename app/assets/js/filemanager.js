(function() {

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
        if (node.type == 'folder') {
          this.trigger("folder:select", m);
          $('.label.label-success').removeClass('label label-success');
          $(node.element()).children('.glyphtree-node-label')
            .addClass('label label-success');
        } else {
          this.trigger("file:select", m);
        }
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
      this.$el.html('<input type="file" name="files[]" multiple />')
      var $input = this.$el.find('input');
      $input.fileupload({
        url: this.url+this.model.path,
        dataType: 'json',
        add: function (e, data) {
          data.submit();
        },
        done: function (e, data) {
          // Don't really need to do anything right now.
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