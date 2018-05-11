$(document).ready(function() {
    var cookieData = GOVUK.getCookie("pbikUrBannerHide");
    if (cookieData == null) {
        $("#ur-panel").addClass("banner-panel--show");
    }

    $(".banner-panel__close").on("click", function (e) {
        e.preventDefault();
        GOVUK.setCookie("pbikUrBannerHide", "suppress_for_all_services", 99999999999);
        $("#ur-panel").removeClass("banner-panel--show");
    });
});