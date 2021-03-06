package core

import org.specs2.mutable._
import akka.actor._
import akka.testkit._
import scala.concurrent.duration._

/* A tiny class that can be used as a Specs2 'context'. */
abstract class AkkaTestkitSpecs2Support extends TestKit(ActorSystem()) with After with ImplicitSender {
  // make sure we shut down the actor system after all tests have run
  def after = system.shutdown()
}

class TestRunnerActorSpec extends Specification {

  "A TestRunnerActor" should {
    "execute commands in order" in new AkkaTestkitSpecs2Support {
      val yamlStr =
        """
        before_jobs:
          - true 1
          - true 2

        jobs:
          - name: job1
            source: ignore
            script:
              - true 3
          - name: job2
            source: ignore
            before_script:
              - true 4
            script:
              - true 5
            after_script:
              - true 6

        after_jobs:
          - true 7
        """.stripMargin

      val actor = system.actorOf(Props(new TestRunnerActor))
      actor ! Run(yamlStr, 1)

      expectMsg(CommandExecuted("true 1"))
      expectMsg(CommandExecuted("true 2"))
      expectMsg(CommandExecuted("true 3"))
      expectMsg(CommandExecuted("true 4"))
      expectMsg(CommandExecuted("true 5"))
      expectMsg(CommandExecuted("true 6"))
      expectMsg(CommandExecuted("true 7"))
    }

    "fail on empty yaml" in new AkkaTestkitSpecs2Support {
      val actor = system.actorOf(Props(new TestRunnerActor))
      actor ! Run("", 1)

      expectMsgClass(classOf[BadConfiguration]) // YamlObject expected, but got YamlNull
    }

    "fail on invalid source" in new AkkaTestkitSpecs2Support {
      val actor = system.actorOf(Props(new TestRunnerActor))
      actor ! Run(
        """
          jobs:
            - name: job1
              source: bad
              script:
                - "true"
        """.stripMargin, 1)

      expectMsgClass(classOf[BadConfiguration]) // job1 has unknown source bad"
    }

    "fail on invalid format" in new AkkaTestkitSpecs2Support {
      val actor = system.actorOf(Props(new TestRunnerActor))
      actor ! Run(
        """
          jobs:
            - name: job1
              source: output
              format: bad
              script:
                - "true"
        """.stripMargin, 1)

      expectMsgClass(classOf[BadConfiguration]) // job1 format bad doesn't match source output
    }

    "fail on 2 jobs with unknown metrics" in new AkkaTestkitSpecs2Support {
      val actor = system.actorOf(Props(new TestRunnerActor))
      actor ! Run(
        """
          jobs:
            - name: job1
              source: bad1
              script:
                - "true"
            - name: job2
              source: bad2
              script:
                - "true"
        """.stripMargin, 1)

      val msg = expectMsgClass(classOf[BadConfiguration]) // jobX has unknown source badX
      msg.errors must have size 2
    }

    "fail on missing required job name" in new AkkaTestkitSpecs2Support {
      val actor = system.actorOf(Props(new TestRunnerActor))
      actor ! Run(
        """
          jobs:
            - script:
              - "true"
        """.stripMargin, 1)

      expectMsgClass(classOf[BadConfiguration]) // YamlObject is missing required member 'name'
    }

    "fail on missing required job source" in new AkkaTestkitSpecs2Support {
      val actor = system.actorOf(Props(new TestRunnerActor))
      actor ! Run(
        """
          jobs:
            - name: job1
              script:
                - "true"
        """.stripMargin, 1)

      expectMsgClass(classOf[BadConfiguration]) // YamlObject is missing required member 'source'
    }

    "fail on missing jobs" in new AkkaTestkitSpecs2Support {
      val actor = system.actorOf(Props(new TestRunnerActor))
      actor ! Run(
        """
          before_jobs:
            - "true"
        """.stripMargin, 1)

      expectMsgClass(classOf[BadConfiguration]) // YamlObject is missing required member 'jobs'
    }

    "fail on garbish yaml" in new AkkaTestkitSpecs2Support {
      val actor = system.actorOf(Props(new TestRunnerActor))
      actor ! Run("garbishblala", 1)

      expectMsgClass(classOf[TestRunnerException])
    }

    "fail on burnin higher than repeat" in new AkkaTestkitSpecs2Support {
      val actor = system.actorOf(Props(new TestRunnerActor))
      actor ! Run(
        """
          jobs:
            - name: job1
              source: ignore
              repeat: 10
              burnin: 100
              script:
                - "true"
        """.stripMargin, 1)

      expectMsgClass(classOf[BadConfiguration]) // job1 has a burn-in (100) higher than repeat (10)
    }

    "tears down correctly" in new AkkaTestkitSpecs2Support {
      val actor = system.actorOf(Props(new TestRunnerActor))
      actor ! Run(
        """
          before_jobs:
            - mkdir ttt
          jobs:
            - name: job1
              source: ignore
              script:
                - "true"
          after_jobs:
            - rmdir ttt
        """.stripMargin, 1)

      expectMsg(CommandExecuted("mkdir ttt"))
      expectMsg(CommandExecuted("true"))
      expectMsg(CommandExecuted("rmdir ttt"))
      expectMsgClass(classOf[Finished])
    }

    def checkNotWindows = System.getProperty("os.name").startsWith("Windows") must be_==(false).orSkip

    "time source works correctly" in new AkkaTestkitSpecs2Support {
      checkNotWindows // source time depends on /usr/bin/time, unavailable on Windows

      val actor = system.actorOf(Props(new TestRunnerActor))
      actor ! Run(
        """
          jobs:
            - name: job1
              source: time
              script:
                - sleep 1
                - sleep 1
        """.stripMargin, 1)

      expectMsgClass(classOf[CommandExecuted])
      expectMsgClass(classOf[CommandStderr]) // real
      expectMsgClass(classOf[CommandStderr]) // user
      expectMsgClass(classOf[CommandStderr]) // sys
      expectMsgClass(classOf[MetricOutput])
    }

    "output source works correctly" in new AkkaTestkitSpecs2Support {
      val actor = system.actorOf(Props(new TestRunnerActor))
      actor ! Run(
        """
          jobs:
            - name: job1
              source: output
              format: seconds
              script:
                - echo 1
        """.stripMargin, 1)

      expectMsgClass(classOf[CommandExecuted])
      val msg = expectMsgClass(classOf[MetricOutput])
      msg.durations === List(1.seconds)
    }

    "repeat executes jobs N times" in new AkkaTestkitSpecs2Support {
      val actor = system.actorOf(Props(new TestRunnerActor))
      actor ! Run(
        """
          jobs:
            - name: job1
              source: output
              format: seconds
              repeat: 3
              script:
                - echo 1
        """.stripMargin, 1)

      expectMsgClass(classOf[CommandExecuted])
      expectMsgClass(classOf[CommandExecuted])
      expectMsgClass(classOf[CommandExecuted])
      val msg = expectMsgClass(classOf[MetricOutput])
      msg.durations === List.fill(3)(1.seconds)
    }

    "burnin discards N results" in new AkkaTestkitSpecs2Support {
      val actor = system.actorOf(Props(new TestRunnerActor))
      actor ! Run(
        """
          jobs:
            - name: job1
              source: output
              format: seconds
              repeat: 3
              burnin: 1
              script:
                - echo 1
        """.stripMargin, 1)

      expectMsgClass(classOf[CommandExecuted])
      expectMsgClass(classOf[CommandExecuted])
      expectMsgClass(classOf[CommandExecuted])
      val msg = expectMsgClass(classOf[MetricOutput])
      msg.durations === List.fill(2)(1.seconds)
    }
  }
}
