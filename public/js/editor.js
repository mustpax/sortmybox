(function(window, $, undefined) {
    $(document).ready(function() {
      $('.new').bind('click', function() {
        var tbody = $('.rules');
        var new2 = $('.rule-template').clone();
        new2.removeClass('rule-template').addClass('rule');
        
        tbody.append(new2);
        new2.show('slow');
      });

      $('.rules .del').live('click', function() {
        $(this).parents('tr').first().hide('fast', function() { $(this).remove(); });
      });

      /**
       * Convert the current rule set into a JSON-serializable object.
       */
      var serialize = function() {
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
            console.log('cell', cell);
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

      $('.save').live('click', function() {
        var rules = serialize();
        console.log(rules);
        loading();
        $.ajax({
            type: 'POST',
            url: '/rules',
            data: {
                'rules': JSON.stringify(rules),
                'authenticityToken' : window.csrfToken
            },
            success: function(data) {
                doneLoading();
                console.log('success');
                $.each(data, function(i, v) {
                    addErrors(i, v);
                });
            }
        });
      });
    });

    /**
     * Get list of directories inside given directory from
     * the server.
     * @param optional path of directory to get a listing of
     * @param cb function is called with the output
     */
    function getDirs(path, cb) {
        $.ajax({
            type: 'GET',
            url: '/dirs',
            data: {
                'path': path || '/',
                'authenticityToken' : window.csrfToken
            },
            success: function(data) {
                if (cb) {
                    cb(data);
                }
            }
        });
        
    };
    
    function displayDirs(dirs, cell) {
        var exp = $(cell).find('.exp');
        if (! exp.length) {
            exp = $('<ul class="exp nav nav-list">');
            $(cell).append(exp);
        }
        
        exp.empty();
        dirs.sort();
        $.each(dirs, function(i, v) {
            var li = $('<li>');
            li.append($('<a href="#">').text(v));
            exp.append(li);
        });
    };
    
    $('.rule .dest').live('focus', function() {
        var cell = $(this).parent('td');
        var path = $(this).val() || '/';
        $(this).parent('td').addClass('exp-active');
        getDirs(path, function(dirs) {
            displayDirs(dirs, cell);
        });
    });

    $('.rule .dest').live('blur', function() {
        $(this).parent('td').removeClass('exp-active');
    });
})(window, jQuery);