var globalRemoveReason = "";

function scrollToError(id) {
    window.location.hash = '#' + id;
}

function selectRadioButtonTable(id) {
    document.getElementById(id).checked = true;
}

function selectCheckBoxTable(id) {
    var isChecked = document.getElementById(id);
    try {
        if(isChecked.checked) {
            isChecked.checked = false;
        } else {
            isChecked.checked = true;
        }
    }
    catch(err) {}
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

function gaEventPrintLink(pageTitle) {
    ga('send', 'event', {
        'eventCategory': 'print - click ',
        'eventAction': pageTitle,
        'eventLabel': 'Print this page'

    });
    javascript:window.print();
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
    var el = document.getElementById("get-help-action");
    if(el != null){
        window.addEventListener("load", function(){
            document.getElementById("get-help-action").addEventListener("click", () => gaEventLinkGetHelp());
        });
   }
}
else {
    window.attachEvent("load", function(){
        document.getElementById("get-help-action").addEventListener("click", () => gaEventLinkGetHelp());
    });
}

function screenReaderHidden(defaultText, changedText) {
    var element = document.getElementById("important-excluded-employee-sr");

    if(defaultText == element.innerText){
        element.innerText = changedText;

    }else {
        element.innerText = defaultText;
    }
}