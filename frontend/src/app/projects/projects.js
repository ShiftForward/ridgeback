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
                                                          $pusher, DTOptionsBuilder) {

  var regex = /.*(bitbucket\.org|github\.com):([a-z0-9\-_\.]+)\/([a-z0-9\-_\.]+).git.*/i;

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

  $scope.buildStillRunning = false;

  $scope.dtOptions = DTOptionsBuilder.newOptions()
    .withPaginationType('full_numbers')
    .withOption('order', [0, 'desc']);

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

      return Restangular.all('jobs').getList({'projId': $stateParams.id});
    })
    .then(function (jobs) {

      jobs.forEach(function (job) {
        var sum = job.durations.reduce(function (d1, d2) { return d1 + d2; });
        job.meanDuration = job.durations.length > 0 ? sum / job.durations.length : Number.NaN;
      });

      var groupedJobs = _.chain(jobs).groupBy('jobName').value();

      var mergeDurations = function (j) {
        meanDurations.push(j.meanDuration);
      };

      var mergeTestIds = function (j) {
        testIds.push(j.testId);
      };

      $scope.jobs = [];

      for (var k in groupedJobs) {
        if (groupedJobs.hasOwnProperty(k)) {
          var groupedJob = groupedJobs[k];

          if (groupedJob.length > 0) {

            var meanDurations = [];
            groupedJob.forEach(mergeDurations);

            var testIds = [];
            groupedJob.forEach(mergeTestIds);

            groupedJob[0].meanDurations = meanDurations;
            groupedJob[0].testIds = testIds;
            $scope.jobs.push(groupedJob[0]);
          }
        }
      }

      $scope.jobs.forEach(function (job) {
        job.options = {
          renderer: 'line'
        };

        var data = [];

        function testDate(testId) {
          var test = $scope.tests.find(function (test) {
            return test.id === testId;
          });

          if (test) {
            return moment(test.startDate).unix();
          } else {
            return undefined;
          }
        }

        job.meanDurations.forEach(function (d, i) {
          data.push({x: testDate(job.testIds[i]), y: d, testId: job.testIds[i]});
        });

        job.series = [{
          name: job.jobName,
          color: 'steelblue',
          data: data
        }];

        job.features = {
          yAxis: {
            tickFormat: 'formatKMBT'
          },
          xAxis: {
            timeUnit: 'day'
          },
          hover: {
            formatter: function(series, x, y, z, d, e) {
              var testId = e.value.testId;
              var date = '<span class="date">' + new Date(x * 1000).toUTCString() + '</span>';
              var swatch = '<span class="detail_swatch" style="background-color: ' + series.color + '"></span>';
              return swatch + 'Test Id ' + testId + ': ' + y + 'ms <br>' + date + '<br>';
            }
          }
        };
      });

      if ($scope.tests.length !== 0) {
        $scope.lastTest = $scope.tests[$scope.tests.length - 1];
        return Restangular.one('tests', $scope.lastTest.id).all('events').getList();
      } else {
        return null;
      }
    })
    .then(function (pastEvents) {
      $scope.buildStillRunning = true;

      if (pastEvents && pastEvents.length > 0) {
        pastEvents.forEach(function (pastEvent) {
          $scope.currentBuild += pastEvent[0] + ": " + pastEvent[1] + "\n";
          if (pastEvent[0] == "Finished") {
            $scope.buildStillRunning = false;
          }
        });
      } else {
        $scope.buildStillRunning = false;
      }

      if ($scope.tests.length !== 0) {
        var API_KEY = 'cd120e0fb030ba0e217b';
        var client = new Pusher(API_KEY);
        var pusher = $pusher(client);

        var channel = pusher.subscribe($scope.project.name + "-" + $scope.lastTest.id);

        $scope.currentBuild += "Bound to channel " + channel.name + ".\n";

        channel.bind_all(function (event, data) {
          $scope.currentBuild += event + ": " + JSON.stringify(data) + "\n";
          if (event == "Finished") {
            $scope.buildStillRunning = false;
          }
        });
      }

      $scope.buildTabDisabled = false;
    }, function (err) {
      SweetAlert.error('Error', JSON.stringify(err));
      $scope.testsTabDisabled = false;
      $scope.buildTabDisabled = false;
    });

  $scope.tabSelected = function () {
    $scope.$broadcast('rickshaw::resize'); // hack to draw the graph at the correct size
  };

  $scope.durationDiff = function (start, end) {
    var duration = moment.duration(moment(end).diff(moment(start)));
    return duration.asMilliseconds();
  };
});
