CKEDITOR.dialog.add('aorrachart', function(editor) {
  return {
    title : 'Insert AORRA Chart',
    minWidth : 300,
    minHeight : 300,
    contents : [{
      id : 'aorra-chart-info',
      elements : [{
        id: 'fileId',
        label: 'File name or ID',
        type: 'text',
        onLoad: function(data) {
          var dialog = data.sender;
          var editor = dialog.getParentEditor();
          var element = dialog.getContentElement('aorra-chart-info','fileId');
          // Fire an editor event so we can tie in our application
          editor.fire('aorrachart_fileId:loaded', element);
        }
      },{
        id: 'chartUrl',
        label: 'Chart',
        type: 'select',
        items: [],
        onLoad: function(data) {
          var dialog = data.sender;
          var editor = dialog.getParentEditor();
          var element = dialog.getContentElement('aorra-chart-info','chartUrl');
          // Fire an editor event so we can tie in our application
          editor.fire('aorrachart_chartUrl:loaded', element);
        },
        commit: function(widget) {
          widget.setData('chartUrl', this.getValue());
        },
        validate: CKEDITOR.dialog.validate.notEmpty("A chart must be selected")
      }]
    }]
  };
});