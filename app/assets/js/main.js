requirejs.config({
    paths: {
      'backbone': '//cdnjs.cloudflare.com/ajax/libs/backbone.js/1.0.0/backbone-min',
      'diff_match_patch': 'lib/diff_match_patch',
      'glyphtree': 'lib/glyphtree',
      'jquery': '//ajax.googleapis.com/ajax/libs/jquery/1.9.1/jquery.min',
      'jquery.iframe-transport': 'lib/jquery.iframe-transport',
      'jquery.fileupload': 'lib/jquery.fileupload',
      'jquery.ui.widget': 'lib/jquery.ui.widget',
      'jquery.bootstrap': '//netdna.bootstrapcdn.com/twitter-bootstrap/2.3.1/js/bootstrap.min',
      'marionette': '//cdnjs.cloudflare.com/ajax/libs/backbone.marionette/1.0.1-bundled/backbone.marionette.min',
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
      }
    }
});

require(['jquery.bootstrap'], function() {
  $('[rel="tooltip"]').tooltip();
  $('a[rel="back"]').click(function(event) {
    event.preventDefault();
    if (window.history.previous) {
      window.history.back();
    } else {
      window.location.pathname = "@routes.Application.index()";
      window.location.hash = "#";
    }
  });
});