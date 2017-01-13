($(document)).ready(function() {
    $(function() {
        $('#upload-file-btn').click(function(event) {
            event.preventDefault();

            $('#progress-bar').empty()
            prog = ' <div class="progress"><div class="indeterminate"></div></div>'
            $('#progress-bar').append(prog)
            var form_data = new FormData($('#upload-file')[0]);
            $.ajax({
                type: 'POST',
                url: '/remote_ocr',
                data: form_data,
                contentType: false,
                cache: false,
                processData: false,
                async: true,
                success: function(result) {
                    console.log(result);
                    jQuery('#file-content').html('');

                    html_str = '<div class="col s12 m7">';
                    $.each(result.result.texts, function(key, value) {
                        html_str = '<div class="card blue-grey darken-1 horizontal medium"><div class="card-image">'
                            //html_str = html_str + '<img src=image/'+value.thumb_id+' alt="nan" style="width:auto;height:228px;>';
                        html_str = html_str + '<a href=/image/' + value.image_id + ' class="gallery" type="image/png"><img border="0" height="128" width="128" alt="img link" src=../image/' + value.thumb_id + '></a>'
                        html_str = html_str + 'span class="card-title">' + value.original_filename + '</span></div>'
                        html_str = html_str + '<div class="card-content"><p class="flow-text">' + value.text.substring(0, 120) + '...' + '</p></div>'
                        html_str = html_str + '<div class="card-action">'
                        if (value.text_found === true) {
                            html_str = html_str + '<a href="data:application/octet-stream;charset=utf-8;base64,' + $.base64.encode(escape(value.text)) + '" download="' + value.original_filename + '.txt">Download as a text file</a>'
                        }
                        html_str = html_str + '</div>'

                        html_str = html_str + '<button data-target="modal' + key + '" class="btn">Show text</button>'
                        html_str = html_str + '<div id="modal' + key + '" class="modal">';
                        html_str = html_str + '<div class="modal-content">';
                        html_str = html_str + '<h4>' + value.original_filename + '</h4>';
                        //modal = modal +'<img class="materialboxed" src="'+reader.result+'" alt="nan" style="width:650;height:auto; />'
                        html_str = html_str + '<p>' + value.text + '</p>';
                        html_str = html_str + '</div>';
                        html_str = html_str + '<div class="modal-footer">';
                        html_str = html_str + '<a href="#!" class=" modal-action modal-close waves-effect waves-green btn-flat">Close</a>';
                        html_str = html_str + '</div></div></div></div>'

                        //$( '#'+key ).append( $( '<div/>' ).html( modal ) );
                        $('#file-content').prepend($('<div/>').html(html_str))

                    });


                    $('.modal').modal();
                    //$('.materialboxed').materialbox()
                    $('#progress-bar').empty()
                    $('a.gallery').colorbox({
                        photo: true
                    });
                },
            });
        });
    });
})