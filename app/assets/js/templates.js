define([
        'tmpl/breadcrumbs',
        'tmpl/delete_button'
        ], function() {
  var baseContext = dust.makeBase({});
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