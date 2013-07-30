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

  return {
    renderSync: function(templateName, context) {
      return templates[templateName].render(context);
    }
  };
});