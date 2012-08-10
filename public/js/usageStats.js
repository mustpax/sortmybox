/*global google:true */

(function($, google, _) {
    'use strict';

    var cols = [
        { name: 'created', label: 'Date', type: 'date' },
        { name: 'users', label: 'Users', type: 'number' },
        { name: 'rules', label: 'Rules', type: 'number' },
        { name: 'fileMoves', label: 'File moves', type: 'number' },
        { name: 'uniqueFileMoveUsers', label: 'Unique File Move Users', type: 'number' }
    ];

    var colIndex = {};
    $.each(cols, function(i, v) {
        colIndex[v.name] = i;
    });


    /**
     * @param data JSON representation for usage stats gathered
     * @param (optional) selectedColumn if present, only display columns with this name
     */
    function tablify(data, selectedColumn) {
        var ret = new google.visualization.DataTable();
        $.each(cols, function(i, v) {
            if ((i === 0) ||
                (! selectedColumn) ||
                (selectedColumn === v.name)) {
                ret.addColumn(v.type, v.label);
            }
        });

        $.each(data, function(i, v) {
            var row = [];
            $.each(cols, function(j, col) {
                // Always include Label column (the first column)
                // If selected column specified, only diplay that column
                if ((j === 0) ||
                    (! selectedColumn) ||
                    (selectedColumn === col.name)) {

                    if ('date' === col.type) {
                        row.push(new Date(v[col.name]));
                    } else {
                        row.push(v[col.name]);
                    }
                }
            });
            ret.addRow(row);
        });

        return ret;
    }

    function displayCharts(data) {
        _.each(['daily', 'aggr'], function(scope) {
            _.each(_.rest(cols), function(col) { 
                var elem = $('.chart.' + scope + '.' + col.name).get(0);
                console.log(scope, col, elem);
                if(col.name === 'uniqueFileMoveUsers' && scope === 'aggr') {
                    return;
                }
                new google.visualization.LineChart(elem)
                    .draw(tablify(data[scope], col.name), { 'title': scope + ' usage stats: ' + col.label,
                                                          'width': 1000,
                                                          'height': 420 });
            });
        });
    }
    
    function init() {
        _.each(['daily', 'aggr'], function(scope) {
            _.each(_.rest(cols), function(col) {
                $('.charts').append(sortbox.template('stats-chart', {'scope' : scope, 'column' : col.name}));
            });
        });

        $.ajax({
            type: 'GET',
            url: '/admin/stats',
            data: {
                'authenticityToken' : window.csrfToken
            },
            success: displayCharts
        });
    }

    // Load the Visualization API and the chart package.
    google.load('visualization',
                '1.0',
                {
                    'packages' : ['corechart'],
                    'callback' : init
                });
})(jQuery, google, _);
