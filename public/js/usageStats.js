/*global google:true */

(function($, google) {
    'use strict';

    var cols = [
        { name: 'created', label: 'Date', type: 'date' },
        { name: 'users', label: 'Users', type: 'number' },
        { name: 'rules', label: 'Rules', type: 'number' },
        { name: 'fileMoves', label: 'File moves', type: 'number' }
    ];

    var colIndex = {};
    $.each(cols, function(i, v) {
        colIndex[v.name] = i;
    });
                        
        
    function tablify(data) {
        var ret = new google.visualization.DataTable();
        $.each(cols, function(k, v) {
            ret.addColumn(v.type, v.label);
        });

        $.each(data, function(i, v) {
            var row = [];
            $.each(cols, function(j, col) {
                if ('date' === col.type) {
                    row.push(new Date(v[col.name]));
                } else {
                    row.push(v[col.name]);
                }
            });
            ret.addRow(row);
        });

        return ret;
    }

    function displayCharts(data) {
        console.log(data);

        // Set chart options
        
        new google.visualization.LineChart(document.getElementById('aggr_chart'))
            .draw(tablify(data.aggr), {'title': 'Aggregate usage stats',
                                        'width': 1000,
                                        'height': 420 });

        new google.visualization.LineChart(document.getElementById('daily_chart'))
            .draw(tablify(data.daily), {'title': 'Daily usage stats',
                                        'width': 1000,
                                        'height': 420 });
    }
    
    function init() {
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

    
    /*
        var data = google.visualization.arrayToDataTable([
          ['Date', 'Users', 'Rules', 'File moves'],
    #{list items:dailyStats, as:'stat'}
          ['${stat.created.format('MM/dd/yyyy')}', ${stat.users}, ${stat.rules}, ${stat.fileMoves}],
    #{/list}
        ]);
    
    // Set chart options
    var options = {
      'title': 'Daily usage stats',
      'width': 1000,
      'height': 420
    };
    
    // Instantiate and draw our chart, passing in some options.
    var chart = new google.visualization.LineChart(document.getElementById('daily_chart'));
    chart.draw(data, options);
    });

    */
})(jQuery, google);
