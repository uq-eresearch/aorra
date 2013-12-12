var FileAPI = {
  staticPath: '/assets/js/lib/',
  flashUrl: '/assets/flash/FileAPI.flash.swf',
  html5: true
};

requirejs.config({
    paths: {
      'backbone': '//cdnjs.cloudflare.com/ajax/libs/backbone.js/1.1.0/backbone-min',
      'backbone.localstorage': '//cdnjs.cloudflare.com/ajax/libs/backbone-localstorage.js/1.1.0/backbone.localStorage-min',
      'ckeditor': '../ckeditor/ckeditor',
      'cryptojs-md5': 'lib/cryptojs-md5',
      'diff_match_patch': 'lib/diff_match_patch',
      'glyphtree': 'lib/glyphtree',
      'htmldiff': 'lib/htmldiff',
      'hogan': 'lib/hogan',
      'jquery': '//ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min',
      'jquery.bootstrap': 'lib/bootstrap',
      'jquery.ckeditor': '../ckeditor/adapters/jquery',
      'jquery.hotkeys': 'lib/jquery.hotkeys',
      'FileAPI': 'lib/FileAPI.min',
      'marionette': 'lib/backbone.marionette',
      'marked': '//cdnjs.cloudflare.com/ajax/libs/marked/0.2.9/marked.min',
      'moment': '//cdnjs.cloudflare.com/ajax/libs/moment.js/2.0.0/moment.min',
      'q': '//cdnjs.cloudflare.com/ajax/libs/q.js/0.9.6/q.min',
      'typeahead': '//cdnjs.cloudflare.com/ajax/libs/typeahead.js/0.9.3/typeahead.min',
      'underscore': '//cdnjs.cloudflare.com/ajax/libs/underscore.js/1.5.2/underscore-min',
      'underscore.string': '//cdnjs.cloudflare.com/ajax/libs/underscore.string/2.3.0/underscore.string.min',
      'unstyler': 'lib/unstyler'
    },
    shim: {
      'backbone': {
        deps: ['jquery', 'underscore'],
        exports: 'Backbone'
      },
      'backbone.localstorage': {
        deps: ['backbone'],
        exports: 'Backbone.LocalStorage'
      },
      'marked': {
        exports: 'marked'
      },
      'marionette': {
        deps: ['backbone'],
        exports: 'Marionette'
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
      'to-markdown': {
        exports: 'toMarkdown'
      },
      'typeahead': {
        deps: ['jquery'],
        exports: 'jQuery'
      },
      'underscore': {
        exports: '_'
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