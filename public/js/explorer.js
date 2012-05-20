!function($) {
    'use strict';

    var dirCache = {};
    var curReq = null; 
    /**
     * Get list of directories inside given directory from
     * the server.
     * @param optional path of directory to get a listing of
     * @param cb function is called with the output
     */
    function getDirs(path, cb) {
        if (curReq) {
            // There is already a request for this
            // path in flight ignore this one
            if (curReq.path === path) {
                console.log('already requesting', path);
                return;
            }

            // There is a request for another path abort it
            console.log('request obsolete, aborting', curReq.path);
            curReq.req.abort();
            curReq = null;
        }

        if (dirCache[path]) {
            console.log('cache hit', path);
            cb(dirCache[path]);
            return;
        }

        console.log('cache miss, request from server', path);
        var req = $.ajax({
            type: 'GET',
            url: '/dirs',
            data: {
                'path': path || '/',
                'authenticityToken' : window.csrfToken
            },
            success: function(dirs) {
                curReq = null;
                dirCache[path] = dirs;
                cb(dirs);
            },
            error: function() {
                cb();
            }
        });
        
        curReq = { path : path,
                   req  : req };
    };

    function basename(path) {
        return _.chain(path.split('/')).filter(function(x) {
		                                    return !!x;
		                                })
		                               .last()
		                               .value();
    };

    function displayDirs(path, dirs, cell, isLoading) {
        var pathList = _.filter(path.split('/'), function(x) { return !!x; });
        
        var exp = $(cell).find('.exp');
        if (! exp.length) {
            exp = $('<ul class="exp nav nav-list">');
            $(cell).find('.dest-wrap').append(exp);
        }
        
        exp.empty();
        
        function select() {
            return sortbox.template('exp-select', { path: basename(path),
                                                     dataPath: path });
        };

        function upLink() {
            var upPath = '/'  + pathList.slice(0, pathList.length - 1).join('/');
            return sortbox.template('exp-uplink', {dataPath : upPath});
        };
        
        // Add up link if not top dir
        if (! _.isEmpty(pathList)) {
            exp.append(select());
            exp.append(upLink());
        }
        
        function loading() {
            return sortbox.template('exp-loading');
        }
        
        if (isLoading) {
            exp.append(loading());
        } else {
            exp.append(sortbox.template('exp-folders', { dirs: dirs,
                                                         basename : basename }));
        }
    };
    
    function dirUpdater() {
        var cell = $(this).parents('td').first();
        var path = $(this).val() || '/';
        displayDirs(path, null, cell, true);
        getDirs(path, function(dirs) {
            displayDirs(path, dirs, cell);
        });
    };
    
    
    $.fn.explorer = function() {
        var $this = $(this);
	    $this.live('focus change', dirUpdater);
	    $this.live('keyup', _.debounce(dirUpdater, 1500));
    }
}(window.jQuery);
