define(['jquery'], function($) {
  'use strict';
  var baseContext = dust.makeBase({});

  // Load templates
  $(function() {
    $('script[id^="tmpl-"]').each(function(i, e) {
      var templateName = $(e).attr('id').replace(/^tmpl-/,"");
      var tmpl = e.innerHTML;
      dust.loadSource(dust.compile(tmpl, templateName));
    })
  });

  var doRender = function(templateName, context, callback) {
    dust.render(
        templateName,
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