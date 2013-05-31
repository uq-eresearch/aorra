/*
Note: This is still very much a work in progress.

It it looks like a mess, that's because it probably is.
*/

requirejs.config({
    baseUrl: '/assets/js',
    paths: {
      'jquery': '//ajax.googleapis.com/ajax/libs/jquery/1.9.1/jquery.min',
      'jquery.iframe-transport': 'lib/jquery.iframe-transport',
      'jquery.fileupload': 'lib/jquery.fileupload',
      'jquery.ui.widget': 'lib/jquery.ui.widget',
      'jquery.bootstrap': '//netdna.bootstrapcdn.com/twitter-bootstrap/2.3.1/js/bootstrap.min'
    },
    shim: {
      'jquery.bootstrap': {
        deps: ['jquery'],
        exports: 'jquery'
      }
    }
});

requirejs('app');