document.addEventListener('DOMContentLoaded', function() {
    //var fileInput = document.getElementById('fileInput');

    var fileInput = document.getElementById('upload-file-btn');
    fileInput.addEventListener('click', handleInputChange);
});


function local_ocr(fileInput) {

    //var input = event.target;
    var file = fileInput.files;
    $('#local').empty()
    $('#progress-bar').empty()

    sz = file.length
    total_time = 0;
    var times = []
    var filetimes = {};
    $.each(file, function(key, value) {
        var t0 = performance.now();
        Tesseract.recognize(value)
            .progress(function(message) {
                //console.log(message);
                if (message.status == "recognizing text" && message.progress === 0) {
                    var progb = '  <div class="progress" id="bar-' + key + '"><div id="value-' + key + '" class="determinate" style="width: 0%"></div></div>'
                    if ($('#bar-' + key).length == 0) {
                        $('#progress-bar').append(progb)
                    }

                } else if (message.status == "recognizing text" && message.progress === 1) {
                    $('#bar-' + key).remove()
                } else {
                    $('#value-' + key).attr("style", "width: " + (message.progress * 100) + "%");
                }

            })
            .then(function(result) {
                //console.log(result)
                var t1 = performance.now();
                total_time += (t1 - t0);
                console.log("Call to doSomething took " + (t1 - t0) + " milliseconds.")
                exec_time = t1 - t0;
                times.push(exec_time);
                var html_str = 'OCR of image ' + value.name + ' took: ' + exec_time;
                //$( '#file-content' ).prepend( $( '<div/>' ).html( html_str ) )
                filetimes[value.name] = exec_time;
                sz = sz - 1;
                if (sz === 0) {
                    html_str = '<table><thead><tr><th data-field="id">Stat</th><th data-field="price">Time</th></tr></thead><tbody>';

                    html_str += '<tr><td>Total execution time</td><td>' + round(math.sum(times) / 1000) + ' Seconds</td></tr>';
                    html_str += '<tr><td>Maximum execution time</td><td>' + round(math.max(times) / 1000) + ' Seconds</td></tr>';
                    html_str += '<tr><td>Minimum execution time</td><td>' + round(math.min(times) / 1000) + ' Seconds</td></tr>';
                    html_str += '<tr><td>Average execution time</td><td>' + round(math.mean(times) / 1000) + ' Seconds</td></tr>';
                    html_str += '<tr><td>Standard deviation</td><td>' + round(math.std(times) / 1000) + ' Seconds</td></tr>';


                    $('#progress-bar').empty();

                    html_str += '</tbody></table>';
                    $('#local').prepend($('<div/>').html(html_str));

                    html_str = '<table><thead><tr><th data-field="id">Filename</th><th data-field="price">OCR time</th></tr></thead><tbody>';
                    for (var key in filetimes) {
                        if (filetimes.hasOwnProperty(key)) {
                            html_str += '<tr><td>' + key + '</td><td>' + round(filetimes[key] / 1000) + ' Seconds</td></tr>';
                        }
                    }
                    html_str += '</tbody></table>';
                    $('#local').append($('<div/>').html(html_str));
                }



            })
            .catch(function(err) {
                console.error(err);
            });

    });
}

function remote_ocr() {
    var form_data = new FormData($('#upload-file')[0]);
    $('#remote-progress-bar').empty()
    prog = ' <div class="progress"><div class="indeterminate"></div></div>'
    $('#remote-progress-bar').append(prog)
    $.ajax({
        type: 'POST',
        url: '/benchmark',
        data: form_data,
        contentType: false,
        cache: false,
        processData: false,
        async: true,
        success: function(result) {
            html_str = '<table><thead><tr><th data-field="id">Stat</th><th data-field="price">Time</th></tr></thead><tbody>'
            console.log(result);
            html_str += '<tr><td>Total execution time</td><td>' + round(result['total_time']) + ' Seconds</td></tr>';
            html_str += '<tr><td>Maximum execution time</td><td>' + round(result['max_time']) + ' Seconds</td></tr>';
            html_str += '<tr><td>Minimum execution time</td><td>' + round(result['min_time']) + ' Seconds</td></tr>';
            html_str += '<tr><td>Average execution time</td><td>' + round(result['average_time']) + ' Seconds</td></tr>';
            html_str += '<tr><td>Standard deviation</td><td>' + round(result['std_dev']) + ' Seconds</td></tr>';
            html_str += '</tbody></table>';
            $('#remote').prepend($('<div/>').html(html_str));

            html_str = '<table><thead><tr><th data-field="id">Filename</th><th data-field="price">OCR time</th></tr></thead><tbody>';
            for (var key in result['images']) {
                if (result['images'].hasOwnProperty(key)) {
                    html_str += '<tr><td>' + key + '</td><td>' + round(result['images'][key]) + ' Seconds</td></tr>';
                }
            }
            html_str += '</tbody></table>';
            $('#remote').append($('<div/>').html(html_str));
            $('#remote-progress-bar').empty()
        },
    });
}

function handleInputChange(event) {
    //upload images
    var fileInput = document.getElementById('fileInput');
    local_ocr(fileInput);
    remote_ocr();
}


function add(a, b) {
    return a + b;
}

Array.prototype.max = function() {
    return Math.max.apply(null, this);
};

Array.prototype.min = function() {
    return Math.min.apply(null, this);
};

function round(num) {
    return Math.round(num * 100) / 100

}