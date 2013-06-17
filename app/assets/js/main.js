requirejs.config({
    paths: {
      'jquery': '//ajax.googleapis.com/ajax/libs/jquery/1.9.1/jquery.min',
      'jquery.iframe-transport': 'lib/jquery.iframe-transport',
      'jquery.fileupload': 'lib/jquery.fileupload',
      'jquery.ui.widget': 'lib/jquery.ui.widget',
      'jquery.bootstrap': '//netdna.bootstrapcdn.com/twitter-bootstrap/2.3.1/js/bootstrap.min',
      'moment': '//cdnjs.cloudflare.com/ajax/libs/moment.js/2.0.0/moment.min'
    },
    shim: {
      'jquery.bootstrap': {
        deps: ['jquery'],
        exports: 'jquery'
      }
    }
});

requirejs(['app']);