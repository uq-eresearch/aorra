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
      'jquery.hotkeys': 'lib/jquery.hotkeys',
      'jquery.bootstrap-wysiwyg': 'lib/bootstrap-wysiwyg',
      'FileAPI': 'lib/FileAPI.min',
      'marionette': 'lib/backbone.marionette',
      'marked': '//cdnjs.cloudflare.com/ajax/libs/marked/0.2.9/marked.min',
      'moment': '//cdnjs.cloudflare.com/ajax/libs/moment.js/2.0.0/moment.min',
      'to-markdown': 'lib/to-markdown'
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
      'jquery.bootstrap-wysiwyg': {
        deps: ['jquery', 'jquery.hotkeys'],
        exports: 'jQuery'
      },
      'FileAPI': {
        exports: 'FileAPI'
      },
      'to-markdown': {
        exports: 'toMarkdown'
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