define([
        'tmpl/alert_box',
        'tmpl/breadcrumbs',
        'tmpl/charts',
        'tmpl/delete_button',
        'tmpl/deleted_page',
        'tmpl/file_view',
        'tmpl/filediff_view',
        'tmpl/flag_button',
        'tmpl/folder_view',
        'tmpl/loading_page',
        'tmpl/permission_row',
        'tmpl/permission_table',
        'tmpl/start_page',
        'tmpl/version_row',
        'tmpl/version_table'
        ], function() {
  'use strict';
  var baseContext = dust.makeBase({});
  var doRender = function(templateName, context, callback) {
    dust.render(
        'js/tmpl/'+templateName,
        baseContext.push(context),
        function(error, output) {
          if (error) {
            throw new Error(error);
          }
          callback(output);
        });
  };

  // Add access to Underscore.String functions as filters
  dust.filters['slug'] = _.str.slugify;
  dust.filters['human'] = _.str.humanize;

  // Add useful helpers
  dust.helpers = {
    first: function(chunk, context, bodies) {
      if (context.stack.index === 0) {
        return bodies.block(chunk, context);
      }
      return chunk;
    }
  };

  return {
    renderInto: function(container, templateName, context, callback) {
      doRender(templateName, context, function(output) {
        container.html(output);
        if (_.isFunction(callback)) {
          callback(container);
        }
      });
      return container;
    },
    // Only safe if nothing asynchronous is likely to happen.
    renderSync: function(templateName, context) {
      var content;
      doRender(templateName, context, function(output) { content = output; });
      return content;
    }
  };
});