// ========= Begin code from HTML5 Boilerplate
// Modified by Mustafa Paksoy, 2012
// Copyright (c) HTML5 Boilerplate

// Permission is hereby granted, free of charge, to any person obtaining a copy of
// this software and associated documentation files (the "Software"), to deal in
// the Software without restriction, including without limitation the rights to
// use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
// of the Software, and to permit persons to whom the Software is furnished to do
// so, subject to the following conditions:

// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

// Avoid `console` errors in browsers that lack a console.
(function() {
    'use strict';

    _.templateSettings.escape = /\{\{(.+?)\}\}/g;
    window.sortbox = {};

    if (!(window.console && window.console.log)) {
        var noop = function() {};
        var methods = ['assert', 'clear', 'count', 'debug', 'dir', 'dirxml', 'error', 'exception', 'group', 'groupCollapsed', 'groupEnd', 'info', 'log', 'markTimeline', 'profile', 'profileEnd', 'markTimeline', 'table', 'time', 'timeEnd', 'timeStamp', 'trace', 'warn'];
        var length = methods.length;
        var console = window.console = {};
        while (length--) {
            console[methods[length]] = noop;
        }
    }
}());
// ========= End code from HTML5 Boilerplate

(function($) {
    'use strict';

    var templateCache = {};
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
    window.sortbox.template = template;

    $('.dropdown-toggle').dropdown();
    $('.easter').live('click', function() {
        $(this).append('<i class="icon-refresh spin"></i>');
    });
})(window.jQuery);
