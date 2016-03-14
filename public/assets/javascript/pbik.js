var globalRemoveReason = "";

function scrollToError(id) {
    window.location.hash = '#' + id;
}

function RemoveBenefitRadioButtonValue() {
    return globalRemoveReason;
}

function capitalise(string) {
    return string.charAt(0).toUpperCase() + string.slice(1).toLowerCase();
}

function RemoveBenefitRadioButton(sel) {
    var selector = sel;
    globalRemoveReason = sel;
    var otherDescTextBx = document.getElementById("other-desc");
    if(selector=="other") {
        otherDescTextBx.style.display = "block";
    }else {
        otherDescTextBx.style.display = "none";
    }
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

function gaEventLinkAtoZGuide() {
    ga('send', 'event', "Click 'A to Z guide' link", "Click A to Z guide from choose benefits screen");
}

function gaEventLinkGetHelp() {
    ga('send', 'event', "Click get help link", "Get help with this page");
}

if (window.addEventListener) {
    window.addEventListener("load", function(){
        document.getElementById("get-help-action").onclick = function(){
            gaEventLinkGetHelp();
        };
    });
}
else {
    window.attachEvent("load", function(){
        document.getElementById("get-help-action").onclick = function(){
             gaEventLinkGetHelp();
        };
    });
}
