//@ sourceMappingURL=unstyler.map
/*!
# Copyright (c) 2013 The University of Queensland
# (UQ ITEE e-Research Group)
#
# MIT Licensed
*/


(function() {
  "use strict";
  (function(exportFunc) {
    var convertLists, foldLeft, locateLists, operations, removeOp, replaceOp, takeWhile, transformOp, unstyle, unstylerModule;
    foldLeft = function(iterable, zero, f) {
      var fPair, foldLeftArray, k, v;
      foldLeftArray = function(iterable, zero, f) {
        var memo, n, _i, _len;
        memo = zero;
        for (_i = 0, _len = iterable.length; _i < _len; _i++) {
          n = iterable[_i];
          memo = f(memo, n);
        }
        return memo;
      };
      if (iterable instanceof Array) {
        return foldLeftArray(iterable, zero, f);
      } else {
        fPair = function(zero, pair) {
          return f(zero, pair[1], pair[0]);
        };
        return foldLeftArray((function() {
          var _results;
          _results = [];
          for (k in iterable) {
            v = iterable[k];
            _results.push([k, v]);
          }
          return _results;
        })(), zero, fPair);
      }
    };
    takeWhile = function(iterable, f) {
      var i, l, _i, _len;
      l = [];
      for (_i = 0, _len = iterable.length; _i < _len; _i++) {
        i = iterable[_i];
        if (f(i)) {
          l.push(i);
        } else {
          break;
        }
      }
      return l;
    };
    replaceOp = function(regex, replacement) {
      return function(text) {
        return text.replace(new RegExp(regex.source, 'mg'), replacement);
      };
    };
    removeOp = function(regex) {
      return function(text) {
        return text.replace(new RegExp(regex.source, 'mg'), '');
      };
    };
    transformOp = function(regex, transformFunc) {
      return function(text) {
        var foundAt, i, m, matchStr, operations, overwriteOp, untilIndex;
        overwriteOp = function(i, j, str) {
          var f;
          f = function(t) {
            return t.slice(0, +(i - 1) + 1 || 9e9) + str + t.slice(j);
          };
          f.toString = function() {
            return "Overwrite @ " + i + "-" + j + ": " + str;
          };
          return f;
        };
        i = 0;
        operations = [];
        while ((m = text.slice(i).match(regex))) {
          matchStr = m[0];
          foundAt = i + m.index;
          untilIndex = foundAt + matchStr.length;
          operations.push(overwriteOp(foundAt, untilIndex, transformFunc(m)));
          i = untilIndex;
        }
        return foldLeft(operations.reverse(), text, function(text, f) {
          return f(text);
        });
      };
    };
    locateLists = function(text) {
      var depth, foundAt, i, indexes, listType, m, matchStr, msoList, re, rootId, untilIndex;
      re = /<p[^>]+style='[^']*mso-list\:[\s\S]+?<\/p>/m;
      indexes = [];
      i = 0;
      while ((m = text.slice(i).match(re))) {
        matchStr = m[0];
        foundAt = i + m.index;
        untilIndex = foundAt + matchStr.length;
        msoList = matchStr.match(/l(\d+) level(\d+)/);
        rootId = parseInt(msoList[1]);
        depth = parseInt(msoList[2]);
        listType = function(str) {
          if (/^<p[^>]*>(?:\s|&nbsp;)*[a-z0-9]+\.(&nbsp;\s*)+/.test(str)) {
            return 'ol';
          } else {
            return 'ul';
          }
        };
        indexes.push({
          start: foundAt,
          end: untilIndex,
          type: listType(text.slice(foundAt, untilIndex)),
          root: rootId,
          depth: depth
        });
        i = untilIndex;
      }
      return indexes;
    };
    convertLists = function(text) {
      var closeListOp, indexes, insertOp, insertions, openListOp, partitionDepths, partitionRoots;
      insertOp = function(i, str) {
        var f;
        f = function(t) {
          return t.slice(0, +(i - 1) + 1 || 9e9) + str + t.slice(i);
        };
        f.toString = function() {
          return "Insert @ " + i + ": " + str;
        };
        return f;
      };
      openListOp = function(indexes) {
        return insertOp(indexes[0].start, "<" + indexes[0].type + ">");
      };
      closeListOp = function(indexes) {
        return insertOp(indexes[indexes.length - 1].end, "</" + indexes[0].type + ">");
      };
      partitionDepths = function(l) {
        var differentDepth;
        if (l.length === 0) {
          return [];
        } else {
          differentDepth = takeWhile(l.slice(1), function(i) {
            return l[0].depth !== i.depth;
          });
          if (differentDepth.length === 0) {
            return (new Array()).concat([insertOp(l[0].start, "<li>"), insertOp(l[0].end, "</li>")], partitionDepths(l.slice(1)));
          } else {
            return (new Array()).concat([insertOp(l[0].start, "<li>"), openListOp(differentDepth)], partitionDepths(differentDepth), [closeListOp(differentDepth), insertOp(differentDepth[differentDepth.length - 1].end, "</li>")], partitionDepths(l.slice(differentDepth.length + 1)));
          }
        }
      };
      partitionRoots = function(l) {
        var sameRoot;
        if (l.length === 0) {
          return [];
        } else {
          sameRoot = takeWhile(l, function(i) {
            return l[0].root === i.root;
          });
          return (new Array()).concat([openListOp(sameRoot)], partitionDepths(sameRoot), [closeListOp(sameRoot)], partitionRoots(l.slice(sameRoot.length)));
        }
      };
      indexes = locateLists(text);
      if (indexes.length === 0) {
        return text;
      } else {
        insertions = partitionRoots(indexes);
        return foldLeft(insertions.reverse(), text, function(text, f) {
          return f(text);
        });
      }
    };
    operations = [
      removeOp(/(?:<\/html>)[\s\S]+/), removeOp(/<!--(\w|\W)+?-->/), removeOp(/<title>(\w|\W)+?<\/title>/), transformOp(/(<\/?)(P|BR|B|I|STRONG|UL|OL|LI)([^>]*?>)/, function(match) {
        return match[1] + match[2].toLowerCase() + match[3];
      }), removeOp(/<(meta|link|\/?o:|\/?style|\/?div|\/?st\d|\/?head|\/?html|body|\/?body|\/?span|!\[)[^>]*?>/), removeOp(/(<[^>]+>)+&nbsp;(<\/\w+>)+/), removeOp(/\s+v:\w+=""[^""]+""/), removeOp(/(\n\r){2,}/), replaceOp(/Ã¢â‚¬â€œ/, "&mdash;"), convertLists, replaceOp(/(?:<p[^>]*>(?:(?:&nbsp;)+\s*)?[a-z0-9]+\.)(?:&nbsp;\s*)+([^<]+)<\/p>/, "$1"), replaceOp(/(?:<p[^>]*>[·o�])(?:&nbsp;\s*)+([^<]+)<\/p>/, "$1"), removeOp(new RegExp('\\s?class=(?:\'[^\']*\'|"[^"]*"|\\w+)')), removeOp(new RegExp('\\s+style=(?:\'[^\']*\'|"[^"]*")')), replaceOp(new RegExp('(<[ou]l)\\s+type=(?:\'[^\']+\'|"[^""]+")'), "$1"), replaceOp(/<b>([^<]*)<\/b>/, "<strong>$1</strong>"), replaceOp(/<i>([^<]*)<\/i>/, "<em>$1</em>"), replaceOp(/<br>(?:<\/br>)?/, "<br/>"), replaceOp(/(<li>[^<]*?)(?=\s*<li)/, "$1</li>"), replaceOp(/(\r\n)/, '\n')
    ];
    unstyle = function(html) {
      return foldLeft(operations, html, function(text, f) {
        return f(text);
      });
    };
    unstylerModule = unstyle;
    unstylerModule.foldLeft = foldLeft;
    unstylerModule.takeWhile = takeWhile;
    return exportFunc(unstylerModule);
  })((typeof exports !== "undefined" && exports !== null ? exports : false) ? function(m) {
    return module.exports = m;
  } : (typeof window !== "undefined" && window !== null ? window : false) ? function(m) {
    return window.unstyle = m;
  } : function(m) {
    return m;
  });

}).call(this);
