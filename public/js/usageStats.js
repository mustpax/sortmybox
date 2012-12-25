/*global google:true */

(function($, _) {
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
        return _.map(data, function(row) {
            return {x: row.created, y: row[selectedColumn]};
        });
    }

    function displayCharts(data) {
        _.each(['daily', 'aggr'], function(scope) {
            _.each(_.rest(cols), function(col) { 
                if(col.name === 'uniqueFileMoveUsers' && scope === 'aggr') {
                    return;
                }
                var chart = nv.models.lineChart()
                                     .forceY([0])
                                     .showLegend(false);

                chart.xAxis
                     .axisLabel('Date')
                     .tickFormat(function(d) {
                         return d3.time.format('%x')(new Date(d));
                     });
                
                chart.yAxis
                     .tickFormat(d3.format(',g'));

                d3.select('.chart.' + scope + '.' + col.name + ' svg')
                    .datum([{
                        values: tablify(data[scope], col.name),
                        key: scope + ' usage stats: ' + col.label
                    }])
                    .transition().duration(500)
                    .call(chart);

                nv.utils.windowResize(chart.update);
            });
        });
    }
    
    function init() {
        _.each(['daily', 'aggr'], function(scope) {
            _.each(_.rest(cols), function(col) {
                if(col.name === 'uniqueFileMoveUsers' && scope === 'aggr') {
                    return;
                }
                
                $('.charts').append(sortbox.template('stats-chart',
                                                     {'scope' : scope, 'column' : col.name}));
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
    $(init);
})(jQuery, _);
