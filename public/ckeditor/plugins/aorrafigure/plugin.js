CKEDITOR.plugins.add('aorrafigure', {
  requires : 'widget',

  icons : 'aorrafigure',

  init : function(editor) {
    editor.widgets.add('aorrafigure', {
      dialog: 'aorrafigure',

      button : 'Insert an AORRA figure',

      data: function() {
        var fileId = this.data.fileId;
        var imageUrl = this.data.imageUrl;
        var width = this.data.width;
        var img = this.element.findOne("img");
        if (img && imageUrl) {
          img.setAttribute('style', "width: "+width);
          img.setAttribute('data-cke-saved-src', imageUrl);
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
          selector : 'figcaption'
        }
      },

      allowedContent : 'figure; figcaption; img[!src]{width}',

      requiredContent : 'figure',

      upcast : function(element) {
        return element.name == 'figure';
      }
    });
    CKEDITOR.dialog.add('aorrafigure', this.path + 'dialogs/aorrafigure.js');
  }
});
