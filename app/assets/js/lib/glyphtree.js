//@ sourceMappingURL=glyphtree.map
/*!
#     Copyright (c) 2013 The University of Queensland
#     MIT Licence - see COPYING for details.
*/


(function() {
  var bindToWindow, defaults, glyphtree, toggleExpansionHandler,
    __indexOf = [].indexOf || function(item) { for (var i = 0, l = this.length; i < l; i++) { if (i in this && this[i] === item) return i; } return -1; };

  toggleExpansionHandler = function(event, node) {
    if (!node.isLeaf()) {
      if (node.isExpanded()) {
        return node.collapse();
      } else {
        return node.expand();
      }
    }
  };

  defaults = function() {
    return {
      classPrefix: "glyphtree-",
      startExpanded: false,
      events: {
        icon: {
          click: [toggleExpansionHandler]
        },
        label: {
          click: [toggleExpansionHandler]
        }
      },
      types: {
        "default": {
          icon: {
            "default": {
              content: "\u25b6"
            },
            leaf: {
              content: "\u2022"
            },
            expanded: {
              content: "\u25bc"
            }
          }
        }
      },
      typeResolver: function(struct) {
        return struct.type;
      },
      nodeComparator: function(nodeA, nodeB) {
        switch (false) {
          case !(nodeA.name < nodeB.name):
            return -1;
          case !(nodeA.name > nodeB.name):
            return 1;
          default:
            return 0;
        }
      }
    };
  };

  glyphtree = function(element, options) {
    var $, GlyphTree, _ref;

    $ = (_ref = this.jQuery) != null ? _ref : this.$;
    if (typeof $ === 'undefined') {
      throw new Error('GlyphTree requires jQuery (or a compatible clone).');
    }
    GlyphTree = (function() {
      var ClassResolver, Node, NodeContainer;

      function GlyphTree(element, _options) {
        var randomId;

        this.element = element;
        this._options = _options;
        randomId = Math.floor(Math.random() * Math.pow(2, 32)).toString(16);
        this.idClass = this._options.classPrefix + 'id' + randomId;
        $(this.element).addClass(this.idClass);
        this.classResolver = new ClassResolver(this._options.classPrefix);
        this.compareNodes = this._options.nodeComparator;
        this.resolveType = this._options.typeResolver;
        this._styleElement = this.setupStyle();
        this.events = this._options.events;
        this.startExpanded = this._options.startExpanded;
        this._setRootContainer(new NodeContainer([], this, null));
      }

      GlyphTree.prototype.setupStyle = function() {
        var $style;

        $style = $('<style type="text/css">' + this.getStyle() + '</style>');
        $('body').append($style);
        return $style;
      };

      GlyphTree.prototype.getStyle = function() {
        var boilerplate, cr, k, styleExpr, typeStyle, v,
          _this = this;

        cr = this.classResolver;
        styleExpr = function(property, value) {
          if (value.match(/^(#|rgb|\d)/)) {
            return "" + property + ": " + value + ";";
          } else {
            return "" + property + ": \"" + value + "\";";
          }
        };
        typeStyle = function(name, config) {
          var k, sSel, sel, state, tSel, v;

          sSel = function(state) {
            if (state === 'default') {
              return '';
            } else {
              return '.' + cr.state(state);
            }
          };
          tSel = function(type) {
            if (type === 'default') {
              return '';
            } else {
              return '.' + cr.type(type);
            }
          };
          sel = function(state, type) {
            return sSel(state) + tSel(type);
          };
          return ((function() {
            var _i, _len, _ref1, _results;

            _ref1 = ['default', 'leaf', 'expanded'];
            _results = [];
            for (_i = 0, _len = _ref1.length; _i < _len; _i++) {
              state = _ref1[_i];
              if (config.icon[state]) {
                _results.push(("." + this.idClass + " ul li." + (cr.node()) + (sel(state, name)) + " > span." + (cr.node('icon')) + ":after {") + ((function() {
                  var _ref2, _results1;

                  _ref2 = config.icon[state];
                  _results1 = [];
                  for (k in _ref2) {
                    v = _ref2[k];
                    _results1.push(styleExpr(k, v));
                  }
                  return _results1;
                })()).join(" ") + "}");
              }
            }
            return _results;
          }).call(_this)).join("\n") + "\n" + ("." + _this.idClass + " ul li." + (cr.node()) + (sel('default', name)) + " > ul." + (cr.tree()) + " {\n  display: none;\n}\n." + _this.idClass + " ul li." + (cr.node()) + (sel('expanded', name)) + " > ul." + (cr.tree()) + " {\n  display: block;\n}");
        };
        boilerplate = "." + this.idClass + " ul {\n  list-style-type: none;\n}\n." + this.idClass + " ul li." + (cr.node()) + " {\n  cursor: pointer;\n}\n." + this.idClass + " ul li." + (cr.node()) + " > span." + (cr.node('icon')) + ":after {\n  width: 1em;\n  text-align: center;\n  display: inline-block;\n  padding-right: 1ex;\n  speak: none;\n}";
        return boilerplate + "\n" + ((function() {
          var _ref1, _results;

          _ref1 = this._options.types;
          _results = [];
          for (k in _ref1) {
            v = _ref1[k];
            _results.push(typeStyle(k, v));
          }
          return _results;
        }).call(this)).join("\n");
      };

      GlyphTree.prototype.load = function(structure) {
        var root;

        this._setRootContainer(new NodeContainer((function() {
          var _i, _len, _results;

          _results = [];
          for (_i = 0, _len = structure.length; _i < _len; _i++) {
            root = structure[_i];
            _results.push(new Node(root, this));
          }
          return _results;
        }).call(this), this, null));
        return this;
      };

      GlyphTree.prototype.add = function(structure, parentId) {
        var parent;

        if (parentId != null) {
          parent = this.find(parentId);
          if (parent == null) {
            throw new Error('Cannot add node - unknown parent node ID');
          }
          parent.addChild(new Node(structure, this));
        } else {
          this.rootNodes.add(new Node(structure, this));
        }
        return this;
      };

      GlyphTree.prototype.update = function(structure) {
        var node, nodeId;

        nodeId = structure.id;
        if (nodeId == null) {
          throw new Error('Cannot update without provided ID');
        }
        node = this.find(nodeId);
        if (node) {
          node.update(structure);
        }
        return this;
      };

      GlyphTree.prototype.remove = function(nodeId) {
        var node;

        node = this.find(nodeId);
        if (node != null) {
          node.remove();
        }
        return this;
      };

      GlyphTree.prototype.expandAll = function() {
        return this.walk(function(node) {
          return node.expand();
        });
      };

      GlyphTree.prototype.collapseAll = function() {
        return this.walk(function(node) {
          return node.collapse();
        });
      };

      GlyphTree.prototype.find = function(id) {
        var n, _i, _len, _ref1;

        _ref1 = this.nodes();
        for (_i = 0, _len = _ref1.length; _i < _len; _i++) {
          n = _ref1[_i];
          if (n.id === id) {
            return n;
          }
        }
        return null;
      };

      GlyphTree.prototype.nodes = function() {
        var nodes;

        nodes = [];
        this.walk(function(n) {
          return nodes.push(n);
        });
        return nodes;
      };

      GlyphTree.prototype.walk = function(f) {
        if (!this.rootNodes.empty()) {
          this.rootNodes.walkNodes(f);
        }
        return this;
      };

      GlyphTree.prototype._setRootContainer = function(container) {
        this.rootNodes = container;
        $(this.element).html(container.element());
        return this;
      };

      Node = (function() {
        function Node(struct, tree) {
          var child, children, expandedClass;

          this.tree = tree;
          this._cr = this.tree.classResolver;
          this.id = struct.id;
          this.name = struct.name;
          this.type = this.tree.resolveType(struct);
          this.attributes = struct.attributes;
          children = struct.children ? (function() {
            var _i, _len, _ref1, _results;

            _ref1 = struct.children;
            _results = [];
            for (_i = 0, _len = _ref1.length; _i < _len; _i++) {
              child = _ref1[_i];
              _results.push(new Node(child, this.tree));
            }
            return _results;
          }).call(this) : [];
          this.children = new NodeContainer(children, this.tree, this);
          expandedClass = this._cr.state('expanded');
          this.isExpanded = function() {
            return this.element().hasClass(expandedClass);
          };
          this.expand = function() {
            return this.element().addClass(expandedClass);
          };
          this.collapse = function() {
            return this.element().removeClass(expandedClass);
          };
        }

        Node.prototype.parent = function() {
          return this.container.parentNode;
        };

        Node.prototype.addChild = function(node) {
          var wasLeaf;

          wasLeaf = this.isLeaf();
          this.children.add(node);
          if (wasLeaf) {
            return this.element().append(this.children.element());
          }
        };

        Node.prototype.update = function(struct) {
          var formerType;

          this.id = struct.id;
          this.name = struct.name;
          formerType = this.type;
          this.type = this.tree.resolveType(struct);
          this.attributes = struct.attributes;
          this.container.refresh();
          this._rebuildElement(formerType);
          return this;
        };

        Node.prototype.remove = function() {
          return this.container.remove(this);
        };

        Node.prototype.element = function() {
          return this._element || (this._element = this._buildElement());
        };

        Node.prototype.isLeaf = function() {
          return this.children.empty();
        };

        Node.prototype._buildElement = function() {
          var $icon, $label, $li;

          $li = $('<li/>').addClass(this._cr.node()).addClass(this._cr.type(this.type));
          $icon = $('<span/>').addClass(this._cr.node('icon'));
          $label = $('<span/>').addClass(this._cr.node('label')).attr('tabindex', -1).text(this.name);
          $li.append($icon);
          $li.append($label);
          if (this.isLeaf()) {
            $li.addClass(this._cr.state('leaf'));
          } else {
            $li.append(this.children.element());
          }
          if (this.tree.startExpanded) {
            $li.addClass(this._cr.state('expanded'));
          }
          this._attachEvents($icon, 'icon');
          this._attachEvents($label, 'label');
          return $li;
        };

        Node.prototype._rebuildElement = function(formerType) {
          var $label;

          if (this._element != null) {
            if (formerType !== this.type) {
              this._element.removeClass(this._cr.type(formerType));
              this._element.addClass(this._cr.type(this.type));
            }
            $label = this._element.children('.' + this._cr.node('label'));
            return $label.text(this.name);
          } else {
            return this.element();
          }
        };

        Node.prototype._attachEvents = function($element, eventMapKey) {
          var watchedEvents,
            _this = this;

          watchedEvents = ['click', 'keydown', 'keypress', 'keyup', 'mouseenter', 'mouseleave'].join(' ');
          return $element.on(watchedEvents, function(e) {
            var handler, _i, _len, _ref1, _results;

            if (_this.tree.events[eventMapKey][e.type] != null) {
              _ref1 = _this.tree.events[eventMapKey][e.type];
              _results = [];
              for (_i = 0, _len = _ref1.length; _i < _len; _i++) {
                handler = _ref1[_i];
                _results.push(handler(e, _this));
              }
              return _results;
            }
          });
        };

        return Node;

      })();

      NodeContainer = (function() {
        function NodeContainer(nodes, tree, parentNode) {
          var node, _i, _len, _ref1;

          this.nodes = nodes;
          this.parentNode = parentNode;
          this._cr = tree.classResolver;
          this._compareNodes = function(a, b) {
            return tree.compareNodes(a, b);
          };
          _ref1 = this.nodes;
          for (_i = 0, _len = _ref1.length; _i < _len; _i++) {
            node = _ref1[_i];
            node.container = this;
          }
          this._sort();
        }

        NodeContainer.prototype.empty = function() {
          return this.nodes.length === 0;
        };

        NodeContainer.prototype.add = function(node) {
          this.nodes.push(node);
          node.container = this;
          return this.refresh();
        };

        NodeContainer.prototype.remove = function(node) {
          var n;

          if (__indexOf.call(this.nodes, node) >= 0) {
            node.element().remove();
            return this.nodes = (function() {
              var _i, _len, _ref1, _results;

              _ref1 = this.nodes;
              _results = [];
              for (_i = 0, _len = _ref1.length; _i < _len; _i++) {
                n = _ref1[_i];
                if (n !== node) {
                  _results.push(n);
                }
              }
              return _results;
            }).call(this);
          } else {
            throw new Error('Node not in this container');
          }
        };

        NodeContainer.prototype.refresh = function() {
          this._sort();
          return this._rebuildElement();
        };

        NodeContainer.prototype.element = function() {
          return this._element || (this._element = this._buildElement());
        };

        NodeContainer.prototype._buildElement = function() {
          var $list, node;

          $list = $("<ul/>");
          $list.addClass(this._cr.tree());
          $list.append((function() {
            var _i, _len, _ref1, _results;

            _ref1 = this.nodes;
            _results = [];
            for (_i = 0, _len = _ref1.length; _i < _len; _i++) {
              node = _ref1[_i];
              _results.push(node.element());
            }
            return _results;
          }).call(this));
          return $list;
        };

        NodeContainer.prototype._rebuildElement = function() {
          var node, _i, _len, _ref1;

          if (this._element != null) {
            _ref1 = this.nodes;
            for (_i = 0, _len = _ref1.length; _i < _len; _i++) {
              node = _ref1[_i];
              node.element().detach();
            }
            return this._element.append((function() {
              var _j, _len1, _ref2, _results;

              _ref2 = this.nodes;
              _results = [];
              for (_j = 0, _len1 = _ref2.length; _j < _len1; _j++) {
                node = _ref2[_j];
                _results.push(node.element());
              }
              return _results;
            }).call(this));
          } else {
            return this.element();
          }
        };

        NodeContainer.prototype._sort = function() {
          return this.nodes.sort(this._compareNodes);
        };

        NodeContainer.prototype.walkNodes = function(f) {
          var node, result, _i, _len, _ref1;

          _ref1 = this.nodes;
          for (_i = 0, _len = _ref1.length; _i < _len; _i++) {
            node = _ref1[_i];
            result = f(node);
            if (!node.children.empty()) {
              node.children.walkNodes(f);
            }
          }
          return void 0;
        };

        return NodeContainer;

      })();

      ClassResolver = (function() {
        function ClassResolver(prefix) {
          this.prefix = prefix;
        }

        ClassResolver.prototype.node = function(attr) {
          if (attr) {
            return this.node() + "-" + attr;
          } else {
            return this.prefix + "node";
          }
        };

        ClassResolver.prototype.tree = function() {
          return this.prefix + "tree";
        };

        ClassResolver.prototype.type = function(type) {
          return this.prefix + 'type-' + (type != null ? type : 'default');
        };

        ClassResolver.prototype.state = function(state) {
          return this.prefix + state;
        };

        return ClassResolver;

      })();

      return GlyphTree;

    })();
    return new GlyphTree(element, options);
  };

  bindToWindow = function(window) {
    var deepMerge, options;

    options = defaults();
    deepMerge = function(obj, defaults) {
      var h, k, v;

      h = {};
      for (k in defaults) {
        v = defaults[k];
        h[k] = v;
      }
      for (k in obj) {
        v = obj[k];
        if (typeof h[k] === 'object') {
          h[k] = deepMerge(v, h[k]);
        } else {
          h[k] = v;
        }
      }
      return h;
    };
    window.glyphtree = function(element, opts) {
      return glyphtree.call(window, element, deepMerge(opts != null ? opts : {}, options));
    };
    window.glyphtree.options = options;
    return window.glyphtree;
  };

  if (typeof exports !== "undefined" && exports !== null) {
    exports.create = function(windowToInit) {
      return bindToWindow(windowToInit);
    };
  } else {
    bindToWindow(this);
  }

}).call(this);
