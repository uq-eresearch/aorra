CKEDITOR.plugins.add('aorrachart', {
  requires : 'widget',

  icons : 'aorrachart',

  init : function(editor) {
    editor.widgets.add('aorrachart', {
      dialog: 'aorrachart',

      button : 'Insert an AORRA chart',

      data: function() {
        var chartUrl = this.data.chartUrl;
        var img = this.element.findOne("img");
        if (img && chartUrl) {
          img.setAttribute('src', chartUrl);
        }
      },

      template : 
        '<figure>' + 
          '<img src=""/>' + 
          '<figcaption>Write a good caption here</figcaption>' +
        '</figure>',

      editables : {
        content : {
          selector : 'figcaption',
          allowedContent : 'strong em s'
        }
      },

      allowedContent : 'figure; figcaption; img[!src]',

      requiredContent : 'figure',

      upcast : function(element) {
        return element.name == 'figure';
      }
    });
    CKEDITOR.dialog.add('aorrachart', this.path + 'dialogs/aorrachart.js');
  }
});