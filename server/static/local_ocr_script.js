document.addEventListener('DOMContentLoaded', function() {
    //var fileInput = document.getElementById('fileInput');

    var fileInput = document.getElementById('upload-file-btn');
    fileInput.addEventListener('click', handleInputChange);
});

function handleInputChange(event) {
    //upload images
    var fileInput = document.getElementById('fileInput');
    var file = fileInput.files;
    $('#file-content').empty()
    $('#progress-bar').empty()

    sz = file.length
    $.each(file, function(key, value) {
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

                html_str = '<div class="col s12 m7">';
                html_str = html_str + '<div class="card blue-grey darken-1 horizontal medium"><div class="card-image">'
                html_str = html_str + '<img class="img-responsive" src="" alt="nan" style="width:auto;height:228px; />'
                html_str = html_str + '<span class="card-title">' + value.name + '</span></div>'
                html_str = html_str + '<div class="card-content"><p class="flow-text">' + result.text.substring(0, 120) + '...' + '</p></div>'
                html_str = html_str + '<div class="card-action">'
                if (result.text !== "") {
                    html_str = html_str + '<a href="data:application/octet-stream;charset=utf-8;base64,' + $.base64.encode(escape(result.text)) + '" download="' + value.name + '.txt">Download as a text file</a>'
                }
                html_str = html_str + '</div><div id="' + key + '"></div><div/>'

                $('#file-content').prepend($('<div/>').html(html_str))


                var reader = new FileReader();
                var preview = document.querySelector('img');
                reader.addEventListener("load", function() {  
                    preview.src = reader.result;
                    modal = '<button data-target="modal' + key + '" class="btn">Show text</button>'
                    modal = modal + '<div id="modal' + key + '" class="modal">';
                    modal = modal + '<div class="modal-content">';
                    modal = modal + '<h4>' + value.name + '</h4>';
                    //modal = modal +'<img class="materialboxed" src="'+reader.result+'" alt="nan" style="width:650;height:auto; />'
                    modal = modal + '<p>' + result.text + '</p>';
                    modal = modal + '</div>';
                    modal = modal + '<div class="modal-footer">';
                    modal = modal + '<a href="#!" class=" modal-action modal-close waves-effect waves-green btn-flat">Close</a>';
                    modal = modal + '</div></div>'

                    $('#' + key).append($('<div/>').html(modal))
                    $('.modal').modal();
                    $('.materialboxed').materialbox();
                    sz = sz - 1;
                    if (sz = 0)
                        $('#progress-bar').empty() 
                }, false);
                reader.readAsDataURL(value);


            })
            .catch(function(err) {
                console.error(err);
            });

    });
}


/*
function upload_images() {
  var form_data = new FormData($('#upload-file')[0]);
  console.log(form_data)
  $.ajax({
    type: 'POST',
    url: '/add_history',
    data: form_data,
    contentType: false,
    cache: false,
    processData: false,
    async: false,
    success: function(result) {
        console.log("KLAKWERWE");
        for (var key in result) { 
            update_image_owner(key);
        }
    }
});
*/
/*
function update_image_owner(image_id) {
  
  user_id = document.getElementById('user_id').getAttribute("value");
  data = {'image_id': image_id, "owner": user_id }
  $.ajax({
    type: 'POST',
    url: '/update_image',
    data: JSON.stringify(data, null, '\t'),
    contentType: 'application/json;charset=UTF-8',
    cache: false,
    processData: false,
    async: false,
    success: function(result) {
        console.log(result);
    }
  });
}};

*/