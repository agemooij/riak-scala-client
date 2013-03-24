$(function() {

  // inject github counts
  $.ajax({
    url: 'https://api.github.com/repos/agemooij/riak-scala-client',
    dataType: 'jsonp',
    success: function(data) {
      $('#watchers').html(data.data.watchers);
      $('#forks').html(data.data.forks);
    }
  });

});
