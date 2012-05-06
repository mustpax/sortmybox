(function(window, $, undefined) {
    'use strict';
    /**
     * Convert the current rule set into a JSON-serializable object.
     */
    function serialize() {
        var ret = [];
        $('.rules tr.rule')
            .each(function() {
                var pat = $(this).find('.pattern')[0].value;
                var dest = $(this).find('.dest')[0].value;
                var cur = {
                    type:    $(this).find('select')[0].value,
                pattern: pat,
                dest:    dest
                };
                ret.push(cur);
            });
        return ret;
    };

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
            return template('exp-select', { path: basename(path),
				                            dataPath: path });
        };

        function upLink() {
            var upPath = '/'  + pathList.slice(0, pathList.length - 1).join('/');
            return template('exp-uplink', {dataPath : upPath});
        };
        
        // Add up link if not top dir
        if (! _.isEmpty(pathList)) {
            exp.append(select());
            exp.append(upLink());
        }
        
        function loading() {
            return template('exp-loading');
        }
        
        if (isLoading) {
            exp.append(loading());
        } else {
            if (_.isUndefined(dirs)){
                exp.append($("<li><a><em>Can't list folder contents</em></a></li>"));
            } else {
	            dirs.sort();
	            if (_.isEmpty(dirs)) {
	                exp.append($("<li><a><em>No more folders</em></a></li>"));
	            } else {
		            $.each(dirs, function(i, v) {
		                exp.append(template('exp-folder', {
		                    path: basename(v),
		                    dataPath: v
		                }));
		            });
	            }
            }
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
    
    $('.rule .dest').live('focus change', dirUpdater);
    $('.rule .dest').live('keyup', _.debounce(dirUpdater, 1500));

    $('.rule .dest').live('focus', function() {
        var cell = $(this).parents('td').first();
        cell.find('.exp-status')
            .addClass('icon-folder-open')
            .removeClass('icon-folder-close');
        cell.addClass('exp-active');
    });
    
    $('.exp a[href]').live('click', function(e) {
        var path = $(this).attr('data-path');
        if (path) {
	        var input = $(this).parents('td').first().find('input');
	        input.val(path);
	        input.trigger('change');
	        input.focus();
        }
        return false;
    });

    function clearIfUnfocused(elem) {
        if (! $(elem).attr('data-focus')) {
	        $(elem).parents('td').first().removeClass('exp-active');
        }
	}

    $('.rule .dest').live('blur', function() {
        $(this).parents('td')
               .first()
               .find('.exp-status')
               .removeClass('icon-folder-open')
               .addClass('icon-folder-close');
        _.delay(clearIfUnfocused, 250, this);
    });
    
    $('input[type="text"]').live('focus', function() {
        $(this).attr('data-focus', 1);
    });

    $('input[type="text"]').live('blur', function() {
        $(this).attr('data-focus', '');
    });

    /**
     * Display the given errors to the given rule.
     * @param i the rank of the rule to add errors to
     * @param errors list of errors
     */
    function addErrors(i, errors) {
        var rule = $('.rules tr.rule').eq(i);
        console.log('errors', i, rule, errors);

        // Clear old errors
        rule.find('td').removeClass('error');
        rule.find('.msg.help-inline').remove();

        $.each(errors, function() {
            var cell = rule.find('input.' + this.field).parents('td').first();
            cell.addClass('error');
            cell.append($('<span class="msg help-inline"></span>').text(this.msg));
        });

        if (errors.length === 0) {
            rule.find('.status').addClass('icon-ok');
        } else {
            rule.find('.status').addClass('icon-remove');
        }
    };

    function loading() {
        $('.rules .rule .status').addClass('icon-refresh')
            .addClass('spin')
            .removeClass('icon-ok')
            .removeClass('icon-remove');
    };

    function doneLoading() {
        $('.rules .rule .status').removeClass('icon-refresh').removeClass('spin');
    };


    $(document).ready(function() {
        $('.new').bind('click', function() {
            var new2 = $('.rule-template').clone();
            new2.removeClass('rule-template').addClass('rule');

            $('.new-rule-row').before(new2);
            new2.show('slow');
        });

        $('.rules .del').live('click', function() {
            $(this).parents('tr').first().hide('fast', function() { $(this).remove(); });
        });

        $('.save').live('click', function() {
            var rules = serialize();
            console.log('serialed rules', rules);
            loading();
            $.ajax({
                type: 'POST',
                url: '/rules',
                data: {
                    'rules': JSON.stringify(rules),
	                'authenticityToken' : window.csrfToken
                },
                success: function(data) {
                             var hasErrors = false,
                                 msg       = null;
                             doneLoading();
                             console.log('save success');
                             $.each(data, function(i, v) {
                                 addErrors(i, v);
                                 hasErrors = hasErrors || !! v.length;
                             });
                             if (! hasErrors) {
	                             msg = $('.save').popover({ title:   "Success!", 
                             						        content: "Your rules will run every 15 minutes.",
                             						        trigger: "manual"})
                             				     .popover('show');
	                             setTimeout(function(){
	                                msg.popover("hide");
	                             }, 2000);
                             }
                         },
                error: function (badRequest) {
					doneLoading();
					if (badRequest.status===400){
						alert("You have too many rules defined, please delete a few and try again.");
					}
            	}
            });
        });

        $('.alert-created').slideDown('slow');
    });

})(window, jQuery);
