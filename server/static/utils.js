$(document).ready(function() {

    $(function() {
        var pgurl = window.location.href.substr(window.location.href.lastIndexOf("/") + 1);
        console.log(pgurl)
        $(".nav-wrapper li a").each(function() {
            if ($(this).attr("href").substring(0, pgurl.length + 1) == "/" + pgurl)
                $(this).parent().addClass("active");
        })
    });
})
$(document).ready(function() {
    // the "href" attribute of .modal-trigger must specify the modal ID that wants to be triggered
    $('.modal').modal();
    $('.materialboxed').materialbox();
    $(".button-collapse").sideNav();
    $('a.gallery').colorbox({
        photo: true
    });
});