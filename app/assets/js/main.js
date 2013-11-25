var FileAPI = {
  staticPath: '/assets/js/lib/',
  flashUrl: '/assets/flash/FileAPI.flash.swf',
  html5: true
};

requirejs.config({
    paths: {
      'backbone': '//cdnjs.cloudflare.com/ajax/libs/backbone.js/1.1.0/backbone-min',
      'ckeditor': '../ckeditor/ckeditor',
      'cryptojs-md5': 'lib/cryptojs-md5',
      'diff_match_patch': 'lib/diff_match_patch',
      'glyphtree': 'lib/glyphtree',
      'htmldiff': 'lib/htmldiff',
      'hogan': 'lib/hogan',
      'jquery': '//ajax.googleapis.com/ajax/libs/jquery/1.10.1/jquery.min',
      'jquery.bootstrap': 'lib/bootstrap',
      'jquery.ckeditor': '../ckeditor/adapters/jquery',
      'jquery.hotkeys': 'lib/jquery.hotkeys',
      'jquery.bootstrap-wysiwyg': 'lib/bootstrap-wysiwyg',
      'jquery.ui': '//code.jquery.com/ui/1.10.3/jquery-ui',
      'FileAPI': 'lib/FileAPI.min',
      'marionette': 'lib/backbone.marionette',
      'marked': '//cdnjs.cloudflare.com/ajax/libs/marked/0.2.9/marked.min',
      'moment': '//cdnjs.cloudflare.com/ajax/libs/moment.js/2.0.0/moment.min',
      'to-markdown': 'lib/to-markdown',
      'typeahead': '//cdnjs.cloudflare.com/ajax/libs/typeahead.js/0.9.3/typeahead.min',
      'unstyler': 'lib/unstyler'
    },
    shim: {
      'backbone': {
        deps: ['jquery'],
        exports: 'Backbone'
      },
      'marked': {
        exports: 'marked'
      },
      'marionette': {
        deps: ['backbone'],
        exports: 'Backbone'
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
      'jquery.bootstrap-wysiwyg': {
        deps: ['jquery', 'jquery.hotkeys'],
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