/*jslint nomen: true, white: true, vars: true, eqeq: true, todo: true */
/*global _: false, window: false */
define(['marionette', 'q'], function(Marionette, Q) {

  var App = new Backbone.Marionette.Application();
  App.vent.on('all', _.bind(console.log, console));

  return App;
});