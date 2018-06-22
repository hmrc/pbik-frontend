$(document).ready(function() {
  // =====================================================
  // Back link mimics browser back functionality
  // =====================================================
  $('#back-link').on('click', function(e){
    e.preventDefault();
    window.history.back();
  })
});
