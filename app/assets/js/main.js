var FileAPI = {
  staticPath: '/assets/js/lib/',
  flashUrl: '/assets/flash/FileAPI.flash.swf',
  html5: true
};

requirejs.config({
    paths: {
      'backbone': '//cdnjs.cloudflare.com/ajax/libs/backbone.js/1.0.0/backbone-min',
      'diff_match_patch': 'lib/diff_match_patch',
      'glyphtree': 'lib/glyphtree',
      'hogan': 'lib/hogan',
      'jquery': '//ajax.googleapis.com/ajax/libs/jquery/1.10.1/jquery.min',
      'jquery.bootstrap': 'lib/bootstrap',
      'FileAPI': 'lib/FileAPI.min',
      'marionette': 'lib/backbone.marionette',
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
      'FileAPI': {
        exports: 'FileAPI'
      }
    },
    waitSeconds: 20
});

require(['jquery.bootstrap'], function() {
  $(function() {
    $('[rel="tooltip"]').tooltip();
    $('a[rel="back"]').click(function(event) {
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