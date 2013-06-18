define([
        'tmpl/alert_box',
        'tmpl/breadcrumbs',
        'tmpl/charts',
        'tmpl/delete_button',
        'tmpl/version_table',
        'tmpl/version_row'
        ], function() {
  'use strict';
  var baseContext = dust.makeBase({});

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
      dust.render(
          'js/tmpl/'+templateName,
          baseContext.push(context),
          function(error, output) {
            if (error) {
              throw new Error(error);
            }
            container.html(output);
            if (_.isFunction(callback)) {
              callback(container);
            }
          });
      return container;
    }
  };
});