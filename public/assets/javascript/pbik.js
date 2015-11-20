function scrollToError(id) {
    window.location.hash = '#' + id;
}

function contactHMRC() {
    var $errorContent = $('.report-error__content');
    $errorContent.removeClass('hidden');
    $errorContent.removeClass('js-hidden');
    scrollToElement('#get-help-action', 1000);
}

function scrollToElement(selector, time) {
    var $time = typeof(time) != 'undefined' ? $time : 800;
    var $verticalOffset = -10;
    var $selector = $(selector);
    var $offsetTop = $selector.offset().top + $verticalOffset;
    $('html, body').animate({
       scrollTop: $offsetTop
    }, $time);
}

function gaEvent(description, benefits) {
    ga('send', 'event', description, benefits);
}

function gaEventLinkSteps() {
    ga('send', 'event', "Click 'back' steps link", "Click steps");
}

function gaEventLinkOverview() {
    ga('send', 'event', "Click back to overview", "Click overview");
}

window.addEventListener("load", function(){
     document.getElementById("get-help-action").onclick = function(){
            ga('send', 'event', "Click get help link", "Get help with this page");

     };
});