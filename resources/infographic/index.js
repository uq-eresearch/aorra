// Begin wrapper
(function() {
    
var configLoaded = $.getJSON('data.json').promise();
var app = Sammy('#main', function() {});

var documentLoaded = (function() {
  var d = $.Deferred();
  $(document).ready(function() {
    d.resolve();
  });
  return d;
})();

var indicatorImage = (function() {
  function img(imgName) {
    return $('<img />').attr('src', 'images/'+imgName+'.png')[0].outerHTML;
  }
  return {
    'grazing': img('grazing'),
    'sugarcane': img('cane'),
    'horticulture': img('banana'),
    'groundcover': img('groundcover'),
    'nitrogen': img('beaker'),
    'sediment': img('beaker'),
    'pesticides': img('beaker')
  };
})();

var template = (function IIFE() {
  var templates = {};
  $(function whenDocumentLoads() {
    $('script[id^="tmpl-"]').each(function(i, e) {
      var templateName = $(e).attr('id').replace(/^tmpl-/,"");
      var tmpl = e.innerHTML;
      templates[templateName] = Hogan.compile(tmpl);
    });
  });
  return function template(templateName, context) {
    if (templates[templateName]) {
      return templates[templateName].render(context, templates)
    }
    throw new Error('Template "'+templateName+'" not found.');
  };
})();

var getFill = (function IIFE() {
  var fills;
  function getFillsFromCSS() {
    function getFillFromCSS(conditionClasses) {
      var $testEl = $('<div/>').addClass('condition '+conditionClasses);
      $testEl.appendTo('body');
      var bgc = $testEl.css('background-color');
      $testEl.remove();
      return bgc;
    };
    return {
      'Very good': {
        'normal': getFillFromCSS('very-good'),
        'active': getFillFromCSS('very-good active')
      },
      'Good': {
        'normal': getFillFromCSS('good'),
        'active': getFillFromCSS('good active'),
      },
      'Moderate': {
        'normal': getFillFromCSS('moderate'),
        'active': getFillFromCSS('moderate active'),
      },
      'Poor': {
        'normal': getFillFromCSS('poor'),
        'active': getFillFromCSS('poor active'),
      },
      'Very poor': {
        'normal': getFillFromCSS('very-poor'),
        'active': getFillFromCSS('very-poor active')
      }
    };
  }
  return function getFillImpl(v) {
    if (!fills) {
      fills = getFillsFromCSS();
    }
    if (fills[v]) {
      return fills[v];
    } else {
      return { normal: '#e5e5e5', active: '#e5e5e5' };
    }
  }
})();

function getMarineStyle(data) {
  return Object.keys(data).reduce(function(lr, region) {
    var values = data[region];
    return lr.concat(Object.keys(values).reduce(function(li, indicator) {
      var value = values[indicator].qualitative;
      return li.concat(
        '.marine-chart.'+region+' #'+indicator+" { fill: "+getFill(value).normal+"; pointer-events: all; }",
        '.marine-chart.'+region+' #'+indicator+':hover, .marine-chart.'+region+' #'+indicator+".active { fill: "+getFill(value).active+"; cursor: pointer; }"
      );
    }, []));
  }, []).join("\n");
}

var otherInfographicYears = [];

var deferredsBeforeStart = {
  'regions':   $.Deferred(),
  'marineSVG': $.Deferred(),
  'yearLinks': $.Deferred(),
};
var promisesBeforeStart = Object.keys(deferredsBeforeStart).map(function (k) {
  return deferredsBeforeStart[k].promise();
});


var routesCreated = $.when(configLoaded).done(function(config) {
  var reportFinalYear = config['reportYears'].match(/\d{4}$/g)[0];
    
  Sammy('#main', function() {
      
    // Default lander
    this.get('#/', function() {
      // this context is a Sammy.EventContext
      this.$element() // $('#main')
          .html(template('home', {
        baseYear: config['baseYear'],
        reportYears: config['reportYears'],
        reportFinalYear: reportFinalYear,
        otherYears: otherInfographicYears,
        isMostRecent: otherInfographicYears.every(function(v) {
          return v.year < config['reportYears'];
        }),
        fullReportUrl: config['fullReportCardURL']
      }));
      this.trigger('region:show', 'gbr');
    });
    
    this.get('#/management', function() {
      this.$element()
          .html(template('management-select'));
      this.trigger('region:show', 'gbr');
      this.trigger('management:show');
    });
    
    this.get('#/catchment', function() {
      this.$element()
          .html(template('catchment-select'));
      this.trigger('region:show', 'gbr');
      this.trigger('catchment:show');
    });
    
    this.get('#/marine', function() {
      var context = this;
      this.$element().html(template('marine-select'));
      this.trigger('region:show', 'gbr');
      this.trigger('marine:show');
    });
    
    // Same handling for management and catchment indicators, just different
    // data sources and URLs.
    [
      ['management', config.data.management], 
      ['catchment', config.data.catchment],
      ['marine', config.data.marine]
    ].forEach(function(args) {
      var indicatorType = args[0];
      var data = args[1];
      this.get('#/'+indicatorType+'/:indicator', function() {
        var indicator = this.params['indicator'];
        var indicatorData = data['gbr'][indicator];
        var caption = template(indicator+"-caption", {
          baseYear: config['baseYear'],
          reportYears: config['reportYears'],
          reportFinalYear: reportFinalYear,
          target: indicatorData['target']
        });
        this.$element().html(template(indicatorType+'-indicator-info', {
          name: config.names.indicators[indicator],
          caption: caption
        }));
        this.trigger('region:show', 'gbr');
        this.trigger('indicator:show', this.params['indicator']);
      });
    }.bind(this));
    
    this.get('#/region/:region', function() {
      var regionId = this.params['region'];
      this.$element().html(template('region-info', {
        id: regionId,
        name: config.names.regions[regionId],
        caption: config.captions.marine[regionId]
      }));
      this.trigger('region:show', regionId);
      this.trigger('marine:show');
      this.trigger('catchment:show');
      this.trigger('management:show');
    });
    
  });
});

$.when(configLoaded, routesCreated, documentLoaded).done(function(args) {
  var config = args[0];
  
  function loadSvg(containerSelector, url, align) {
    $(containerSelector).svg();
    var svg = $(containerSelector).svg('get');
    var d = $.Deferred();
    svg.load(url, { 
      onLoad: function() {
        svg.configure({
          'height': '100%',
          'width': '100%',
          'preserveAspectRatio': 'x'+align+'Y'+align+' meet'
        }, false);
        d.resolve(svg);
      }
    });
    return d.promise();
  }
  
  var setFocus = (function() {
    var focused = null;
    return function (element) {
      if (focused) {
        $(focused).attr('class', 
          $(focused).attr('class').replace(' active', ''));
      }
      focused = element;
      $(focused).attr('class', $(focused).attr('class')+' active');
    };
  })();
  
  $('body').append(
    $('<style type="text/css"/>').html(getMarineStyle(config.data.marine)));
    
  (function initmap() {
    // set up the map
    var map = new L.Map('map');
  
    // create the tile layer with correct attribution
    var osmUrl='http://otile1.mqcdn.com/tiles/1.0.0/osm/{z}/{x}/{y}.png';
    var osmAttrib='Tiles Courtesy of <a href="http://www.mapquest.com/" target="_blank">MapQuest</a> <img src="http://developer.mapquest.com/content/osm/mq_logo.png">';
    var osm = new L.TileLayer(osmUrl, {minZoom: 2, maxZoom: 12, attribution: osmAttrib});		
  
    map.setView(new L.LatLng(-18.83, 147.68), 6);
    map.addLayer(osm);
    
    $.getJSON("./reef-regions.geojson").done(function(data) {
      var reefGeo = L.geoJson(data, {
        style: function (feature) {
          return {
            stroke: true,
            color: '#000000',
            weight: 0.5,
            fill: true,
            fillOpacity: 0.05,
            fillColor: '#0000ff'
          };
        }
      });
      reefGeo.getLayers().forEach(function(region) {
        var regionName = region.feature.properties.Region.toLowerCase().replace(' ', '-');
        region.onClickHandler = function() {
          function endsWith(str, suffix) {
            return str.indexOf(suffix, str.length - suffix.length) !== -1;
          };
          if(endsWith(app.getLocation(), regionName)) {
            app.setLocation('#/');
          } else {
            app.setLocation('#/region/'+regionName);
          }
        };
        region.onMouseOverHandler = function() {
          map.fire('highlight-on-'+regionName);
        };
        region.onMouseOutHandler = function() {
          map.fire('highlight-off-'+regionName);
        };
        region.highlightOn = function() {
          region.setStyle({weight: 2});
        };
        region.highlightOff = function() {
          reefGeo.resetStyle(region);
        };
        region.on({
          click : region.onClickHandler,
          mouseover : region.onMouseOverHandler,
          mouseout : region.onMouseOutHandler
        });
        var mapEventHandler = {};
        mapEventHandler['highlight-on-'+regionName] = region.highlightOn;
        mapEventHandler['highlight-off-'+regionName] = region.highlightOff;
        map.on(mapEventHandler);
      });
      reefGeo.addTo(map);
    });
    // Detect low-res device and use simpler regions
    var regionsUrl = 768 < window.screen.width ?
      "./regions.geojson" :
      "./regions-simplified.geojson";
    $.get(regionsUrl, function(data) {
      var regionsGeo = L.geoJson(data, {
        style: function (feature) {
          return {
            weight: 1 
          };
        }
      });
      // Fast hash-based lookups
      var regionLookup = (function regionLookupInit() {
        function getRegionId(region) {
          return region.feature.properties.OBJECTID;
        }
        var regionById = regionsGeo.getLayers().reduce(function(h, region) {
          h[getRegionId(region)] = region;
          return h;
        }, {});
        var regionNameToId = {};
        var regionIdToName = {};
        regionsGeo.getLayers().forEach(function(region) {
          var displayName = region.feature.properties.Region;
          var regionName = displayName.toLowerCase().replace(' ', '-');
          regionNameToId[regionName] = getRegionId(region);
          regionIdToName[getRegionId(region)] = regionName;
        });
        return {
          nameToRegion: function nameToRegion(regionName) {
            return regionById[regionNameToId[regionName]];
          },
          regionToName: function regionToName(region) {
            return regionIdToName[getRegionId(region)];
          }
        }
      })();
      var zoomed = regionsGeo;
      regionsGeo.getLayers().forEach(function(region) {
        var regionName = regionLookup.regionToName(region);
        region.setStyle({
          className: regionName
        });
        region.onClickHandler = function() {
          if (zoomed == region) {
            app.setLocation('#/');
          } else {
            app.setLocation('#/region/'+regionName);
          }
        };
        region.onMouseOverHandler = function() {
          map.fire('highlight-on-'+regionName);
        };
        region.onMouseOutHandler = function() {
          map.fire('highlight-off-'+regionName);
        };
        region.highlightOn = function() {
          region.bringToFront();
          region.setStyle({weight: 4});
        };
        region.highlightOff = function() {
          regionsGeo.resetStyle(region);
        };
        region.on({
          click : region.onClickHandler,
          mouseover : region.onMouseOverHandler,
          mouseout : region.onMouseOutHandler
        });
        var mapEventHandler = {};
        mapEventHandler['highlight-on-'+regionName] = region.highlightOn;
        mapEventHandler['highlight-off-'+regionName] = region.highlightOff;
        map.on(mapEventHandler);
      });
      Sammy('#main', function() {
        this.bind('region:show', function(evt, regionName) {
          if (regionName == 'gbr') {
            zoomed = regionsGeo;
          } else {
            zoomed = regionLookup.nameToRegion(regionName);
          }
          map.fitBounds(zoomed.getBounds());
        })
      }); 
      regionsGeo.addTo(map);
      regionsGeo.getLayers().forEach(function(region) {
        var displayName = region.feature.properties.Region;
        var labelDir = displayName == 'Burdekin' ? 'left' : 'right';
        var label = new L.Label({
          clickable: true,
          direction: labelDir,
          noHide: true
        });
        region.setQuantitativeValue = function setContent(newContent) {
          label.setContent( newContent ?
            displayName + "<br />" + newContent : displayName );
        };
        region.setQuantitativeValue(null);
        label.setLatLng(region.getBounds().getCenter());
        label.on({
          click : region.onClickHandler,
          mouseover : region.onMouseOverHandler,
          mouseout : region.onMouseOutHandler
        });
        map.showLabel(label);
      });
      map.fitBounds(regionsGeo.getBounds());
      $(window).resize(function() {
        map.fitBounds(zoomed.getBounds());
      });

      function clearRegionFills() {
        $('.leaflet-label')
          .removeAttr('title')
          .removeClass('condition na very-good good moderate poor very-poor');
        Object.keys(config.data.marine).forEach(function(regionName) {
          var region = regionLookup.nameToRegion(regionName);
          if (region != null) {
            region.setQuantitativeValue(null);
          }
        });
      }
      
      function setRegionFills(indicator) {
        var data;
        if (config.data.marine.gbr[indicator]) {
          data = config.data.marine;
        } else if (config.data.management.gbr[indicator]) {
          data = config.data.management;
        } else if (config.data.catchment.gbr[indicator]) {
          data = config.data.catchment;
        } else {
          clearRegionFills();
          return;
        }
        Object.keys(data).forEach(function(regionName) {
          var region = regionLookup.nameToRegion(regionName);
          if (region != null) {
            var displayName = region.feature.properties.Region;
            var condition = data[regionName][indicator].qualitative;
            var value = data[regionName][indicator].quantitative;
            if (condition == null) {
              $('.leaflet-label:contains("'+displayName+'")')
                .addClass('condition na');
              region.setQuantitativeValue(null);
            } else {
              var conditionClass = condition.toLowerCase().replace(' ','-');
              $('.leaflet-label:contains("'+displayName+'")')
                .attr('title', condition)
                .addClass('condition '+conditionClass);
              region.setQuantitativeValue(value);
            }
          }
        });
      }
  
      Sammy('#main', function() {
        this.bind('indicator:show', function(evt, id) {
          setRegionFills(id);
        });
        this.bind('region:show', function(evt, id) {
          if (id == 'gbr') {
            clearRegionFills();
          }
        });
      });
      
      Sammy('#main', function() {
        function addProgressButtons(data, regionName, e) {
          var region = data[regionName];
          Object.keys(region).forEach(function(indicator) {
            Sammy('#main', function() {
              var condition = region[indicator].qualitative || 'NA';
              var value = region[indicator].quantitative || '';
              var target = region[indicator].target || '';
              var name = indicator.substring(0,1).toUpperCase() + indicator.substring(1);
              var $button = $(template('progress-tile', {
                conditionId: condition.toLowerCase().replace(' ', '-'),
                conditionName: condition,
                indicatorId: indicator,
                indicatorName: name,
                indicatorImage: indicatorImage[indicator],
                target: target,
                value: value
              }));
              $button.appendTo(e);
            });
          });
        }
        this.bind('management:show', function(evt) {
          this.$element().find('.management-data').each(function(i, e) {
            var regionName = $(e).data('region');
            addProgressButtons(config.data.management, regionName, e);
          });
        });
        this.bind('catchment:show', function(evt) {
          this.$element().find('.catchment-data').each(function(i, e) {
            var regionName = $(e).data('region');
            addProgressButtons(config.data.catchment, regionName, e);
          });
        });
        ['catchment', 'management'].forEach(function(arg) {
          this.bind(arg + ':show', function() {
            this.$element().find('button[data-indicator]').click(function(evt) {
              var $button = $(evt.delegateTarget);
              this.redirect('#', arg, $button.attr('data-indicator'));
            }.bind(this));
          });
        }.bind(this));
      });
      
      loadSvg('#marine-chart', 'marine.svg', 'Mid').done(function(marine) {
        $('#marine-chart').hide();
        Sammy('#main', function() {
          this.bind('marine:show', function() {
            var $chart = this.$element().find('.marine-chart');
            $chart.html($('#marine-chart').html());
            $chart.find('#indicators path').click(function(evt) {
              var indicator = evt.delegateTarget.id;
              this.redirect('#', 'marine', indicator);
            }.bind(this));
          });
        });
        // Marine SVG has been loaded
        deferredsBeforeStart['marineSVG'].resolve();
      });
      // Regions have been loaded
      deferredsBeforeStart['regions'].resolve();
    }, 'json');

    var imageBounds = [[-24.444389342999955,153.2289409640001], [-9.999868392999815,142.6685123440003]];
    L.imageOverlay('reef.svg', imageBounds, {opacity: 1}).addTo(map);
  })();
});

$.when(configLoaded).done(function(config) {
  $.ajax({
    url: config.otherReportCardsJSONP,
    dataType: 'jsonp',
    jsonpCallback: 'reportCardYearsCallback',
    success: function(data) {
      otherInfographicYears = data.infographics.filter(function(v) {
        return v.year != config.reportYears;
      });
    }
  }).always(function() {
    // Year links have been loaded
    deferredsBeforeStart['yearLinks'].resolve();
  });
});

// Run app when everything has been loaded
$.when.apply($, promisesBeforeStart).done(function() {
  app.run('#/');
});

// End wrapper
})();