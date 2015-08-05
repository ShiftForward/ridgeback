angular.module('ngBoilerplate.projects', [
  'ui.router',
  'restangular',
  'oitozero.ngSweetAlert',
  'datatables',
  'angularMoment',
  'pusher-angular',
  'luegg.directives',
  'angular-rickshaw'
]).config(function config($stateProvider) {
  $stateProvider.state('home.projects', {
    url: '/projects/:id',
    controller: 'ProjectsCtrl',
    templateUrl: 'projects/projects.tpl.html',
    data: {pageTitle: 'Projects'}
  });
}).controller('ProjectsCtrl', function ProjectsController($scope, $stateParams, Restangular, SweetAlert,
                                                          $pusher) {

  var regex = /.*(bitbucket\.org|github\.com):([A-Za-z0-9\-_\.]+)\/([A-Za-z0-9\-_\.]+).git.*/g;

  $scope.currentBuild = '';

  $scope.testsSeries = [{
    color: 'steelblue',
    data: []
  }];

  $scope.testsOptions = {
    renderer: 'line',
    min: 'auto'
    // interpolation: 'linear'
  };

  $scope.testsFeatures = {
    hover: {
      formatter: function(series, x, y, z, d, e) {
        var test = $scope.tests[e.value.id];
        var date = '<span class="date">' + new Date(x * 1000).toUTCString() + '</span>';
        var branch = test.branch + " PR #" + test.prId;
        var swatch = '<span class="detail_swatch" style="background-color: ' + series.color + '"></span>';
        return swatch + 'Test Id ' + test.id + ': ' + y + 'ms <br>' + date + '<br>' + branch;
      }
    },
    xAxis: {
      timeUnit: 'day'
    },
    yAxis: {
      tickFormat: 'formatKMBT'
    }
  };

  $scope.testsTabDisabled = true;
  $scope.buildTabDisabled = true;

  Restangular.one('projects', $stateParams.id).get()
    .then(function (project) {
      $scope.project = project;

      var match = regex.exec($scope.project.gitRepo);
      if (match) {
        var provider = match[1];
        var org = match[2];
        var name = match[3];

        $scope.providerIconClass = "fa fa-" + provider.slice(0, -4);
        $scope.project.providerName = provider.slice(0, -4);
        $scope.project.url = provider + '/' + org + '/' + name;
      }

      return Restangular.all('tests').getList({'projId': $stateParams.id});
    })
    .then(function (tests) {
      $scope.tests = tests;

      var testsData = [];
      $scope.tests.forEach(function (test, idx) {
        testsData.push({x: moment(test.startDate).unix(), y: $scope.durationDiff(test.startDate, test.endDate), id: idx});
      });

      $scope.testsSeries[0].data = testsData;
      $scope.testsTabDisabled = false;

      if ($scope.tests.length !== 0) {
        return Restangular.one('tests', $scope.tests[$scope.tests.length - 1].id).all('pastEvents').getList();
      } else {
        return null;
      }
    })
    .then(function (pastEvents) {
      if (pastEvents) {
        pastEvents.forEach(function (pastEvent) {
          $scope.currentBuild += pastEvent[0] + ": " + pastEvent[1] + "\n";
        });
      }

      if ($scope.tests.length !== 0) {
        var API_KEY = 'cd120e0fb030ba0e217b';
        var client = new Pusher(API_KEY);
        var pusher = $pusher(client);

        var channel = pusher.subscribe($scope.project.name + "-" + $scope.tests[$scope.tests.length - 1].id);

        $scope.currentBuild += "Bound to channel " + channel.name + ".\n";

        channel.bind_all(function (event, data) {
          $scope.currentBuild += event + ": " + JSON.stringify(data) + "\n";
        });
      }

      $scope.buildTabDisabled = false;
    }, function (err) {
      SweetAlert.error('Error', JSON.stringify(err));
      $scope.testsTabDisabled = false;
      $scope.buildTabDisabled = false;
    });

  $scope.tabTestHistory = function () {
    $scope.$broadcast('rickshaw::resize'); // hack to draw the graph at the correct size
  };

  $scope.durationDiff = function (start, end) {
    var duration = moment.duration(moment(end).diff(moment(start)));
    return duration.asMilliseconds();
  };
});
