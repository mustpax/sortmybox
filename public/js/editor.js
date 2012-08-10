(function($) {
    'use strict';

     // Extensions for EXT_EQ rule typeahead
     var ext = ['3gp',
                '7z',
                'ai',
                'avi',
                'bat',
                'bin',
                'bmp',
                'bup',
                'chm',
                'dat',
                'divx',
                'dll',
                'dmg',
                'doc',
                'exe',
                'fla',
                'flv',
                'gif',
                'gz',
                'htm',
                'html',
                'ifo',
                'iso',
                'jar',
                'jpeg',
                'jpg',
                'log',
                'mpeg',
                'mpg',
                'msi',
                'ogg',
                'pdf',
                'png',
                'pps',
                'ppt',
                'ps',
                'psd',
                'pub',
                'ram',
                'rar',
                'rm',
                'rtf',
                'swf',
                'tgz',
                'thm',
                'tif',
                'tmp',
                'torrent',
                'txt',
                'vob',
                'wav',
                'wmv',
                'xpi',
                'xls',
                'zip'
               ];
     sortbox.ext = ext.sort();

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
    }

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

    $('.pattern').live('focus', function() {
        var typeahead = $(this).data('typeahead');
        if (!typeahead) {
            $(this).typeahead({
                matcher: function(item) {
                    // match the part of the query following the last comma 
                    var query = this.query.substring(this.query.lastIndexOf(',') + 1, this.query.legnth);
                    return ~item.toLowerCase().indexOf($.trim(query).toLowerCase());
                },
                updater: function(item) {
                    var idx = this.query.lastIndexOf(',');
                    var ret = $.trim(item).toLowerCase();
                    if (!~idx) {
                        // item is the only query term
                        return ret;
                    } else {
                        // append item to the end of query terms
                        return this.query.substring(0, idx) + ", " + ret;
                    }
                }
            });
            typeahead = $(this).data('typeahead');
        }

        var ruleType = $(this).parents('tr').first()
            .find('select[name="type"] option:selected').val();
        if (ruleType === 'EXT_EQ') {
            typeahead.source = sortbox.ext;
        } else {
            typeahead.source = [];
        }
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
    }
    
    function updateActivity() {
        $('.moves').empty().append(sortbox.template('moves-loading'));

        $.ajax({
            type: 'GET',
            url: '/activity',
            data: {
                'authenticityToken' : window.csrfToken
            },
            success: function(moves) {
                var el = $('.moves');
                el.empty();
                if (_.isEmpty(moves)) {
                    el.append(sortbox.template('moves-empty'));
                } else {
                    el.append(sortbox.template('moves-list', { moves : moves }));
                }
            },
            error: function() {
                $('.moves').empty().append(sortbox.template('moves-error'));
            }
        });
    }
    $(updateActivity);

    function loading() {
        $('.rules .rule .status').addClass('icon-refresh')
            .addClass('spin')
            .removeClass('icon-ok')
            .removeClass('icon-remove');
    }

    function doneLoading() {
        $('.rules .rule .status').removeClass('icon-refresh').removeClass('spin');
    }

    function save() {
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
                                               trigger: "manual" })
                                    .popover('show');
                    setTimeout(function(){
                        msg.popover("hide");
                    }, 2000);
                    updateActivity();
                }
            },
            error: function (badRequest) {
                doneLoading();
                if (badRequest.status === 400){
                    alert("You have too many rules, delete a few and try again.");
                }
            }
        });
    }

    $(function() {
        $('.new').bind('click', function() {
            var new2 = $('.rule-template').clone();
            new2.removeClass('rule-template').addClass('rule');

            $('.new-rule-row').before(new2);
            new2.show('slow');
        });

        $('.rules .del').live('click', function() {
            $(this).parents('tr').first().remove();
        });

        $('.save').live('click', save);

        $('.alert-created').slideDown('slow');
    });
    
    $(function() {
        $('.rule .dest').explorer();
    });

    $(function() {
        $('.rules tbody').sortable({ 'cursor': 'move' });
    });
})(jQuery);
