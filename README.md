# [Ridgeback](http://ShiftForward.github.io/ridgeback) [![Build Status](https://travis-ci.org/ShiftForward/ridgeback.svg?branch=master)](https://travis-ci.org/ShiftForward/ridgeback)

![logo](http://i.imgur.com/NdO9orC.png)
Ridgeback is a continuous integration service for performance tests.

***

## Quick Start

```bash
# Install SBT and Node.js

$ git clone git://github.com/ShiftForward/ridgeback
$ cd ridgeback
$ sudo npm -g install grunt-cli karma bower
$ sbt gruntBuild
$ sbt run

# The API is now served at http://localhost:8080
# and the GUI is available at http://localhost:8080/gui/index.html
```

### Docker

It is also possible to package `ridgeback` as a Docker container. To do so run:

```bash
$ sbt docker:publishLocal
$ docker run -p 8080:8080 -v $(realpath db):/opt/docker/db ridgeback:latest
```

This will create the database in a local folder `db` so that the database state can be preserved.

## Introduction

While performance may not be the most important factor and benchmarks always need to be taken with a grain of salt, performance is too big of a deal to be simply ignored in any medium to large scale project. Ridgeback aims at tracking performance in the evolution of software projects, using a continuous integration approach.

Tests can be triggered by writing a comment on a pull request with a certain keyword (usually PERFTESTS) and Ridgeback proceeds to clone the repository, fetch the pull request commits and run what is defined in the `.perftests.yml` file on the root directory of the repository. Once this step is done, the duration or durations of the jobs are stored in the database (currently a SQLite file) for posterior analysis. Quickly afterwards, the bot replies to the pull request with a quick overview of the tests ran and a comparison to previous tests (see `messageTemplate` below).

An overview of all projects, history of tests and jobs can be seen in the provided dashboard (by default, http://localhost:8080/gui/index.html) (screenshots below!).

The API and job runner are done in Scala, using Spray to serve HTTP and Akka to manage actors. The frontend is made using [Angular](https://angularjs.org/) and [Rickshaw](http://code.shutterstock.com/rickshaw/) to draw charts.

## .perftests.yml format

```yaml
commentTemplate: ... # optional, format of the comment (see below)
before_jobs:         # optional, list of commands to run before all jobs
  - first command
  - second command
jobs:                # required, list of jobs to run each test
  - name: job1       # required, name of the job
    source: output   # required, source of the job (see below)
    format: seconds  # optional, depends on source (see below)
    repeat: 10       # optional, number of times to repeat this job
    burnin: 3        # optional, number of runs to discard
    threshold: 10    # optional, threshold used when comparing job durations, 5 by default
    before_script:   # optional, list of commands to run before the script
      - first command
      - second command
    script:          # required, list of commands to benchmark
      - first command
      - second command
    after_script:    # optional, list of commands to run after the script
      - first command
      - second command
  - name: job2
    source: time
    script:
      - some command that takes a while
after_jobs:          # optional, list of commands to run after all jobs
  - first command
  - second command
```

#### `messageTemplate` field

Message template defines the format of the comment made by the bot account after each test ran. By default, this template is the following: `- $actionMean Job $name ($id) took in average $mean [$min, $max] (before $prevMean5)` which translates into something like


- :broken_heart: Job googlehttp (25) took in average 109ms \[109ms, 110ms\] (before 105ms)
- :blue_heart: Job googlehttps (26) took in average 273ms \[266ms, 281ms\] (before 271ms)

Supported keywords:

keyword | description | example
--------|-------------|--------
$id | id of the job | 1
$name | name of the job | job1
$mean | mean duration of the job | 12.5
$min | min duration of the job | 10
$max | max duration of the job | 15
$prevMean | mean duration of the previous job | 11
$prevMean5 | mean duration of the previous 5 jobs | 10
$actionMean | comparison (emoji) of the mean of the job with the previous job | :blue_heart:
$actionMean5 | comparison (emoji) of the mean of the job with the previous 5 jobs | :green_heart:
$diffMean | difference between current mean and previous job mean | -1.5
$diffMean5 | difference between current mean and previous 5 job means | -2.5
$diff%Mean | percentage difference between current mean and previous job mean | -12
$diff%Mean5 | percentage difference between current mean and previous 5 job means | -20

A :blue_heart: is used when there are no significative changes in performance (between -`threshold`% and +`threshold`%), a :green_heart: is used when the times have been improved and :broken_heart: is used when the times decreased. :new: is used for the first time the job is ran and :grey_question: happens when stuff goes haywire. This can be changed by extending the trait `CommentWriter` in the file `CommentWriter.scala`.

#### `source` field

**time**: all the commands are wrapped in single call to [`/usr/bin/time`](http://man7.org/linux/man-pages/man1/time.1.html) and the elapsed real time is used. `format` is ignored.

**output**: the *stdout* of the last command in the `script` is used as the duration of the job.
To specify the granularity of the duration the `format` field should be used and it can be one the following values: `days`, `hours`, `microseconds`, `milliseconds`, `minute`, `nanoseconds` or `seconds`.
Example: The command `echo 2` is the last command in the `script` list, `source: output` and `format: minute` means that the job took 2 minutes to execute.
Tip: this source can be used with [curl](http://curl.haxx.se/) in the following way: `curl -w %{time_total} -o /dev/null -s http://example.com`.

**ignore**: the duration of the job is ignored, nothing happens. `format` is ignored.

## application.conf

The default configuration can be changed by creating a file named application.conf which overrides the contents of [reference.conf](https://github.com/ShiftForward/ridgeback/blob/master/src/main/resources/reference.conf).

Config options worth noting:
- `app.port`: port used to serve the API and the GUI
- `app.interface`: IP address bind
- `worker.keyword`: keyword used to trigger tests in comments
- `worker.defaultThreshold`: threshold used when one is not specified in the .perftests.yml file
- `worker.commentTemplate`: template used when replying to comments
- `bitbucket.user`: username of the account used to comment on pull requests
- `bitbucket.pass`: username of the account used to comment on pull requests
- `pusher.appId`: application id of pusher.com
- `pusher.apiKey`: API key of pusher.com
- `pusher.apiSecret`: API secret of pusher.com

## API Reference

- **projects**: Operations about projects
  - `GET /projects` Returns all projects
  - `GET /projects/{projId}` Returns a project based on ID
  - `POST /projects` Add Project
  - `POST /projects/{projId}/trigger` Trigger Project Build
  - `POST /projects/{projId}/trigger/bb` Trigger Project Build from Bitbucket-
- **tests**: Operations about tests
  - `GET /tests?projId={id}` Returns all tests, optionally by project
  - `GET /tests/{testId}` Returns a test based on ID
  - `GET /tests/{testId}/events` Returns events of a test
- **jobs**: Operations about jobs
  - `GET /jobs/{jobId}` Returns a job based on ID
  - `GET /jobs?projId={id}&testId={id}` Returns all jobs, optionally by test OR by project

More documentation of the API is available with [Swagger](http://swagger.io/) (http://localhost:8080, by default).

## Acknowledgments

Ridgeback has been inspired on [Travis CI](https://travis-ci.org).
The interface was adapted from the Bootstrap template Dream by [WebThemez](http://webthemez.com/).
[ngbp](https://github.com/ngbp/ngbp) by @joshdmiller was used to kickstart the Angular.js frontend.
Thanks to the authors of all the tools and libraries used in this project.
Also thanks to [ShiftForward](http://www.shiftforward.eu/) (in particularly @jcazevedo and @ruippeixotog) for being home to this project during a summer internship.

With :heart: from Portugal.
