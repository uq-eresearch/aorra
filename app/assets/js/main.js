requirejs.config({
    paths: {
      'backbone': '//cdnjs.cloudflare.com/ajax/libs/backbone.js/1.0.0/backbone-min',
      'diff_match_patch': 'lib/diff_match_patch',
      'glyphtree': 'lib/glyphtree',
      'hogan': '//cdnjs.cloudflare.com/ajax/libs/hogan.js/2.0.0/hogan.min',
      'jquery': '//ajax.googleapis.com/ajax/libs/jquery/1.10.1/jquery.min',
      'jquery.iframe-transport': 'lib/jquery.iframe-transport',
      'jquery.fileupload': 'lib/jquery.fileupload',
      'jquery.ui.widget': 'lib/jquery.ui.widget',
      'jquery.bootstrap': 'lib/bootstrap',
      'marionette': '//cdnjs.cloudflare.com/ajax/libs/backbone.marionette/1.0.4-bundled/backbone.marionette.min',
      'moment': '//cdnjs.cloudflare.com/ajax/libs/moment.js/2.0.0/moment.min'
    },
    shim: {
      'backbone': {
        deps: ['jquery'],
        exports: 'Backbone'
      },
      'marionette': {
        deps: ['backbone'],
        exports: 'Backbone'
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
      'hogan': {
        exports: 'Hogan'
      }
    },
    waitSeconds: 20
});

require(['jquery.bootstrap'], function() {
  $('[rel="tooltip"]').tooltip();
  $('a[rel="back"]').click(function(event) {
    event.preventDefault();
    if (window.history.previous) {
      window.history.back();
    } else {
      window.location.pathname = "/";
      window.location.hash = "#";
    }
  });
});