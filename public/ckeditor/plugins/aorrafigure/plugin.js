CKEDITOR.plugins.add('aorrafigure', {
  requires : 'widget',

  icons : 'aorrafigure',

  init : function(editor) {
    editor.widgets.add('aorrafigure', {
      dialog: 'aorrafigure',

      button : 'Insert an AORRA figure',

      data: function() {
        var imageUrl = this.data.imageUrl;
        var img = this.element.findOne("img");
        if (img && imageUrl) {
          img.setAttribute('src', imageUrl);
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
    CKEDITOR.dialog.add('aorrafigure', this.path + 'dialogs/aorrafigure.js');
  }
});