var FileAPI = {
  staticPath: '/assets/js/lib/',
  flashUrl: '/assets/flash/FileAPI.flash.swf',
  html5: true
};

requirejs.config({
    paths: {
      'backbone.projections': '//cdnjs.cloudflare.com/ajax/libs/backbone.projections/1.0.0/backbone.projections.min',
      'ckeditor': '../ckeditor/ckeditor',
      'cryptojs-md5': 'lib/cryptojs-md5',
      'diff_match_patch': 'lib/diff_match_patch',
      'glyphtree': 'lib/glyphtree',
      'htmldiff': 'lib/htmldiff',
      'hogan': 'lib/hogan',
      'jquery.bootstrap': 'lib/bootstrap',
      'jquery.ckeditor': '../ckeditor/adapters/jquery',
      'jquery.hotkeys': 'lib/jquery.hotkeys',
      'FileAPI': 'lib/FileAPI.min',
      'marionette': 'lib/backbone.marionette',
      'marked': '//cdnjs.cloudflare.com/ajax/libs/marked/0.2.9/marked.min',
      'moment': '//cdnjs.cloudflare.com/ajax/libs/moment.js/2.0.0/moment.min',
      'typeahead': '//cdnjs.cloudflare.com/ajax/libs/typeahead.js/0.9.3/typeahead.min',
      'underscore.string': '//cdnjs.cloudflare.com/ajax/libs/underscore.string/2.3.0/underscore.string.min',
      'unstyler': 'lib/unstyler'
    },
    shim: {
      'backbone.projections': {
        deps: ['backbone']
      },
      'marked': {
        exports: 'marked'
      },
      'jquery.ckeditor': {
        deps: ['jquery', 'ckeditor'],
        exports: 'jQuery'
      },
      'cryptojs-md5': {
        exports: 'CryptoJS'
      },
      'diff_match_patch': {
        exports: 'diff_match_patch'
      },
      'glyphtree': {
        deps: ['jquery'],
        exports: 'glyphtree'
      },
      'jquery.bootstrap': {
        deps: ['jquery'],
        exports: 'jQuery'
      },
      'jquery.hotkeys': {
        deps: ['jquery'],
        exports: 'jQuery'
      },
      'jquery.ui': {
        deps: ['jquery'],
        exports: 'jQuery'
      },
      'FileAPI': {
        exports: 'FileAPI'
      },
      'marionette': {
        deps: ['backbone'],
        exports: 'Backbone.Marionette'
      },
      'typeahead': {
        deps: ['jquery'],
        exports: 'jQuery'
      },
      'underscore.string': {
        deps: ['underscore'],
        exports: '_'
      },
      'unstyler': {
        exports: 'unstyle'
      }
    },
    waitSeconds: 20
});

define('q', ['webjars!q.js'], function(Q) {
  return Q;
})

define('jquery', ['webjars!jquery.js'], function() {
  return jQuery;
});

define('underscore', ['webjars!underscore.js'], function() {
  return _;
});

define('backbone', ['webjars!backbone.js'], function() {
  return Backbone;
});

define('spin', ['webjars!spin.js'], function(Spin) {
  return Spin;
});

require(['jquery.bootstrap'], function() {
  $(function() {
    $('[rel="tooltip"]').tooltip();
    $('#nav-back button').click(function(event) {
      event.preventDefault();
      if (window.history.previous) {
        window.history.back();
      } else {
        window.location.pathname = "/";
        window.location.hash = "#";
      }
    })
  });
});