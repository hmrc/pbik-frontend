#!/usr/bin/env bash

sbt scalafmtAll scalastyleAll clean compile coverage Test/test coverageOff dependencyUpdates coverageReport
