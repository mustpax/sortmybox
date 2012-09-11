(function($) {
    'use strict';

    var templateCache = {},
        sortbox = {};

    window.sortbox = sortbox;
    _.templateSettings.escape = /\{\{(.+?)\}\}/g;

    /**
     * Render the template with the given name into a jQuery
     * wrapped unattached DOM element.
     */
    function template(name, context) {
        var t = templateCache[name];
        if (! t) {
            var e = $('#' + name);
            if (! e.length) {
                throw 'Cannot find template with name: ' + name;
            }
            
            t = _.template(e.html());
            templateCache[name] = t;
        }

        return $(t(context));
    }
    sortbox.template = template;

    $('.dropdown-toggle').dropdown();
    $('.easter').live('click', function() {
        $(this).append('<i class="icon-refresh spin"></i>');
    });
})(window.jQuery);
