$(function() {
  if ($.cookie('cfoptout')) {
    return;
  }
  var $modal = $(
    '<div class="modal hide fade">                                                '+
    '  <div class="modal-header">                                                 '+
    '    <button type="button" class="close" data-dismiss="modal">&times;</button>'+
    '    <h4>The web browser you are using is at least 4 years old!</h4>          '+
    '  </div>                                                                     '+
    '  <div class="modal-body">                                                   '+
    '    <p>                                                                      '+
    '    There may be quite good reasons for this, but this website requires more '+
    '    recent technology.                                                       '+
    '    </p>                                                                     '+
    '    <p>                                                                      '+
    '    We recommend:                                                            '+
    '    </p>                                                                     '+
    '    <ul>                                                                     '+
    '      <li>using a more modern web browser if you have one available, or</li> '+
    '      <li>installing the Google Chrome Frame plugin.</li>                    '+
    '    </ul>                                                                    '+
    '    <p>                                                                      '+
    '    The Google Chrome Frame plugin allows this website to use more modern    '+
    '    web technologies without affecting your ability to use less modern       '+
    '    websites.                                                                '+
    '    </p>                                                                     '+
    '  </div>                                                                     '+
    '  <div class="modal-footer">                                                 '+
    '    <button class="btn pull-left" data-dismiss="modal" aria-hidden="true">   '+
    '      I\'ll take my chances&hellip;                                          '+
    '    </button>                                                                '+
    '    <a class="btn btn-success"                                               '+
    '      href="http://www.google.com/chromeframe/" target="_blank">             '+
    '      <i class="icon-ok"></i>                                                '+
    '      Install Google Chrome Frame                                            '+
    '    </a>                                                                     '+
    '  </div>                                                                     '+
    '</div>                                                                       ');
  CFInstall.check({
    destination: window.location.href,
    preventPrompt: true,
    onmissing: function() {
      $('body').append($modal);
      $modal.modal();
      $modal.on('hide', function() {
        // Create session cookie to opt out
        $.cookie('cfoptout', '1');
      })
    }
  });
});