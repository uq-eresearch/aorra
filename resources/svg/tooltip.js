function init(evt) {
}

function showTooltip(evt, ttText) {
  var tooltip_bg = document.getElementById('tooltip_bg');
  var tooltip = document.getElementById('tooltip');
  tooltip.firstChild.data = ttText;
  tooltip.setAttributeNS(null,"visibility","visible");
  length = tooltip.getComputedTextLength();
  width = length+8
  tooltip_bg.setAttributeNS(null,"width",width);
  var x = evt.pageX-evt.clientX+25;
  var y = evt.pageY-evt.clientY+25;
  tooltip_bg.setAttributeNS(null,"x",x);
  tooltip_bg.setAttributeNS(null,"y",y-(tooltip_bg.height.baseVal.value+2));
  tooltip.setAttributeNS(null,"x",x+2);
  tooltip.setAttributeNS(null,"y",y-(tooltip_bg.height.baseVal.value+2)+12);
  tooltip_bg.setAttributeNS(null,"visibility","visibile");
}

function hideTooltip(evt) {
  var tooltip_bg = document.getElementById('tooltip_bg');
  var tooltip = document.getElementById('tooltip');
  tooltip.setAttributeNS(null,"visibility","hidden");
  tooltip_bg.setAttributeNS(null,"visibility","hidden");
}
