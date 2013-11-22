CKEDITOR.dialog.add('aorrafigure', function(editor) {
  return {
    title : 'Insert AORRA figure',
    minWidth : 300,
    minHeight : 300,
    contents : [{
      id : 'aorra-figure-info',
      elements : [{
        id: 'fileId',
        label: 'File name or ID',
        type: 'text',
        onLoad: function(data) {
          var dialog = data.sender;
          var editor = dialog.getParentEditor();
          var element = dialog.getContentElement('aorra-figure-info', 'fileId');
          // Fire an editor event so we can tie in our application
          editor.fire('aorrafigure_fileId:loaded', element);
        }
      },{
        id: 'imageUrl',
        label: 'Image',
        type: 'select',
        items: [],
        onLoad: function(data) {
          var dialog = data.sender;
          var editor = dialog.getParentEditor();
          var element = dialog.getContentElement(
              'aorra-figure-info', 'imageUrl');
          // Fire an editor event so we can tie in our application
          editor.fire('aorrafigure_imageUrl:loaded', element);
        },
        commit: function(widget) {
          widget.setData('imageUrl', this.getValue());
        },
        validate: CKEDITOR.dialog.validate.notEmpty("A chart must be selected")
      }]
    }]
  };
});