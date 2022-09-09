#!/usr/bin/env bash
sbt scalafmtAll scalastyleAll clean compile coverage test coverageOff dependencyUpdates coverageReport
