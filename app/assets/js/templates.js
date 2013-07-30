define(['jquery', 'hogan'], function($, hogan) {
  'use strict';

  var templates = {};
  // Load templates
  $(function() {
    $('script[id^="tmpl-"]').each(function(i, e) {
      var templateName = $(e).attr('id').replace(/^tmpl-/,"");
      var tmpl = e.innerHTML;
      templates[templateName] = hogan.compile(tmpl);
    })
  });

  var doRender = function(templateName, context) {
    return templates[templateName].render(context);
  };

  return {
    renderInto: function(container, templateName, context, callback) {
      container.html(doRender(templateName, context));
      if (_.isFunction(callback)) {
        callback(container);
      }
      return container;
    },
    // Only safe if nothing asynchronous is likely to happen.
    renderSync: function(templateName, context) {
      return doRender(templateName, context);
    }
  };
});