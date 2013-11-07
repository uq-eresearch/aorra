/*jslint nomen: true, white: true, vars: true, eqeq: true, todo: true, unparam: true */
/*global _: false, define: false */
define(['jquery', 'hogan'], function($, hogan) {
  'use strict';

  var templates = {};
  // Load templates
  $(function() {
    $('script[id^="tmpl-"]').each(function(i, e) {
      var templateName = $(e).attr('id').replace(/^tmpl-/,"");
      var tmpl = e.innerHTML;
      templates[templateName] = hogan.compile(tmpl);
    });
  });

  return {
    render: function(templateName, context) {
      return templates[templateName].render(context, templates);
    }
  };
});