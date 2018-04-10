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

    function sum(a, b) {
        return a + b;
    }
    
    function avg(list){
        if (! (_.isArray(list) && list.length)) {
            throw 'Empty or non-array input.';
        }
        
        if (list.length === 1) {
            return list[0];
        }
        
        return _.reduce(list, sum) / list.length;
    }

    /**
     * @param values a list of data points, each data point has a x and y value.
     *               y must be numeric. x must be an integer epoch-millis date
     *
     * @return list of data points of a movign average
     */
    function movingAvg(values, slideSize) {
        var sorted = _.sortBy(values, function(d) { return d.x; }),
            ret = [],
            sliding = [];

        $.each(sorted, function() {
            sliding.push(this.y);
            while (sliding.length > slideSize) {
                sliding.shift();
            }
            ret.push({y: avg(sliding), x: this.x});
        });

        return ret;
    }

    function displayCharts(data) {
        _.each(['daily', 'aggr'], function(scope) {
            _.each(_.rest(cols), function(col) { 
                if(col.name === 'uniqueFileMoveUsers' && scope === 'aggr') {
                    return;
                }
                var chart = nv.models.lineChart()
                                     .forceY([0]);

                chart.xAxis
                     .axisLabel('Date')
                     .tickFormat(function(d) {
                         return d3.time.format('%x')(new Date(d));
                     });
                
                chart.yAxis
                     .tickFormat(d3.format(',g'));

                var values = tablify(data[scope], col.name);
                d3.select('.chart.' + scope + '.' + col.name + ' svg')
                    .datum([{
                        values: values,
                        key: 'Daily: ' + scope + ' usage stats: ' + col.label
                    },
                    {
                        values: movingAvg(values, 7),
                        key: 'Weekly mov avg: ' + scope + ' usage stats: ' + col.label
                    },
                    {
                        values: movingAvg(values, 30),
                        key: '30-day mov avg: ' + scope + ' usage stats: ' + col.label
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
